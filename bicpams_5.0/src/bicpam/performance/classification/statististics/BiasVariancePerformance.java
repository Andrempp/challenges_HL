package performance.classification.statististics;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import performance.classification.ClassifierEvaluation;
import utils.BicMath;
import weka.classifiers.Classifier;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.supervised.instance.Resample;


/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class BiasVariancePerformance {

	public static double[] run(Classifier classifier, Instances dataset, String[] evaluationOption) throws Exception {
		
    	//for(int i=0, l=evaluationOption.length; i<l; i++) System.out.print(evaluationOption[i]+";");
		List<List<Double>> results = new ArrayList<List<Double>>(); 
		int nrIterations = 5;
		results.add(ClassifierEvaluation.run(classifier, dataset, evaluationOption.clone()).values.get(0));
		for(int i=0; i<nrIterations; i++){
			dataset.randomize(new Random());
			//for(int j=0; j<dataset.numInstances(); j++) System.out.print((int)dataset.instance(j).classValue()+",");
			//System.out.println();
			Resample resample = new Resample();
			resample.setInputFormat(dataset);
			resample.listOptions();
			resample.setSampleSizePercent(100);
			Instances data = new Instances(Filter.useFilter(new Instances(dataset), resample));
			//for(int j=0; j<data.numInstances(); j++) System.out.print((int)data.instance(j).classValue()+","+data.instance(j).value(0)+";");
			//System.out.println();
			results.add(ClassifierEvaluation.run(classifier, data, evaluationOption.clone()).values.get(0));
		}
		
		double[] result = new double[2];
		result[0] = computeVariance(results);
		result[1] = computeBiasAcrossFolds(results);
		//result[2] = computeBiasWithinFolds(results);
			
		return result;
	}

	private static double computeBiasAcrossFolds(List<List<Double>> results) {
		List<Double> values = new ArrayList<Double>();
		for(List<Double> result : results) values.add(BicMath.std(result));
		System.out.println(values);
		return BicMath.mean(values);
	}
	private static double computeVariance(List<List<Double>> results) {
		List<Double> values = new ArrayList<Double>();
		for(List<Double> result : results) values.add(BicMath.mean(result));
		System.out.println(values);
		return BicMath.std(values);
	}

}