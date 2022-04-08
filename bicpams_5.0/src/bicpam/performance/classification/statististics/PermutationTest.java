package performance.classification.statististics;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import performance.classification.ClassifierEvaluation;
import utils.BicMath;
import weka.classifiers.Classifier;
import weka.core.Instances;

/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class PermutationTest {

	public int nrIterations = 10;
	public List<List<Double>> permutations;
	
	public PermutationTest(Classifier classifier, Instances dataset, String[] evaluationOption) throws Exception {
		permutations = new ArrayList<List<Double>>();
		int nrLabels = dataset.numClasses();
		Random random = new Random(1);
		for(int k=0; k<nrIterations; k++){
			for(int i=0, l=dataset.numInstances(); i<l; i++) 
				dataset.instance(i).setClassValue((int)(nrLabels*random.nextDouble()));
			List<Double> resRandom = ClassifierEvaluation.run(classifier, dataset, evaluationOption.clone()).getAccuracy();
			permutations.add(resRandom);
		}
	}
	
	
	public double getPValueAverage(List<Double> resTarget) throws Exception {
		List<Double> pvalues = new ArrayList<Double>();
		for(List<Double> permutation : permutations){
			//System.out.println(permutation);
			pvalues.add(SuperiorityTest.pvalueForDifferentPerformance(resTarget, permutation));
		}
		//System.out.println("pvalues: " + pvalues);
		double result = 0;
		for(Double pvalue : pvalues) result += pvalue;
		return result/pvalues.size(); //Collections.sort(resRandom);
	}

	public double getPercentageBelow(List<Double> resTarget) throws Exception {
		double error = BicMath.mean(resTarget), count = 0;
		//System.out.println("error: "+error);
		for(List<Double> permutation : permutations)
			for(int i=0, l=permutation.size(); i<l; i++)
				if(permutation.get(i)>error) count++;
		return count/((double)(permutations.get(0).size()*permutations.size())); //Collections.sort(resRandom);
	}

}
