package tests.cpam;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import bicpam.mapping.Discretizer;
import bicpam.mapping.ItemMapper;
import bicpam.mapping.Itemizer.DiscretizationCriteria;
import bicpam.mapping.Itemizer.NoiseRelaxation;
import bicpam.mapping.Itemizer.NormalizationCriteria;
import bicpam.mapping.Normalizer;
import cpam.BClassifier;
import domain.Dataset;
import performance.classification.ClassifierEvaluation;
import performance.classification.domain.Results;
import tests.cpam.utils.ListClassifiers;
import utils.BicPrinting;
import utils.BicResult;
import utils.WekaUtils;
import weka.classifiers.Classifier;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.OptionHandler;

/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class CpamRealTest {

  public static String path = "data/classifier/";
  
  public static void main(String[] args) throws Exception {
	List<Map<String,Results>> results = getClassifiersPerformance();
	for(Map<String,Results> res : results) System.out.println(res.toString());
  }
			
  public static List<Map<String,Results>> getClassifiersPerformance() throws Exception {
	List<Map<String,Results>> results = new ArrayList<Map<String,Results>>();
	for (Instances dataset : getRealDatasets())
		for(Classifier clt : getClassifiers()) { 
	    	System.out.println("index="+dataset.classIndex());
			results.addAll(test(clt,dataset));
		}
	return results;
  }
  
  public static List<Map<String,Results>> test(Classifier classifier, Instances dataset) throws Exception {
	List<Map<String,Results>> results = new ArrayList<Map<String,Results>>();
	List<String[]> classifierOptions = ListClassifiers.getClassifierOptions(classifier);
	String[] evaluationOptions = new String[]{"-smooth","CV"};//"-split-percentage","80"};//"-smooth","CV"};"-bootstrap"};//
	System.out.println("\n"+dataset.relationName()+" :: "+classifier.getClass().toString());
	//if(!BalanceData.isDataBalanced(dataset)) dataset = BalanceData.balanceDataset(dataset);
    //BicResult.println(arffdata.toString());

    for (String[] classifierOption : classifierOptions) {
    	if(classifier instanceof OptionHandler) ((OptionHandler)classifier).setOptions(classifierOption);
    	results.add(ClassifierEvaluation.run(classifier, dataset, evaluationOptions));
    }
	return results;
  }

  public static List<Classifier> getClassifiers() {
	List<Classifier> suite = new ArrayList<Classifier>();
	suite.add(new BClassifier());
	//suite.add(new J48());
	//suite.add(new BSigJ48());
	//suite.add(new RandomForest());
    return suite;
  }

	public static List<Instances> getRealDatasets() throws Exception {
		List<String> names = new ArrayList<String>();
		List<Instances> datasets = new ArrayList<Instances>();
		//names.add("ColonDiff62x2000.txt");//atts with same name
		//names.add("Lymphoma45x4026L2.arff");
		names.add("Leukemia72x7129.arff");
		//names.add("Embryo60x7129.arff");
		//names.add(path+"GlobalCancer190x16063.arff");//L14 3Min with J48; good res
		//names.add(path+"Lymphoma96x4026L9.arff");
		//names.add(path+"Lymphoma96x4026L11.arff");
		for(String name : names){
			Instances dataset = new Instances(new BufferedReader(new FileReader(path+name))); 
			dataset.setClassIndex(dataset.numAttributes()-1);
			datasets.add(preprocess(dataset));
		}
		return datasets;
	}

	private static Instances preprocess(Instances dataset) throws Exception {
		int nrLabels = 5;
		List<Integer> removals = Arrays.asList(); 

		/*int[] classIndexes = new int[dataset.numInstances()];
		int numClasses = dataset.numClasses();
		for(int i=0; i<dataset.numInstances(); i++) classIndexes[i]=(int)dataset.instance(i).classValue();
		dataset.setClassIndex(0);
		dataset.deleteAttributeAt(dataset.numAttributes()-1);*/
		Dataset data = new Dataset(dataset);
		//data.classValues = classIndexes;
		
		data.nrLabels = nrLabels;
		//data = Normalizer.run(data,NormalizationCriteria.Row);
		data = Normalizer.run(data,NormalizationCriteria.Column);
		data = Discretizer.run(data,DiscretizationCriteria.NormalDist,NoiseRelaxation.None,data.nrLabels);
		for(int j=0, l1=data.columns.size(); j<l1; j++){
			int[] counts = new int[nrLabels];
			for(int i=0, l2=data.rows.size(); i<l2; i++)
				counts[data.intscores.get(i).get(j)]++;
			BicResult.println("Att"+j+":"+BicPrinting.plot(counts));
		}
		data = ItemMapper.remove(data,removals);
		Instances arffdata = WekaUtils.toDiscreteInstances(data);
		return arffdata;
	}
}
