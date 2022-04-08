package cpam.tree.others;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import domain.Bicluster;
import utils.BicResult;
import weka.classifiers.trees.j48.*;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;

/**
 *  @author Rui Henriques
 *  @version 1.0
 */
public class C45Tree {

	static final long serialVersionUID = -4813820170260388194L;
	
	public boolean m_significance; /** True if the tree is to rely on significant subspaces. */
	public BJ49 m_sigcalculator;
	
	public boolean m_pruneTheTree = false; /** True if the tree is to be pruned. */
	public boolean m_collapseTree = true; /** Collapse tree? */
	public boolean m_cleanup = true; /** Cleanup after the tree has been built. */
	public boolean m_subtreeRaising = true; /** Subtree raising to be performed? */
	public float m_CF = 0.25f; /** Confidence level */

	  public ModelSelection m_toSelectModel; /** The model selection method. */     
	  public ClassifierSplitModel m_localModel; /** Local model at node. */  
	  public Distribution m_test; /** The pruning instances. */

	  public C45Tree[] m_sons; /** References to sons. */           
	  public boolean m_isLeaf; /** True if node is leaf. */                   
	  public boolean m_isEmpty; /** True if node is empty. */
	  
	  public Instances m_train; /** The training instances. */
	  public int m_id; /** The id for the node. */
	  public List<Integer> m_tids = new ArrayList<Integer>();

	  /** Constructor for pruneable tree structure 
	 * @param instances */
	  public C45Tree(ModelSelection toSelectLocModel, BJ49 sigcalculator) throws Exception {
		m_toSelectModel = toSelectLocModel;
		m_significance = (sigcalculator!=null);
		m_sigcalculator = sigcalculator;
	  }

	  /** Method for building a pruneable classifier tree. */
	  public void buildClassifier(Instances data) throws Exception {
	    data = new Instances(data);
	    data.deleteWithMissingClass();
	    buildTree(data, null, new Bicluster(new TreeSet<Integer>(),new TreeSet<Integer>()), m_subtreeRaising); //>>BUILD TREE<<
	    BicResult.println(graph());

	    if (m_collapseTree) collapse();
	    if (m_pruneTheTree) prune(); 
	    if (m_cleanup) cleanup(new Instances(data, 0));
		//System.out.println(graph());
	  }
	  	  
	  public void buildTree(Instances data, List<Integer> IDs, Bicluster bic, boolean keepData) throws Exception {
		  if(keepData) m_train = data;
		  if(IDs==null) for(int k=0,l=data.numInstances(); k<l; k++) m_tids.add(k);
		  else m_tids = IDs;
		  m_test = null; m_isLeaf = false; m_isEmpty = false; m_sons = null; 
		  
		  // A: BUILD ROOT TREE
		  m_localModel = m_toSelectModel.selectModel(data); 
		  boolean sig = m_significance && (bic.columns.size()>2);//(m_sigcalculator.getSignificance(bic)<0.01);
		  //System.out.println("SIG:"+sig);
		  
		  if (m_localModel.numSubsets()>1 && (!m_significance || (bic.columns.size()<3))) {
			  int attIndex = ((C45Split)m_localModel).attIndex();
		      m_sons = new C45Tree[m_localModel.numSubsets()];
		      
		      //System.out.println("ATT:"+attIndex);
		      Instances[] localInstances = new Instances[m_localModel.numSubsets()];
			  if(!m_significance) localInstances = m_localModel.split(data);
			  else {
				  data.deleteAttributeAt(attIndex);
				  for(int i=0; i<m_sons.length; i++) localInstances[i]=new Instances(data);
			  }
			  
		      for(int i=0; i<m_sons.length; i++){
			    List<Integer> instancesIDs = new ArrayList<Integer>();
			    for(int k=0,l=data.numInstances(); k<l; k++) 
			    	if(m_localModel.whichSubset(data.instance(k))==i) instancesIDs.add(k);
		    	
				// A: BUILD SUB TREES
		    	m_sons[i] = new C45Tree(m_toSelectModel,m_sigcalculator);
				  SortedSet<Integer> cols = bic.columns;
				  cols.add(1);
		    	m_sons[i].buildTree(localInstances[i], instancesIDs, new Bicluster(bic.rows,cols), keepData); //BUILD NEW SUBTREE
		    	cols.remove(0);
		  		localInstances[i] = null;
		      }
		      //System.out.println();
		  } else {
		      m_isLeaf = true;
		      if (Utils.eq(data.sumOfWeights(), 0)) m_isEmpty = true;
		  }
	      data = null;
	  }

      //======================================
	  //========= CLASSIFY INSTANCES =========
      //======================================

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
	      int attIndex = ((C45Split)m_localModel).attIndex();
		  //System.out.println(">["+classIndex+"] A"+attIndex+"="+instance.stringValue(attIndex));
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
	      StringBuffer text = new StringBuffer("J48Tree {\n");
	      assignIDs(-1);
	      if (m_isLeaf) {
	    	  if(!m_localModel.dumpLabel(0,m_train).contains("(0.0)"))
	    		  text.append("N" + m_id + " C" + m_localModel.dumpLabel(0,m_train)+"\n");
	          //if (m_train != null && m_train.numInstances() > 0) text.append(" |X|=" + m_train.numInstances());//+"::"+m_tids);
	      } else {
	          text.append("N" + m_id + " [" + m_localModel.leftSide(m_train));
	          if (m_train != null && m_train.numInstances() > 0) text.append(" |X|=" + m_train.numInstances());//+"::"+m_tids);
	          text.append("] ");
	          graphTree(text);
	      }
	      return text.toString() +"}\n";
	  }
	    
	  private void graphTree(StringBuffer text) throws Exception {
	      for (int i = 0; i < m_sons.length; i++) {
	          text.append("N" + m_id + "->" + "N" + m_sons[i].m_id + " [" + m_localModel.rightSide(i,m_train).trim() + "] ");
	          if (m_sons[i].m_isLeaf){  
		    	if(!m_localModel.dumpLabel(i,m_train).contains("(0.0)"))
	        	  text.append("N" + m_sons[i].m_id + " C" + m_localModel.dumpLabel(i,m_train)+"\n");
		    	//if (m_train != null && m_train.numInstances() > 0) text.append(" |X|="+ m_sons[i].m_train.numInstances());//+"::"+m_sons[i].m_tids);
	          } else {
	        	  text.append("N" + m_sons[i].m_id + " ["+m_sons[i].m_localModel.leftSide(m_train));
	        	  if (m_train != null && m_train.numInstances() > 0) 
	        		  text.append(" |X|="+ m_sons[i].m_train.numInstances());//+"::"+m_sons[i].m_tids);
	        	  text.append("] ");
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
