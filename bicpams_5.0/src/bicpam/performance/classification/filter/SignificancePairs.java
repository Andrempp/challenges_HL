package performance.classification.filter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import performance.classification.statististics.SuperiorityTest;

public class SignificancePairs {

	public List<String> classifiers;
	public Map<String,List<String>> comparisons;
	public Map<String,List<List<Double>>> metricsValues;
	public Map<String,List<String>> metricsNames;
	public Map<String,List<List<Double>>> pvalues;
	
	public SignificancePairs() {
		classifiers = new ArrayList<String>();
		comparisons = new HashMap<String,List<String>>();
		metricsNames = new HashMap<String,List<String>>();
		metricsValues = new HashMap<String,List<List<Double>>>();
		pvalues = new HashMap<String,List<List<Double>>>();
	}

	public void calculateAllSignificances(){
		for(String cl1: classifiers){
        	List<String> clnames = new ArrayList<String>();
			for(String cl2: classifiers)
				if(cl1!=cl2) clnames.add(cl2);
			comparisons.put(cl1, clnames);
		}
		calculateSignificance();
	}

	public void calculateSignificance() {
		for(String cl1 : classifiers){
			List<List<Double>> comppvalues = new ArrayList<List<Double>>();
			for(String cl2 : comparisons.get(cl1)){
				List<Double> clpvalues = new ArrayList<Double>();
				for(int i=0, l=metricsNames.get(cl1).size(); i<l; i++) {
					clpvalues.add(SuperiorityTest.pvalueForDifferentPerformance(metricsValues.get(cl1).get(i), metricsValues.get(cl2).get(i)));
				}
				comppvalues.add(clpvalues);
			}
			pvalues.put(cl1,comppvalues);
		}
	}
	
	public String toString() {
		StringBuffer result = new StringBuffer();
		for(String cl : classifiers){
			for(int i=0, l1=comparisons.get(cl).size(); i<l1; i++){
				result.append("\nComparing("+cl+","+comparisons.get(cl).get(i)+")->[");
				for(int j=0, l2=metricsNames.get(cl).size(); j<l2; j++){
					result.append(metricsNames.get(cl).get(j)+":"+pvalues.get(cl).get(i).get(j)+",");
				}
				result.setCharAt(result.length()-1, ']');
			}
		}
		return result.toString();
	}
}
