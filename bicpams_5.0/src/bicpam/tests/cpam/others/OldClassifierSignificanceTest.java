package tests.cpam.others;

import java.util.List;
import java.util.Map;

import performance.classification.ClassifierEvaluation;
import performance.classification.domain.Results;
import performance.classification.statististics.AllStatistics;
import performance.classification.statististics.BiasVariancePerformance;
import performance.classification.statististics.PerformanceBounds;
import tests.cpam.utils.ClassificationUtils;
import tests.cpam.utils.ListClassifiers;
import tests.cpam.utils.ListDatasets;
import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.OptionHandler;


/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class OldClassifierSignificanceTest {

  public static void main(String[] args) throws Exception {
	//List<Results> results = new ArrayList<Results>();
	
	boolean synthetic = true;
	boolean bounds = false;
	boolean sampling = false;
	boolean significance = false;
	boolean bias = true;
	boolean accuracyonly = true;
	  
	// PARAMETERS  
	List<Classifier> classifiers = ListClassifiers.getClassifiers();
	List<Instances> datasets;
	if(synthetic) datasets = ListDatasets.getSyntheticDatasets();
	else datasets = ListDatasets.getRealDatasets();
	List<String[]> samplingOptions = ClassificationUtils.getSamplingOptions(sampling);
	for(int i=0, l=samplingOptions.get(0).length; i<l; i++) System.out.print(samplingOptions.get(0)[i]+";");
	List<Integer> lossOptions = ClassificationUtils.getLossOptions();  

	for(Classifier classifier : classifiers){
		System.out.println(classifier.getClass().toString());
		List<String[]> classifierOptions = ClassificationUtils.getClassifierOptions(classifier);
  
		//Build tests for different datasets
		for (Instances dataset : datasets) {
		    //dataset = new Instances(dataset,0,300);
			System.out.println(">>"+dataset.relationName()+" ("+dataset.numInstances()+"x"+dataset.numAttributes()+")");
	    	if(dataset.relationName().contains("heritage")){
	    		dataset.setClassIndex(122);
	    		FastVector vector = new FastVector(); 
	    		vector.addElement("true"); 
	    		vector.addElement("false");
	    		dataset.insertAttributeAt(new Attribute("class",vector), dataset.numAttributes());
	    		for(int i=0, l=dataset.numInstances(), s=dataset.numAttributes()-1; i<l; i++){
	    			Instance inst = dataset.instance(i);
	    			boolean value = false;
	    			for(int j=70; j<78; j++) if(inst.value(j)>1){ value=true; break; }
	    			if(value) inst.setValue(s, "true");
	    			else inst.setValue(s, "false");
	    		}
	    		dataset.setClassIndex(dataset.numAttributes()-1);
	    		//System.out.println(dataset.toSummaryString());
		    	dataset = ClassificationUtils.balanceData(dataset);
	    	} else {
	    		if(dataset.classIndex()<0) dataset.setClassIndex(dataset.numAttributes()-1);
			    //if(ClassificationUtils.isDataBalanced(dataset)) 
			    	//dataset = ClassificationUtils.balanceData(dataset);
	    	}
		    //dataset.randomize(new Random());
		    if(dataset.classAttribute().isNominal()) dataset.stratify(ClassifierEvaluation.nrfolds);
			lossOptions = ClassificationUtils.getLossOptions(dataset.numClasses(),accuracyonly);  
		          
		    //Build classifiers with different options
		    for (String[] classifierOption : classifierOptions) {
		    	((OptionHandler) classifier).setOptions(classifierOption);
		      
		    	//A: SAMPLING CHOICE
		    	for(String[] samplingOption : samplingOptions){
	    	    	System.out.print(samplingOption[1]+":");
		    		Map<String,Results> results = ClassifierEvaluation.run(classifier, dataset, samplingOption.clone());
		    		for(Results res : results.values()) {
		    	    	//System.out.println(res.toString());
		    	    	//for(String sname : res.name) System.out.println(sname); 
		    	    	//for(List<Double> sample : res.values) System.out.println(sample); 
		    	    	
		    	    	//B: LOSS FUNCTIONS
		    	    	for(int lossOption : lossOptions){ 
		    	    		//System.out.println(res.values.get(lossOption));
		    	    		if(bounds){
		    	    			double[] pbounds = PerformanceBounds.run(res.values.get(lossOption));
		    	    			System.out.println("Bounds:"+res.names.get(lossOption)+" ["+pbounds[0]+","+pbounds[2]+"]("+pbounds[1]+")");
		    	    		}
		    	    		if(significance){
		    		    		double[] conf = AllStatistics.run(classifier, dataset, samplingOption);
		    		    		System.out.println("sig views:{"+conf[0]+","+conf[1]+","+conf[2]+"}");
		    	    		}
		    	    		if(bias){
		    		    		double[] varbias = BiasVariancePerformance.run(classifier, dataset, samplingOption);
		    		    		System.out.println("variance:"+varbias[0]+", bias:"+varbias[1]);	    	    			
		    	    		}
		    	    	}
		    		}
	    	    	
		    		//LCurvesBounds.getBounds(classifier, dataset, evaluationOption);
		    		//double conf = Bounds.getConfidence(classifier, dataset, evaluationOption);
		    		//System.out.println("Conf:"+conf);
		    	}
		    }
		}
	}
  }	
}

