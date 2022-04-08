package cpam.tree.j48;

import weka.core.Instance;
import weka.core.Instances;

/**
 * Class implementing a "no-split"-split.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.9 $
 */
public final class NoSplit extends SplitModel {

  private static final long serialVersionUID = -1292620749331337546L;

  /** Creates "no-split"-split for given distribution */
  public NoSplit(Distribution distribution){
    m_distribution = new Distribution(distribution);
    m_numSubsets = 1;
  }
  /** Creates a "no-split"-split for a given set of instances */
  public final void buildClassifier(Instances instances) throws Exception {
    m_distribution = new Distribution(instances);
    m_numSubsets = 1;
  }
  public final int whichSubset(Instance instance){ return 0; }
  public final double [] weights(Instance instance){ return null; }
  public final String leftSide(Instances instances){ return ""; }
  public final String rightSide(int index, Instances instances){ return ""; }
  public final String sourceExpression(int index, Instances data) { return "true";  }  
}
