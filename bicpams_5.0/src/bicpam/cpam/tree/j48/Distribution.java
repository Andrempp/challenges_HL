package cpam.tree.j48;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import java.io.Serializable;
import java.util.Enumeration;

/**
 * Class for handling a distribution of class values.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.12 $
 */
public class Distribution
  implements Cloneable, Serializable {

  private static final long serialVersionUID = 8526859638230806576L;

  private double m_perClassPerBag[][]; // Weight of instances per class per bag 
  private double m_perBag[]; //Weight of instances per bag           
  private double m_perClass[]; //Weight of instances per class         
  private double totaL; //Total weight of instances            

  /** Creates and initializes a new distribution. */
  public Distribution(int numBags,int numClasses) {
    m_perClassPerBag = new double [numBags][0];
    m_perBag = new double [numBags];
    m_perClass = new double [numClasses];
    for (int i=0;i<numBags;i++) m_perClassPerBag[i] = new double [numClasses];
    totaL = 0;
  }

  /**
   * Creates a distribution with only one bag according to instances in source.
   * @exception Exception if something goes wrong
   */
  public Distribution(Instances source) throws Exception {
    m_perClassPerBag = new double [1][0];
    m_perBag = new double [1];
    totaL = 0;
    m_perClass = new double [source.numClasses()];
    m_perClassPerBag[0] = new double [source.numClasses()];
    Enumeration enu = source.enumerateInstances();
    while (enu.hasMoreElements()) add(0,(Instance) enu.nextElement());
  }

  /**
   * Creates a distribution according to given instances and split model.
   * @exception Exception if something goes wrong
   */
  public Distribution(Instances source, SplitModel modelToUse) throws Exception {
    m_perClassPerBag = new double [modelToUse.numSubsets()][0];
    m_perBag = new double [modelToUse.numSubsets()];
    totaL = 0;
    m_perClass = new double [source.numClasses()];
    for (int i = 0; i < modelToUse.numSubsets(); i++)
      m_perClassPerBag[i] = new double [source.numClasses()];
    Enumeration enu = source.enumerateInstances();
    while (enu.hasMoreElements()) {
      Instance instance = (Instance) enu.nextElement();
      int index = modelToUse.whichSubset(instance);
      if (index != -1) add(index, instance);
      else {
    	  double[] weights = modelToUse.weights(instance);
    	  addWeights(instance, weights);
      }
    }
  }

  /** Creates distribution with only one bag by merging all bags of given distribution. */
  public Distribution(Distribution toMerge) {
    totaL = toMerge.totaL;
    m_perClass = new double [toMerge.numClasses()];
    System.arraycopy(toMerge.m_perClass,0,m_perClass,0,toMerge.numClasses());
    m_perClassPerBag = new double [1] [0];
    m_perClassPerBag[0] = new double [toMerge.numClasses()];
    System.arraycopy(toMerge.m_perClass,0,m_perClassPerBag[0],0,toMerge.numClasses());
    m_perBag = new double [1];
    m_perBag[0] = totaL;
  }
  
  /** Returns number of non-empty bags of distribution */
  public final int actualNumBags() {
    int returnValue = 0;
    for (int i=0;i<m_perBag.length;i++)
      if (Utils.gr(m_perBag[i],0)) returnValue++;
    return returnValue;
  }

  /** Returns number of classes actually occuring in distribution. */
  public final int actualNumClasses() {
    int returnValue = 0;
    for (int i=0;i<m_perClass.length;i++)
      if (Utils.gr(m_perClass[i],0)) returnValue++;
    return returnValue;
  }

  /** Returns number of classes actually occuring in given bag. */
  public final int actualNumClasses(int bagIndex) {
    int returnValue = 0;
    for (int i=0;i<m_perClass.length;i++)
      if (Utils.gr(m_perClassPerBag[bagIndex][i],0)) returnValue++;
    return returnValue;
  }

  /**
   * Adds given instance to given bag.
   * @exception Exception if something goes wrong
   */
  public final void add(int bagIndex,Instance instance) throws Exception {
    int classIndex = (int)instance.classValue();
    double weight = instance.weight();
    m_perClassPerBag[bagIndex][classIndex] = m_perClassPerBag[bagIndex][classIndex]+weight;
    m_perBag[bagIndex] = m_perBag[bagIndex]+weight;
    m_perClass[classIndex] = m_perClass[classIndex]+weight;
    totaL = totaL+weight;
  }

  /**
   * Subtracts given instance from given bag.
   * @exception Exception if something goes wrong
   */
  public final void sub(int bagIndex,Instance instance) throws Exception {
    int classIndex = (int)instance.classValue();
    double weight = instance.weight();
    m_perClassPerBag[bagIndex][classIndex] = m_perClassPerBag[bagIndex][classIndex]-weight;
    m_perBag[bagIndex] = m_perBag[bagIndex]-weight;
    m_perClass[classIndex] = m_perClass[classIndex]-weight;
    totaL = totaL-weight;
  }

  /** Adds counts to given bag. */
  public final void add(int bagIndex, double[] counts) {
    double sum = Utils.sum(counts);
    for (int i = 0; i < counts.length; i++)
      m_perClassPerBag[bagIndex][i] += counts[i];
    m_perBag[bagIndex] = m_perBag[bagIndex]+sum;
    for (int i = 0; i < counts.length; i++)
      m_perClass[i] = m_perClass[i]+counts[i];
    totaL = totaL+sum;
  }

  /**
   * Adds all instances with unknown values for given attribute, weighted according to frequency of instances in each bag.
   * @exception Exception if something goes wrong
   */
  public final void addInstWithUnknown(Instances source, int attIndex) throws Exception {
    double weight, newWeight;
    double[] probs = new double [m_perBag.length];
    for(int j=0;j<m_perBag.length;j++) {
      if (Utils.eq(totaL, 0))  probs[j] = 1.0 / probs.length;
      else  probs[j] = m_perBag[j]/totaL;
    }
    Enumeration enu = source.enumerateInstances();
    while (enu.hasMoreElements()) {
      Instance instance = (Instance) enu.nextElement();
      if (instance.isMissing(attIndex)) {
	    int classIndex = (int)instance.classValue();
		weight = instance.weight();
		m_perClass[classIndex] = m_perClass[classIndex]+weight;
		totaL = totaL+weight;
		for (int j=0; j<m_perBag.length; j++) {
		  newWeight = probs[j]*weight;
		  m_perClassPerBag[j][classIndex] = m_perClassPerBag[j][classIndex]+newWeight;
		  m_perBag[j] = m_perBag[j]+newWeight;
		}
      }
    }
  }

  /**
   * Adds all instances in given range to given bag.
   * @exception Exception if something goes wrong
   */
  public final void addRange(int bagIndex,Instances source, int startIndex, int lastPlusOne) throws Exception {
    double sumOfWeights = 0;
    for(int i=startIndex; i<lastPlusOne; i++) {
      Instance instance = (Instance) source.instance(i);
      int classIndex = (int)instance.classValue();
      sumOfWeights = sumOfWeights+instance.weight();
      m_perClassPerBag[bagIndex][classIndex] += instance.weight();
      m_perClass[classIndex] += instance.weight();
    }
    m_perBag[bagIndex] += sumOfWeights;
    totaL += sumOfWeights;
  }

  /**
   * Adds given instance to all bags weighting it according to given weights.
   * @exception Exception if something goes wrong
   */
  public final void addWeights(Instance instance, double [] weights) throws Exception {
    int classIndex = (int)instance.classValue();
    for(int i=0;i<m_perBag.length;i++) {
      double weight = instance.weight() * weights[i];
      m_perClassPerBag[i][classIndex] = m_perClassPerBag[i][classIndex] + weight;
      m_perBag[i] = m_perBag[i] + weight;
      m_perClass[classIndex] = m_perClass[classIndex] + weight;
      totaL = totaL + weight;
    }
  }

  /** Checks if at least two bags contain a minimum number of instances. */
  public final boolean check(double minNoObj) {
    int counter = 0;
    for (int i=0;i<m_perBag.length;i++)
      if (Utils.grOrEq(m_perBag[i],minNoObj))
	counter++;
    if (counter > 1) return true;
    else return false;
  }

  /** Clones distribution (Deep copy of distribution). */
  public final Object clone() {
    Distribution newDistribution = new Distribution (m_perBag.length,m_perClass.length);
    for (int i=0;i<m_perBag.length;i++) {
      newDistribution.m_perBag[i] = m_perBag[i];
      for (int j=0;j<m_perClass.length;j++)
    	  newDistribution.m_perClassPerBag[i][j] = m_perClassPerBag[i][j];
    }
    for (int j=0;j<m_perClass.length;j++)
      newDistribution.m_perClass[j] = m_perClass[j];
    newDistribution.totaL = totaL;
    return newDistribution;
  }

  /**
   * Deletes given instance from given bag.
   * @exception Exception if something goes wrong
   */
  public final void del(int bagIndex,Instance instance) throws Exception {
    int classIndex = (int)instance.classValue();
    double weight = instance.weight();
    m_perClassPerBag[bagIndex][classIndex] = m_perClassPerBag[bagIndex][classIndex]-weight;
    m_perBag[bagIndex] = m_perBag[bagIndex]-weight;
    m_perClass[classIndex] = m_perClass[classIndex]-weight;
    totaL = totaL-weight;
  }

  /**
   * Deletes all instances in given range from given bag.
   * @exception Exception if something goes wrong
   */
  public final void delRange(int bagIndex,Instances source, int startIndex, int lastPlusOne) throws Exception {
    double sumOfWeights = 0;
    for (int i=startIndex; i<lastPlusOne; i++) {
      Instance instance = (Instance) source.instance(i);
      int classIndex = (int)instance.classValue();
      sumOfWeights = sumOfWeights+instance.weight();
      m_perClassPerBag[bagIndex][classIndex] -= instance.weight();
      m_perClass[classIndex] -= instance.weight();
    }
    m_perBag[bagIndex] -= sumOfWeights;
    totaL -= sumOfWeights;
  }

  /** Prints distribution. */
  public final String dumpDistribution() {
    StringBuffer text = new StringBuffer();
    for (int i=0;i<m_perBag.length;i++) {
      text.append("Bag num "+i+"\n");
      for (int j=0;j<m_perClass.length;j++)
    	  text.append("Class num "+j+" "+m_perClassPerBag[i][j]+"\n");
    }
    return text.toString();
  }

  /** Sets all counts to zero. */
  public final void initialize() {
    for (int i = 0; i < m_perClass.length; i++) m_perClass[i] = 0;
    for (int i = 0; i < m_perBag.length; i++) m_perBag[i] = 0;
    for (int i = 0; i < m_perBag.length; i++)
      for (int j = 0; j < m_perClass.length; j++)
    	  m_perClassPerBag[i][j] = 0;
    totaL = 0;
  }

  /** Returns matrix with distribution of class values. */
  public final double[][] matrix() {
    return m_perClassPerBag;
  }
  
  /** Returns index of bag containing maximum number of instances. */
  public final int maxBag() {
    double max = 0;
    int maxIndex = -1;
    for (int i=0;i<m_perBag.length;i++)
      if (Utils.grOrEq(m_perBag[i],max)) {
		max = m_perBag[i];
		maxIndex = i;
      }
    return maxIndex;
  }

  /** Returns class with highest frequency over all bags. */
  public final int maxClass() {
    double maxCount = 0;
    int maxIndex = 0;
    for (int i=0;i<m_perClass.length;i++)
      if (Utils.gr(m_perClass[i],maxCount)) {
		maxCount = m_perClass[i];
		maxIndex = i;
      }
    return maxIndex;
  }

  /** Returns class with highest frequency for given bag. */
  public final int maxClass(int index) {
    double maxCount = 0;
    int maxIndex = 0;
    if (Utils.gr(m_perBag[index],0)) {
      for (int i=0;i<m_perClass.length;i++)
		if (Utils.gr(m_perClassPerBag[index][i],maxCount)) {
		  maxCount = m_perClassPerBag[index][i];
		  maxIndex = i;
		}
      return maxIndex;
    } else return maxClass();
  }

  /** Returns number of bags. */
  public final int numBags() {
    return m_perBag.length;
  }

  /** Returns number of classes. */
  public final int numClasses() {
    return m_perClass.length;
  }

  /** Returns perClass(maxClass()). */
  public final double numCorrect() {
    return m_perClass[maxClass()];
  }

  /** Returns perClassPerBag(index,maxClass(index)). */
  public final double numCorrect(int index) {
    return m_perClassPerBag[index][maxClass(index)];
  }

  /** Returns total-numCorrect(). */
  public final double numIncorrect() {
    return totaL-numCorrect();
  }

  /** Returns perBag(index)-numCorrect(index). */
  public final double numIncorrect(int index) {
    return m_perBag[index]-numCorrect(index);
  }

  /** Returns number of (possibly fractional) instances of given class in given bag. */
  public final double perClassPerBag(int bagIndex, int classIndex) {
    return m_perClassPerBag[bagIndex][classIndex];
  }

  /** Returns number of (possibly fractional) instances in given bag. */
  public final double perBag(int bagIndex) {
    return m_perBag[bagIndex];
  }

  /** Returns number of (possibly fractional) instances of given class. */
  public final double perClass(int classIndex) {
    return m_perClass[classIndex];
  }

  /** Returns relative frequency of class over all bags. */
  public final double prob(int classIndex) {
    if (!Utils.eq(totaL, 0)) return m_perClass[classIndex]/totaL;
    else return 0;
  }

  /** Returns relative frequency of class for given bag. */
  public final double prob(int classIndex,int intIndex) {
    if (Utils.gr(m_perBag[intIndex],0)) return m_perClassPerBag[intIndex][classIndex]/m_perBag[intIndex];
    else return prob(classIndex);
  }

  /** Subtracts the given distribution from this one. The results has only one bag. */
  public final Distribution subtract(Distribution toSubstract) {
    Distribution newDist = new Distribution(1,m_perClass.length);
    newDist.m_perBag[0] = totaL-toSubstract.totaL;
    newDist.totaL = newDist.m_perBag[0];
    for (int i = 0; i < m_perClass.length; i++) {
      newDist.m_perClassPerBag[0][i] = m_perClass[i] - toSubstract.m_perClass[i];
      newDist.m_perClass[i] = newDist.m_perClassPerBag[0][i];
    }
    return newDist;
  }

  /** Returns total number of (possibly fractional) instances. */
  public final double total() {
    return totaL;
  }

  /**
   * Shifts given instance from one bag to another one.
   * @exception Exception if something goes wrong
   */
  public final void shift(int from,int to,Instance instance) throws Exception {
    int classIndex = (int)instance.classValue();
    double weight = instance.weight();
    m_perClassPerBag[from][classIndex] -= weight;
    m_perClassPerBag[to][classIndex] += weight;
    m_perBag[from] -= weight;
    m_perBag[to] += weight;
  }

  /**
   * Shifts all instances in given range from one bag to another one.
   * @exception Exception if something goes wrong
   */
  public final void shiftRange(int from,int to,Instances source, int startIndex,int lastPlusOne) throws Exception {
    for (int i=startIndex; i < lastPlusOne; i++) {
      Instance instance = (Instance) source.instance(i);
      int classIndex = (int)instance.classValue();
      double weight = instance.weight();
      m_perClassPerBag[from][classIndex] -= weight;
      m_perClassPerBag[to][classIndex] += weight;
      m_perBag[from] -= weight;
      m_perBag[to] += weight;
    }
  }
}
