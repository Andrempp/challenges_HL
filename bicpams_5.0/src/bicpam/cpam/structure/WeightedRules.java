package cpam.structure;

import java.util.ArrayList;
import java.util.List;

import utils.BicMath;
import utils.BicPrinting;
import utils.BicResult;
import cpam.Learner;
import cpam.Tester;
import cpam.BClassifier.DiscMetric;
import domain.Bicluster;
import domain.Biclusters;
import domain.Biclusters.Order;
import domain.Dataset;

public class WeightedRules extends Learner {

	public int numRows, numClasses;
	public Biclusters[] rules;

	public WeightedRules(Biclusters[] bics, Dataset data, int maxBics){
		numRows = data.rows.size();
		numClasses = data.numClasses();
		rules = new Biclusters[numClasses];
		for(int k=0; k<numClasses; k++) {
			List<Bicluster> kbics = bics[k].order(Order.Score).getBiclusters();
			//System.out.println(kbics.get(0)+" --- "+kbics.get(1));
			rules[k] = new Biclusters(kbics.subList(0, Math.min(kbics.size(),maxBics)));
		}
		System.out.println(toString());
		//BicResult.println("AQUI\n");
	}

	public double[] test(Tester tester, double[] instance) {
		//System.out.println(BicPrinting.plot(instance));
		List<Bicluster> matches = new ArrayList<Bicluster>(), mismatches = new ArrayList<Bicluster>();
		List<Double> scores = new ArrayList<Double>(), miscores = new ArrayList<Double>();
		for(Biclusters rule : rules){ 
			List<Double> matching = new ArrayList<Double>();
			for(Bicluster bic : rule.getBiclusters()){ 
				double match = (bic.orders==null) ? tester.nominalMatch(bic, instance) : tester.orderMatch(bic, instance);
				matching.add(match);
				if(match>=0.8){
					matches.add(bic);
					double score = Math.min(bic.score,1.5);
					scores.add(score*tester.penalizedCount(match)/Math.sqrt(rule.size()));
				} else if(match<0.25){
					mismatches.add(bic);
					double score = Math.min(bic.score,1.5);
					miscores.add(0.2*score*tester.penalizedCount(1-match)/Math.sqrt(rule.size()));
				}
			}
			//System.out.println(matching);
		}
		return tester.test(matches,scores,mismatches,miscores,numClasses);
	}
	
	public  String rulesToString(){
		String result ="";
		int index=0;
		for(Biclusters bicsClass : rules){
			result+="Class="+(index++)+" #Rules:"+bicsClass.size()+"\n";
			for(Bicluster bic : bicsClass.getBiclusters())
				result+="[wlift="+(int)(bic.wlift*100)+"%,wconf="+(int)(bic.wconf*100)+"%] "+bic.toString()+" => "+bic.condition+"\n";
		}
		return result;
	}

	public String rulesToString(List<String> rows, List<String> columns) {
		String result ="";
		int index=0;
		for(Biclusters bicsClass : rules){
			result+="\n\nClass="+(index++)+" #Rules:"+bicsClass.size()+"\n";
			for(Bicluster bic : bicsClass.getBiclusters())
				result+="[wlift="+(int)(bic.wlift*100)+"%,wconf="+(int)(bic.wconf*100)+"%] "+bic.toString(rows,columns)+" => "+bic.condition+"\n";
		}
		return result;
	}
	
	public  String toString(){
		String result ="";
		int index=0;
		for(Biclusters bicsClass : rules){
			result+="Class="+(index++)+" #Rules:"+bicsClass.size()+"\n";
			for(Bicluster bic : bicsClass.getBiclusters())
				result+=bic.patternToString()+" wsup="+((double)(int)(BicMath.sum(bic.weightedSupport)*100))/100.0
					+" wlift="+((double)(int)(bic.wlift*100))/100.0+" wconf="+((double)(int)(bic.wconf*100))/100.0+"\n";
		}
		return result;
	}

}
