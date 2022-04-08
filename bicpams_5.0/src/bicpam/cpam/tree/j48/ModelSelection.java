package cpam.tree.j48;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Set;

import weka.core.Attribute;
import weka.core.Instances;
import weka.core.Utils;

/**
 * Abstract class for model selection criteria.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.8 $
 */
public class ModelSelection implements Serializable {

  private static final long serialVersionUID = 3372204862440821989L;

  private int m_minNoObj; //Minimum number of objects in interval               
  private boolean m_useMDLcorrection; //Use MDL correction?         
  private Instances m_allData; //All the training data 

  /**
   * Initializes the split selection method with the given parameters.
   * @param minNoObj minimum number of instances that have to occur in at least two subsets induced by split
   * @param allData FULL training dataset (necessary for selection of split points).
   * @param useMDLcorrection whether to use MDL adjustement when finding splits on numeric attributes
   */
  public ModelSelection(int minNoObj, Instances allData, boolean useMDLcorrection) {
    m_minNoObj = minNoObj;
    m_allData = allData;
    m_useMDLcorrection = useMDLcorrection;
  }

  /** Sets reference to training data to null */
  public void cleanup() {
    m_allData = null;
  }

  /** Selects C4.5-type split for the given dataset. */
  public final SplitModel selectModel(Instances data, Set<Integer> atts){
    SplitModel [] currentModel;
    SplitModel bestModel = null;
    double averageInfoGain = 0, sumOfWeights;
    int validModels = 0;
    boolean multiVal = true;
    
    try{
      // Check if all Instances belong to one class or if not enough Instances to split.
      Distribution checkDistribution = new Distribution(data);
      NoSplit noSplitModel = new NoSplit(checkDistribution);
      if (Utils.sm(checkDistribution.total(),2*m_minNoObj) || 
    	  Utils.eq(checkDistribution.total(),checkDistribution.perClass(checkDistribution.maxClass())))
    	  return noSplitModel;

      // Check if all attributes are nominal and have a lot of values
      if (m_allData != null) {
		Enumeration enu = data.enumerateAttributes();
		while(enu.hasMoreElements()) {
		  Attribute attribute = (Attribute) enu.nextElement();
		  if ((attribute.isNumeric()) || (Utils.sm((double)attribute.numValues(),(0.3*(double)m_allData.numInstances())))){
		    multiVal = false;
		    break;
		  }
		}
      } 
      currentModel = new SplitModel[data.numAttributes()];
      sumOfWeights = data.sumOfWeights();
      
      for (int i = 0; i < data.numAttributes(); i++){// For each attribute.
		if (i!=(data).classIndex() && !atts.contains(i)){// Apart from class attribute.
		  // Get models for current attribute.
		  currentModel[i] = new SplitModel(i,m_minNoObj,sumOfWeights,m_useMDLcorrection);
		  currentModel[i].buildClassifier(data);
		  // Check if useful split for current attribute exists and check for enumerated attributes with a lot of values.
		  if (currentModel[i].checkModel())
		    if (m_allData != null) {
		      if ((data.attribute(i).isNumeric()) || (multiVal || Utils.sm((double)data.attribute(i).numValues(), (0.3*(double)m_allData.numInstances())))){
				averageInfoGain = averageInfoGain+currentModel[i].infoGain();
				validModels++;
		      } 
		    } else {
		      averageInfoGain = averageInfoGain+currentModel[i].infoGain();
		      validModels++;
		    }
		} else currentModel[i] = null;
	  }
      // Check if any useful split was found.
      if (validModels == 0) return noSplitModel;
      averageInfoGain = averageInfoGain/(double)validModels;
      double minResult = 0;
      for (int i=0;i<data.numAttributes();i++){ // Find "best" attribute to split on.
		if((i!=(data).classIndex()&&!atts.contains(i)) && (currentModel[i].checkModel()))
		  // Use 1E-3 here to get a closer approximation to the original implementation.
		  if ((currentModel[i].infoGain() >= (averageInfoGain-1E-3)) && Utils.gr(currentModel[i].gainRatio(),minResult)){ 
		    bestModel = currentModel[i];
		    minResult = currentModel[i].gainRatio();
		  } 
	  }
      // Check if useful split was found.
      if (Utils.eq(minResult,0)) return noSplitModel;
      // Add all Instances with unknown values for the corresponding attribute to the distribution for the model, so that the complete distribution is stored with the model. 
      bestModel.distribution().addInstWithUnknown(data,bestModel.attIndex());
      // Set the split point analogue to C45 if attribute numeric.
      if (m_allData != null) bestModel.setSplitPoint(m_allData);
      return bestModel;
    } catch(Exception e){
      e.printStackTrace();
    }
    return null;
  }
}
