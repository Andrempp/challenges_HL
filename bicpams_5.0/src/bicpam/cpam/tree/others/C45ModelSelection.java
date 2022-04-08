package cpam.tree.others;

import java.util.Enumeration;

import weka.classifiers.trees.j48.*;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.Utils;

/** 
 *  Select attribute and split data
 *  @author Rui Henriques
 *  @version 1.0
 */
public class C45ModelSelection extends ModelSelection {
	
  private static final long serialVersionUID = 4062909413462612369L;
  private Instances m_allData; /** All the training data */ 
  private int m_minNumObj; /** All the training data */ 

  /** Initializes the split selection method with the given parameters. */
  public C45ModelSelection(Instances allData, int minNumObj) { 
	  m_allData = allData;
	  m_minNumObj = minNumObj;
  }
  public void cleanup() { m_allData = null; }
  public final ClassifierSplitModel selectModel(Instances train, Instances test) throws Exception { return selectModel(train); }
  public String getRevision() { return null; }

  /** Selects C4.5-type split for the given dataset. */
  public final ClassifierSplitModel selectModel(Instances data) throws Exception {
	  
      //============================================
	  //==== A: CHECK ENOUGH INSTANCES TO SPLIT ====
      //============================================
	  boolean multiVal = true;
	  Distribution checkDistribution = new Distribution(data);
	  NoSplit noSplitModel = new NoSplit(checkDistribution);
      if (Utils.sm(checkDistribution.total(),2*m_minNumObj) || Utils.eq(checkDistribution.total(),checkDistribution.perClass(checkDistribution.maxClass())))
    	  return noSplitModel;
      if (m_allData != null) { 
		Enumeration enu = data.enumerateAttributes();
		while (enu.hasMoreElements()) {// Check if all attributes are nominal and have a lot of values
		  Attribute attribute = (Attribute) enu.nextElement();
		  if ((attribute.isNumeric()) || (Utils.sm((double)attribute.numValues(),(0.3*(double)m_allData.numInstances())))){
		    multiVal = false;
		    break;
		  }
		}
      } 
      
      //=============================================
	  //======== B: LEARN GAIN RATIO PER ATT ========
      //=============================================
      double averageInfoGain=0;
      double sumOfWeights = data.sumOfWeights();
	  int validModels = 0;
      C45Split[] currentModel = new C45Split[data.numAttributes()];
      for (int i = 0; i < data.numAttributes(); i++){
		if (i != (data).classIndex()){// Apart from class attribute.
		  currentModel[i] = new C45Split(i,m_minNumObj,sumOfWeights, false); //m_useMDLcorrection
		  currentModel[i].buildClassifier(data);
		  
		  if (currentModel[i].checkModel()){ // Check if useful split for current attribute exists
		      if ((m_allData == null) || (data.attribute(i).isNumeric()) || (multiVal || Utils.sm((double)data.attribute(i).numValues(),(0.3*(double)m_allData.numInstances())))){
			    	  averageInfoGain = averageInfoGain+currentModel[i].infoGain();
			    	  validModels++;
		      } 
		  }
		} else currentModel[i] = null;
      }
      if (validModels == 0) return noSplitModel; // Check if any useful split was found.
      averageInfoGain = averageInfoGain/(double)validModels;

      //========================================
	  //======== C: FIND BEST ATTRIBUTE ========
      //========================================
	  double minResult = 0;
	  C45Split bestModel = null;
      for (int i=0;i<data.numAttributes();i++){
		if ((i != (data).classIndex()) && (currentModel[i].checkModel()))
		  // Use 1E-3 here to get a closer approximation to the original implementation.
		  if ((currentModel[i].infoGain() >= (averageInfoGain-1E-3)) && Utils.gr(currentModel[i].gainRatio(),minResult)){ 
		    bestModel = currentModel[i];
		    minResult = currentModel[i].gainRatio();
		  } 
      }
      if (Utils.eq(minResult,0)) return noSplitModel; // Check if useful split was found.
      
      //============================================
	  //======== D: SPLIT INSTANCES PER ATT ========
      //============================================
      bestModel.distribution().
	  addInstWithUnknown(data,bestModel.attIndex());
      if (m_allData != null) bestModel.setSplitPoint(m_allData);//split point if attribute is numeric.
      return bestModel;
  }
}
