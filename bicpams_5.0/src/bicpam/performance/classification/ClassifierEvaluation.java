package performance.classification;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import performance.classification.domain.*;
import tests.cpam.utils.ListClassifiers;
import java.util.Random;
import utils.BicMath;
import utils.BicPrinting;
import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.OptionHandler;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.supervised.instance.Resample;

public class ClassifierEvaluation {
	
	public static int nrfolds = 10;
	public static boolean smooth = false;
	public static boolean printClassifier = false;

	public static Map<String,Results> run(Classifier classifier, Instances dataset, String[] options) throws Exception {
		return run(Arrays.asList(classifier), dataset, options);
	}
	public static Map<String,Results> run(List<Classifier> classifiers, Instances dataset, String[] options) throws Exception {
		Map<String,Results> results = new HashMap<String,Results>();
		smooth = Utils.getFlag("smooth",options);
        if(!Utils.getFlag("preserve-order", options)) dataset.randomize(new Random(1));
        System.out.println("=>"+dataset.get(0).toString());
		String split = Utils.getOption("split-percentage", options);
	    if (split.length()!=0) {	        
	    	double splitPercentage = Double.parseDouble(split);
	    	int splitfolds = (int)(100.0/(100.0-splitPercentage));
		    Instances train = dataset.trainCV(splitfolds, 0);
		    Instances test = dataset.testCV(splitfolds, 0);
		    System.out.println("#>"+splitfolds);
	        /*int trainSize = (int) Math.round(dataset.numInstances()*splitPercentage/100);
	        Instances train = new Instances(dataset, 0, trainSize);
	        Instances test = splitPercentage==100 ? new Instances(dataset,0,10) : new Instances(dataset, trainSize, dataset.numInstances()-trainSize);*/
	    	
	        Map<String,Result> result = evaluateModel(classifiers,train,test);
	        for(Entry<String, Result> entry : result.entrySet())
	        	results.put(entry.getKey(), new Results(entry.getKey(), entry.getValue(), smooth));
	        return results;
	    }
	    if(Utils.getFlag("bootstrap",options)) {
	    	for(Classifier clf : classifiers) {
	    		Results res = bootstrapModel(clf,dataset,nrfolds);
	    		results.put(res.id, res);
	    	}
	    	return results;
		}
	    else if(Utils.getFlag("resubstitution",options)) 
	    	return crossValidateModel(classifiers,dataset,dataset.numInstances());
	    return crossValidateModel(classifiers,dataset,nrfolds);
	}

	public static Results bootstrapModel(Classifier classifier, Instances dataset, int folds) throws Exception {
		System.out.println("bootstraping!");
	    Instances data = new Instances(dataset);
	    Results results = new Results(classifier.getClass().getName()+":"+dataset.relationName());
    	/*data.randomize(new Random());
    	double splitPercentage = 0.632;
        int trainSize = (int) Math.round(data.numInstances()*splitPercentage/100);*/

        int[] outMain = new int[data.numInstances()];
        int[] outOptimal = new int[data.numInstances()];        
    	double[] variance = new double[data.numInstances()];
    	int l=20;//data.numInstances();
	    for(int i=0; i<l; i++) {
	    	
	    	int[] inObserved = new int[folds];
	    	for(int j=0; j<folds; j++) {
			    Instance testi = (Instance) data.instance(j).copy();
			    
			    data.randomize(new Random());
			    Resample resample = new Resample();
		        resample.setInputFormat(dataset);
		        resample.setBiasToUniformClass(1);
	            Instances train = new Instances(Filter.useFilter(data,resample));
			    classifier.buildClassifier(train);
				inObserved[j] = (int) classifier.classifyInstance(testi);
		        //results.addNullAccuracy(evaluateModel(nullclassifier, train, test).getAccuracy());
	    	}
	    	int main=BicMath.mode(inObserved);
		    //System.out.println("In:("+main+")=>"+BicPrinting.plot(inObserved));
	    	int counts=0;
	    	for(int v : inObserved) if(v!=main) counts++;
	    	variance[i]=((double)counts)/(double)folds;
	    	outOptimal[i]=(int)data.instance(i).classValue();
	    	outMain[i]=main;
	    }
	    System.out.println("Variance:"+BicPrinting.plot(variance));
	    System.out.println("Modes:"+BicPrinting.plot(outMain));
	    System.out.println("Optimals:"+BicPrinting.plot(outOptimal));
	    
    	int counts=0;
    	for(int k=0; k<l; k++) if(outMain[k]!=outOptimal[k]) counts++;
    	double bias=((double)counts)/(double)l;
	    System.out.println("Avg.Variance="+BicMath.mean(Arrays.copyOfRange(variance,0,l)));
	    System.out.println("Avg.Bias="+bias);
	    results.calculateStatistics(smooth);
	    return results;
	}

	private static Map<String,Results> crossValidateModel(List<Classifier> classifiers, Instances dataset, int folds) throws Exception {
		System.out.println("#folds="+folds);
	    Instances data = new Instances(dataset);
        Map<String,Results> results = new HashMap<String,Results>();
	    for(int i=0; i<folds; i++) {
	      Instances train = data.trainCV(folds, i);
	      Instances test = data.testCV(folds, i);
	      
	      /*int[] counts = new int[dataset.numClasses()];
	      for(Instance instance : train.getInstances()) 
	    	  counts[(int)instance.classValue()]++;
	      System.out.println("=>"+BicPrinting.plot(counts));*/	      
	      //System.out.println("I:"+i+">"+test.instance(0).toDoubleArray()[0]);
	      //Classifier copiedClassifier = Classifier.makeCopy(classifier);
	      
	      Map<String,Result> res = evaluateModel(classifiers, train, test); 
	      for(Entry<String,Result> entry : res.entrySet()) {
	    	  String key = entry.getKey();
	    	  Result value = entry.getValue();
	    	  if(!results.containsKey(key)) results.put(key, new Results(key)); 
	    	  results.get(key).add(value);
	      }
	      /*results.addNullAccuracy(evaluateModel(nullclassifier, train, test).getAccuracy());
	      if(smooth==true){
	      	Results results2 = new Results(title);
		  	results2.calculateStatistics(false);
		    System.out.println("Raw Accuracy:\n"+results2.toString()+"\nSmoothed Accuracy");
	      }*/
	    }
	    for(Results entry : results.values())
	    	entry.calculateStatistics(smooth);
	    return results;
	}

	public static Map<String,Results> leaveOneOutModel(List<Classifier> classifiers, Instances dataset) throws Exception {
	    return crossValidateModel(classifiers, dataset, dataset.numInstances()); 
	}

	private static Map<String,Result> evaluateModel(List<Classifier> classifiers, Instances train, Instances test) throws Exception {
		Map<String,Result> results = new HashMap<String,Result>();
		for(Classifier classifier : classifiers) {
			List<String[]> classifierOptions = ListClassifiers.getClassifierOptions(classifier);
			int k=0;
			for(String[] classifierOption : classifierOptions) {
		    	if(classifier instanceof OptionHandler) ((OptionHandler)classifier).setOptions(classifierOption);
		    	String title = classifier.getClass().getName()+":params["+k+"]:"+train.relationName();
			    double trainTime = System.currentTimeMillis();
			    classifier.buildClassifier(train);
			    if(printClassifier) System.out.println(classifier.toString());
			    trainTime = System.currentTimeMillis() - trainTime;
			    Result result = new Result(train.numClasses(),smooth);
			    for(int i=0; i<test.numInstances(); i++){
				      double testTimeElapsed = System.currentTimeMillis();
				      Instance testi=(Instance) test.instance(i).copy();
				      if(smooth){
					      double[] prediction = classifier.distributionForInstance(testi);
					      System.out.println(BicPrinting.plot(prediction)+" <> real:"+testi.classValue());
					      result.add(prediction, testi.classValue());
				      } else {
					      double prediction = classifier.classifyInstance(testi);
					      System.out.println(prediction+" <> real:"+testi.classValue());
					      result.add(prediction, testi.classValue());
				      }
				      testTimeElapsed = System.currentTimeMillis() - testTimeElapsed;
				}
			    System.out.println();
			    results.put(title,result);
			}
		}
	    return results;
	}

}
