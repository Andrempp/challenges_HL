package tests.cpam.others;

import generator.BicMatrixGenerator.PatternType;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import cpam.BClassifier;
import cpam.Tester;
import cpam.BClassifier.DiscMetric;
import cpam.BClassifier.LearningFunction;
import cpam.BClassifier.Relaxation;
import cpam.BClassifier.TestingFunction;
import performance.classification.ClassifierEvaluation;
import performance.classification.domain.Results;
import domain.Biclusters.Order;
import domain.Dataset;
import utils.BicPrinting;
import utils.BicResult;
import utils.WekaUtils;
import weka.core.Instances;


/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class CpamTFactorTests {
	
  //static public String datafile = "/data/EnhancerPromoterLabeled.txt";
  static public String datafile = "/data/plants.exp.transit.nr.dat.txt";

  public static void main(String[] args) throws Exception {
	  
	/** Part 0: learning task **/
	boolean classification = false; //true for 10-CV or false to learn disc bics only

	/** Part 1: read labeled data **/
	Dataset data = readData(datafile, "\t", false /*whether class is first/true or last/false index*/);
	data.nrLabels = 2; /*coherence strength for binary data*/
	data.name = "EnhancerPromoterLabeled";
	//BicResult.println(data.toString());
	
	/** Part 2: parameterize biclustering **/
	BClassifier cpam = new BClassifier(); //SPMClassifier(50,2,10);
	cpam.coherencies = Arrays.asList(PatternType.Constant);
	/*cpam.minOverlapMerging = 0.8;
	cpam.minBiclusters = 20; //set high to guarantee the presence of disc bics
	cpam.minColumns = 1;
	cpam.nrIterations = 1;
	cpam.minDiscBics = 5;
	cpam.discMetric = DiscMetric.Confidence;*/

	/** Part 3: parameterize classifier (OPTIONAL) **/
	cpam.learningFunction = LearningFunction.WeightedRules;
	cpam.tester = new Tester(TestingFunction.WCS,Relaxation.Linear);

	/** Part 4: no need for mapping options **/
	//data = Normalizer.run(data,NormalizationCriteria.Column);
	//data = Discretizer.run(data,DiscretizationCriteria.NormalDist,NoiseRelaxation.None,data.nrLabels);
	//data = ItemMapper.remove(data,Arrays.asList()); //items to remove
	Instances arffdata = WekaUtils.toDiscreteInstances(data);
	
	/** Part 5: learn and test classifier **/  
	if(classification){
		String[] evaluationOptions = new String[]{"","CV"};
		//String[] evaluationOptions = new String[]{"-preserve-order","-split-percentage","80"};
		System.out.println("\n"+data.name+" :: "+cpam.getClass().toString());
	   	Map<String,Results> results = ClassifierEvaluation.run(cpam, arffdata, evaluationOptions);
	   	System.out.println(results.toString());
	}
	
	/** Part 6: learn discriminative biclusters **/
	if(!classification){
		cpam.buildClassifier(arffdata);
		//cpam.orderRules(Order.WLift);
		//BicResult.println(cpam.rulesToString());
		BicResult.println(cpam.rulesToString(data.rows,data.columns));
	}
  }

  /** Read labeled binary dataset **/
  private static Dataset readData(String dataname, String delimeter, boolean firstIndex) throws IOException {
	List<String[]> table = new ArrayList<String[]>();
	BufferedReader br = new BufferedReader(new FileReader(new File("").getAbsolutePath() + dataname));
    String line;
    String[] features = br.readLine().split(delimeter);
    while((line=br.readLine())!=null && line.contains(delimeter)) 
       	table.add(line.split(delimeter));
    br.close();
    String[][] matrix = new String[table.size()][]; 
   	for(int i=0, l1=table.size(); i<l1; i++) matrix[i]=table.get(i);
	
    List<String> nodesX = new ArrayList<String>(), nodesY = new ArrayList<String>();
    List<List<Integer>> indexes = new ArrayList<List<Integer>>();
    List<List<Integer>> scores = new ArrayList<List<Integer>>();

    int c = firstIndex ? 1 : 0;
    for(int i=1+c; i<features.length-1+c; i++) nodesY.add(features[i]);    
    for(int i=0, l1=matrix.length; i<l1; i++){
    	nodesX.add(matrix[i][0]);
    	List<Integer> indexing = new ArrayList<Integer>();
    	List<Integer> scoring = new ArrayList<Integer>();
	    for(int j=1+c, l2=features.length-1+c; j<l2; j++){
	    	int value = Integer.valueOf(matrix[i][j]);
	    	if(value==0) continue;
		    indexing.add(j-1-c);
	    	scoring.add(1);
	    }
   		indexes.add(indexing);
   		scores.add(scoring);
    }
    Dataset data = new Dataset(nodesX,nodesY,indexes,scores);

    List<String> classSet = new ArrayList<String>();
	int[] classes = new int[table.size()];
    for(int i=0, l1=matrix.length; i<l1; i++){
    	String value = firstIndex ? matrix[i][1] : matrix[i][features.length-1]; 
		if(!classSet.contains(value)) classSet.add(value);
		classes[i]=classSet.indexOf(value);
    }
	data.classValues = classes;
    System.out.println(BicPrinting.plot(data.classValues));
    System.out.println("Dataset created!");
	return data;
  }

}