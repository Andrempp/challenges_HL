package performance.classification.domain;

import java.util.ArrayList;
import java.util.List;

import utils.BicMath;
import utils.BicPrinting;

/**
 * @author Rui Henriques
 */
public class Result {

	public int nrLabels;
	public boolean smooth = false;
	public int[][] confusionmatrix;
	public double[][] smoothmatrix;
	public int nrInstances = 0;
	public List<String> names;

	public Result(int labels, boolean _smooth) throws Exception {
		smooth = _smooth;
		nrLabels = labels;
		confusionmatrix = new int[nrLabels][nrLabels];
		smoothmatrix = new double[nrLabels][nrLabels]; 
		names = new ArrayList<String>();
		names.add("Accuracy");
		/*if(nrLabels==2) {
			names.add("Sensitivity");
			//names.add("Specificity");
		} else {*/
			for(int i=0; i<nrLabels; i++){
				names.add("Sensitivity::L"+i);
				names.add("Specificity::L"+i);
				names.add("Precision::L"+i);
				names.add("F-Measure::L"+i);
				names.add("G-Measure::L"+i);
			}
		//}
	}
	public List<String> getNames() {
		return names;
	}
	public void add(double prediction, double observed){
		confusionmatrix[(int)observed][(int)prediction]++;
		nrInstances++;
	}
	public void add(double[] prediction, double observed) {
		prediction = normalize(prediction);
		confusionmatrix[(int)observed][(int)BicMath.maxindex(prediction)]++;
		for(int i=0, l=prediction.length; i<l; i++)
			smoothmatrix[(int)observed][i]+=prediction[i];
		nrInstances++;
	}
	private double[] normalize(double[] prediction) {
		double min = BicMath.min(prediction);
		if(min<0) for(int i=0,l=prediction.length; i<l; i++) prediction[i]-=min;
		double sum = BicMath.sum(prediction);
		for(int i=0,l=prediction.length; i<l; i++) prediction[i]/=sum;
		return prediction;
	}
	public double getValue(String name) {
		if(!name.contains("::")) return getAccuracy();
		int index = Integer.valueOf(name.split("::L")[1]);
		if(name.startsWith("Sensitivity")) return getSensitivity(index);
		if(name.startsWith("Specificity")) return getSpecificity(index);
		if(name.startsWith("Precision")) return getPrecision(index);
		if(name.startsWith("F-Measure")) return getFMeasure(index);
		if(name.startsWith("G-Measure")) return getGMeasure(index);
		else return -1;
	}
	public void reset(){
		confusionmatrix = new int[nrLabels][nrLabels];
		smoothmatrix = new double[nrLabels][nrLabels];
		nrInstances = 0;
	}
	public double getAccuracy(){
		double sum = 0;
		if(smooth){
			for(int i=0; i<nrLabels; i++)
				sum += smoothmatrix[i][i];
			return sum/BicMath.sum(smoothmatrix);
		}
		for(int i=0; i<nrLabels; i++)
			sum += confusionmatrix[i][i];
		return sum/BicMath.sum(confusionmatrix);
	}
	public double getSensitivity(int i){
		if(smooth){
			return (smoothmatrix[i][i])/(BicMath.sum(smoothmatrix[i]));
		} else 	return ((double)confusionmatrix[i][i])/(BicMath.sum(confusionmatrix[i]));
	}
	public double getPrecision(int i){
		double sum=0;
		if(smooth){
			for(int j=0; j<nrLabels; j++) sum+=smoothmatrix[j][i];
			return (smoothmatrix[i][i])/sum;
		} else {
			for(int j=0; j<nrLabels; j++) sum+=confusionmatrix[j][i];
			return ((double)confusionmatrix[i][i])/sum;
		}
	}
	public double getFMeasure(int i){
		double recall = getSensitivity(i);
		double precision = getPrecision(i);
		if((precision+recall)==0) return 0;
		return 2*(precision*recall)/(precision+recall);
	}
	public double getGMeasure(int i){
		double recall = getSensitivity(i);
		double precision = getPrecision(i);
		return Math.sqrt(precision*recall);
	}
	public double getSpecificity(int i){
		double sum=0, denominator=0;
		for(int j=0; j<nrLabels; j++){
			if(i==j) continue;
			if(smooth){
				sum += smoothmatrix[j][j];
				denominator += BicMath.sum(smoothmatrix[j]);
			} else {
				sum += confusionmatrix[j][j];
				denominator += BicMath.sum(confusionmatrix[j]);
			}
		}
		return sum/denominator;
	}
	public String toString() {
		String result = "--";
		for(String name : names) result += name + "=" + getValue(name) + ";";
		return result;
	}
}
