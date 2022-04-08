package tests.cpam.utils;

import java.util.ArrayList;
import java.util.List;
import weka.classifiers.Classifier;
import weka.classifiers.bayes.BayesNet;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.LinearRegression;
import weka.classifiers.functions.Logistic;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.classifiers.functions.SMO;
import weka.classifiers.functions.SMOreg;
import weka.classifiers.lazy.IBk;
import weka.classifiers.rules.DecisionTable;
import weka.classifiers.rules.PART;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.LMT;
import weka.classifiers.trees.RandomForest;


/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public final class ListClassifiers {

	  private static boolean single = true;
	  private static boolean categorical = true;
	  private static boolean numeric = false;
		
	  public static List<Classifier> getClassifiers() {
		List<Classifier> suite = new ArrayList<Classifier>();

		if(single){
			suite.add(new J48());
			return suite;
		}
		suite.add(new DecisionTable());
		suite.add(new IBk());
		if(categorical){
			suite.add(new J48());
			suite.add(new BayesNet()); 		
			suite.add(new NaiveBayes()); 
			suite.add(new PART());
			suite.add(new SMO());
			suite.add(new MultilayerPerceptron());
			suite.add(new LMT()); 
			suite.add(new RandomForest());
			suite.add(new Logistic());
		}
		if(numeric){
			//suite.add(new GaussianProcesses());
			suite.add(new SMOreg());
			suite.add(new LinearRegression());	
		}		
	    return suite;
	  }

	public static List<String[]> getClassifierOptions(Classifier classifier) {
		List<String[]> options = new ArrayList<String[]>();
		if(classifier.getClass().toString().contains("IBk")) options.add(new String[]{"-K","3"});		
		else if(classifier.getClass().toString().contains("HyperSeqClassifier")){
			options.add(new String[]{"1","2","0"});		
			options.add(new String[]{"2","1","0"});		
			options.add(new String[]{"0","1","2"});		
		}
		else options.add(new String[]{""});
		return options;
	}
	
}

