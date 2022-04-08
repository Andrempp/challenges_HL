package cpam.tree.j48;

import weka.core.RevisionUtils;
import weka.core.Utils;

/**
 * Class for computing the gain ratio for a given distribution.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.8 $
 */
public final class GainRatioSplitCrit extends EntropySplitCriterion {

  private static final long serialVersionUID = -433336694718670930L;

  /** Gain ratio criterion for the given distribution. */
  public final double splitCritValue(Distribution bags) {
    double numerator = oldEnt(bags)-newEnt(bags);
    if (Utils.eq(numerator,0)) return Double.MAX_VALUE; // Splits with no gain are useless.
    double denumerator = splitEnt(bags);
    if (Utils.eq(denumerator,0)) return Double.MAX_VALUE; // Test if split is trivial.
    return denumerator/numerator; //Reciprocal value because to minimize the splitting criterion's value.
  }

  /**
   * This method computes the gain ratio in the same way C4.5 does.
   * @param bags the distribution
   * @param totalnoInst the weight of ALL instances
   * @param numerator the info gain
   */
  public final double splitCritValue(Distribution bags, double totalnoInst, double numerator){
    double denumerator = splitEnt(bags,totalnoInst);
    if (Utils.eq(denumerator,0)) return 0; // Test if split is trivial.  
    denumerator = denumerator/totalnoInst;
    return numerator/denumerator;
  }
  
  /** Help method for computing the split entropy. */
  private final double splitEnt(Distribution bags,double totalnoInst){
    double returnValue = 0;
    double noUnknown = totalnoInst-bags.total();
    if (Utils.gr(bags.total(),0)){
      for (int i=0;i<bags.numBags();i++) returnValue = returnValue-logFunc(bags.perBag(i));
      returnValue = returnValue-logFunc(noUnknown);
      returnValue = returnValue+logFunc(totalnoInst);
    }
    return returnValue;
  }
}
