package tests.cpam.utils;

import java.util.ArrayList;
import java.util.List;

import performance.classification.filter.Compensation;
import weka.classifiers.Classifier;
import weka.core.Instances;

public class ClassificationUtils {

	public static List<String[]> getSamplingOptions(boolean sampling) {
		List<String[]> options = new ArrayList<String[]>();
		options.add(new String[]{"-preserve-order","CV"});//});//"-x","4","-i","-k""-split-percentage","80",
		if(sampling) {
			options.add(new String[]{"-preserve-order","resubstitution"});
			options.add(new String[]{"-preserve-order","bootstrap"});
		}
		return options;
	}

	public static boolean isDataBalanced(Instances data) {
		int avgPerClass = data.numInstances()/data.numClasses();
		int[] classes = data.attributeStats(data.classIndex()).nominalCounts;
		double distance = 0;
		for(int i=0, l=data.numClasses(); i<l; i++)
			distance += Math.abs(classes[i]-avgPerClass)/avgPerClass;
		return distance < 0.5;
	}

	public static List<String[]> getClassifierOptions(Classifier classifier) {
		List<String[]> options = new ArrayList<String[]>();
		options.add(new String[]{""});
		return options;
	}

	public static Instances balanceData(Instances dataset) throws Exception {
		return Compensation.balanceDataset(dataset);
	}

	public static List<Integer> getLossOptions() {
		return getLossOptions(2,false);
	}

	public static List<Integer> getLossOptions(int numClasses, boolean accuracyonly) {
		int j=0;
		List<Integer> lossoptions = new ArrayList<Integer>();
		lossoptions.add(j++); //accuracy
		
		if(accuracyonly) return lossoptions;
		for(int i=0; i<numClasses; i++){
			lossoptions.add(j++); //sensitivity
			lossoptions.add(j++); //specificity
		}
		return lossoptions;
	}

}
