package bicpam.significance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.math3.distribution.NormalDistribution;

public class MultiHypothesisCorrection {

	public enum Correction { Bonferroni , Holm , Sidak, Hochberg , Other }
	public enum Threshold { SimpleRatio , Difference , None }
	
	public static double runCorrection(Threshold threshold, double observedSig, double significance){
		switch(threshold){
			case SimpleRatio : return observedSig/significance;
			case Difference : return significance-observedSig;
			default : return observedSig;
		}
	}

	public static double runCorrection(Correction correction, double alpha, int count) {
		if(correction==Correction.Bonferroni) return alpha/count;
		if(correction==Correction.Sidak) return 1-Math.pow(1-alpha,1.0/(double)count);
		return -1;
	}

	public static double runCorrection(Correction correction, double alpha, List<Double> prob){
		switch(correction){
			case Bonferroni : return alpha/prob.size(); //System.out.println("alpha:"+alpha+" length:"+prob.size());
			case Sidak : return 1-Math.pow(1-alpha,1.0/(double)prob.size());
			case Holm : return getHolm(alpha,prob);
			case Hochberg :	return getHochberg(alpha,prob);
			default : return -1;
		}
	}
	
	private static double getHochberg(double alpha, List<Double> probs) {
		Collections.sort(probs);
		//System.out.println("\nordered probs:"+probs);
		for(int l=probs.size(), i=l-1; i>=0; i--){
			if(probs.get(i)<(alpha/(i+1))) {
				System.out.println("HochbertIndex["+l+"]>>"+i+":"+(alpha/(i+1)));
				return (alpha/(i+1));
			}
			//System.out.println("No:"+i+" n:"+(l-i)+" sig:"+prob[i]);
		}
		return 0;
	}

	private static double getHolm(double alpha, List<Double> probs) {
		Collections.sort(probs);
		for(int i=0, l=probs.size(); i<l; i++)
			if(probs.get(i)<=(alpha/(l-i))){
				System.out.println(">>"+l+":"+i);
				return alpha/(l-i);
			}
		return 0;
	}

	public static double runCorrection(Correction correction, double alpha, int count, int avgprob){
		System.out.println("CORRECTION:" + alpha +" "+count+","+avgprob);
		switch(correction){
			case Bonferroni : return alpha/count;
			case Sidak : return 1-Math.pow(1-alpha,1.0/(double)count);
			case Holm : 
				for(int i=0; i<count; i++)
					if(avgprob>(alpha/(count-i))) return avgprob;
				break;
			case Hochberg : 
				for(int i=count; i>0; i++)
					if(avgprob<=(alpha/(count+1-i))) return avgprob;
				break;
			default : return -1;
		}
		return -1;
	}

	public static double runCorrection(Correction correction, double alpha, int count, double mean, double std){
		System.out.println("CORRECTION:" + alpha +" "+count+"("+mean+","+std+")");
		if(correction==Correction.Bonferroni) return alpha/count;
		if(correction==Correction.Sidak) return 1-Math.pow(1-alpha,1.0/(double)count);
		List<Double> prob = generateNormalProbs(count,mean,std);
		switch(correction){
			case Holm : return getHolm(alpha,prob);
			case Hochberg :	return getHochberg(alpha,prob);
			default : return -1;
		}
	}

	private static List<Double> generateNormalProbs(int count, double mean, double std) {
		System.out.println("count:"+count);
		List<Double> result = new ArrayList<Double>();
		NormalDistribution gauss = new NormalDistribution(mean,std);
		double inc = 1.0/(double)count;
		for(int i=0; i<count; i++){ 
			result.add(gauss.inverseCumulativeProbability(i*inc));
			//if(i%100==0) System.out.print(i+":"+result[i]+",");
		}
		return result;
	}

}
