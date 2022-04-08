package cpam.tree.others;

import java.util.Enumeration;

import cpam.BClassifier;
import domain.Dataset;
import weka.classifiers.trees.j48.*;
import weka.core.Capabilities;
import weka.core.Drawable;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Matchable;
import weka.core.OptionHandler;
import weka.core.Summarizable;
import weka.core.Utils;
import weka.core.WeightedInstancesHandler;


/** BJ49.java 2011 IST, Lisboa, Portugal
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
public class BJ49 extends BClassifier 
	implements OptionHandler, Drawable, Matchable, WeightedInstancesHandler, Summarizable {

	static final long serialVersionUID = -217733168393644444L;
	public boolean m_significance = false;
	
	protected C45Tree m_root; /** The decision tree */
	protected int m_minNumObj = 2; /** Minimum number of instances */
	protected int m_numFolds = 3; /** Number of folds for reduced error pruning. */
	protected int m_Seed = 1; /** Random number seed for reduced-error pruning. */
	
	protected boolean m_useMDLcorrection = true; /** Use MDL correction? */         
	protected boolean m_useLaplace = false; /** Whether probs are smoothed using Laplace correction when predictions are generated */
	protected boolean m_reducedErrorPruning = false; 
	protected boolean m_binarySplits = false; /** Binary splits on nominal attributes? */

	
	/******************************************
	 *********** INVOKING C45 TREE ************ 
	 ******************************************/
	
	/** Generates the classifier. */
	public void buildClassifier(Instances instances) throws Exception {
		ModelSelection modSelection;	 
		//if(m_binarySplits) modSelection = new BinC45ModelSelection(m_minNumObj, instances); //m_useMDLcorrection
		modSelection = new C45ModelSelection(instances,m_minNumObj);//m_useMDLcorrection
		
		if(m_significance) m_root = new C45Tree(modSelection,new BJ49());//new Dataset(instances)));
		else m_root = new C45Tree(modSelection,null);
		m_root.buildClassifier(instances);
		//if(m_binarySplits) ((BinC45ModelSelection)modSelection).cleanup(); else 
		((C45ModelSelection)modSelection).cleanup();
	}
	
	/** Assess the significance of the classifier. */
	public String assessSignificance(Dataset data) throws Exception{
		return new BJ49().toString();//data,m_root).toString();
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
		m_reducedErrorPruning = Utils.getFlag('R', options);
		String confidenceString = Utils.getOption('C', options);
		if (confidenceString.length() != 0 && m_reducedErrorPruning) throw new Exception("Setting the confidence doesn't make sense for reduced error pruning.");
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
		if (m_reducedErrorPruning) {
			options[current++] = "-R";
			options[current++] = "-N"; options[current++] = "" + m_numFolds;
			options[current++] = "-Q"; options[current++] = "" + m_Seed;
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
		//if (!m_pruneTheTree) return "J48 unpruned tree\n------------------\n" + m_root.toString();
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

	public int getMinNumObj() { return m_minNumObj; }
	public void setMinNumObj(int v) { m_minNumObj = v; }
	public boolean getReducedErrorPruning() { return m_reducedErrorPruning; }
	public void setReducedErrorPruning(boolean v) { m_reducedErrorPruning = v; }
	public int getNumFolds() { return m_numFolds; }
	public void setNumFolds(int v) { m_numFolds = v; }
	public boolean getBinarySplits() { return m_binarySplits; }
	public void setBinarySplits(boolean v) { m_binarySplits = v; }

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