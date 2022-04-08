package cpam.tree.j48;

import java.io.Serializable;

/**
 * Abstract class for computing splitting criteria with respect to distributions of class values.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.8 $
 */
public abstract class EntropySplitCriterion implements Serializable {

  private static final long serialVersionUID = 5490996638027101259L;

  /**
   * Computes result of splitting criterion for given distribution.
   * @return value of splitting criterion. 0 by default
   */
  public abstract double splitCritValue(Distribution bags);

  protected static double log2 = Math.log(2);

  /** Help method for computing entropy. */
  public final double logFunc(double num) {
    if (num < 1e-6) return 0; // constant hard coded for efficiency reasons
    else return num*Math.log(num)/log2;
  }

  /** Computes entropy of distribution before splitting. */
  public final double oldEnt(Distribution bags) {
    double returnValue = 0;
    for (int j=0;j<bags.numClasses();j++)
      returnValue = returnValue+logFunc(bags.perClass(j));
    return logFunc(bags.total())-returnValue; 
  }

  /** Computes entropy of distribution after splitting. */
  public final double newEnt(Distribution bags) {
    double returnValue = 0;
    for (int i=0;i<bags.numBags();i++){
      for (int j=0;j<bags.numClasses();j++)
    	  returnValue = returnValue+logFunc(bags.perClassPerBag(i,j));
      returnValue = returnValue-logFunc(bags.perBag(i));
    }
    return -returnValue;
  }

  /** Computes entropy after splitting without considering the class values. */
  public final double splitEnt(Distribution bags) {
    double returnValue = 0;
    for (int i=0;i<bags.numBags();i++)
      returnValue = returnValue+logFunc(bags.perBag(i));
    return logFunc(bags.total())-returnValue;
  }
}
