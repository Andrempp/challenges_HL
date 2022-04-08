package tests.cpam;

import generator.ClassifierGenerator;
import generator.DefaultDatasets;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import performance.classification.ClassifierEvaluation;
import performance.classification.domain.Result;
import performance.classification.domain.Results;
import domain.Dataset;
import tests.cpam.utils.ListClassifiers;
import tests.cpam.utils.ListDatasets;
import weka.classifiers.Classifier;
import weka.core.Instances;


/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class CpamGeneratorTests {

  public static void main(String[] args) throws Exception {
	minitest();
	fulltest();
  }

  public static void fulltest() throws Exception {
	DefaultDatasets defaultdata = new DefaultDatasets();
	List<Dataset> datasets = defaultdata.getClassifierSyntheticData();
	for(Dataset dataset : datasets)
		ListDatasets.convertToArff(dataset); //you can choose to print data!
  }


  public static void minitest() throws Exception {
	int numClasses = 4;
	ClassifierGenerator gen = new ClassifierGenerator(100,10,numClasses,new double[]{0,1,2,3},new double[]{1,1,1,1});
	gen.run();
	//core(ListDatasets.convertToArff(gen.run()));
	gen.setNoise(2);
	gen.run();
	//core(convertToArff(gen.run()));
	gen.setNoise(0);
	gen.setImbalance(0.5);
	gen.run();
	//core(ListDatasets.convertToArff(gen.run()));
	gen.setImbalance(0);
	gen.setSkewedFeatures(1);
	gen.run();
	//core(convertToArff(gen.run()));
  }

  public static void core(Instances dataset) throws Exception {
	for(Classifier classifier : ListClassifiers.getClassifiers()){
		System.out.println(">>"+dataset.relationName());
		dataset.randomize(new Random());
    	dataset.setClassIndex(dataset.numAttributes()-1);
    	String[] evaluationOption = new String[]{"-preserve-order","-i","-k","-v"};
    	Map<String,Results> res = ClassifierEvaluation.run(classifier, dataset, evaluationOption);
    	System.out.println(res.toString());
    }
  }
}

