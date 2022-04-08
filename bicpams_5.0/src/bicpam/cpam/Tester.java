package cpam;

import java.util.List;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;
import utils.BicMath;
import utils.BicPrinting;
import cpam.BClassifier.TestingFunction;
import cpam.BClassifier.Relaxation;
import domain.Bicluster;

public final class Tester {
	
	public TestingFunction function;
	public Relaxation relaxation;
	
	public Tester(TestingFunction _function, Relaxation _relaxation){
		function = _function;
		relaxation = _relaxation;
	}

	/*****************
	 **** TESTING ****
	 *****************/
	
	public double[] test(List<Bicluster> matches, List<Double> scores, List<Bicluster> mismatches, List<Double> misscores, int numClasses) {
		double[] result = new double[numClasses];
		int[] count = new int[numClasses];
		if(matches.isEmpty() && mismatches.isEmpty()){
			Random r = new Random();
			for(int i=0; i<numClasses; i++) result[i]=1.0+0.1*r.nextDouble();
			double sum = BicMath.sum(result);
			for(int i=0; i<numClasses; i++) result[i]=result[i]/sum;
		} else {
			int index=0;
			for(Bicluster bic : matches){
				//if(function.equals(TestingFunction.WCS)) result[bic.condition] += (bic.getChi2()*bic.getChi2())/bic.getChi2UpperBound();
				count[bic.condition]++;
				result[bic.condition] += scores.get(index++);
			}
			System.out.print("\nMatches=("+BicPrinting.plot(count)+")");
			//System.out.println("("+BicPrinting.plot(result)+")");
			
			index=0;
			count = new int[numClasses];
			for(Bicluster bic : mismatches){
				//if(function.equals(TestingFunction.WCS)) result[bic.condition] += (bic.getChi2()*bic.getChi2())/bic.getChi2UpperBound();
				count[bic.condition]++;
				result[bic.condition] -= misscores.get(index++);
			}
			System.out.println("Mismatches=("+BicPrinting.plot(count)+")");
		}
		return result;
	}
	
	/**************************
	 **** INDIVIDUAL MATCH ****
	 **************************/
	
	public double nominalMatch(Bicluster bic, double[] instance) {
		int correct=0, index=0;
		for(Integer col : bic.columns){
			try {
				if(instance[col]==bic.items.get(index++)) correct++;
			} catch(Exception e){
				System.out.println(">>"+bic.columns+" | "+bic.items);
				e.printStackTrace();
				System.out.println(1/0);
			}
		}
		return ((double)correct)/((double)bic.numColumns());
	}
	
	public double orderMatch(Bicluster bic, double[] instance) {
		List<Integer> order = bic.orders;
		double match = 0;
		for(int j=1, l=bic.columns.size(); j<l; j++) {
			if(instance[order.get(j-1)]<=instance[order.get(j)]) match++;
		}
		//System.out.print(match+"|"+(instance.length-1));
		return match/(order.size()-1); 
	}
	
	public double penalizedCount(Bicluster bic, double[] instance) {
		switch(relaxation){
			case None: return 1;
			case Linear: return nominalMatch(bic,instance);
			case Squared: return Math.pow(nominalMatch(bic,instance),2);
			case Adjusted: 
				int correct=0, index=0;
				for(Integer col : bic.columns)
					if(instance[col]==bic.items.get(index++)) correct++;
				return Math.pow(correct,2)/Math.pow(bic.numColumns(),2);
			default: return 0;
		}
	}
	
	public double penalizedCount(double prob) {
		switch(relaxation){
			case None: return 1;
			case Linear: return prob;
			case Squared: return Math.pow(prob,2);
			default : return -1;
		}
	}
	
	public SortedSet<Integer> match(Bicluster bic, double[] instance) {
		SortedSet<Integer> result = new TreeSet<Integer>();
		int index=0;
		for(Integer col : bic.columns)
			if(instance[col]==bic.items.get(index++)) result.add(col);
		return result;
	}
	
	public boolean isSubset(Bicluster bic, double[] instance) {
		if(nominalMatch(bic,instance)>=0.8) return true;
		else return false;
	}	
	
	public String toString(){
		return "Func="+function+" Relaxation="+relaxation;
	}
}
