package cpam.structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import cpam.DiscMetrics;
import cpam.Learner;
import cpam.Tester;
import cpam.BClassifier.DiscMetric;
import domain.Bicluster;
import domain.Biclusters;
import domain.Biclusters.Order;
import domain.Dataset;

public class TreeLearner extends Learner {

	public int numRows, numClasses;
	public List<TreeMap<Bicluster,List<Double>>> rules;

	public TreeLearner(Biclusters[] bics, Dataset data, DiscMetric metric){
		numRows = data.rows.size();
		numClasses = data.numClasses();
		rules = new ArrayList<TreeMap<Bicluster,List<Double>>>();
		for(int k=0; k<numClasses; k++) rules.add(new TreeMap<Bicluster,List<Double>>());
		for(Biclusters bicsI : bics) {
		  for(Bicluster bic : bicsI.getBiclusters()){
			List<Double> metrics = new ArrayList<Double>();
			metrics.add(bic.wconf*100);
			metrics.add(bic.wlift*100);
			//metrics.add(DiscMetrics.getFoil(bic)*100);
			if(rules.isEmpty()) rules.get(bic.condition).put(bic,metrics);
			else {
				boolean greater = false;
				Set<Bicluster> biclusters = rules.get(bic.condition).keySet();
				for(Bicluster ibic : biclusters){
			    	if(bic.wlift>ibic.wlift) greater=true;
			    	else if(bic.numRows()>ibic.numRows()) greater=true;
			    	if(!greater) continue;
			    	rules.get(bic.condition).put(bic,metrics);
					break;
				}
			}
		  }
		}
	}

	public double[] test(Tester tester, double[] instance) {
		int valueindex = 0;
		List<Bicluster> matches = new ArrayList<Bicluster>();
		List<Double> scores = new ArrayList<Double>();
		for(Map<Bicluster,List<Double>> rule : rules){ 
			for(Bicluster bic : rule.keySet()){ 
				if(tester.isSubset(bic,instance)){
					matches.add(bic);
					double value = rule.get(bic).get(valueindex);
					scores.add(value*tester.penalizedCount(bic, instance));
				}
			}
		}
		List<Bicluster> mismatches = new ArrayList<Bicluster>();
		List<Double> miscores = new ArrayList<Double>();
		return tester.test(matches,scores,mismatches,miscores,numClasses);
	}

	public void order(Order order){}

	public String rulesToString() {
		return null;
	}
	public String rulesToString(List<String> rows, List<String> columns) {
		return null;
	}
}
