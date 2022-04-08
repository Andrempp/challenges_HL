package cpam;

import java.util.List;

import org.apache.commons.math3.stat.inference.TTest;

import domain.Dataset;
import utils.BicMath;
import utils.WekaUtils;
import weka.classifiers.Classifier;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;

public class DistClassifier implements Classifier {

	double[] mean, var, params, number;

	public class MyTTest extends TTest {
		public double tTest(double u1, double u2, double v1, double v2, double n1, double n2){
			return super.tTest(u1,u2,v1,v2,n1,n2);
		}
	}
	
	public DistClassifier(){}

	public void buildClassifier(Instances instances) throws Exception {
    	Dataset data = new Dataset(instances);
    	int numClasses = data.countClasses.length;
    	mean = new double[numClasses];
    	var = new double[numClasses];
    	number =  new double[numClasses];
        for(int k=0; k<numClasses; k++){
        	Dataset dataI = data.getPartition(k);
    		List<List<Double>> dataset = dataI.scores;
    		mean[k] = BicMath.meanL(dataset); 
    		var[k] = BicMath.varianceL(dataset);
    		number[k] = dataset.size()*data.columns.size();
        }
	}
	
	public double classifyInstance(Instance instance) throws Exception {
		return BicMath.maxindex(distributionForInstance(instance));
	}

	public double[] distributionForInstance(Instance instance) throws Exception {
		double[] vector = instance.toDoubleArray();
		double[] result = new double[number.length];
		double u2 = BicMath.mean(vector);
		double v2 = BicMath.variance(vector);
		double n2 = vector.length;
        for(int k=0, l=number.length; k<l; k++)
        	result[k]=new MyTTest().tTest(mean[k],u2,var[k],v2,number[k],n2);
    	return result;
	}
	
	public Capabilities getCapabilities() { return null; }
	//public abstract String getModelName();
	//public abstract boolean satisfiesHomogeneity(Bicluster bic);
	//public abstract boolean satisfiesSignificance(Bicluster bic);
}
