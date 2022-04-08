package cpam.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import utils.BicPrinting;
import utils.BicResult;
import weka.classifiers.AbstractClassifier;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.OptionHandler;
import weka.core.Utils;
import weka.core.WeightedInstancesHandler;
import cpam.tree.j48.ClassifierTree;
import cpam.tree.j48.ModelSelection;

/** 
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
 */
public class BSigJ48 extends AbstractClassifier implements OptionHandler, WeightedInstancesHandler {

  static final long serialVersionUID = -217733168393644444L;

  protected SigTree trees; //decision tree
  private float m_CF = 0.25f; //confidence level
  private int m_minNumObj = 2; //minimum #instances
  private int m_numFolds = 3; //folds for reduced error pruning
  private int m_Seed = 1; //seed for reduced-error pruning
  private boolean m_unpruned = false; //unpruned tree?
  private boolean m_collapseTree = true; //collapse tree?
  private boolean m_useMDLcorrection = true; //use MDL correction?         
  private boolean m_reducedErrorPruning = false; //error pruning?
  private boolean m_binarySplits = false; //binary splits on nominal att
  private boolean m_useLaplace = false; //smoothed probs for predictions?
  private boolean m_subtreeRaising = true; //subtree raising?
  private boolean m_noCleanup = false; //cleanup after the built tree
  
  public double measureTreeSize() { return trees.numNodes(); }
  public double measureNumLeaves() { return trees.numLeaves(); }
  public double measureNumRules() { return trees.numLeaves(); }
  
  /**
   * Generates the classifier.
   * @param instances the data to train the classifier with
   * @throws Exception if classifier can't be built successfully
   */
  public void buildClassifier(Instances instances) throws Exception {
	List<Integer> allLabels = new ArrayList<Integer>();
	for(Attribute att : instances.getAttributes())
		if(att.isNominal()||att.isOrdinal()) allLabels.add(att.numValues());
	Collections.sort(allLabels);
	int nrLabels = allLabels.get(allLabels.size()/2);
	int maxLabels = allLabels.get(allLabels.size()-1);
	double[][] probs = new double[instances.numAttributes()][maxLabels];
	for(int i=0, l=instances.numAttributes(); i<l; i++){
		if(instances.attribute(i).isNumeric()){
		} else {
			for(Instance x : instances.getInstances()) probs[i][(int)x.value(i)]++;
			for(int j=0; j<maxLabels; j++) probs[i][j]/=(double)instances.numInstances();
		}
	}
	//BicResult.println(BicPrinting.plot(probs));
	ModelSelection modSelection = new ModelSelection(m_minNumObj, instances, m_useMDLcorrection);
	trees = new SigTree(modSelection, instances.size(), probs, !m_unpruned, m_CF, m_subtreeRaising, !m_noCleanup, m_collapseTree);
	trees.buildClassifier(instances);
    modSelection.cleanup();
  }

  /**
   * Classifies an instance.
   * @param instance the instance to classify
   * @return the classification for the instance
   * @throws Exception if instance can't be classified successfully
   */
  public double classifyInstance(Instance instance) throws Exception {
    return trees.classifyInstance(instance);
  }

  /** 
   * Returns class probabilities for an instance.
   * @param instance the instance to calculate the class probabilities for
   * @return the class probabilities
   * @throws Exception if distribution can't be computed successfully
   */
  public final double [] distributionForInstance(Instance instance) throws Exception {
    return trees.distributionForInstance(instance);
  }

  /**
   * Returns graph describing the tree.
   * @return the graph describing the tree
   * @throws Exception if graph can't be computed
   */
  public String graph() throws Exception {
    return trees.graph();
  }

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

    // Pruning options
    m_unpruned = Utils.getFlag('U', options);
    m_collapseTree = !Utils.getFlag('O', options);
    m_subtreeRaising = !Utils.getFlag('S', options);
    m_noCleanup = Utils.getFlag('L', options);
    if ((m_unpruned) && (!m_subtreeRaising)) throw new Exception("Subtree raising doesn't need to be unset for unpruned tree!");
    m_reducedErrorPruning = Utils.getFlag('R', options);
    if ((m_unpruned) && (m_reducedErrorPruning)) throw new Exception("Unpruned tree and reduced error pruning can't be selected simultaneously!");
    String confidenceString = Utils.getOption('C', options);
    if (confidenceString.length() != 0) {
      if (m_reducedErrorPruning) throw new Exception("Setting the confidence doesn't make sense for reduced error pruning.");
      else if (m_unpruned) throw new Exception("Doesn't make sense to change confidence for unpruned tree!");
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
    if (m_noCleanup) options[current++] = "-L";
    if (!m_collapseTree) options[current++] = "-O";
    if (m_unpruned) options[current++] = "-U";
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

  /**
   * Returns a description of the classifier.
   * @return a description of the classifier
   */
  public String toString() {
    if (trees == null) return "No classifier built";
    if (m_unpruned) return "J48 unpruned tree\n------------------\n" + trees.toString();
    else return "J48 pruned tree\n------------------\n" + trees.toString();
  }  
}

