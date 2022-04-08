package tests.cpam;

import java.util.List;
import java.util.Map;

import performance.classification.ClassifierEvaluation;
import performance.classification.domain.Results;
import performance.classification.statististics.NullClassifierTest;
import performance.classification.statististics.PerformanceBounds;
import performance.classification.statististics.PermutationTest;
import tests.cpam.utils.ClassificationUtils;
import tests.cpam.utils.ListClassifiers;
import tests.cpam.utils.ListDatasets;
import utils.BicResult;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.core.Instances;
import weka.core.OptionHandler;


/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class CpamSignificanceTest {

  static boolean accuracy = true;
  static boolean sensitivity = true;
  static boolean specificity = true;
  static boolean bounds = true;
  static boolean nullclassifier = true;
  static boolean permutation = true;

  static boolean synthetic = false;
  static boolean sampling = false;

  public static void main(String[] args) throws Exception {
	List<Instances> datasets;
	
	if(synthetic) datasets = ListDatasets.getSyntheticDatasets();
	else datasets = ListDatasets.getRealDatasets();
	
	for(Instances dataset : datasets){
	    //List<String[]> evaloptions = Arrays.asList(new String[]{"CV"});
		List<String[]> samplingOptions = ClassificationUtils.getSamplingOptions(sampling);
		
	    for(String[] evaloption : samplingOptions){
	    	for(Classifier cl : ListClassifiers.getClassifiers()){
				//hyperseq = cl.getClass().getName().contains("HyperSeq");
			    for(String[] classifierOption : ListClassifiers.getClassifierOptions(cl)) {
			    	Classifier classifier = AbstractClassifier.makeCopy(cl);
					((OptionHandler) classifier).setOptions(classifierOption);
					printHeader(classifier,dataset,classifierOption,evaloption);
					testDatasets(classifier,dataset,evaloption);
				  	String options = "[";
				  	for(int i=0;i<classifierOption.length;i++) options += ((OptionHandler) classifier).getOptions()[i]+",";
				  	System.out.println(options+"]");
			    }
		    }
		}
	}
  }
  
  public static void testDatasets(Classifier classifier, Instances dataset, String[] evaloption) throws Exception {
    //if(!BalanceData.isDataBalanced(dataset)) dataset = BalanceData.balanceDataset(dataset);
    
	Map<String,Results> allres = ClassifierEvaluation.run(classifier, dataset, evaloption);
	for(Results res : allres.values()) {
	   	List<Double> results = res.getAccuracy();
		   	
	   	if(accuracy) BicResult.print(res.toTableString("Accuracy"));
	   	if(sensitivity) BicResult.print(res.toTableString("Sensitivity"));
	   	if(specificity) BicResult.print(res.toTableString("Specificity"));
		   	
	    if(permutation){	
		   	PermutationTest permutation = new PermutationTest(AbstractClassifier.makeCopy(classifier), new Instances(dataset), evaloption);
		   	double averagePValue = permutation.getPValueAverage(results);
		   	double percentageBelow = permutation.getPercentageBelow(results);
		   	BicResult.println("\tPermutations:\t"+((int)(averagePValue*1000)/1000.0)+"\t"+((int)(percentageBelow*100)/100.0));
		   	//BResult.println("\tPermutations: avg p-value:"+((int)(averagePValue*1000)/1000.0)+"  fraction below avg acc:"+((int)(percentageBelow*100)/100.0));
	    }
	    if(bounds){
		   	double[] pBounds = PerformanceBounds.run(results, 0.01);
		   	BicResult.println("\tBounds:\t"+((int)(pBounds[0]*100)/100.0)+"\t"+((int)(pBounds[2]*100)/100.0));
		   	//BResult.println("\tBounds: ["+((int)(pBounds[0]*100)/100.0)+","+((int)(pBounds[2]*100)/100.0)+"]("+((int)(pBounds[1]*100)/100.0)+")  baseline:"+((int)((1.0/dataset.numClasses())*100)/100.0));
	    }
	    else if(nullclassifier){ 
	    	double pvalueNullClassifier = NullClassifierTest.run(results, res.nullaccuracy);
	    	BicResult.println("\tSuperiorityZeroR:\t"+((int)(pvalueNullClassifier*1000)/1000.0));
	    	//BResult.println("\tSup p-value against ZeroR:"+((int)(pvalueNullClassifier*1000)/1000.0));
	    }
	}
  }

  private static void printHeader(Classifier classifier, Instances dataset, String[] classifierOption, String[] evaloption) {
	  	BicResult.print(dataset.relationName()+"\t"+classifier.getClass().getName().split("classifiers.")[1]);
	  	String options = "[";
	  	for(int i=0;i<classifierOption.length;i++) options += ((AbstractClassifier)classifier).getOptions()[i]+",";
	  	options += "]\tEVAL[";
	  	for(int i=0;i<evaloption.length;i++) options += evaloption[i]+",";
	  	BicResult.println(options+"]");
  }
}
