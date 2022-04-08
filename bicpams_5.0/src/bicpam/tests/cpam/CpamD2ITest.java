package tests.cpam;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import cpam.BClassifier;
import cpam.tree.J48;
import domain.Dataset;
import performance.classification.ClassifierEvaluation;
import performance.classification.domain.Results;
import utils.BicPrinting;
import utils.BicResult;
import utils.WekaUtils;
import weka.classifiers.Classifier;
import weka.classifiers.bayes.BayesNet;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.Logistic;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.classifiers.functions.SMO;
import weka.classifiers.trees.LMT;
import weka.classifiers.trees.RandomForest;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.unsupervised.instance.RemoveWithValues;

/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class CpamD2ITest {

  public static String path = "data/rereunio/";
  
  public static void main(String[] args) throws Exception {
	for (Instances dataset : getRealDatasets()) {
		System.out.println(dataset.numAttributes());
		BicResult.println("\n===== "+dataset.relationName()+" index= "+dataset.classAttribute()+" c="+dataset.numClasses()+" n="+dataset.numInstances()+" m="+dataset.numAttributes()+" ====");
		Map<String,Results> res = test(getClassifiers(),dataset);
		BicResult.println(res.toString());
	}
  }
  
  public static Map<String,Results> test(List<Classifier> classifiers, Instances dataset) throws Exception {
	String[] evaluationOptions = new String[]{"CV"};//"-split-percentage","80"};//"-smooth","CV"};
	//new BClassifier().buildClassifier(dataset);
	//if(!BalanceData.isDataBalanced(dataset)) dataset = BalanceData.balanceDataset(dataset);
	return ClassifierEvaluation.run(classifiers, dataset, evaluationOptions);
  }

  public static List<Classifier> getClassifiers() {
	List<Classifier> suite = new ArrayList<Classifier>();
	suite.add(new BClassifier());
	//suite.add(new RandomForest());
	//suite.add(new SMO());
	/*suite.add(new J48());
	suite.add(new NaiveBayes());
	suite.add(new Logistic());*/
	//suite.add(new BayesNet());
	//suite.add(new MultilayerPerceptron());
	//suite.add(new BSigJ48());
    return suite;
  }

  public static List<Instances> getRealDatasets() throws Exception {
		List<Instances> datasets = new ArrayList<Instances>();
		String[] names = new String[] {
				//"kmeans", 
				//"quantile", "uniform",
				//"yeast_no_border_single_with_kol","yeast_no_border_single_without_kol", 
				//"yeast_no_border_whole_with_kol", "yeast_no_border_whole_without_kol",*/
				//"yeast_single_with_kol_v2","yeast_single_without_kol_v2", 
				//"yeast_whole_with_kol_v2", "yeast_whole_without_kol_v2"

				//"kmeans_v2",
				//"quantile_v2", "uniform_v2",
				"yeast_no_border_single_with_kol_v2", 
				//"yeast_no_border_single_without_kol_v2", 
				//"yeast_no_border_whole_with_kol_v2", "yeast_no_border_whole_without_kol_v2",
				//"yeast_single_with_kol", "yeast_single_without_kol", 
				//"yeast_whole_with_kol", "yeast_whole_without_kol",
		};
		for(String name : names){
			Instances dataset = new Instances(new BufferedReader(new FileReader(path+name+".arff"))); 
			dataset.setRelationName(name);
			if(dataset.classIndex()<0) dataset.setClassIndex(dataset.numAttributes()-1);
			/*System.out.println("a>"+dataset.numInstances());
			remove(new String[]{"EXC","VAC","POX","ERL"},dataset);
			System.out.println("b>"+dataset.numInstances());*/
			
			if(name.startsWith("example")) {
				Dataset data = new Dataset(dataset);
				data.roundValues();
				System.out.println("nrLabels="+data.nrLabels);
				datasets.add(WekaUtils.toDiscreteInstances(data));
			} else datasets.add(dataset); //preprocess(dataset));
		}
		return datasets;
	}
  
	private static void remove(String[] classes, Instances dataset) {
		//BicResult.println(data.toIntString());
		for(int l=dataset.numInstances(); l>0; l--) {
			Instance idata = dataset.instance(l-1);
			if(idata.classValue()>5) dataset.remove(l-1);
		}
	}

	private static void printStatistics(Dataset data) {
		//BicResult.println(data.toIntString());
		for(int j=0, l1=data.columns.size(); j<l1; j++){
			int[] counts = new int[data.nrLabels];
			for(int i=0, l2=data.rows.size(); i<l2; i++)
				counts[data.intscores.get(i).get(j)]++;
			BicResult.println("Att"+j+":"+BicPrinting.plot(counts));
		}
	}
}
