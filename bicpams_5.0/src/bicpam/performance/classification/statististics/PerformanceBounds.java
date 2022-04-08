package performance.classification.statististics;

import java.util.List;
import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import utils.BicMath;


/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class PerformanceBounds {

	public static double[] run(List<Double> sample, double significance) {
		double[] result = new double[3];
		SummaryStatistics summary = new SummaryStatistics();
		for(double value : sample) summary.addValue(value);
		TDistribution tDist = new TDistribution(summary.getN() - 1);
		double a = tDist.inverseCumulativeProbability(1.0-significance/2);
		double confidence = a * summary.getStandardDeviation() / Math.sqrt(summary.getN());

		result[0]=summary.getMean()-confidence;
		result[1]=summary.getMean();
		result[2]=summary.getMean()+confidence;
		return result;
	}
	
	public static double[] run(List<Double> sample) {
		return run(sample,0.01);
	}

    public static double[] confidenceIntervals(double[] sample, double significance){
		double mean = BicMath.mean(sample);
		SummaryStatistics summary = new SummaryStatistics();
		for(int i=0; i<sample.length; i++) summary.addValue(sample[i]);
		TDistribution tDist = new TDistribution(summary.getN() - 1);
		double a = tDist.inverseCumulativeProbability(1.0-significance/2);
		double deviation = a *summary.getStandardDeviation() / Math.sqrt(summary.getN());
		System.out.println("["+(mean-deviation)+","+(mean+deviation)+"]");
		return new double[]{(mean-deviation),(mean+deviation)};
    }
    
    public static double[] confidenceIntervals(List<Double> sample, double significance){
		SummaryStatistics summary = new SummaryStatistics();
		for(Double val : sample) summary.addValue(val);
		TDistribution tDist = new TDistribution(summary.getN() - 1);
		double a = tDist.inverseCumulativeProbability(1.0-significance/2);
		double deviation = a *summary.getStandardDeviation() / Math.sqrt(summary.getN());
		double mean = summary.getMean();
		System.out.println("["+(mean-deviation)+","+(mean+deviation)+"]");
		return new double[]{(mean-deviation),(mean+deviation)};
    }
}