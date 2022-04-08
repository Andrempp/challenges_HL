package cpam.tree.j48;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import cpam.tree.SigTree;
import performance.significance.BSignificance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Statistics;
import weka.core.Utils;

/**
 * Class for handling a tree structure used for classification.
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 5530 $
 */
public class ClassifierTree implements Cloneable {

  static final long serialVersionUID = -8722249377542734193L;
  
  /** ================================
   *  ======== 0.VARIABLES ===========
   *  ================================ */

  protected ModelSelection m_toSelectModel; //model selection     
  protected SplitModel m_localModel; //Local model at node  
  protected ClassifierTree [] sons; //sons           
  protected boolean m_isLeaf; //if node is leaf                   
  protected boolean m_isEmpty; //if node is empty                  
  protected Instances m_train; //training instances                  
  protected Distribution m_test; //pruning instances     
  protected int m_id; //id
  protected double significance = -1;

  boolean m_pruneTheTree = false; //True if the tree is to be pruned
  boolean m_collapseTheTree = false; //True if the tree is to be collapsed
  float m_CF = 0.25f; //The confidence factor for pruning
  boolean m_subtreeRaising = true; //Is subtree raising to be performed?
  boolean m_cleanup = true; //Cleanup after the tree has been built
  
  private static long PRINTED_NODES = 0; //unique print ID
  protected static long nextID(){ return PRINTED_NODES ++; }
  protected static void resetID(){ PRINTED_NODES = 0; }

  public ClassifierTree(ModelSelection toSelectLocModel) {
    m_toSelectModel = toSelectLocModel;
  }
  
  /**
   * Constructor for pruneable tree structure
   * @param toSelectLocModel selection method for local splitting model
   * @param pruneTree true if the tree is to be pruned
   * @param cf the confidence factor for pruning
   * @param raiseTree
   * @param cleanup
   * @throws Exception if something goes wrong
   */
  /*public ClassifierTree(ModelSelection toSelectLocModel, double[] _attProbs,
		  boolean pruneTree, float cf, boolean raiseTree, boolean cleanup, boolean collapseTree) throws Exception {
	  this(toSelectLocModel,pruneTree,cf,raiseTree,cleanup,collapseTree);
	  attProbs = _attProbs;
  }*/
  public ClassifierTree(ModelSelection toSelectLocModel, boolean pruneTree, float cf,
		  boolean raiseTree, boolean cleanup, boolean collapseTree) throws Exception {
    this(toSelectLocModel);
    m_pruneTheTree = pruneTree;
    m_CF = cf;
    m_subtreeRaising = raiseTree;
    m_cleanup = cleanup;
    m_collapseTheTree = collapseTree;
  }
  
  public ClassifierTree clone(){
	  try {
		return new ClassifierTree(m_toSelectModel,m_pruneTheTree,m_CF,m_subtreeRaising,m_cleanup,m_collapseTheTree);
	} catch (Exception e) {
		e.printStackTrace();
		return null;
	}
  }

  /**
   * Returns number of leaves in tree structure.
   * @return the number of leaves
   */
  public int numLeaves() {
    int num = 0;
    if (m_isLeaf) return 1;
    else for(int i=0;i<sons.length;i++) num = num+sons[i].numLeaves();
    return num;
  }

  /**
   * Returns number of nodes in tree structure.
   * @return the number of nodes
   */
  public int numNodes() {
    int no = 1;
    if (!m_isLeaf) for (int i=0;i<sons.length;i++) no = no+sons[i].numNodes();
    return no;
  }

  /** =============================
   *  ====== 1.BUILD TREE =========
   *  ============================= */

  /**
   * Method for building a classifier tree.
   * @param data the data to build the tree from
   * @throws Exception if something goes wrong
   */
  public void buildClassifier(Instances data) throws Exception {
    data = new Instances(data);
    data.deleteWithMissingClass();
    buildTree(data, m_subtreeRaising, new HashSet<Integer>());
    if (m_collapseTheTree) collapse();
    if (m_pruneTheTree) prune();
    //annotateTree(0,new Stack<Integer>(),new Stack<Integer>());
    if (m_cleanup) cleanup(new Instances(data, 0));
  }

  public Set<Integer> buildSigClassifier(Instances data, Set<Integer> atts) throws Exception {
    data = new Instances(data);
    data.deleteWithMissingClass();
    buildTree(data, m_subtreeRaising, atts);
    return annotateTree(0,new Stack<Integer>(),new Stack<Integer>());
  }
  
  public boolean fullSignificance() {
	return false;
  }

  /** Computes significance per branch */
  public Set<Integer> annotateTree(int depth, Stack<Integer> attIndexes, Stack<Integer> values) {
	Set<Integer> result = new HashSet<Integer>();
	attIndexes.push(m_localModel.attIndex());
	result.add(m_localModel.attIndex());
    for(int i=0;i<sons.length;i++) {
      if(sons[i].m_isLeaf){
    	  values.push(i); 
    	  sons[i].significance=computeSignificance(attIndexes,values,m_localModel.m_distribution.maxClass(i));//split.significance(i,m_train);
    	  values.pop();
      } else {
    	  values.push(i); 
    	  result.addAll(sons[i].annotateTree(depth+1,attIndexes,values));
    	  values.pop();
      }
    }
    attIndexes.pop();
    return result;
  }
  
  private double computeSignificance(Stack<Integer> attIndexes, Stack<Integer> values, int maxClass) {
	int support=0;
	double prob=1;
	//List<Integer> countPerAtt = new ArrayList<Integer>();
	for(int i=0, l=attIndexes.size(); i<l; i++) 
		prob*=SigTree.probs[attIndexes.get(i)][values.get(i)];
	for(Instance row : m_train){
		boolean count=true;
		for(int i=0, l=attIndexes.size(); i<l; i++){
			if(row.value(attIndexes.get(i))!=values.get(i)){
				count=false;
				break;
			}
		}
		if(count) support++;
	}
	int n=support, N=SigTree.numInstances;
	//System.out.println("p="+prob);
	if(n==0) return 1;
	return BSignificance.binomialTest(n,N,prob);//SigTree.numInstances;
  }
  
  /**
   * Builds the tree structure.
   * @param data the data for which the tree structure is to be generated.
   * @param keepData is training data to be kept?
   * @throws Exception if something goes wrong
   */
  public void buildTree(Instances data, boolean keepData, Set<Integer> atts) throws Exception {
    if(keepData) m_train = data;
    m_test = null;
    m_isLeaf = false;
    m_isEmpty = false;
    sons = null;
    m_localModel = m_toSelectModel.selectModel(data,atts);
    if (m_localModel.numSubsets() > 1) {
      Instances[] localInstances = m_localModel.split(data);
      data = null;
      sons = new ClassifierTree[m_localModel.numSubsets()];
      for (int i = 0; i < sons.length; i++) {
		sons[i] = new ClassifierTree(m_toSelectModel, m_pruneTheTree, m_CF, m_subtreeRaising, m_cleanup, m_collapseTheTree);
		sons[i].buildTree(localInstances[i], m_subtreeRaising, atts);
		localInstances[i] = null;
      }
    } else {
      m_isLeaf = true;
      if(Utils.eq(data.sumOfWeights(), 0)) m_isEmpty = true;
      data = null;
    }
  }
  
  /** ================================
   *  ====== 2.CLASSIFY/DIST =========
   *  ================================ */

  /** 
   * Classifies an instance.
   * @param instance the instance to classify
   * @return the classification
   * @throws Exception if something goes wrong
   */
  public double classifyInstance(Instance instance) throws Exception {
    double maxProb = -1, currentProb;
    int maxIndex = 0, j;
    for (j = 0; j < instance.numClasses(); j++) {
      currentProb = getProbs(j, instance, 1);
      if (Utils.gr(currentProb,maxProb)) {
		maxIndex = j;
		maxProb = currentProb;
      }
    }
    return (double)maxIndex;
  }

  /** 
   * Returns class probabilities for a weighted instance.
   * @param instance the instance to get the distribution for
   * @param useLaplace whether to use laplace or not
   * @return the distribution
   * @throws Exception if something goes wrong
   */
  public final double [] distributionForInstance(Instance instance, boolean useLaplace) throws Exception {
    double [] doubles = new double[instance.numClasses()];
    for (int i = 0; i < doubles.length; i++)
      doubles[i] = getProbs(i, instance, 1);
    return doubles;
  }
  
  public double significanceForInstance(Instance instance) throws Exception {
    return getSigs(instance,1);
  }

  /**
   * Help method for computing class probabilities of a given instance.
   * @param classIndex the class index
   * @param instance the instance to compute the probabilities for
   * @param weight the weight to use
   * @return the probs
   * @throws Exception if something goes wrong
   */
  private double getProbs(int classIndex, Instance instance, double weight) throws Exception {
    double prob = 0;
    if(m_isLeaf) return weight * m_localModel.classProb(classIndex, instance, -1);
    else {
      int treeIndex = m_localModel.whichSubset(instance);
      if (treeIndex == -1) {
		double[] weights = m_localModel.weights(instance);
		for (int i = 0; i < sons.length; i++)
		  if (!sons[i].m_isEmpty) prob += sons[i].getProbs(classIndex, instance,weights[i] * weight);
		return prob;
      } else {
		if (sons[treeIndex].m_isEmpty) return weight * m_localModel.classProb(classIndex, instance,treeIndex);
		else return sons[treeIndex].getProbs(classIndex, instance, weight);
      }
    }
  }  

  private double getSigs(Instance instance, double weight) throws Exception {
    double prob=0;
    if(m_isLeaf) return weight * significance;//m_localModel.classProb(classIndex, instance, -1);
    else {
      int treeIndex = m_localModel.whichSubset(instance);
      if (treeIndex == -1) {
    	System.out.println("Full paths");
		double[] weights = m_localModel.weights(instance);
		for (int i = 0; i < sons.length; i++)
		  if(!sons[i].m_isEmpty) prob+=sons[i].getSigs(instance,weights[i] * weight);
    	//if(Double.isNaN(prob)) System.out.println("EEEEHHH");
		return prob;
      } else return sons[treeIndex].getSigs(instance, weight);
    }
  }  

  /** ================================
   *  ====== 3.PRUNE/COLLAPSE ========
   *  ================================ */

  /**
   * Cleanup in order to save memory.
   * @param justHeaderInfo
   */
  public final void cleanup(Instances justHeaderInfo) {
    m_train = justHeaderInfo;
    m_test = null;
    if (!m_isLeaf) for (int i = 0; i < sons.length; i++)
	sons[i].cleanup(justHeaderInfo);
  }

  /** Collapses a tree to a node if training error doesn't increase. */
  public final void collapse(){
    if (!m_isLeaf){
      double errorsOfSubtree = getTrainingErrors();
      double errorsOfTree = m_localModel.distribution().numIncorrect();
      if (errorsOfSubtree >= errorsOfTree-1E-3){
		sons = null; // Free adjacent trees
		m_isLeaf = true;
		m_localModel = new NoSplit(m_localModel.distribution());
      } else for(int i=0;i<sons.length;i++) sons[i].collapse();
    }
  }

  /**
   * Prunes a tree using C4.5's pruning procedure.
   * @throws Exception if something goes wrong
   */
  public void prune() throws Exception {
    if(!m_isLeaf){
      double errorsLargestBranch;
      // Prune all subtrees
      for (int i=0;i<sons.length;i++) sons[i].prune();
      // Compute error for largest branch
      int indexOfLargestBranch = m_localModel.distribution().maxBag();
      if (m_subtreeRaising) errorsLargestBranch = sons[indexOfLargestBranch]. getEstimatedErrorsForBranch((Instances)m_train);
      else errorsLargestBranch = Double.MAX_VALUE;

      // Compute error if this Tree would be leaf
      double errorsLeaf = getEstimatedErrorsForDistribution(m_localModel.distribution());
      // Compute error for the whole subtree
      double errorsTree = getEstimatedErrors();

      // Decide if leaf is best choice.
      if (Utils.smOrEq(errorsLeaf,errorsTree+0.1) && Utils.smOrEq(errorsLeaf,errorsLargestBranch+0.1)){
		sons = null;// Free son Trees
		m_isLeaf = true;
		m_localModel = new NoSplit(m_localModel.distribution());
		return;
      }
      // Decide if largest branch is better choice than whole subtree.
      if (Utils.smOrEq(errorsLargestBranch,errorsTree+0.1)){
	    ClassifierTree largestBranch = sons[indexOfLargestBranch];
		sons = largestBranch.sons;
		m_localModel = largestBranch.m_localModel;
		m_isLeaf = largestBranch.m_isLeaf;
		newDistribution(m_train);
		prune();
      }
    }
  }

  /**
   * Computes new distributions of instances for nodes in tree.
   * @param data the data to compute the distributions for
   * @throws Exception if something goes wrong
   */
  private void newDistribution(Instances data) throws Exception {
    m_localModel.resetDistribution(data);
    m_train = data;
    if (!m_isLeaf){
      Instances [] localInstances = (Instances [])m_localModel.split(data);
      for(int i=0; i<sons.length; i++)
    	  sons[i].newDistribution(localInstances[i]);
    } else { // Check whether there are some instances at the leaf now!
      if (!Utils.eq(data.sumOfWeights(), 0)) m_isEmpty = false;
    }
  }

  /**
   * Computes estimated errors for tree.
   * @return the estimated errors
   */
  private double getEstimatedErrors(){
    double errors = 0;
    if (m_isLeaf) return getEstimatedErrorsForDistribution(m_localModel.distribution());
    else{
      for (int i=0;i<sons.length;i++) errors = errors+sons[i].getEstimatedErrors();
      return errors;
    }
  }
  
  /**
   * Computes estimated errors for one branch.
   * @param data the data to work with
   * @return the estimated errors
   * @throws Exception if something goes wrong
   */
  private double getEstimatedErrorsForBranch(Instances data) throws Exception {
    double errors = 0;
    if (m_isLeaf) return getEstimatedErrorsForDistribution(new Distribution(data));
    else{
      Distribution savedDist = m_localModel.m_distribution;
      m_localModel.resetDistribution(data);
      Instances[] localInstances = (Instances[])m_localModel.split(data);
      m_localModel.m_distribution = savedDist;
      for (int i=0;i<sons.length;i++) errors = errors+
	  sons[i].getEstimatedErrorsForBranch(localInstances[i]);
      return errors;
    }
  }

  /**
   * Computes estimated errors for leaf.
   * @param theDistribution the distribution to use
   * @return the estimated errors
   */
  private double getEstimatedErrorsForDistribution(Distribution theDistribution){
    if (Utils.eq(theDistribution.total(),0)) return 0;
    else return theDistribution.numIncorrect()+addErrs(theDistribution.total(),theDistribution.numIncorrect(),m_CF);
  }

  /**
   * Computes estimated extra error for given total number of instances
   * and error using normal approximation to binomial distribution (and continuity correction).
   * @param N number of instances
   * @param e observed error
   * @param CF confidence value
   */
  public static double addErrs(double N, double e, float CF){
    if (CF > 0.5) {// Ignore stupid values for CF
      System.err.println("WARNING: confidence value for pruning too high. Error estimate not modified.");
      return 0;
    }
    if (e < 1) { // Check for extreme cases at the low end because the normal approximation won't work
      double base = N * (1 - Math.pow(CF, 1 / N)); // Base case (i.e. e == 0) from documenta Geigy Scientific: Tables, 6th edition, page 185 
      if (e == 0) return base; 
      return base + e * (addErrs(N, 1, CF) - base); // Use linear interpolation between 0 and 1 like C4.5 does
    }
    // Use linear interpolation at the high end (i.e. between N - 0.5 and N) because of the continuity correction
    if (e + 0.5 >= N) return Math.max(N - e, 0); // never return anything smaller than zero
    double z = Statistics.normalInverse(1 - CF); // Get z-score corresponding to CF
    double  f = (e + 0.5) / N; // Compute upper limit of confidence interval
    double r = (f + (z * z) / (2 * N) + z * Math.sqrt((f / N) -(f * f / N) +(z * z / (4 * N * N)))) /(1 + (z * z) / N);
    return (r * N) - e;
  }

  /**
   * Computes errors of tree on training data.
   * @return the training errors
   */
  private double getTrainingErrors(){
    double errors = 0;
    if (m_isLeaf) return m_localModel.distribution().numIncorrect();
    else {
      for (int i=0;i<sons.length;i++) errors = errors+sons[i].getTrainingErrors();
      return errors;
    }
  }

  /** ================================
   *  ========== 4.PRINTING ==========
   *  ================================ */
  
  /**
   * Prints tree structure.
   * @return the tree structure
   */
  public String toString() {
    try {
      StringBuffer text = new StringBuffer();
      if(m_isLeaf) text.append(": "+m_localModel.dumpLabel(0,m_train));
      else dumpTree(0,text);
      text.append("\n\nNumber of Leaves  : \t"+numLeaves()+"\n\nSize of the tree : \t"+numNodes()+"\n");
      return text.toString();
    } catch (Exception e) {
      return "Can't print classification tree.";
    }
  }

  /**
   * Assigns a uniqe id to every node in the tree.
   * @param lastID the last ID that was assign
   * @return the new current ID
   */
  public int assignIDs(int lastID) {
    int currLastID = lastID + 1;
    m_id = currLastID;
    if (sons != null) {
      for (int i=0; i<sons.length; i++) 
    	  currLastID = sons[i].assignIDs(currLastID);
    }
    return currLastID;
  }

  /**
   * Returns graph describing the tree.
   * @throws Exception if something goes wrong
   * @return the tree as graph
   */
  public String graph() throws Exception {
    StringBuffer text = new StringBuffer();
    text.append("digraph J48Tree {\n");
    if(m_isLeaf) {
      text.append("N" + m_id+" [label=\""+m_localModel.dumpLabel(0,m_train)+"\" shape=box style=filled ");
      if (m_train != null && m_train.numInstances() > 0) text.append("data =\n" + m_train + "\n,\n");
      text.append("]\n");
    } else {
      text.append("N" + m_id+ " [label=\"" +m_localModel.leftSide(m_train) + "\" ");
      if (m_train != null && m_train.numInstances() > 0) text.append("data =\n" + m_train + "\n,\n");
      text.append("]\n");
      graphTree(text);
    }
    return text.toString() +"}\n";
  }

  /**
   * Help method for printing tree structure.
   * @param depth the current depth
   * @param text for outputting the structure
   * @throws Exception if something goes wrong
   */
  private void dumpTree(int depth, StringBuffer text) throws Exception {
    int i,j;
    for (i=0;i<sons.length;i++) {
      text.append("\n");;
      for (j=0;j<depth;j++) text.append("|   ");
      text.append(m_localModel.leftSide(m_train)+m_localModel.rightSide(i, m_train));
      if(sons[i].m_isLeaf) text.append(": "+m_localModel.dumpLabel(i,m_train)+"=>"+sons[i].significance);
      else sons[i].dumpTree(depth+1,text);
    }
  }

  /**
   * Help method for printing tree structure as a graph.
   * @param text for outputting the tree
   * @throws Exception if something goes wrong
   */
  private void graphTree(StringBuffer text) throws Exception {
    for (int i = 0; i < sons.length; i++) {
      text.append("N" + m_id+ "->N" + sons[i].m_id +" [label=\"" + m_localModel.rightSide(i,m_train).trim() +"\"]\n");
      if (sons[i].m_isLeaf) {
		text.append("N" + sons[i].m_id +" [label=\""+m_localModel.dumpLabel(i,m_train)+"\" shape=box style=filled ");
		if (m_train != null && m_train.numInstances() > 0)  text.append("data =\n" + sons[i].m_train + "\n,\n");
		text.append("]\n");
      } else {
		text.append("N" + sons[i].m_id +" [label=\""+sons[i].m_localModel.leftSide(m_train) +"\" ");
		if (m_train != null && m_train.numInstances() > 0) text.append("data =\n" + sons[i].m_train + "\n,\n");
		text.append("]\n");
		sons[i].graphTree(text);
      }
    }
  }
}
