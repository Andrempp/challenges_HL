package tests.cpam.others;

import java.util.ArrayList;
import java.util.List;

import performance.classification.domain.Results;
import performance.classification.filter.SignificancePairs;
import performance.classification.statististics.AllStatistics;
import tests.cpam.utils.ClassificationUtils;
import tests.cpam.utils.ListClassifiers;
import tests.cpam.utils.ListDatasets;
import weka.classifiers.Classifier;
import weka.core.Instances;
import weka.core.OptionHandler;


/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class OldClassifierPerformanceTest {

  public static void main(String[] args) throws Exception {
	List<Results> results = new ArrayList<Results>();
	for(Classifier clt : ListClassifiers.getClassifiers())
		results.addAll(testDatasets(clt));
	for(Results res : results) System.out.println(res.toString());
	//calculateSignificances(results);
  }
			
  public static List<Results> testDatasets(Classifier classifier) throws Exception {
	System.out.println(classifier.getClass().toString());
	List<Instances> datasets = ListDatasets.getRealDatasets();
	List<String[]> classifierOptions = ClassificationUtils.getClassifierOptions(classifier);
	List<String[]> evaluationOptions = ClassificationUtils.getSamplingOptions(false);
	List<Results> results = new ArrayList<Results>();
  
	//Build tests for different datasets
	for (Instances dataset : datasets) {
		System.out.println(">>"+dataset.relationName());
    	dataset.setClassIndex(dataset.numAttributes()-1);
	    if(ClassificationUtils.isDataBalanced(dataset))
	    	dataset = ClassificationUtils.balanceData(dataset);
	          
	    //Build classifiers with different options
	    for (String[] classifierOption : classifierOptions) {
	    	//for(int i=0;i<classifierOption.length;i++) System.out.print(classifierOption[i]+",");
	    	((OptionHandler) classifier).setOptions(classifierOption);
	      
	    	//Build tests with different evaluation option
	    	for(String[] evaluationOption : evaluationOptions){
	    		//LCurvesBounds.getBounds(classifier, dataset, evaluationOption);
	    		double[] conf = AllStatistics.run(classifier, dataset, evaluationOption);
	    		System.out.println("sig views:{"+conf[0]+","+conf[1]+","+conf[2]+"}");
    	    	//Results res = ClassifierEvaluation.evaluate(classifier, dataset, evaluationOption);
    	    	//System.out.println(res.toString());
    	    	//results.add(res);
	    	}
	    }
	}
	return results;
  }
  
  public static void calculateSignificances(List<Results> results) {
	SignificancePairs significance = new SignificancePairs();
	for(Results result : results){
		significance.classifiers.add(result.id);
		significance.metricsNames.put(result.id, result.names);
		significance.metricsValues.put(result.id, result.values);
	}
	significance.calculateAllSignificances();
	System.out.println(significance.toString());
  }
}

