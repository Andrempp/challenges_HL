package cpam.tree.others;

import java.util.Enumeration;
import java.util.Vector;

import cpam.BClassifier;
import weka.classifiers.trees.J48;
import weka.core.Capabilities;
import weka.core.Drawable;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.Utils;

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
public class BJ48 extends BClassifier {

	  /** for storing C4.5 main arguments */
	  protected J48 m_J48 = new J48();
	  
	  /** Generates the classifier
	   *  @param instances the data to train the classifier with
	   *  @throws Exception if classifier can't be built successfully
	   */
	  public void buildClassifier(Instances instances) throws Exception {
	   	m_J48.buildClassifier(instances);
	   	//System.out.println(toSummaryString());
	   	//System.out.println(graph());
	   	m_J48.setOptions(new String[]{"-U","-L"});//"-C","0.25"
	   	m_J48.buildClassifier(instances);
	   	//System.out.println(toSummaryString());
	   	//System.out.println(graph());
	  }

	  /** Classifies an instance.
	   *  @param instance the instance to classify
	   *  @return the classification for the instance
	   *  @throws Exception if instance can't be classified successfully
	   */
	  public double classifyInstance(Instance instance) throws Exception {
	    return m_J48.classifyInstance(instance);
	  }

	  /** Returns class probabilities for an instance.
	   *  @param instance the instance to calculate the class probabilities for
	   *  @return the class probabilities
	   *  @throws Exception if distribution can't be computed successfully
	   */
	  public final double[] distributionForInstance(Instance instance) throws Exception {
		return m_J48.distributionForInstance(instance);
	  }


	  /** Returns an enumeration describing the available options.
	   *  Valid options are: -U -C -M -R -N -B -S -L -A -Q
	   *  @return an enumeration of all the available options.
	   */
	  public Enumeration<Option> listOptions() {
	    Vector<Option> newVector = new Vector<Option>(12);
	    newVector.addElement(new Option("\tUse unpruned tree.","U", 0, "-U"));
	    newVector.addElement(new Option("\tDo not collapse tree.","O", 0, "-O"));
	    newVector.addElement(new Option("\tSet confidence threshold for pruning.\n\t(default 0.25)","C", 1, "-C <pruning confidence>"));
	    newVector.addElement(new Option("\tSet minimum number of instances per leaf.\n\t(default 2)","M", 1, "-M <minimum number of instances>"));
	    newVector.addElement(new Option("\tUse reduced error pruning.","R", 0, "-R"));
	    newVector.addElement(new Option("\tSet number of folds for reduced error\n\tpruning. One fold is used as pruning set.\n" + "\t(default 3)","N", 1, "-N <number of folds>"));
	    newVector.addElement(new Option("\tUse binary splits only.","B", 0, "-B"));
	    newVector.addElement(new Option("\tDon't perform subtree raising.","S", 0, "-S"));
	    newVector.addElement(new Option("\tDo not clean up after the tree has been built.","L", 0, "-L"));
	    newVector.addElement(new Option("\tLaplace smoothing for predicted probabilities.","A", 0, "-A"));
	    newVector.addElement(new Option("\tDo not use MDL correction for info gain on numeric attributes.","J", 0, "-J"));
	    newVector.addElement(new Option("\tSeed for random data shuffling (default 1).", "Q", 1, "-Q <seed>"));
	    return newVector.elements();
	  }

	  public String toString() {
	    if (m_J48 == null) return "No classifier built";
	    if (m_J48.getUnpruned()) return "Unpruned tree:\n" + m_J48.toString();
	    else return "Pruned tree\n" + m_J48.toString();
	  }
	  
	  /** Returns a superconcise version of the model
	   *  @return a summary of the model
	   */
	  public String toSummaryString() {
	    return "Nr of leaves: " + m_J48.measureNumLeaves() + "\nTree size: " + m_J48.measureTreeSize() + "\n";
	  }

	  /** Returns the type of graph this classifier represents.
	   *  @return Drawable.TREE
	   */   
	  public int graphType() {
	      return Drawable.TREE;
	  }

	  /** Returns graph describing the tree.
	   *  @return the graph describing the tree
	   *  @throws Exception if graph can't be computed
	   */
	  public String graph() throws Exception {
	    return m_J48.graph();
	  }

	  public String getModelName(){
		return "BJ48";
	  }


	  /** Parses a given list of options
	   *  Valid options are: -U -O -C -M -R -N -B -S -L -A -J -Q
	   *  @param options the list of options as an array of strings
	   *  @throws Exception if an option is not supported
	   */
	  public void setOptions(String[] options) throws Exception {
	    String minNumString = Utils.getOption('M', options);    
	    if (minNumString.length() != 0) m_J48.setMinNumObj(Integer.parseInt(minNumString));
	    else m_J48.setMinNumObj(2);
	    m_J48.setBinarySplits(Utils.getFlag('B', options));
	    m_J48.setUseLaplace(Utils.getFlag('A', options));

	    // Pruning options
	    m_J48.setUnpruned(Utils.getFlag('U', options));
	    m_J48.setSubtreeRaising(!Utils.getFlag('S', options));
	    m_J48.setSaveInstanceData(Utils.getFlag('L', options));
	    if ((m_J48.getUnpruned()) && (!m_J48.getSubtreeRaising())) throw new Exception("Subtree raising doesn't need to be unset for unpruned tree!");
	    m_J48.setReducedErrorPruning(Utils.getFlag('R', options));
	    if ((m_J48.getUnpruned()) && (m_J48.getReducedErrorPruning())) throw new Exception("Unpruned tree and reduced error pruning can't be selected simultaneously!");
	    String confidenceString = Utils.getOption('C', options);
	    if (confidenceString.length() != 0) {
	      if (m_J48.getReducedErrorPruning()) throw new Exception("Setting the confidence doesn't make sense for reduced error pruning");
	      else if (m_J48.getUnpruned()) throw new Exception("Doesn't make sense to change confidence for unpruned tree!");
	      else m_J48.setConfidenceFactor((new Float(confidenceString)).floatValue());
	    }
		if ((m_J48.getConfidenceFactor() <= 0) || (m_J48.getConfidenceFactor() >= 1)) throw new Exception("Confidence has to be greater than zero and smaller than one!");
	    else m_J48.setConfidenceFactor(0.25f);
	    String numFoldsString = Utils.getOption('N', options);
	    if (numFoldsString.length() != 0) {
	      if (!m_J48.getReducedErrorPruning()) throw new Exception("Setting the number of folds doesn't make sense if reduced error pruning is not selected.");
	      else m_J48.setNumFolds(Integer.parseInt(numFoldsString));
	    } 
	    else m_J48.setNumFolds(3);
	    String seedString = Utils.getOption('Q', options);
	    if (seedString.length()!=0) m_J48.setSeed(Integer.parseInt(seedString));
	    else m_J48.setSeed(1);
	  }

	  /** Gets the current settings of the Classifier.
	   *  @return an array of strings suitable for passing to setOptions
	   */
	  public String [] getOptions() {
	    String [] options = new String [16];
	    int current = 0;
	    if (m_J48.getSaveInstanceData()) options[current++] = "-L";
	    if (m_J48.getUnpruned()) options[current++] = "-U";
	    else {
	      if (!m_J48.getSubtreeRaising()) options[current++] = "-S";
	      if (m_J48.getReducedErrorPruning()) {
	    	  options[current++] = "-R";
	    	  options[current++] = "-N"; 
	    	  options[current++] = "" + m_J48.getNumFolds();
	    	  options[current++] = "-Q"; 
	    	  options[current++] = "" + m_J48.getSeed();
	      } else {
	    	  options[current++] = "-C"; 
	    	  options[current++] = "" + m_J48.getConfidenceFactor();
	      }
	    }
	    if (m_J48.getBinarySplits()) options[current++] = "-B";
	    options[current++] = "-M"; 
	    options[current++] = "" + m_J48.getMinNumObj();
	    if (m_J48.getUseLaplace()) options[current++] = "-A";
	    while (current < options.length) options[current++] = "";
	    return options;
	  }

	@Override
	public Capabilities getCapabilities() {
		return null;
	}
}
