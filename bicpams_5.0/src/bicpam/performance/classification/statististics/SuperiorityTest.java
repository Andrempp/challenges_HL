package performance.classification.statististics;

import java.util.List;
import java.util.Random;

import org.apache.commons.math3.stat.inference.TTest;

/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class SuperiorityTest {
	
	/** 
	 * Why statistical tests?
	 * Comparing the performance of different ML algorithms on a given problem is not as easy as it sounds
	 * We need to be certain that apparent differences are not due to chance effects
	 * 
	 * Overall error? Average error on each fold
	 * Note: in 10-fold CV data is split into 10 equal partitions (each one used for testing while the remainder used for training)
	 * 
	 * Problem: Unreliable; Substantial variance in individual ten-fold cross-validation;
	 * Variance can be reduced by averaging the results of several folds, but may still be unreliable;
	 * 
	 * Assumptions: limited data, ten-fold cross-validation is best indicator of the “true” performance of a learning scheme
	 * CV yields an >approximation< of the true error (we can sample from the distribution underlying the fold results)
	 * 
	 * T-test: mean of a set of samples is significantly > or < the mean of another => comparing two learning schemes (by comparing average error rate)
	 * Paired t-Test: if same cross-validation splits are used for both schemes then we have matched pairs of results => more sentitive stat test
	 * 
	 * avg_a = mean of a1, a2, …,ak (ai is the result of fold-i using learner A)
	 * avg_b = mean of o1, o2, …,ok (oi is the result of fold-i using learner B)
	 * Is avg_a significantly different from avg_b? => confidence limits using normal distribution (the case when there are enough folds), but we need to also know std!
	 * 
	 * Unfortunately std is not known => but can be approximated by a Student t-test (wider and shorter than normal dist)
	 * Student’s distribution is more conservative: for a given degree of confidence, interval is slightly wider (reflect greater variance by estimating std)
	 * T-test with k-1 degrees of freedom (in our case: 9 => specific distribution)
	 * di=ai-oi, avg_d=avg_a-avg_o, std=std_a-std_o has student's distribution => avg_dtrue=avg_d+-std_d
	 * 
	 * Null hypothesis: Is avg_a significantly different from avg_b? => Is avg_dtrue = 0?
	 * Answer: yes=if null hypothesis is rejected; no=otherwise
	 * 
	 * To allow the null hypothesis to be tested:
	 * a) reduce the difference avg_d to a 0-mean (=> /std_d) t-statistic, consult t-distribution values
	 * b) reject null-hypothesis if t > t(conf,k-1) or t < -t(conf,k-1) => A two tailed test is appropriate
	 */
    public static double pvalueForDifferentPerformance(double[] classifier1Folds, double[] classifier2Folds){
    	TTest ttest = new TTest();
    	return ttest.pairedTTest(classifier1Folds, classifier2Folds);
    }

    public static boolean isPerformanceDifferent(double[] classifier1Folds, double[] classifier2Folds, double confidence){
    	TTest ttest = new TTest();
    	return ttest.pairedTTest(classifier1Folds, classifier2Folds, confidence);
    }

	public static double pvalueForDifferentPerformance(List<Double> cl1folds, List<Double> cl2folds) {
		int length = cl1folds.size();
		double[] cl1=new double[length], cl2=new double[length];
		for(int i=0; i<length; i++){
			cl1[i]=cl1folds.get(i);
			cl2[i]=cl2folds.get(i);
		}
		return pvalueForDifferentPerformance(cl1,cl2);
	}
    
    public static void main(String [] argv) {    	
    	
    	int folds = 10;
    	double confidence = 0.01;
    	
    	// Metric (accuracy, error rate, etc.) from CV
    	double[] cl1 = new double[folds];
    	double[] cl2 = new double[folds]; //similar to c1
    	double[] cl3 = new double[folds]; //very similar to c1
    	double[] cl4 = new double[folds]; //different from c1
    	
    	// Generate results
    	Random random = new Random(1);
    	for(int i=0; i<folds; i++){
    		cl1[i]=0.7+0.1*random.nextDouble();
    		cl2[i]=0.6+0.1*random.nextDouble();
    		cl3[i]=0.7+0.1*random.nextDouble();
    		cl4[i]=0.2+0.1*random.nextDouble();
    	}
    	TTest ttest = new TTest();
   
    	/** 
    	 * pairedT(double[],double[])
    	 * Computes a paired, 2-sample t-statistic based on the data in the input arrays.
    	 * 
    	 * pairedTTest(double[],double[])
    	 * Returns the observed p-value, associated with a paired, two-sample, two-tailed t-test. 
    	 * The number returned is the smallest significance level at which one can reject the null 
    	 * hypothesis that the mean of the paired differences is 0 in favor of the two-sided 
    	 * alternative that the mean paired diff is not equal to 0. For a one-sided: divide by 2.
    	 * 
    	 * optional argument alpha: significance level of the test (returns a boolean). 
    	 */

    	System.out.println("Running Rui's checking to test if the performance of two learners statistically differs");

    	double pvalue = ttest.pairedTTest(cl1, cl2);
    	System.out.println("PValue=" + pvalue + " (similar learners)");
    	pvalue = ttest.pairedTTest(cl1, cl3);
    	System.out.println("PValue=" + pvalue + " (very similar learners)");
    	pvalue = ttest.pairedTTest(cl1, cl1);
    	System.out.println("PValue=" + pvalue + " (same learners)");
    	pvalue = ttest.pairedTTest(cl1, cl4);
    	System.out.println("PValue=" + pvalue + " (different learners)");
    	
    	boolean rejectNull = ttest.pairedTTest(cl1, cl2, confidence);
    	System.out.println("Performance statistically different? " + rejectNull + " (similar learners using p-value=" + confidence + ")");
    	rejectNull = ttest.pairedTTest(cl1, cl3, confidence);
    	System.out.println("Performance statistically different? " + rejectNull + " (very similar learners using p-value=" + confidence + ")");
    	rejectNull = ttest.pairedTTest(cl1, cl1, confidence);
    	System.out.println("Performance statistically different? " + rejectNull + " (same learners using p-value=" + confidence + ")");
    	rejectNull = ttest.pairedTTest(cl1, cl4, confidence);
    	System.out.println("Performance statistically different? " + rejectNull + " (different learners using p-value=" + confidence + ")");
    }
    
}