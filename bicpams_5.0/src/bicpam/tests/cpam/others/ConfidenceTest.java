package tests.cpam.others;

import java.util.Random;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import utils.BicMath;


/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class ConfidenceTest {

	public static void main(String[] args) throws Exception{
		//NormalDistribution n = new NormalDistribution();
		//datagen();
		confidenceIntervals();
	}

	private static void confidenceIntervals() {
		double[] sample = {2.3, 2.7, 2.1, 3.4, 1.9};
		double mean = BicMath.mean(sample);
		//double std = BMath.std(sample);
		double significance = 0.01;
		SummaryStatistics summary = new SummaryStatistics();
		for(int i=0; i<sample.length; i++) summary.addValue(sample[i]);
		TDistribution tDist = new TDistribution(summary.getN() - 1);
		double a = tDist.inverseCumulativeProbability(1.0-significance/2);
		double confidence = a *summary.getStandardDeviation() / Math.sqrt(summary.getN());
		System.out.println("INTERVAL:"+confidence);
		System.out.println("["+(mean-confidence)+","+(mean+confidence)+"]");
	}

	public static void datagen() {
		Random r = new Random();
		for(int i=0; i<20; i++){
			for(int j=0; j<10; j++)
				System.out.print(r.nextInt(7)+" ");
			System.out.println();
		}
		NormalDistribution normal = new NormalDistribution(3,2);
		System.out.println("4:"+normal.density(4));
		System.out.println("3:"+normal.density(3));
		System.out.println("2:"+normal.density(2));
		System.out.println("1:"+normal.density(1));
		System.out.println("5:"+normal.density(5));
		System.out.println("0:"+normal.density(0));
		System.out.println("6:"+normal.density(6));
		System.out.println(normal.density(3)*normal.density(6)*normal.density(2)*normal.density(4));
		System.out.println(Math.pow(1.0/6.0,4));
		System.out.println(0.01/BicMath.combination(10,4));
		System.out.println((51.0*23.0*52.0*44.0)/Math.pow(200,4));
	}

}