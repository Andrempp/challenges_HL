package tests.cpam;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Arrays;
import java.util.List;
import bicpam.mapping.Discretizer;
import bicpam.mapping.ItemMapper;
import bicpam.mapping.Itemizer.DiscretizationCriteria;
import bicpam.mapping.Itemizer.NoiseRelaxation;
import bicpam.mapping.Itemizer.NormalizationCriteria;
import bicpam.mapping.Normalizer;
import cpam.BClassifier;
import domain.Dataset;
import performance.classification.ClassifierEvaluation;
import utils.BicResult;
import utils.WekaUtils;
import weka.classifiers.Classifier;
import weka.core.Instances;


/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class CpamSimpleTest {

  public static void main(String[] args) throws Exception {
	  
	/** A: Read dataset using Weka **/  
	Instances dataset = new Instances(new BufferedReader(new FileReader("data/classifier/ColonDiff62x2000.txt"))); 
	dataset.setClassIndex(dataset.numAttributes()-1);
	int[] classIndexes = new int[dataset.numInstances()];
	int numClasses = dataset.numClasses();
	for(int i=0; i<dataset.numInstances(); i++) 
		classIndexes[i]=(int)dataset.instance(i).classValue();
	
	/** B: Store dataset **/  
	dataset.setClassIndex(0);
	dataset.deleteAttributeAt(dataset.numAttributes()-1);
	Dataset data = new Dataset(dataset);
	data.classValues = classIndexes;
	
	/** C: Discretize dataset **/  
	data.nrLabels = 6;
	data = Normalizer.run(data,NormalizationCriteria.Column);
	data = Discretizer.run(data,DiscretizationCriteria.NormalDist,NoiseRelaxation.None,data.nrLabels);
	data = ItemMapper.remove(data,Arrays.asList());
	Instances arffdata = WekaUtils.toDiscreteInstances(data);
	System.out.println(data.nrLabels+"=="+arffdata.attribute(0).numValues());
    BicResult.println(arffdata.toString());
	
	/** C: Apply Classifier **/  
	Classifier classifier = new BClassifier(); //SPMClassifier(50,2,10);
	String[] evaluationOptions = new String[]{"-smooth","-split-percentage","80"};//"CV"};"-bootstrap"};//
	System.out.println("\n"+dataset.relationName()+" :: "+classifier.getClass().toString());
   	System.out.println(ClassifierEvaluation.run(classifier, arffdata, evaluationOptions));
  }
  
}
