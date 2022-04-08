package tests.cpam;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import cpam.BClassifier;
import cpam.tree.J48;
import generator.BicMatrixGenerator.PatternType;
import performance.classification.ClassifierEvaluation;
import performance.classification.domain.Results;
import utils.BicResult;
import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.SMO;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;


/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class FlebicSimpleTest {

	public static String path = "data/classifier/";
  
	public static void main(String[] args) throws Exception {
		//runSimpleConstant();
		runSimpleOrderPreserving();
	}	  

	public static void runSimpleOrderPreserving() throws Exception {
		Instances dataset = new Instances(new BufferedReader(new FileReader(path+"example_op.arff")));
		dataset.setClassIndex(dataset.numAttributes()-1);
		List<Classifier> classifiers = getClassifiers();
		classifiers.add(new BClassifier(PatternType.OrderPreserving));
		String[] evaluationOptions = new String[]{"CV"};
		Map<String,Results> res = ClassifierEvaluation.run(classifiers, dataset, evaluationOptions);
		BicResult.println(res.toString());
	}

	public static void runSimpleConstant() throws Exception {
		Instances dataset = new Instances(new BufferedReader(new FileReader(path+"example_constant.arff")));
		dataset.setClassIndex(dataset.numAttributes()-1);
		List<Classifier> classifiers = getClassifiers();
		classifiers.add(new BClassifier(PatternType.Constant,5));
		String[] evaluationOptions = new String[]{"CV"};
		Map<String,Results> res = ClassifierEvaluation.run(classifiers, dataset, evaluationOptions);
		BicResult.println(res.toString());
	}

	public static List<Classifier> getClassifiers() {
		List<Classifier> suite = new ArrayList<Classifier>();
		suite.add(new SMO());
		suite.add(new NaiveBayes());
		suite.add(new J48());
		suite.add(new RandomForest());
	    return suite;
	}
}
