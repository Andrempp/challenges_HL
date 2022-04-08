package performance.classification.statististics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import performance.classification.ClassifierEvaluation;
import utils.BicPrinting;
import weka.classifiers.Classifier;
import weka.core.Instances;


public class LCurvesBounds {

	public static double[][] getBounds(Classifier classifier, Instances dataset, String[] evaluationOption) throws Exception{
		int nrPoints = 5;
		int samples = 5;
		int[] size = new int[nrPoints];
		for(int j=0, i=20, add=(dataset.numInstances()-i)/(nrPoints-1); j<nrPoints; j++, i=i+add) size[j]=i;
		List<List<List<Double>>> allvalues = new ArrayList<List<List<Double>>>();
		for(int j=0; j<nrPoints; j++){
			List<List<Double>> values = new ArrayList<List<Double>>();
			for(int k=0; k<samples; k++){
				dataset.randomize(new Random());
				Instances subdata = new Instances(dataset);
				subdata.delete();
				for(int i=0, l=size[j]; i<l; i++) subdata.add(dataset.instance(i));
		    	List<Double> value = ClassifierEvaluation.leaveOneOutModel(classifier, subdata).values.get(0);
				Collections.sort(value);
		    	values.add(value);
			}
			allvalues.add(values);
		}
		//System.out.println(allvalues);
		double[][] result = new double[4][nrPoints];
		for(int j=0; j<nrPoints; j++){
			result[0][j]=size[j];
			result[1][j]=1-dist(allvalues.get(j));
			//result[2][j]=1-dist(allvalues.get(j),25);
			//result[3][j]=1-dist(allvalues.get(j),75);
		}
		//double[][] result = new double[][]{{10,20,30,40,50},{0.24,0.22,0.20,0.18,0.17}};
		System.out.println(BicPrinting.plot(result));
		estimateAndPlotLCurve(result);
		return result;
	}

	private static void estimateAndPlotLCurve(double[][] values) {
		double a, b, alpha;
		double min=1000, aR=-1, bR=-1, alphaR=-1;
		for(b=0; b<=0.2; b+=0.02){
			for(a=1.0; a<=2; a+=0.1){
				for(alpha=0; alpha<1; alpha+=0.05){
					double sum=0;
					for(int i=0, l=values[0].length; i<l; i++){
						//System.out.println(Math.log(a)+"|"+alpha*values[0][i]+"|"+Math.log(b-values[1][i]));
						sum += Math.pow(a*Math.pow(values[0][i],-alpha)+b-values[1][i], 2);
						//sum += Math.pow(Math.log(a)-alpha*values[0][i]+Math.log(b-values[1][i]), 2);
					}
					//System.out.println(sum +"=>"+a+","+alpha+","+b);
					if(sum<min){
						min=sum;
						aR=a; bR=b; alphaR=alpha;
					}
				}
			}
		}
		System.out.println("CURVE a:"+aR+",alpha:"+alphaR+",b:"+bR);
	}

	public static double dist(List<List<Double>> values, int percentil) {
		double result = 0;
		for(List<Double> value : values){
			int estimatorIndex = (int) (value.size()/((double) 100.0/(double)percentil))-1;
			result += value.get(estimatorIndex);
		}
		return result/(double)values.size();
	}
	public static double dist(List<List<Double>> values) {
		double result = 0;
		for(List<Double> value : values){
			double avg = 0;
			for(Double val : value) avg += val;
			result += avg/value.size();
		}
		return result/(double)values.size();
	}
	
}
