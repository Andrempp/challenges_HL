package performance.classification.statististics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import performance.classification.ClassifierEvaluation;
import utils.BicMath;
import weka.classifiers.Classifier;
import weka.classifiers.rules.ZeroR;
import weka.core.Instances;

public class AllStatistics {

	public static double[] run(Classifier classifier, Instances dataset, String[] evaluationOption) throws Exception {
    	//for(int i=0, l=evaluationOption.length; i<l; i++) System.out.print(evaluationOption[i]+";");
		List<Double> resTarget = ClassifierEvaluation.run(classifier, dataset, evaluationOption.clone()).values.get(0);
		List<List<Double>> permutations = getPermutations(classifier, dataset, evaluationOption);

		double[] result = new double[3];
		result[0] = getPValueAverage(resTarget,permutations);
		System.out.println("avgPvalue: "+result[0]);

		result[1] = getPercentageBelow(permutations,BicMath.mean(resTarget));
		System.out.println("percentage: "+result[1]);

		List<Double> resNullData = ClassifierEvaluation.run(new ZeroR(), dataset, evaluationOption.clone()).values.get(0);
		result[2] = SuperiorityTest.pvalueForDifferentPerformance(resTarget, resNullData);
		System.out.println("pvalueNull: "+result[2]);

		return result;
	}

	private static double getPercentageBelow(List<List<Double>> permutations, double error) {
		System.out.println("error: "+error);
		double count = 0;
		for(List<Double> permutation : permutations)
			for(int i=0, l=permutation.size(); i<l; i++)
				if(permutation.get(i)>error) count++;
		return count/((double)(permutations.get(0).size()*permutations.size())); //Collections.sort(resRandom);
	}

	private static double getPValueAverage(List<Double> resTarget, List<List<Double>> permutations) {
		List<Double> pvalues = new ArrayList<Double>();
		for(List<Double> permutation : permutations){
			//System.out.println(permutation);
			pvalues.add(SuperiorityTest.pvalueForDifferentPerformance(resTarget, permutation));
		}
		System.out.println("pvalues: " + pvalues);
		double result = 0;
		for(Double pvalue : pvalues) result += pvalue;
		return result/pvalues.size(); //Collections.sort(resRandom);
	}

	private static List<List<Double>> getPermutations(Classifier classifier, Instances dataset, String[] evaluationOption) throws Exception {
		int nrIterations = 5;
		int nrLabels = dataset.numClasses();
		Random random = new Random(1);
		
		List<List<Double>> permutations = new ArrayList<List<Double>>();
		for(int k=0; k<nrIterations; k++){
			for(int i=0, l=dataset.numInstances(); i<l; i++) 
				dataset.instance(i).setClassValue((int)(nrLabels*random.nextDouble()));
			List<Double> resRandom = ClassifierEvaluation.run(classifier, dataset, evaluationOption.clone()).values.get(0);
			permutations.add(resRandom);
		}
		return permutations;
	}

	public static double getNullDataConfidence(Classifier classifier, Instances dataset, String[] evaluationOption) throws Exception {
		return 1;
	}
	
	public static double[] getConfidenceIntervals(Classifier classifier, Instances dataset, String[] evaluationOption) throws Exception {
		List<Double> resTarget = ClassifierEvaluation.run(classifier, dataset, evaluationOption).values.get(0);
		Collections.sort(resTarget);
		return PerformanceBounds.confidenceIntervals(resTarget, 0.01);
	}

}
