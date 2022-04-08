package cpam.structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import utils.BicMath;
import cpam.BClassifier.DiscMetric;
import cpam.DiscMetrics;
import cpam.Learner;
import cpam.Tester;
import domain.Bicluster;
import domain.Biclusters;
import domain.Biclusters.Order;
import domain.Dataset;

public class CMAR extends Learner {

	public int numRows, numClasses;
	public List<Bicluster> rules;

	public CMAR(Biclusters[] bics, Dataset data, DiscMetric metric){
		numRows = data.rows.size();
		numClasses = data.numClasses();
		rules = new ArrayList<Bicluster>();
		for(Biclusters bicsI : bics) {
		  for(Bicluster bic : bicsI.getBiclusters()){
			bic.chi2=DiscMetrics.getChi2(bic, data);
			if(bic.chi2<DiscMetrics.THRESHOLD_10) continue;
			bic.chi2UB=DiscMetrics.getChi2UpperBound(bic, data);
			
			if(rules.isEmpty()) rules.add(bic);
			else if(moreGeneralRuleExists(bic)) continue; 
			else if(insertWithPriority(bic)) continue;
			else rules.add(bic);
		  }
		}
	}

	public double[] test(Tester tester, double[] instance){
    	double[] classesprob = new double[numClasses];
		List<Bicluster> match = new ArrayList<Bicluster>();
		for(Bicluster rule : rules) 
		    if(tester.isSubset(rule,instance)) match.add(rule);
		if(match.isEmpty()) {
			Random r = new Random();
			for(int i=0; i<numClasses; i++) classesprob[i]=1.0+0.1*r.nextDouble();
			double sum = BicMath.sum(classesprob);
			for(int i=0; i<numClasses; i++) classesprob[i]=classesprob[i]/sum;
		} else {
			for(Bicluster bic : match)
				classesprob[bic.condition] += (bic.chi2*bic.chi2)/bic.chi2UB;
		}
		return classesprob;
	}
	
	protected boolean moreGeneralRuleExists(Bicluster bic) {
		for(Bicluster ibic : rules)
			if(bic.condition==ibic.condition && bic.numColumns()<ibic.numColumns()){
		    	if(bic.conf>ibic.conf) return true;
		        if(bic.conf==ibic.conf && bic.numRows()>ibic.numRows()) return true;
			}
		return false;
	}
	
	protected boolean insertWithPriority(Bicluster bic) {
		boolean cmarGreater = false;
		for(int i=0, s=rules.size(); i<s; i++){
			Bicluster ibic = rules.get(i);
	    	if(bic.conf>ibic.conf) cmarGreater=true;
	    	else if(bic.conf==ibic.conf){
	    		if(bic.numRows()>ibic.numRows()) cmarGreater=true;
	    		else if(bic.numRows()==ibic.numRows()) cmarGreater=bic.numColumns()<ibic.numColumns();
	    	}
	    	if(cmarGreater){
				rules.add(i,bic);
				break;
	    	}
		}
		return cmarGreater;
	}
	
	public void order(Order order){}
	public String rulesToString() {
		return null;
	}
	public String rulesToString(List<String> rows, List<String> columns) {
		return null;
	}
}
