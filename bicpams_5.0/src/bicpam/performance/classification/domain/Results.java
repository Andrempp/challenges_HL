package performance.classification.domain;

import java.util.ArrayList;
import java.util.List;

/** Simple class that includes an array, whose elements are lists of evaluations.
 *  Used to compute means and standard deviations of multiple evaluations (e.g. cross-validation).
 *  @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class Results {

	public String id;
	public List<Result> evaluations;
	public List<String> names;
	public List<List<Double>> values;
	public List<Double> mean, std;
	public List<Double> nullaccuracy;

    /** Constructs a new object */
    public Results(String identifier) {
    	id = identifier;
        evaluations = new ArrayList<Result>();
        values = new ArrayList<List<Double>>();
        names = new ArrayList<String>();
        mean = new ArrayList<Double>();
        std = new ArrayList<Double>();
        nullaccuracy = new ArrayList<Double>();
    }
    
    /** Constructs a new object from a single estimate */
    public Results(String identifier, Result singleResult, boolean smooth) {
    	this(identifier);
    	add(singleResult);
    	calculateStatistics(smooth);
    }

    /** Constructs a new object with given array of evaluations and calculates statistics
     * @param someEvaluations
     */
    public void add(Result evaluation) {
        evaluations.add(evaluation);
    }

    /** Computes mean and standard deviation of all evaluation measures for cross-validation */
    public void calculateStatistics(boolean smooth) {
    	if(evaluations.size()==0) return;
		names = evaluations.get(0).getNames();
		//for(Result res : evaluations) System.out.println(res.confusionMatrixToString());
		for(String name : names){
			double sum=0;
			List<Double> value = new ArrayList<Double>();
			for(Result res : evaluations){
				res.smooth = smooth;
				double val = res.getValue(name);
				if(!Double.isNaN(val)){
					value.add(val);
					sum += val;
				}
			}
			values.add(value);
			mean.add(sum/value.size());
		}
		for(int i=0, l=names.size(); i<l; i++){
			double sum=0;
			for(Result res : evaluations) sum += Math.pow(res.getValue(names.get(i))-mean.get(i),2);
			std.add(Math.sqrt(sum/evaluations.size()));
		}
    }
    public List<Double> getAccuracy() {
    	return values.get(0);
    }
	public void addNullAccuracy(double accuracy) {
		nullaccuracy.add(accuracy);
	}
    public String toString() {
      String result=id+"\n";
      for(int i=0, l=names.size(); i<l; i++) result += toString(i); 
      return result;
    }

	public String toString(int i) {
	    String result=names.get(i) + "=" + ((int)(mean.get(i)*100)/100.0) + "\u00B1" + ((int)(std.get(i)*100)/100.0)+" {";
		for(Double val : values.get(i)) result += ((int)(val*100)/100.0) + ",";
		return result+"}\n";
	}

	public String toTableString(String metric) {
	    String result="";
		for(int i=0; i<names.size(); i++){
			if(names.get(i).contains(metric)){
				result += "\t" + names.get(i) + "\t" + ((int)(mean.get(i)*100)/100.0) + "\t" + ((int)(std.get(i)*100)/100.0) + "\t{";
				for(Double val : values.get(i)) result += ((int)(val*100)/100.0) + ",";
		    	result += "}\n";
		    }
		}
		return result;
	}
}
