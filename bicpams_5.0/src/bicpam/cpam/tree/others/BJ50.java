package cpam.tree.others;

import java.util.Enumeration;

import cpam.BClassifier;
import performance.significance.BSignificance;
import weka.classifiers.trees.j48.*;
import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.Drawable;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Matchable;
import weka.core.OptionHandler;
import weka.core.Summarizable;
import weka.core.Utils;
import weka.core.WeightedInstancesHandler;


/** BJ48.java 2011 IST, Lisboa, Portugal
 *  Iterated pruned or unpruned C4.5 decision tree (adapted from Ross Quinlan 1993)
 *  OPTIONS: 
 *  -U (unpruned tree) 
 *  -O (not collapsed) 
 *  -R (reduced error pruning)
 *  -B (binary splits only) 
 *  -S (no subtree raising) 
 *  -L (no clean up after built)
 *  -C (set pruning confidence threshold, default 0.25)
 *  -M (minimum number of instances, default 2)
 *  -N (set number of folds for reduced error pruning, default 3)
 *  -A (Laplace smoothing for predicted probabilities)
 *  -J (no MDL correction for info gain on numeric attributes)
 *  -Q (seed for random data shuffling, default 1)
 *  @author Rui Henriques
 *  @version 1.0
 */
public class BJ50 extends BClassifier 
	implements OptionHandler, Drawable, Matchable, WeightedInstancesHandler, Summarizable {

	static final long serialVersionUID = -217733168393644444L;
	
	protected C45Tree m_root; /** The decision tree */
	protected boolean m_pruneTheTree = false; /** True if the tree is to be pruned. */
	protected boolean m_collapseTree = true; /** Collapse tree? */
	protected float m_CF = 0.25f; /** Confidence level */
	protected int m_minNumObj = 2; /** Minimum number of instances */
	protected boolean m_useMDLcorrection = true; /** Use MDL correction? */         
	protected boolean m_useLaplace = false; /** Whether probs are smoothed using Laplace correction when predictions are generated */
	protected boolean m_reducedErrorPruning = false; 
	protected int m_numFolds = 3; /** Number of folds for reduced error pruning. */
	protected boolean m_binarySplits = false; /** Binary splits on nominal attributes? */
	protected boolean m_subtreeRaising = true; /** Subtree raising to be performed? */
	protected boolean m_cleanup = true; /** Cleanup after the tree has been built. */
	protected int m_Seed = 1; /** Random number seed for reduced-error pruning. */

	
	/******************************************
	 ********* SELECT ATT AND SPLIT *********** 
	 ******************************************/
	public class C45ModelSelection extends ModelSelection {
	  private static final long serialVersionUID = 4062909413462612369L;
	  private Instances m_allData; /** All the training data */ 

	  /** Initializes the split selection method with the given parameters. */
	  public C45ModelSelection(Instances allData) { m_allData = allData; }
	  public void cleanup() { m_allData = null; }
	  public final ClassifierSplitModel selectModel(Instances train, Instances test) throws Exception { return selectModel(train); }

	  /** Selects C4.5-type split for the given dataset. */
	  public final ClassifierSplitModel selectModel(Instances data) throws Exception {
		  
	      //============================================
		  //==== A: CHECK ENOUGH INSTANCES TO SPLIT ====
	      //============================================
		  boolean multiVal = true;
		  Distribution checkDistribution = new Distribution(data);
		  NoSplit noSplitModel = new NoSplit(checkDistribution);
	      if (Utils.sm(checkDistribution.total(),2*m_minNumObj) || Utils.eq(checkDistribution.total(),checkDistribution.perClass(checkDistribution.maxClass())))
	    	  return noSplitModel;
	      if (m_allData != null) { 
			Enumeration enu = data.enumerateAttributes();
			while (enu.hasMoreElements()) {// Check if all attributes are nominal and have a lot of values
			  Attribute attribute = (Attribute) enu.nextElement();
			  if ((attribute.isNumeric()) || (Utils.sm((double)attribute.numValues(),(0.3*(double)m_allData.numInstances())))){
			    multiVal = false;
			    break;
			  }
			}
	      } 
	      
	      //=============================================
		  //======== B: LEARN GAIN RATIO PER ATT ========
	      //=============================================
	      double averageInfoGain=0;
	      double sumOfWeights = data.sumOfWeights();
		  int validModels = 0;
	      C45Split[] currentModel = new C45Split[data.numAttributes()];
	      for (int i = 0; i < data.numAttributes(); i++){
			if (i != (data).classIndex()){// Apart from class attribute.
			  currentModel[i] = new C45Split(i,m_minNumObj,sumOfWeights,m_useMDLcorrection); //
			  currentModel[i].buildClassifier(data);
			  
			  if (currentModel[i].checkModel()){ // Check if useful split for current attribute exists
			      if ((m_allData == null) || (data.attribute(i).isNumeric()) || (multiVal || Utils.sm((double)data.attribute(i).numValues(),(0.3*(double)m_allData.numInstances())))){
				    	  averageInfoGain = averageInfoGain+currentModel[i].infoGain();
				    	  validModels++;
			      } 
			  }
			} else currentModel[i] = null;
	      }
	      if (validModels == 0) return noSplitModel; // Check if any useful split was found.
	      averageInfoGain = averageInfoGain/(double)validModels;

	      //========================================
		  //======== C: FIND BEST ATTRIBUTE ========
	      //========================================
		  double minResult = 0;
		  C45Split bestModel = null;
	      for (int i=0;i<data.numAttributes();i++){
			if ((i != (data).classIndex()) && (currentModel[i].checkModel()))
			  // Use 1E-3 here to get a closer approximation to the original implementation.
			  if ((currentModel[i].infoGain() >= (averageInfoGain-1E-3)) && Utils.gr(currentModel[i].gainRatio(),minResult)){ 
			    bestModel = currentModel[i];
			    minResult = currentModel[i].gainRatio();
			  } 
	      }
	      if (Utils.eq(minResult,0)) return noSplitModel; // Check if useful split was found.
	      
	      //============================================
		  //======== D: SPLIT INSTANCES PER ATT ========
	      //============================================
	      bestModel.distribution().
		  addInstWithUnknown(data,bestModel.attIndex());
	      if (m_allData != null) bestModel.setSplitPoint(m_allData);//split point if attribute is numeric.
	      return bestModel;
	  }
  	  public String getRevision() {
		return null;
	  }
	}

	/******************************************
	 *********** C45 DECISION TREE ************ 
	 ******************************************/
	public class C45Tree {
	  static final long serialVersionUID = -4813820170260388194L;
	    
	  protected ModelSelection m_toSelectModel; /** The model selection method. */     
	  protected ClassifierSplitModel m_localModel; /** Local model at node. */  
	  protected C45Tree[] m_sons; /** References to sons. */           
	  protected boolean m_isLeaf; /** True if node is leaf. */                   
	  protected boolean m_isEmpty; /** True if node is empty. */                  
	  protected Instances m_train; /** The training instances. */                  
	  protected Distribution m_test; /** The pruning instances. */     
	  protected int m_id; /** The id for the node. */

	  /** Constructor for pruneable tree structure */
	  public C45Tree(ModelSelection toSelectLocModel) throws Exception {
	    m_toSelectModel = toSelectLocModel;
	  }

	  /** Method for building a pruneable classifier tree. */
	  public void buildClassifier(Instances data) throws Exception {
	    data = new Instances(data);
	    data.deleteWithMissingClass();
	    buildTree(data, m_subtreeRaising); //>>BUILD TREE<<
	    //System.out.println(graph()+"\n\n++++++++++++++");

	    if (m_collapseTree) collapse();
	    if (m_pruneTheTree) prune(); 
	    if (m_cleanup) cleanup(new Instances(data, 0));
		System.out.println(graph()+"\n\n++++++++++++++");
	  }
	  	  
	  public void buildTree(Instances data, boolean keepData) throws Exception {
		  if (keepData) m_train = data;
		  m_test = null; m_isLeaf = false; m_isEmpty = false; m_sons = null;
		  m_localModel = m_toSelectModel.selectModel(data); //BUILD TREE
		  if (m_localModel.numSubsets() > 1) {
			  Instances[] localInstances = m_localModel.split(data);
			  int attIndex = ((C45Split)m_localModel).attIndex();
		      System.out.println("ATT:"+attIndex);
		      m_sons = new C45Tree[m_localModel.numSubsets()];
		      System.out.print(">"+localInstances.length+":");
		      for (int i=0; i<m_sons.length; i++){
		    	System.out.print(localInstances[i].numInstances()+"("+i+"),");
		    	localInstances[i].deleteAttributeAt(attIndex);
		    	m_sons[i] = new C45Tree(m_toSelectModel);
		    	m_sons[i].buildTree(localInstances[i], false); //BUILD NEW SUBTREE
		  		localInstances[i] = null;
		      }
		      System.out.println();
		  } else {
		      m_isLeaf = true;
		      if (Utils.eq(data.sumOfWeights(), 0)) m_isEmpty = true;
		  }
	      data = null;
	  }

	  /** Returns class probabilities for a weighted instance. */
	  public final double [] distributionForInstance(Instance instance, boolean useLaplace) throws Exception {
	    double [] doubles = new double[instance.numClasses()];
	    for (int i = 0; i < doubles.length; i++) {
	      if (!useLaplace) doubles[i] = getProbs(i, instance, 1);
	      else doubles[i] = getProbsLaplace(i, instance, 1);
	    }
	    return doubles;
	  }
	  private double getProbsLaplace(int classIndex, Instance instance, double weight) throws Exception {
	    if (m_isLeaf) return weight * m_localModel.classProbLaplace(classIndex, instance, -1);
	    else {
	      int treeIndex = m_localModel.whichSubset(instance);
	      if (treeIndex == -1) {
	  	    double prob = 0;
			double[] weights = m_localModel.weights(instance);
			for (int i = 0; i < m_sons.length; i++)
			  if (!m_sons[i].m_isEmpty) prob += m_sons[i].getProbsLaplace(classIndex, instance, weights[i] * weight);
			return prob;
	      } else {
			if (m_sons[treeIndex].m_isEmpty) return weight * m_localModel.classProbLaplace(classIndex, instance,treeIndex);
			else return m_sons[treeIndex].getProbsLaplace(classIndex, instance, weight);
	      }
	    }
	  }

	  /** Classifies an instance. */
	  public double classifyInstance(Instance instance) throws Exception {
	    double maxProb = -1, currentProb;
	    int maxIndex = 0;
	    for (int j = 0; j < instance.numClasses(); j++) {
	      currentProb = getProbs(j, instance, 1);
	      if (Utils.gr(currentProb,maxProb)) {
	    	  maxIndex = j;
	    	  maxProb = currentProb;
	      }
	    }
	    return (double)maxIndex;
	  }
	  private double getProbs(int classIndex, Instance instance, double weight) throws Exception {
	    double prob = 0;
	    if (m_isLeaf) return weight * m_localModel.classProb(classIndex, instance, -1);
	    else {
	      int treeIndex = m_localModel.whichSubset(instance);
	      if (treeIndex == -1) {
	    	double[] weights = m_localModel.weights(instance);
	    	for (int i = 0; i < m_sons.length; i++) 
	    		  if (!m_sons[i].m_isEmpty) prob += m_sons[i].getProbs(classIndex, instance, weights[i] * weight);
	    	return prob;
	      } else {
			if (m_sons[treeIndex].m_isEmpty) return weight * m_localModel.classProb(classIndex, instance, treeIndex);
			else return m_sons[treeIndex].getProbs(classIndex, instance, weight);
	      }
	    }
	  }

      //=========================================
	  //========= COLLAPSE, PRUNE, CLEAN ========
      //=========================================
	  /** Collapses a tree to a node if training error doesn't increase. */
	  public final void collapse(){
	    double errorsOfSubtree, errorsOfTree;
	    if (!m_isLeaf){
	      errorsOfSubtree = getTrainingErrors();
	      errorsOfTree = m_localModel.distribution().numIncorrect();
	      if (errorsOfSubtree >= errorsOfTree-1E-3){
			m_sons = null; // Free adjacent trees
			m_isLeaf = true;
			m_localModel = new NoSplit(m_localModel.distribution()); // Get NoSplit Model for tree.
	      } else for (int i=0;i<m_sons.length;i++) m_sons[i].collapse();
		}
	  }

	  /** Prunes a tree using C4.5's pruning procedure. */
	  public void prune() throws Exception {
	    double errorsLargestBranch, errorsLeaf, errorsTree;
	    int indexOfLargestBranch;
	    C45Tree largestBranch;

	    if (!m_isLeaf){
	      for (int i=0;i<m_sons.length;i++) m_sons[i].prune(); // Prune all subtrees.
	      indexOfLargestBranch = m_localModel.distribution().maxBag(); // Compute error for largest branch
	      if (m_subtreeRaising) errorsLargestBranch = m_sons[indexOfLargestBranch].getEstimatedErrorsForBranch((Instances)m_train);
	      else errorsLargestBranch = Double.MAX_VALUE;
	      errorsLeaf = getEstimatedErrorsForDistribution(m_localModel.distribution());// Compute error if this Tree would be leaf
	      errorsTree = getEstimatedErrors(); // Compute error for the whole subtree

	      // Decide if leaf is best choice.
	      if (Utils.smOrEq(errorsLeaf,errorsTree+0.1) && Utils.smOrEq(errorsLeaf,errorsLargestBranch+0.1)){
			m_sons = null; // Free son Trees
			m_isLeaf = true;
			m_localModel = new NoSplit(m_localModel.distribution()); // Get NoSplit Model for node.
			return;
	      }
	      if (Utils.smOrEq(errorsLargestBranch,errorsTree+0.1)){ // Decide if largest branch is better choice than whole subtree.
			largestBranch = m_sons[indexOfLargestBranch];
			m_sons = largestBranch.m_sons;
			m_localModel = largestBranch.m_localModel;
			m_isLeaf = largestBranch.m_isLeaf;
			newDistribution(m_train);
			prune();
	      }
	    }
	  }

	  /** Cleanup in order to save memory. */
	  public final void cleanup(Instances justHeaderInfo) {
	    m_train = justHeaderInfo;
	    m_test = null;
	    if (!m_isLeaf)
	      for (int i = 0; i < m_sons.length; i++)
	    	  m_sons[i].cleanup(justHeaderInfo);
	  }

	  /** Computes estimated errors for tree. */
	  private double getEstimatedErrors(){
	    double errors = 0;
	    if (m_isLeaf) return getEstimatedErrorsForDistribution(m_localModel.distribution());
	    else {
	      for(int i=0;i<m_sons.length;i++) errors = errors+m_sons[i].getEstimatedErrors();
	      return errors;
	    }
	  }
	  
	  /** Computes estimated errors for one branch. */
	  private double getEstimatedErrorsForBranch(Instances data) throws Exception {
	    Instances [] localInstances;
	    double errors = 0;
	    if (m_isLeaf) return getEstimatedErrorsForDistribution(new Distribution(data));
	    else{
	      //Distribution savedDist = m_localModel.distribution();
	      //m_localModel.resetDistribution(data);
	      localInstances = (Instances[])m_localModel.split(data);
	      //m_localModel.m_distribution = savedDist;
	      for(int i=0;i<m_sons.length;i++) errors = errors+
		  m_sons[i].getEstimatedErrorsForBranch(localInstances[i]);
	      return errors;
	    }
	  }

	  /** Computes estimated errors for leaf. */
	  private double getEstimatedErrorsForDistribution(Distribution theDistribution){
	    if (Utils.eq(theDistribution.total(),0)) return 0;
	    else return theDistribution.numIncorrect()+
		Stats.addErrs(theDistribution.total(),theDistribution.numIncorrect(),m_CF);
	  }

	  /** Computes errors of tree on training data. */
	  private double getTrainingErrors(){
	    double errors = 0;
	    if (m_isLeaf) return m_localModel.distribution().numIncorrect();
	    else{
	      for (int i=0;i<m_sons.length;i++) errors = errors+m_sons[i].getTrainingErrors();
	      return errors;
	    }
	  }

	  /** Computes new distributions of instances for nodes in tree. */
	  private void newDistribution(Instances data) throws Exception {
	    Instances [] localInstances;
	    m_localModel.resetDistribution(data);
	    m_train = data;
	    if (!m_isLeaf){
	      localInstances = (Instances [])m_localModel.split(data);
	      for (int i = 0; i < m_sons.length; i++) m_sons[i].newDistribution(localInstances[i]);
	    } else if (!Utils.eq(data.sumOfWeights(), 0)) m_isEmpty = false; // Check whether there are some instances at the leaf now!
	  }

      //=========================================
	  //=========== AUXILIARY METHODS ===========
      //=========================================
	  public String graph() throws Exception {
	      StringBuffer text = new StringBuffer("digraph J48Tree {\n");
	      assignIDs(-1);
	      if (m_isLeaf) {
	          text.append("N" + m_id + " [label=\"" + m_localModel.dumpLabel(0,m_train) + "\" shape=box style=filled ");
	          if (m_train != null && m_train.numInstances() > 0) text.append("data =" + m_train.numInstances());
	          text.append("]\n");
	      } else {
	          text.append("N" + m_id + " [label=\"" + m_localModel.leftSide(m_train) + "\" ");
	          if (m_train != null && m_train.numInstances() > 0) text.append("data =" + m_train.numInstances());
	          text.append("]\n");
	          graphTree(text);
	      }
	      return text.toString() +"}\n";
	  }
	    
	  private void graphTree(StringBuffer text) throws Exception {
	      for (int i = 0; i < m_sons.length; i++) {
	          text.append("N" + m_id + "->" + "N" + m_sons[i].m_id + " [label=\"" + m_localModel.rightSide(i,m_train).trim() + "\"]\n");
	          if (m_sons[i].m_isLeaf){  
	        	text.append("N" + m_sons[i].m_id + " [label=\""+m_localModel.dumpLabel(i,m_train)+"\" shape=box style=filled ");
		    	if (m_train != null && m_train.numInstances() > 0)  
		    		text.append("data="); //+ m_sons[i].m_train.numInstances());
		    	text.append("]\n");
	          } else {
	        	  text.append("N" + m_sons[i].m_id + " [label=\""+m_sons[i].m_localModel.leftSide(m_train) + "\" ");
	        	  if (m_train != null && m_train.numInstances() > 0) 
	        		  text.append("data="); //+ m_sons[i].m_train.numInstances());
	        	  text.append("]\n");
	        	  m_sons[i].graphTree(text);
	          }
	      }
	  }
	  
	  public int assignIDs(int lastID) {
	    int currLastID = lastID + 1;
	    m_id = currLastID;
	    if (m_sons != null)
		      for (int i = 0; i < m_sons.length; i++) currLastID = m_sons[i].assignIDs(currLastID);
	    return currLastID;
	  }

	  public int numLeaves() {
	    int num = 0;
	    if (m_isLeaf) return 1;
	    else for (int i=0;i<m_sons.length;i++) num = num+m_sons[i].numLeaves();
	    return num;
	  }
		  
	  public int numNodes() {
	    int no = 1;
	    if (!m_isLeaf) for (int i=0;i<m_sons.length;i++) no = no+m_sons[i].numNodes();
	    return no;
	  }

	}

	/******************************************
	 *********** INVOKING C45 TREE ************ 
	 ******************************************/
	
	/** Generates the classifier. */
	public void buildClassifier(Instances instances) throws Exception {
		ModelSelection modSelection;	 
		//if (m_binarySplits) modSelection = new BinC45ModelSelection(m_minNumObj, instances); //m_useMDLcorrection
		modSelection = new C45ModelSelection(instances);//m_useMDLcorrection
		//if (!m_reducedErrorPruning) else m_root = new PruneableClassifierTree(modSelection, !m_unpruned, m_numFolds, !m_noCleanup, m_Seed); 
		m_root = new C45Tree(modSelection);
		m_root.buildClassifier(instances);
		//if(m_binarySplits) ((BinC45ModelSelection)modSelection).cleanup(); else 
		((C45ModelSelection)modSelection).cleanup();
		System.out.println(calculateSignificance(m_root));
	}

	
	private String calculateSignificance(C45Tree root) {
	    StringBuffer text = new StringBuffer("Significance {\n");
		double significance = 1;
		if(root.m_localModel instanceof C45Split){
			text.append("N" + root.m_id + ":" + significance + "\n");
			Distribution dist = ((C45Split)root.m_localModel).distribution(); 
			int[] correctPerClass = new int[dist.numClasses()], allPerClass = new int[dist.numClasses()];;
			double[] sigs = new double[dist.numClasses()];
			for(int i=0,l=dist.numClasses();i<l;i++){
				correctPerClass[i]=(int)dist.numCorrect(i);
				allPerClass[i]=(int)(dist.numCorrect(i)+dist.numIncorrect(i));
				//p=(double)correctPerClass[i]/(double)allPerClass[i]);
				sigs[i] = BSignificance.binomialTest(correctPerClass[i],allPerClass[i],0.5);
				System.out.println(">"+sigs[i]);
			}
			System.out.println(
					((C45Split)root.m_localModel).distribution().numCorrect(0)+","+((C45Split)root.m_localModel).distribution().numCorrect(1)+","
					+((C45Split)root.m_localModel).distribution().numIncorrect(0)+","+((C45Split)root.m_localModel).distribution().numIncorrect(1));
			if(!root.m_isLeaf){ 
				//for(C45Tree son : root.m_sons) 
					significance = significance;//*calculateSignificance(root.m_sons[1]);
			}
		}
		return text.toString() +"}\n";
	}

	/** Classifies an instance. */
	public double classifyInstance(Instance instance) throws Exception {
		return m_root.classifyInstance(instance);
	}
	
	/** Returns class probabilities for an instance. */
	public final double [] distributionForInstance(Instance instance) throws Exception {
		return m_root.distributionForInstance(instance, m_useLaplace);
	}

	
	/**********************************************
	 **********************************************
	 *********** AUXILIARY METHODS ****************
	 **********************************************
	 **********************************************/
	
	public int graphType() { return Drawable.TREE; }
	public String graph() throws Exception { return m_root.graph(); }
	public String prefix() throws Exception { return null; }//m_root.prefix(); }
	public int getSeed() { return m_Seed; }
	public void setSeed(int newSeed) { m_Seed = newSeed; }
	public boolean getUseLaplace() { return m_useLaplace; }
	public void setUseLaplace(boolean newuseLaplace) { m_useLaplace = newuseLaplace; }
	public boolean getUseMDLcorrection() { return m_useMDLcorrection; }
	public void setUseMDLcorrection(boolean newuseMDLcorrection) { m_useMDLcorrection = newuseMDLcorrection; }

	/**
	* Parses a given list of options.
	* @param options the list of options as an array of strings
	* @throws Exception if an option is not supported
	*/
	public void setOptions(String[] options) throws Exception {
		String minNumString = Utils.getOption('M', options);
		if (minNumString.length() != 0) m_minNumObj = Integer.parseInt(minNumString);
		else m_minNumObj = 2;
		m_binarySplits = Utils.getFlag('B', options);
		m_useLaplace = Utils.getFlag('A', options);
		m_useMDLcorrection = !Utils.getFlag('J', options);
		
		m_pruneTheTree = !Utils.getFlag('U', options);
		m_collapseTree = !Utils.getFlag('O', options);
		m_subtreeRaising = !Utils.getFlag('S', options);
		m_cleanup = !Utils.getFlag('L', options);
		if ((!m_pruneTheTree) && (!m_subtreeRaising)) throw new Exception("Subtree raising doesn't need to be unset for unpruned tree!");
		m_reducedErrorPruning = Utils.getFlag('R', options);
		if ((!m_pruneTheTree) && (m_reducedErrorPruning)) throw new Exception("Unpruned tree and reduced error pruning can't be selected " + "simultaneously!");
		String confidenceString = Utils.getOption('C', options);
		if (confidenceString.length() != 0) {
			if (m_reducedErrorPruning) throw new Exception("Setting the confidence doesn't make sense for reduced error pruning.");
			else if(!m_pruneTheTree) throw new Exception("Doesn't make sense to change confidence for unpruned tree!");
			else {
				m_CF = (new Float(confidenceString)).floatValue();
				if ((m_CF <= 0) || (m_CF >= 1)) throw new Exception("Confidence has to be greater than zero and smaller than one!");
			}
		} else m_CF = 0.25f;
		String numFoldsString = Utils.getOption('N', options);
		if (numFoldsString.length() != 0) {
			if (!m_reducedErrorPruning) throw new Exception("Setting the number of folds doesn't make sense if reduced error pruning is not selected.");
			else m_numFolds = Integer.parseInt(numFoldsString);
		} else m_numFolds = 3;
		String seedString = Utils.getOption('Q', options);
		if (seedString.length() != 0) m_Seed = Integer.parseInt(seedString);
		else m_Seed = 1;
	}
	
	/**
	* Gets the current settings of the Classifier.
	* @return an array of strings suitable for passing to setOptions
	*/
	public String [] getOptions() {
		String [] options = new String [16];
		int current = 0;
		if (m_cleanup) options[current++] = "-L";
		if (!m_collapseTree) options[current++] = "-O";
		if (!m_pruneTheTree) options[current++] = "-U";
		else {
			if (!m_subtreeRaising) options[current++] = "-S";
			if (m_reducedErrorPruning) {
				options[current++] = "-R";
				options[current++] = "-N"; options[current++] = "" + m_numFolds;
				options[current++] = "-Q"; options[current++] = "" + m_Seed;
			} else options[current++] = "-C"; options[current++] = "" + m_CF;
		}
		if (m_binarySplits) options[current++] = "-B";
		options[current++] = "-M"; options[current++] = "" + m_minNumObj;
		if (m_useLaplace) options[current++] = "-A";
		if (!m_useMDLcorrection) options[current++] = "-J";
		
		while (current < options.length) options[current++] = "";
		return options;
	}

	public String getModelName() {
		return "BSigJ48";
	}

	/**
	* Returns a description of the classifier.
	* @return a description of the classifier
	*/
	public String toString() {
		if (m_root == null) return "No classifier built";
		if (!m_pruneTheTree) return "J48 unpruned tree\n------------------\n" + m_root.toString();
		else return "J48 pruned tree\n------------------\n" + m_root.toString();
	}
	
	/**
	* Returns a superconcise version of the model
	* @return a summary of the model
	*/
	public String toSummaryString() {
		return "Number of leaves: " + m_root.numLeaves() + "\nSize of the tree: " + m_root.numNodes() + "\n";
	}
	
	public double measureTreeSize() { return m_root.numNodes(); }
	public double measureNumLeaves() { return m_root.numLeaves(); }
	public double measureNumRules() { return m_root.numLeaves(); }

	public boolean getUnpruned() { return m_pruneTheTree; }
	public void setUnpruned(boolean v) { m_pruneTheTree = v; }
	public boolean getCollapseTree() { return m_collapseTree; }
	public void setCollapseTree(boolean v) { m_collapseTree = v; }
	public float getConfidenceFactor() { return m_CF; }
	public void setConfidenceFactor(float v) { m_CF = v; }
	public int getMinNumObj() { return m_minNumObj; }
	public void setMinNumObj(int v) { m_minNumObj = v; }
	public boolean getReducedErrorPruning() { return m_reducedErrorPruning; }
	public void setReducedErrorPruning(boolean v) { m_reducedErrorPruning = v; }
	public int getNumFolds() { return m_numFolds; }
	public void setNumFolds(int v) { m_numFolds = v; }
	public boolean getBinarySplits() { return m_binarySplits; }
	public void setBinarySplits(boolean v) { m_binarySplits = v; }
	public boolean getSubtreeRaising() { return m_subtreeRaising; }
	public void setSubtreeRaising(boolean v) { m_subtreeRaising = v; }
	public boolean getSaveInstanceData() { return m_cleanup; }
	public void setSaveInstanceData(boolean v) { m_cleanup = v; }


	@Override
	public Capabilities getCapabilities() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Enumeration listOptions() {
		// TODO Auto-generated method stub
		return null;
	}
}