package cpam;

import java.util.ArrayList;
import java.util.List;
import utils.BicMath;
import utils.BicPrinting;
import weka.classifiers.Classifier;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;

public class EnsembleClassifier implements Classifier {

	List<Classifier> learners;
	int numClasses;

	public EnsembleClassifier(){
		learners = new ArrayList<Classifier>();
		learners.add(new BClassifier());
		learners.add(new DistClassifier());
	}

	public void buildClassifier(Instances instances) throws Exception {
		numClasses = instances.numClasses();
		for(Classifier learner : learners)
			learner.buildClassifier(instances);
	}
	
	public double classifyInstance(Instance instance) throws Exception {
		return BicMath.maxindex(distributionForInstance(instance));
	}

	public double[] distributionForInstance(Instance instance) throws Exception {
		double[] result = new double[numClasses];
		double[] localresult = learners.get(0).distributionForInstance(instance);
		for(int i=0, l=localresult.length; i<l; i++)
			if(Double.isInfinite(localresult[i])) localresult[i] = 1000;
		double[] globalresult = learners.get(1).distributionForInstance(instance);
		double additivefactor = BicMath.sum(localresult)/((double)(numClasses*2));
		for(int i=0, l=result.length; i<l; i++)	result[i] = localresult[i]+additivefactor;
		double localsum = BicMath.sum(result), globalsum = BicMath.sum(globalresult);
		for(int i=0, l=result.length; i<l; i++)
			result[i] = (result[i]/localsum + globalresult[i]/globalsum)/2;
		if(((int)BicMath.maxindex(result)) != ((int)instance.classValue())){ 
			System.out.println("Local:"+BicPrinting.plot(localresult));
			System.out.println("Global:"+BicPrinting.plot(globalresult));
	    	System.out.println("Final:"+BicPrinting.plot(result)+">>>>>"+instance.classValue());
		}
    	return result;
	}
	
	public Capabilities getCapabilities() { return null; }
	
	//public abstract String getModelName();
	//public abstract boolean satisfiesHomogeneity(Bicluster bic);
	//public abstract boolean satisfiesSignificance(Bicluster bic);
}
