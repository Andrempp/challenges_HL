package tests.cpam;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import bicpam.mapping.Discretizer;
import bicpam.mapping.ItemMapper;
import bicpam.mapping.Itemizer.DiscretizationCriteria;
import bicpam.mapping.Itemizer.NoiseRelaxation;
import bicpam.mapping.Itemizer.NormalizationCriteria;
import bicpam.mapping.Normalizer;
import cpam.BClassifier;
import cpam.tree.J48;
import domain.Dataset;
import performance.classification.ClassifierEvaluation;
import performance.classification.domain.Results;
import tests.cpam.utils.ListClassifiers;
import utils.BicPrinting;
import utils.BicResult;
import utils.WekaUtils;
import weka.attributeSelection.InfoGainAttributeEval;
import weka.attributeSelection.Ranker;
import weka.classifiers.Classifier;
import weka.classifiers.bayes.BayesNet;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.SMO;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.OptionHandler;
import weka.filters.Filter;
import weka.filters.supervised.attribute.AttributeSelection;

/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class CpamRuiTest {

  public static String path = "data/classifier/";
  
  public static void main(String[] args) throws Exception {
	for (Instances dataset : getRealDatasets()) {
		AttributeSelection selector = new AttributeSelection();
		InfoGainAttributeEval evaluator = new InfoGainAttributeEval();
		Ranker ranker = new Ranker();
		ranker.setThreshold(0.07);//setNumToSelect(1000);
		selector.setEvaluator(evaluator);
		selector.setSearch(ranker);
		selector.setInputFormat(dataset); 
		String name = dataset.relationName();
		dataset = Filter.useFilter(dataset, selector);
		dataset.setRelationName(name);
		System.out.println(dataset.numAttributes());
		//selector.SelectAttributes(dataset); 
		//int[] attIndex = selector.selectedAttributes();
		//System.out.println(attIndex.length);
		//double[][] attIndex2 = selector.rankedAttributes();
		//BicResult.println(BicPrinting.plot(attIndex2));
		BicResult.println("\n===== "+dataset.relationName()+" index= "+dataset.classAttribute()+" c="+dataset.numClasses()+" n="+dataset.numInstances()+" m="+dataset.numAttributes()+" ====");
    	//BicResult.println("index="+dataset.classIndex());
		Map<String,Results> res = test(getClassifiers(),dataset);
		BicResult.println(res.toString());
	}
  }
  
  public static Map<String,Results> test(List<Classifier> classifiers, Instances dataset) throws Exception {
	String[] evaluationOptions = new String[]{"CV"};//-split-percentage","80"};//"-smooth","CV"};"-bootstrap"};//
	//new BClassifier().buildClassifier(dataset);
	//if(!BalanceData.isDataBalanced(dataset)) dataset = BalanceData.balanceDataset(dataset);
	//return null;
	return ClassifierEvaluation.run(classifiers, dataset, evaluationOptions);
  }

  public static List<Classifier> getClassifiers() {
	List<Classifier> suite = new ArrayList<Classifier>();
	suite.add(new BClassifier());
	//suite.add(new SMO());
	//suite.add(new NaiveBayes());
	//suite.add(new J48());
	//suite.add(new BSigJ48());
	//suite.add(new RandomForest());
    return suite;
  }

  public static List<Instances> getRealDatasets() throws Exception {
		List<String> names = new ArrayList<String>();
		List<Instances> datasets = new ArrayList<Instances>();
		//names.add("example_constant.txt");
		names.add("ColonDiff62x2000.txt");//atts with same name
		//names.add("Lymphoma45x4026L2.arff");
		//names.add("Leukemia72x7129.arff");
		//names.add("Embryo60x7129.arff");
		/*names.add("GlobalCancer190x16063.arff");//L14 3Min with J48; good res
		names.add("Lymphoma96x4026L9.arff");
		names.add("Lymphoma96x4026L11.arff");*/
		for(String name : names){
			Instances dataset = new Instances(new BufferedReader(new FileReader(path+name)));
			System.out.println("A====>"+dataset.classIndex());
			if(dataset.classIndex()<0) dataset.setClassIndex(dataset.numAttributes()-1);
			if(name.startsWith("example")) {
				Dataset data = new Dataset(dataset);
				data.roundValues();
				System.out.println("nrLabels="+data.nrLabels);
				datasets.add(WekaUtils.toDiscreteInstances(data));
			} else datasets.add(dataset); //preprocess(dataset));
		}
		return datasets;
	}

	private static Instances preprocess(Instances dataset) throws Exception {
		
		/** A: PARAMS **/
		boolean verbose = false;
		int nrLabels = 4;
		List<Integer> removals = Arrays.asList(); 
		
		/** B: PREPROCESS **/
		Dataset data = new Dataset(dataset);
		BicResult.println(BicPrinting.plot(data.countClasses));
		data.nrLabels = nrLabels;
		data = Normalizer.run(data,NormalizationCriteria.Column);
		data = Discretizer.run(data,DiscretizationCriteria.NormalDist,NoiseRelaxation.None,data.nrLabels);
		if(verbose) printStatistics(data);
		data = ItemMapper.remove(data,removals);
		Instances arffdata = WekaUtils.toDiscreteInstances(data);
		if(verbose) BicResult.println(BicPrinting.printInstances(arffdata));
		return arffdata;
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
