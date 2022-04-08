package cpam.tree.j48;

import java.io.Serializable;
import java.util.Enumeration;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;

/** 
 * Abstract class for classification models that can be used recursively to split the data.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.11 $
 */
public class SplitModel implements Cloneable, Serializable {

  private static final long serialVersionUID = 4280730118393457457L;
    
  /** ==============================
   *  ======= 0.ATTRIBUTES =========
   *  ============================== */

  protected Distribution m_distribution; //Distribution of class values  
  protected int m_numSubsets; //Number of created subsets         

  /** C45 split */
  private int m_complexityIndex; //Desired number of branches.  
  private int m_attIndex; //Attribute to split on.         
  private int m_minNoObj; //Minimum number of objects in a split.         
  private boolean m_useMDLcorrection; //Use MDL correction?         
  private double m_splitPoint; //Value of split point.   
  private double m_infoGain; //InfoGain of split. 
  private double m_gainRatio; //GainRatio of split.   
  private double m_sumOfWeights; //The sum of the weights of the instances.   
  private int m_index; //Number of split points. 

  //Static reference to splitting criterion.
  private static InfoGainSplitCrit infoGainCrit = new InfoGainSplitCrit();
  private static GainRatioSplitCrit gainRatioCrit = new GainRatioSplitCrit();

  public final int attIndex(){ return m_attIndex; }
  public double splitPoint(){ return m_splitPoint; }
  public final double codingCost(){ return Utils.log2(m_index); }
  public final double gainRatio() { return m_gainRatio; }
  public final Distribution distribution() { return m_distribution; }
  public final double infoGain() { return m_infoGain; }
  public String leftSide(Instances data) { return data.attribute(m_attIndex).name(); }
  public final int numSubsets() { return m_numSubsets; }

  /** Initializes the split model. */
  public SplitModel(){}
  public SplitModel(int attIndex, int minNoObj, double sumOfWeights, boolean useMDLcorrection) {
    m_attIndex = attIndex;
    m_minNoObj = minNoObj;
    m_sumOfWeights = sumOfWeights;
    m_useMDLcorrection = useMDLcorrection;
  }

  /** Allows to clone a model (shallow copy) */
  public Object clone() {
    Object clone = null;
    try {
      clone = super.clone();
    } catch (CloneNotSupportedException e) {} 
    return clone;
  }

  /** Checks if generated model is valid */
  public final boolean checkModel() {
    if (m_numSubsets > 0) return true;
    else return false;
  }

  /** ===============================
   *  ======= 1.BUILD SPLIT =========
   *  =============================== */

  /**
   * Builds the classifier split model for the given set of instances.
   * @exception Exception if something goes wrong
   */
  public void buildClassifier(Instances trainInstances) throws Exception {
    m_numSubsets = 0;
    m_splitPoint = Double.MAX_VALUE;
    m_infoGain = 0;
    m_gainRatio = 0;
    // Different treatment for enumerated and numeric attributes.
    if (trainInstances.attribute(m_attIndex).isNominal()) {
      m_complexityIndex = trainInstances.attribute(m_attIndex).numValues();
      m_index = m_complexityIndex;
      handleEnumeratedAttribute(trainInstances);
    }else{
      m_complexityIndex = 2;
      m_index = 0;
      trainInstances.sort(trainInstances.attribute(m_attIndex));
      handleNumericAttribute(trainInstances);
    }
  }

  /**
   * Creates split on enumerated attribute.
   * @exception Exception if something goes wrong
   */
  private void handleEnumeratedAttribute(Instances trainInstances) throws Exception {
    Instance instance;
    m_distribution = new Distribution(m_complexityIndex, trainInstances.numClasses());
    // Only Instances with known values are relevant.
    Enumeration enu = trainInstances.enumerateInstances();
    while (enu.hasMoreElements()) {
      instance = (Instance) enu.nextElement();
      if (!instance.isMissing(m_attIndex)) m_distribution.add((int)instance.value(m_attIndex),instance);
    }
    // Check if minimum number of Instances in at least two subsets.
    if (m_distribution.check(m_minNoObj)) {
      m_numSubsets = m_complexityIndex;
      m_infoGain = infoGainCrit. splitCritValue(m_distribution,m_sumOfWeights);
      m_gainRatio = gainRatioCrit.splitCritValue(m_distribution,m_sumOfWeights,m_infoGain);
    }
  }
  
  /**
   * Creates split on numeric attribute.
   * @exception Exception if something goes wrong
   */
  private void handleNumericAttribute(Instances trainInstances) throws Exception {
    int firstMiss, next = 1, last = 0, splitIndex = -1;
    double currentInfoGain, defaultEnt, minSplit;
    Instance instance;
    // Current attribute is a numeric attribute.
    m_distribution = new Distribution(2,trainInstances.numClasses());
    // Only Instances with known values are relevant.
    Enumeration enu = trainInstances.enumerateInstances();
    int i = 0;
    while (enu.hasMoreElements()) {
      instance = (Instance) enu.nextElement();
      if (instance.isMissing(m_attIndex)) break;
      m_distribution.add(1,instance);
      i++;
    }
    firstMiss = i;
	
    // Compute minimum number of Instances required in each subset.
    minSplit =  0.1*(m_distribution.total())/((double)trainInstances.numClasses());
    if (Utils.smOrEq(minSplit,m_minNoObj)) minSplit = m_minNoObj;
    else if (Utils.gr(minSplit,25)) minSplit = 25;
	
    // Enough Instances with known values?
    if (Utils.sm((double)firstMiss,2*minSplit)) return;
    // Compute values of criteria for all possible split indices.
    defaultEnt = infoGainCrit.oldEnt(m_distribution);
    while (next < firstMiss) {
      if (trainInstances.instance(next-1).value(m_attIndex)+1e-5 < trainInstances.instance(next).value(m_attIndex)) { 
		// Move class values for all Instances up to next possible split point.
		m_distribution.shiftRange(1,0,trainInstances,last,next);
		// Check if enough Instances in each subset and compute values for criteria.
		if (Utils.grOrEq(m_distribution.perBag(0),minSplit) &&
		    Utils.grOrEq(m_distribution.perBag(1),minSplit)) {
		  currentInfoGain = infoGainCrit.splitCritValue(m_distribution,m_sumOfWeights,defaultEnt);
		  if (Utils.gr(currentInfoGain,m_infoGain)) {
		    m_infoGain = currentInfoGain;
		    splitIndex = next-1;
		  }
		  m_index++;
		}
		last = next;
      }
      next++;
    }
    // Was there any useful split?
    if(m_index == 0) return;
    // Compute modified information gain for best split.
    if(m_useMDLcorrection) m_infoGain = m_infoGain-(Utils.log2(m_index)/m_sumOfWeights);
    if(Utils.smOrEq(m_infoGain,0)) return;
    // Set instance variables' values to values for best split.
    m_numSubsets = 2;
    m_splitPoint = (trainInstances.instance(splitIndex+1).value(m_attIndex)+ trainInstances.instance(splitIndex).value(m_attIndex))/2;
    // In case we have a numerical precision problem we need to choose the smaller value
    if (m_splitPoint == trainInstances.instance(splitIndex + 1).value(m_attIndex))
      m_splitPoint = trainInstances.instance(splitIndex).value(m_attIndex);
    // Restore distributioN for best split.
    m_distribution = new Distribution(2,trainInstances.numClasses());
    m_distribution.addRange(0,trainInstances,0,splitIndex+1);
    m_distribution.addRange(1,trainInstances,splitIndex+1,firstMiss);
    // Compute modified gain ratio for best split.
    m_gainRatio = gainRatioCrit. splitCritValue(m_distribution,m_sumOfWeights,m_infoGain);
  }

  /** =================================
   *  ======= 2.CLASSIFY/DIST =========
   *  ================================= */

  /**
   * Gets class probability for instance.
   * @exception Exception if something goes wrong
   */
  public final double classProb(int classIndex, Instance instance, int theSubset) throws Exception {
    if (theSubset <= -1) {
      double [] weights = weights(instance);
      if (weights == null)  return m_distribution.prob(classIndex);
      else {
		double prob = 0;
		for (int i = 0; i < weights.length; i++) 
		  prob += weights[i] * m_distribution.prob(classIndex, i);
		return prob;
      }
    } else {
      if (Utils.gr(m_distribution.perBag(theSubset), 0))  return m_distribution.prob(classIndex, theSubset);
      else return m_distribution.prob(classIndex);
    }
  }
  
  /**
   * Classifies a given instance.
   * @exception Exception if something goes wrong
   */
  public final double classifyInstance(Instance instance) throws Exception {
    int theSubset = whichSubset(instance);
    if (theSubset > -1) return (double)m_distribution.maxClass(theSubset);
    else return (double)m_distribution.maxClass();
  }

  /** =============================
   *  ======= 3.AUXILIARY =========
   *  ============================= */

  /**
   * Splits the given set of instances into subsets.
   * @exception Exception if something goes wrong
   */
  public final Instances[] split(Instances data) throws Exception { 
    Instances [] instances = new Instances [m_numSubsets];
    double [] weights;
    double newWeight;
    Instance instance;
    int subset, i, j;

    for (j=0;j<m_numSubsets;j++)
      instances[j] = new Instances((Instances)data,data.numInstances());
    for (i = 0; i < data.numInstances(); i++) {
      instance = ((Instances) data).instance(i);
      weights = weights(instance);
      subset = whichSubset(instance);
      if (subset > -1) instances[subset].add(instance);
      else {
    	  for (j = 0; j < m_numSubsets; j++)
			  if (Utils.gr(weights[j],0)) {
			    newWeight = weights[j]*instance.weight();
			    instances[j].add(instance);
			    instances[j].lastInstance().setWeight(newWeight);
			  }
      }
    }
    for (j = 0; j < m_numSubsets; j++) instances[j].compactify();
    return instances;
  }

  /** Sets distribution associated with model. */
  public void resetDistribution(Instances data) throws Exception {
    Instances insts = new Instances(data, data.numInstances());
    for (int i = 0; i < data.numInstances(); i++)
      if (whichSubset(data.instance(i)) > -1) insts.add(data.instance(i));
    Distribution newD = new Distribution(insts, this);
    newD.addInstWithUnknown(data, m_attIndex);
    m_distribution = newD;
  }

  /**
   * Sets split point to greatest value in given data smaller or equal to old split point.
   * (C4.5 does this for some strange reason).
   */
  public final void setSplitPoint(Instances allInstances) {
    double newSplitPoint = -Double.MAX_VALUE;
    double tempValue;
    Instance instance;
    if ((allInstances.attribute(m_attIndex).isNumeric()) && (m_numSubsets > 1)) {
      Enumeration enu = allInstances.enumerateInstances();
      while (enu.hasMoreElements()) {
		instance = (Instance) enu.nextElement();
		if (!instance.isMissing(m_attIndex)) {
		  tempValue = instance.value(m_attIndex);
		  if (Utils.gr(tempValue,newSplitPoint) && 
		      Utils.smOrEq(tempValue,m_splitPoint))
		    newSplitPoint = tempValue;
		}
      }
      m_splitPoint = newSplitPoint;
    }
  }
  
  /**
   * Returns weights if instance is assigned to more than one subset.
   * Returns null if instance is only assigned to one subset.
   */
  public double [] weights(Instance instance) {
    double [] weights;
    if (instance.isMissing(m_attIndex)) {
      weights = new double [m_numSubsets];
      for (int i=0;i<m_numSubsets;i++) weights [i] = m_distribution.perBag(i)/m_distribution.total();
      return weights;
    } else return null;
  }
  
  /**
   * Returns index of subset instance is assigned to.
   * Returns -1 if instance is assigned to more than one subset.
   * @exception Exception if something goes wrong
   */
  public int whichSubset(Instance instance) throws Exception {
    if (instance.isMissing(m_attIndex)) return -1;
    else{
      if (instance.attribute(m_attIndex).isNominal()) return (int)instance.value(m_attIndex);
      else if (Utils.smOrEq(instance.value(m_attIndex),m_splitPoint)) return 0;
      else return 1;
    }
  }
  
  /** =========================
   *  ======= 4.PRINT =========
   *  ========================= */

  /**
   * Prints label for subset index of instances (eg class).
   * @exception Exception if something goes wrong
   */
  public final String dumpLabel(int index,Instances data) throws Exception {
    StringBuffer text = new StringBuffer(((Instances)data).classAttribute().value(m_distribution.maxClass(index)));
    text.append(" ("+Utils.roundDouble(m_distribution.perBag(index),2));
    if (Utils.gr(m_distribution.numIncorrect(index),0))
      text.append("/"+Utils.roundDouble(m_distribution.numIncorrect(index),2));
    return text.append(")").toString();
  }
  public double significance(int index, Instances data) {
    //int label=data.classAttribute().value(m_distribution.maxClass(index));
	return 0.5;
  }
  
  /**
   * Prints the split model.
   * @exception Exception if something goes wrong
   */
  public final String dumpModel(Instances data) throws Exception {
    StringBuffer text = new StringBuffer();
    for (int i=0;i<m_numSubsets;i++)
      text.append(leftSide(data)+rightSide(i,data)+": "+dumpLabel(i,data)+"\n");
    return text.toString();
  }
  
  /**
   * Prints the condition satisfied by instances in a subset.
   * @param index of subset 
   * @param data training set.
   */
  public String rightSide(int index,Instances data) {
    StringBuffer text;
    text = new StringBuffer();
    if (data.attribute(m_attIndex).isNominal()) text.append(" = "+data.attribute(m_attIndex).value(index));
    else if (index == 0) text.append(" <= "+Utils.doubleToString(m_splitPoint,6));
    else text.append(" > "+Utils.doubleToString(m_splitPoint,6));
    return text.toString();
  }
}
