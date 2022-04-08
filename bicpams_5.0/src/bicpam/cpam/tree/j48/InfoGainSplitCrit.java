package cpam.tree.j48;

import weka.core.RevisionUtils;
import weka.core.Utils;

/**
 * Class for computing the information gain for a given distribution.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.10 $
 */
public final class InfoGainSplitCrit extends EntropySplitCriterion {

  private static final long serialVersionUID = 4892105020180728499L;

  /** This method is a straightforward implementation of the information gain criterion for the given distribution. */
  public final double splitCritValue(Distribution bags) {
    double numerator = oldEnt(bags)-newEnt(bags);
    if (Utils.eq(numerator,0)) return Double.MAX_VALUE; // Splits with no gain are useless
    return bags.total()/numerator; //Reciprocal value to minimize the splitting criterion's value
  }

  /**
   * This method computes the information gain in the same way C4.5 does.
   * @param bags the distribution
   * @param totalNoInst weight of ALL instances (including the ones with missing values).
   */
  public final double splitCritValue(Distribution bags, double totalNoInst) {
    double noUnknown = totalNoInst-bags.total();
    double unknownRate = noUnknown/totalNoInst;
    double numerator = (oldEnt(bags)-newEnt(bags));
    numerator = (1-unknownRate)*numerator;
    if (Utils.eq(numerator,0)) return 0; // Splits with no gain are useless
    return numerator/bags.total();
  }

  /**
   * This method computes the information gain in the same way C4.5 does.
   * @param bags the distribution
   * @param totalNoInst weight of ALL instances 
   * @param oldEnt entropy with respect to "no-split"-model.
   */
  public final double splitCritValue(Distribution bags, double totalNoInst, double oldEnt) {
    double noUnknown = totalNoInst-bags.total();
    double unknownRate = noUnknown/totalNoInst;
    double numerator = (oldEnt-newEnt(bags));
    numerator = (1-unknownRate)*numerator;
    if (Utils.eq(numerator,0)) return 0; //Splits with no gain are useless
    return numerator/bags.total();
  }
}
