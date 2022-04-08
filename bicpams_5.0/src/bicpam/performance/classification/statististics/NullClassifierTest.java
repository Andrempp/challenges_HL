package performance.classification.statististics;

import java.util.List;

import performance.classification.ClassifierEvaluation;
import weka.classifiers.Classifier;
import weka.classifiers.rules.ZeroR;
import weka.core.Instances;


/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class NullClassifierTest {

	public static Classifier nullclassifier = new ZeroR();
	
	public static double run(List<Double> resTarget, Instances dataset) throws Exception {
		List<Double> resNullData = ClassifierEvaluation.run(nullclassifier, dataset, new String[]{}).values.get(0);
		return run(resTarget, resNullData);
	}
	public static double run(List<Double> resTarget, List<Double> resNullData) throws Exception {
		return SuperiorityTest.pvalueForDifferentPerformance(resTarget, resNullData);
	}
}
