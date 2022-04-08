package cpam.tree;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import utils.BicMath;
import utils.BicPrinting;
import weka.core.Instance;
import weka.core.Instances;
import cpam.tree.j48.ClassifierTree;
import cpam.tree.j48.ModelSelection;

/**
 * Class for handling an enhanced tree structure with statistical significance guarantees
 * @author Rui Henriques
 */
public class SigTree {

  static final long serialVersionUID = -8722249377542734193L;
  
  /** ====================================
   *  ======== 0.GENERAL METHODS =========
   *  ==================================== */

  public static double[][] probs;
  public static int numInstances;
  protected List<ClassifierTree> trees = new ArrayList<ClassifierTree>(); //model selection     
  protected ClassifierTree prototype; //model selection     
  
  public SigTree(ModelSelection toSelectLocModel) {
    prototype = new ClassifierTree(toSelectLocModel);
  }
  public SigTree(ModelSelection toSelectLocModel, int _numInstances, double[][] _probs, boolean pruneTree,float cf,
		  boolean raiseTree, boolean cleanup, boolean collapseTree) throws Exception {
    prototype = new ClassifierTree(toSelectLocModel,pruneTree,cf,raiseTree,cleanup,collapseTree);
    numInstances = _numInstances;
    probs = _probs;
  }

  /** Returns number of leaves in tree structure. */
  public int numLeaves() {
    int num = 0;
    for(ClassifierTree tree : trees) num+=tree.numLeaves();
    return num;
  }

  /** Returns number of nodes in tree structure. */
  public int numNodes() {
    int num = 0;
    for(ClassifierTree tree : trees) num+=tree.numNodes();
    return num;
  }

  /**
   * Method for building a classifier tree.
   * @param data the data to build the tree from
   * @throws Exception if something goes wrong
   */
  public void buildClassifier(Instances data) throws Exception {
	int i=0, k=4;
	Set<Integer> allAtts = new HashSet<Integer>();
	while(i++<k){
		ClassifierTree tree = prototype.clone();
		Set<Integer> atts = tree.buildSigClassifier(data,allAtts); 
		allAtts.addAll(atts);
		trees.add(tree);
		if(tree.fullSignificance()) break;
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
    return BicMath.maxindex(distributionForInstance(instance));
  }

  /** 
   * Returns class probabilities for a weighted instance.
   * @param instance the instance to get the distribution for
   * @return the distribution
   * @throws Exception if something goes wrong
   */
  public final double[] distributionForInstance(Instance instance) throws Exception {
	double[] probs=new double[instance.numClasses()];
	for(ClassifierTree tree : trees){
		double[] iprobs=tree.distributionForInstance(instance,false);
		//System.out.println("1>"+BicPrinting.plot(iprobs));
		double isig =Math.abs(Math.log10(tree.significanceForInstance(instance)));
		//System.out.println("2>"+isig);
		if(isig<0.01) isig=0.01;
		for(int i=0, l=iprobs.length; i<l; i++) iprobs[i]*=isig;
		for(int i=0, l=probs.length; i<l; i++) probs[i]+=iprobs[i];
	}
	double sum=BicMath.sum(probs);
	for(int i=0, l=probs.length; i<l; i++) probs[i]/=sum;
	//System.out.println("c>"+BicPrinting.plot(probs));
    return probs;
  }
  

  /** ================================
   *  ========== 3.PRINTING ==========
   *  ================================ */
  
  /**
   * Prints tree structure.
   * @return the tree structure
   */
  public String toString() {
    StringBuffer text = new StringBuffer();
    int i=0,l=trees.size();
    for(ClassifierTree tree : trees) text.append("Tree "+(i++)+"/"+l+"\n"+tree.toString()+"\n");
    return text.toString();
  }

  /**
   * Returns graph describing the tree.
   * @throws Exception if something goes wrong
   * @return the tree as graph
   */
  public String graph() throws Exception {
    StringBuffer text = new StringBuffer();
    int i=0,l=trees.size();
    for(ClassifierTree tree : trees) text.append("Tree "+(i++)+"/"+l+"\n"+tree.graph()+"\n");
    return text.toString();
  }
}
