package tests.cpam;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import bicpam.bicminer.BiclusterMiner;
import bicpam.bicminer.BiclusterMiner.Orientation;
import bicpam.bicminer.constant.ConstantBiclusterMiner;
import bicpam.closing.BiclusterMerger;
import bicpam.closing.Biclusterizer;
import bicpam.mapping.Discretizer;
import bicpam.mapping.ItemMapper;
import bicpam.mapping.Itemizer.DiscretizationCriteria;
import bicpam.mapping.Itemizer.NoiseRelaxation;
import bicpam.mapping.Itemizer.NormalizationCriteria;
import bicpam.mapping.Normalizer;
import bicpam.pminer.fim.ClosedFIM;
import cpam.BClassifier;
import domain.Bicluster;
import domain.Biclusters;
import domain.Dataset;
import performance.classification.ClassifierEvaluation;
import performance.classification.domain.Results;
import utils.BicMath;
import utils.BicPrinting;
import utils.BicReader;
import utils.BicResult;
import utils.WekaUtils;
import utils.others.CopyUtils;
import weka.classifiers.Classifier;
import weka.classifiers.bayes.BayesNet;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.classifiers.functions.SMO;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class CpamHealthData {

  public static String hpath = "data/health/";
  public static String cpath = "data/classifier/";
  
  public static void main(String[] args) throws Exception {
	for (Instances dataset : getRealDatasets()){
		BicResult.println("\n===== "+dataset.relationName()+" c="+dataset.numClasses()+" n="+dataset.numInstances()+" m="+dataset.numAttributes()+" ====");
		//new BClassifier().buildClassifier(dataset);
		//for(int i=0, l=dataset.numAttributes(); i<l; i++) BicResult.print(""+dataset.attributeStats(i));
		String[] evaluationOptions = new String[]{"CV"};//-split-percentage","80"};//"CV"};//"-smooth","-bootstrap"};//
		//if(!BalanceData.isDataBalanced(dataset)) dataset = BalanceData.balanceDataset(dataset);
	    //BicResult.println(arffdata.toString());
		Map<String,Results> results = ClassifierEvaluation.run(getClassifiers(), dataset, evaluationOptions);
		BicResult.println("==== "+dataset.relationName()+" ====\n"+dataset.classAttribute()+"\n"+results.toString());
	}
  }

  public static List<Instances> getRealDatasets() throws Exception {
	List<String> names = new ArrayList<String>();
	List<Integer> classes = new ArrayList<Integer>();
	List<Integer> remAtt = new ArrayList<Integer>();
	List<Integer> remVal = new ArrayList<Integer>();
	List<Instances> datasets = new ArrayList<Instances>();

	names.add(hpath+"lung-cancer.data");//m=57nomial (well-distributed) c=3[0] n=32
	classes.add(0); remAtt.add(null); remVal.add(null); //yes but 0.4 few bics
	names.add(hpath+"post-operative.data");//m=9nominal (well-distributed) c=3[last] n=90
	classes.add(-1); remAtt.add(null); remVal.add(null); //0.8 imbalance YES
	names.add(hpath+"hepatitis.data");//m=20mix c=2[0] n=155 OP on 5 attributes <<<
	classes.add(0); remAtt.add(null); remVal.add(null); //0.8 and high imbalance
	names.add(hpath+"heart.dat");//m=13r c=2[last] n=270 major nominal
	classes.add(-1); remAtt.add(null); remVal.add(null); //0.8 ok YES =)
	names.add(hpath+"heart/processed.cleveland.data"); //m=13r c=5[last] n=300 3-4 nominal atts
	classes.add(-1); remAtt.add(null); remVal.add(null); //0.6 only c0 with disc bics
	names.add(hpath+"heart/processed.hungarian.data"); //m=13r c=5[last] n=300 YES =) 3-4 nominal atts
	classes.add(-1); remAtt.add(null); remVal.add(null); //0.83
	names.add(hpath+"mammographic_masses.data");//m=5nominal c=2[last|remove att1] n=961
	classes.add(-1); remAtt.add(1); remVal.add(null); //0.8 imbalance
	names.add(hpath+"breast-tissue.data");//m=9r c=6[att1|remove att0] n=106 => real <<<
	classes.add(1); remAtt.add(0); remVal.add(null);
	//names.add(cpath+"Lymphoma45x4026L2.arff");//m=9r c=6[att1|remove att0] n=106
	//classes.add(-1); remAtt.add(null); remVal.add(null);

	/*names.add("arcene.data"); //m=10000r 7k good 3k fake (0 as uninformative) c=[last] n=100
	classes.add(-1); remAtt.add(null); remVal.add(0);
	names.add("lsvt.data");//m=314r c=2[att3|remove att1] n=126 => att0 with multiple patient id entries
	classes.add(3); remAtt.add(1); remVal.add(null); //>>0.3
	names.add("arrhythmia.data"); //m=280r (att1 binary; val 0 as uninformative) c=16[last] n=452 att0,att2,att3 can be removed
	classes.add(1); remAtt.add(null); remVal.add(0); //>>0.5
	names.add("audiology.standardized.data"); //m=71nominal (2<l<8|remove atts with 1 only) c=2[0] n=200
	classes.add(0); remAtt.add(null); remVal.add(null); //>>SLOW*/
	/*names.add("SPECTF.train");//m=45r c=2[att0] n=80 <<<
	classes.add(0); remAtt.add(null); remVal.add(null); //ok but 0.62
	names.add("dermatology.data"); //m=35nominal c=6[last|remove age/penultimo] n=366
	classes.add(-1); remAtt.add(-2); remVal.add(null); //ok 0.7 imbalance
	names.add("wpbc.data");//m=35r c=2[att1|remove att0] n=198
	classes.add(1); remAtt.add(0); remVal.add(null); //0.3 but not high imbalance :S
	names.add("ctg.data");//m=32mix c=3[last]|10[penultimo] n=2126 
	classes.add(-1); remAtt.add(null); remVal.add(null); //0.7 high imbalance
	names.add("parkinson_train_data.txt");//m=29r c=2[last] n=1040
	classes.add(-1); remAtt.add(null); remVal.add(null); //0.5 but no imbalance
	names.add("SPECT.train");//m=22binary c=2[att0] n=80 
	classes.add(0); remAtt.add(null); remVal.add(null); //0.6 high imbalance
	names.add("parkinsons.data");//m=23r c=2[att17|remove att0] n=195
	classes.add(17); remAtt.add(0); remVal.add(null); //0.3 but high imbalance
	names.add("horse-colic.data");//m=28mix c=[24,23..27] n=300
	classes.add(23); remAtt.add(null); remVal.add(null); //0.4 but high imbalance*/
	/*names.add("messidor_features.arff"); //m=19r c=2[last] n=1151
	classes.add(-1); remAtt.add(null); remVal.add(null); //0.5 yet low nr bics
	names.add("eeg_eye_state.arff"); //m=14r c=2[last] n=14980
	classes.add(-1); remAtt.add(null); remVal.add(null); //0.5
	names.add("echocardiogram.data"); //m=12mix c=2[att1|remove att0] n=132
	classes.add(1); remAtt.add(0); remVal.add(null); //0.5 low nr of disc bics
	names.add("thoraric_surgery.arff");//m=16 c=7[att0] ou 2[last] (remove penultimo) n=470
	classes.add(0); remAtt.add(-2); remVal.add(null); //0.3 high imbalance*/
	//names.add("breast-cancer-wisconsin.data"); //m=10nominal c=2[last|remove att0] n=699
	//classes.add(-1); remAtt.add(0); remVal.add(null); //0.9 some imbalance*/ 
	/*classes.add(1); remAtt.add(0); remVal.add(null); //0.3 imbalance OK
	names.add("wdbc.data");//m=32r c=2[att1|remove att0] n=569
	names.add("fertility_Diagnosis.txt"); //m=9mix c=2[last] n=100
	classes.add(-1); remAtt.add(null); remVal.add(null); //1.0 :)*/
	//>>>ERR names.add("heart/processed.switzerland.data");//m=13r c=5[last] n=123
	//classes.add(-1); remAtt.add(null); remVal.add(null);
	/*names.add("heart/processed.va.data");//m=13r c=5[last] n=200
	classes.add(-1); remAtt.add(null); remVal.add(null); //0.3 high imbalance*/
	/*names.add("pima-indians-diabetes.data");//m=8 c=2[last] n=768
	classes.add(-1); remAtt.add(null); remVal.add(null); //0.4 high imbalance*/
	
	for(int i=0, l=names.size(); i<l; i++){
		String name = names.get(i);
		Reader reader = name.contains("arff") ? new FileReader(name) : new StringReader(getArff(name,remVal.get(i)));
		Instances dataset = new Instances(new BufferedReader(reader));
		dataset.setRelationName(name);
		Integer cindex = classes.get(i), rematt = remAtt.get(i);
		if(cindex<0) cindex=dataset.numAttributes()+cindex;
		dataset.setClassIndex(cindex);
		if(rematt!=null){
			int index = rematt>=0 ? rematt : dataset.numAttributes()+rematt;
			Remove remove = new Remove();
		    remove.setAttributeIndicesArray(new int[]{index});
		    remove.setInputFormat(dataset);
		    dataset = Filter.useFilter(dataset,remove);
			//dataset.deleteAttributeAt(index);
			System.out.println("b>>"+dataset.classIndex());
		}
		//datasets.add(preprocess(dataset));
		datasets.add(dataset);
	}
	return datasets;
  }
  
  private static String getArff(String filename, Integer remVal) throws IOException {
	StringBuffer res = new StringBuffer("@RELATION test\n\n");
	String delimiter = BicReader.detectDelimeter(filename);
	List<String[]> table = BicReader.getStringTable(filename,delimiter);
	int nrAttributes = table.get(0).length;
	for(int i=0, l=nrAttributes; i<l; i++){
		List<String> values = new ArrayList<String>();
		for(String[] row : table) values.add(row[i]);
		res.append("\n@attribute Att"+i+" "+identifyDomain(values));
	}
	res.append("\n\n@data");
	for(String[] row : table){
	  res.append("\n"+row[0]);
	  for(int i=1, l=row.length; i<l; i++){
		  String v = remVal==null ? row[i] : preserve(remVal,row[i]);
		  res.append(","+v);
	  }
	}
	//BicResult.println(res.toString());
	return res.toString();
  }

  private static String preserve(Integer remVal, String val) {
	try {
		Integer v = Integer.valueOf(val);
		return remVal.equals(v) ? "?" : val; 
	} catch(Exception e){ return val; }
  }

  private static String identifyDomain(List<String> values) {
	boolean isDouble=true, isInteger=true;
	Set<String> domain = new HashSet<String>();
	for(String v : values){ 
		if(!v.equals("?")){
			domain.add(v);
			try{ Integer.valueOf(v);} catch(Exception e){ isInteger=false;}
			try{ Double.valueOf(v); } catch(Exception e){ isDouble=false; }
		}
	}
	String res = "";
	for(String v : domain) res+=","+v;
	if(isInteger && domain.size()<=11) return "{"+res.substring(1)+"}";
	return isDouble ? "numeric" : "{"+res.substring(1)+"}";
  }

  public static List<Classifier> getClassifiers() {
	List<Classifier> suite = new ArrayList<Classifier>();
	//suite.add(new BClassifier());
	suite.add(new J48());
	//suite.add(new BSigJ48());
	//suite.add(new RandomForest());
	//suite.add(new SMO());
	//suite.add(new BayesNet());
	//suite.add(new NaiveBayes());
	//suite.add(new MultilayerPerceptron());
    return suite;
  }

  private static Instances preprocess(Instances dataset) throws Exception {
	int nrLabels = 5;
	List<Integer> removals = Arrays.asList(); 
	int[] classIndexes = new int[dataset.numInstances()];
	int numClasses = dataset.numClasses();
	for(int i=0; i<dataset.numInstances(); i++) classIndexes[i]=(int)dataset.instance(i).classValue();
	dataset.setClassIndex(0);
	dataset.deleteAttributeAt(dataset.numAttributes()-1);
	Dataset data = new Dataset(dataset);
	data.classValues = classIndexes;
	data.nrLabels = nrLabels;
	//data = Normalizer.run(data,NormalizationCriteria.Row);
	data = Normalizer.run(data,NormalizationCriteria.Column);
	data = Discretizer.run(data,DiscretizationCriteria.NormalDist,NoiseRelaxation.None,data.nrLabels);
	for(int j=0, l1=data.columns.size(); j<l1; j++){
		int[] counts = new int[nrLabels];
		for(int i=0, l2=data.rows.size(); i<l2; i++)
			counts[data.intscores.get(i).get(j)]++;
		//BicResult.println("Att"+j+":"+BicPrinting.plot(counts));
	}
	data = ItemMapper.remove(data,removals);
	Instances arffdata = WekaUtils.toDiscreteInstances(data);
	return arffdata;
  }

}