package tests.cpam;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import performance.classification.ClassifierEvaluation;
import performance.classification.domain.Results;
import tests.cpam.utils.ListClassifiers;
import tests.cpam.utils.ListDatasets;
import weka.classifiers.Classifier;
import weka.core.Instances;
import weka.core.OptionHandler;


/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class CpamSyntheticTest {

  public static void main(String[] args) throws Exception {
	List<Map<String,Results>> results = getClassifiersPerformance();
	for(Map<String,Results> res : results) System.out.println(res.toString());
  }
			
  public static List<Map<String,Results>> getClassifiersPerformance() throws Exception {
	List<Map<String,Results>> results = new ArrayList<Map<String,Results>>();
	for (Instances dataset : ListDatasets.getRealDatasets())
		for(Classifier clt : ListClassifiers.getClassifiers())
			results.addAll(test(clt,dataset));
	return results;
  }

  public static List<Map<String,Results>> test(Classifier classifier, Instances dataset) throws Exception {
	List<Map<String,Results>> results = new ArrayList<Map<String,Results>>();
	List<String[]> classifierOptions = ListClassifiers.getClassifierOptions(classifier);
	String[] evaluationOptions = new String[]{"-preserve-order","CV"};

	System.out.println("\n"+dataset.relationName()+" :: "+classifier.getClass().toString());
	//if(!BalanceData.isDataBalanced(dataset)) dataset = BalanceData.balanceDataset(dataset);

	//Build classifiers with different options
    for (String[] classifierOption : classifierOptions) {
    	((OptionHandler) classifier).setOptions(classifierOption);
    	results.add(ClassifierEvaluation.run(classifier, dataset, evaluationOptions));
    }
	return results;
  }
  
}
