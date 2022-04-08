package performance.classification.filter;

import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.supervised.instance.Resample;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;

/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class BalanceData {
	
	private static boolean printing = true;
	private static boolean reducesample = false;
	private static boolean resample = true;
	private static boolean replaceMissings = false;

	public static boolean isDataBalanced(Instances data) {
		int avgPerClass = data.numInstances()/data.numClasses();
		int[] classes = data.attributeStats(data.classIndex()).nominalCounts;
		double distance = 0;
		for(int i=0, l=data.numClasses(); i<l; i++)
			distance += Math.abs(classes[i]-avgPerClass)/avgPerClass;
		return distance < 0.5;
	}

	public static Instances balanceDataset(Instances input) throws Exception {
	  if(printing) System.out.println(printDataBalanceBasics(input));	  
	  if(reducesample) ;
		//input = createSubsample(input,0.8,30); //1st: bias to Uniform class, 2nd: sample size %
	  if(resample){
		Resample resample = new Resample();
		resample.setInputFormat(input);
		resample.setBiasToUniformClass(0.8);
		resample.setSampleSizePercent(50);
		input = new Instances(Filter.useFilter(input, resample));
	  }
	  if(replaceMissings){
		  ReplaceMissingValues filler = new ReplaceMissingValues();
		  filler.setInputFormat(input);
		  input = new Instances (Filter.useFilter(input, filler));
	  }
	  if(printing) System.out.println(printDataBalanceBasics(input));
	  return new Instances(input);
	}
	  
	  private static String printDataBalanceBasics(Instances input) {
		StringBuffer text = new StringBuffer("#"+input.numInstances()+"=>");
		int[] sum=new int[input.numClasses()];
		for(int j=0,l=input.numInstances();j<l;j++) sum[(int) input.instance(j).classValue()]++;
		for(int i=0,l=input.numClasses();i<l;i++) text.append(sum[i]+",");
		return text.toString();
	  }
}
