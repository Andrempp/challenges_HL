package cpam;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import bicpam.bicminer.BiclusterMiner;
import bicpam.bicminer.BiclusterMiner.Orientation;
import bicpam.bicminer.coherent.AdditiveBiclusterMiner;
import bicpam.bicminer.coherent.MultiplicativeBiclusterMiner;
import bicpam.bicminer.coherent.SymmetricBiclusterMiner;
import bicpam.bicminer.constant.ConstantBiclusterMiner;
import bicpam.bicminer.order.OrderPreservingBiclusterMiner;
import bicpam.closing.BiclusterMerger;
import bicpam.closing.Biclusterizer;
import bicpam.mapping.Discretizer;
import bicpam.mapping.Itemizer.DiscretizationCriteria;
import bicpam.mapping.Itemizer.NoiseRelaxation;
import bicpam.mapping.Itemizer.NormalizationCriteria;
import bicpam.mapping.Normalizer;
import bicpam.pminer.fim.ClosedFIM;
import bicpam.pminer.spm.SequentialPM;
import bicpam.pminer.spm.SequentialPM.SequentialImplementation;
import domain.Bicluster;
import domain.Biclusters;
import domain.Biclusters.Order;
import domain.Dataset;
import generator.BicMatrixGenerator.PatternType;
import utils.BicMath;
import utils.BicPrinting;
import utils.BicResult;
import utils.others.CopyUtils;
import weka.classifiers.Classifier;
import weka.classifiers.bayes.BayesNet;
import weka.classifiers.functions.SMO;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;


public class BClassifier implements Classifier {
	
	public enum DiscMetric { Foil, Confidence, Lift, LowInterClassOverlap, Chi2, Fisher };
	public enum LearningFunction { WeightedRules, TreeStructure, CMAR, kNN };
	public enum TestingFunction { WCS, SimpleWeighting, Exclusion };
	public enum Relaxation { None, Linear, Squared, Adjusted };

	
	/***********************
	 **** A: PARAMETERS **** 
	 ***********************/
	
	public static double minMatchSupport = .85, minLift = 1.21;
	private int maxLabels, nrLabels, maxDiscBiclusters = 17;

	public boolean hybrid = false;
	public RandomForest svm = new RandomForest(); //SMO
	public LearningFunction learningFunction = LearningFunction.WeightedRules;
	public List<PatternType> coherencies = new ArrayList<PatternType>();
	public Orientation orientation = Orientation.PatternOnRows;
	public Learner learner; 
	public Tester tester = new Tester(TestingFunction.WCS,Relaxation.Squared);
	
	private List<Attribute> attributes;
	private List<Double> mean, std;
	
	
	/*************************
	 **** B: CONSTRUCTORS **** 
	 *************************/
	
	public BClassifier(){
		this.coherencies.add(PatternType.Constant);
		this.coherencies.add(PatternType.OrderPreserving);
		this.nrLabels = 5; //4
	}
	
	public BClassifier(PatternType coherence){
		this(coherence,coherence.equals(PatternType.OrderPreserving) ? 20 : 5);
	}
	
	public BClassifier(PatternType _coherence, int _numLabels){
		this.coherencies.add( _coherence);
		this.nrLabels = _numLabels;
	}

	/***************************
	 **** C: TRAIN and TEST **** 
	 ***************************/

	public void buildClassifier(Instances dataset) throws Exception {

		/** C1: identify cardinalities **/
		if(hybrid) svm.buildClassifier(dataset);
		maxLabels = nrLabels;
		attributes = dataset.getAttributes();
		for(Attribute att : attributes)
			if(att.isNominal()) maxLabels = Math.max(maxLabels, att.numValues());
    	Dataset data = new Dataset(dataset);
    	BicResult.println(attributes + "\n" + BicPrinting.plot(data.countClasses));
    	
		/** C2: noise-tolerant discretization **/
    	boolean example = true;
    	if(example) {
		    data = Discretizer.run(data, DiscretizationCriteria.MultiDisc, NoiseRelaxation.None, nrLabels); //discretize numeric atts
    	} else {
	    	data = Normalizer.run(data, NormalizationCriteria.Column); //normalize numeric atts
	    	mean = data.mean;
	    	std = data.std;
		    data = Discretizer.run(data, DiscretizationCriteria.NormalDist, NoiseRelaxation.None, nrLabels); //discretize numeric atts
    	}
	    //BicResult.println(data.toIntString(20));
    	
    	/** C3: training by mining discriminative biclusters **/
    	
    	Biclusters[] discBics = bicDiscovery(data);
		int maxBics = (int)(((double)maxDiscBiclusters*coherencies.size())*0.75);
		learner = Learner.learn(discBics, data, learningFunction, maxBics);
	}
	
	public double classifyInstance(Instance instance) throws Exception {
		double[] output = distributionForInstance(instance);
		return BicMath.maxindex(output);
	}

	public double[] distributionForInstance(Instance instance) throws Exception {
		instance.toDoubleArray();
		double[] result = discretizeInstance(instance);
		//System.out.println("C>>"+BicPrinting.plot(result));
		double[] out_associative = learner.test(tester, result);
		if(!hybrid) return out_associative;
		double[] out_svm = svm.distributionForInstance(instance);
		double[] out = new double[out_svm.length];
		for(int i=0; i<out_svm.length; i++)
			out[i] = out_svm[i]*out_associative[i];
		System.out.println("A>>"+BicPrinting.plot(out_associative));
		System.out.println("B>>"+BicPrinting.plot(out_svm));
		System.out.println("C>>"+BicPrinting.plot(out));
		return out;
	}

	/********************************
	 **** D: DISC PATTERN MINING **** 
	 ********************************/

	public Biclusters[] bicDiscovery(Dataset data) throws Exception {
    	int numClasses = data.numClasses();
    	Biclusters[] bicsPerClass = new Biclusters[numClasses];
		for(int k=0, l=data.numClasses(); k<l; k++){
			bicsPerClass[k] = new Biclusters();
	    	System.out.println("=== Classe="+k+" ===");
		    Dataset dataI = data.getPartition(k);
	        dataI.nrLabels = maxLabels;
	        for(PatternType coherence : coherencies) {
		        for(double support=1.0; support>=0.2; support=support*0.9){
		        	Biclusters allBics = mineBiclusters(support,dataI,coherence);
		        	allBics = getDiscriminative(allBics,data,k,coherence);
		        	if(allBics.size()>maxDiscBiclusters) {
		        		bicsPerClass[k].addAll(allBics);
		        		break;
		        	} else if(support*0.9<0.2) bicsPerClass[k].addAll(allBics);
		        }
	        }
    	}
        return bicsPerClass;
	}
	
	private Biclusters getDiscriminative(Biclusters allBics, Dataset data, int condition, PatternType coherence) {
		Biclusters discBics = new Biclusters();
		int numClasses = data.numClasses();
		int[] classes = data.classValues;
		double n = (double)data.rows.size();
		double classSupport = ((double)data.countClasses[condition])/n;
		
		for(Bicluster bic : allBics.getBiclusters()) {
			int[] sup = new int[numClasses];
			double[] wsup = new double[numClasses];
			int ncols = bic.numColumns();
			int maxMis = (int)(ncols*(1-minMatchSupport));
			for(int i=0, l1=data.rows.size(); i<l1; i++){
				int mismatches = 0;
				if(coherence.equals(PatternType.Constant)) {
					int j = 0;
					for(Integer col : bic.columns) {
						if(data.getElement(i,col)!=bic.items.get(j++)) mismatches++;
					} 
				} else if(coherence.equals(PatternType.OrderPreserving)) {
					if(bic.orders==null) continue;
					//System.out.println(">"+bic.orders+bic.toShortString());
					for(int j=1, l=bic.columns.size(); j<l; j++) {
						int a=data.getElement(i,bic.orders.get(j-1));
						int b=data.getElement(i,bic.orders.get(j));
						if(a>b) mismatches++;
					}
				}
				if(mismatches<=maxMis) wsup[classes[i]]+=Math.pow(((double)(ncols-mismatches))/(double)ncols,2);
				if(mismatches==0) sup[classes[i]]++;
			}
			double sumWSup = BicMath.sum(wsup); 
			bic.condition = condition;
			bic.wlift = wsup[bic.condition]/(sumWSup*classSupport);
			
			if(bic.wlift>minLift){
				double sumSup = BicMath.sum(sup); 
				bic.support = sup;
				bic.weightedSupport = wsup;
				bic.conf = sup[bic.condition]/sumSup;
				bic.lift = sup[bic.condition]/(sumSup*classSupport);
				bic.wconf = wsup[bic.condition]/sumWSup;
				
				double maxlift = 0;
				for(int k=0; k<numClasses; k++) {
					if(k==bic.condition) continue;
					double kClassSupport = ((double)data.countClasses[k])/n;
					double kLift = wsup[k]/(sumWSup*kClassSupport);
					maxlift = Math.max(maxlift, kLift);
				}
				bic.score = bic.wlift*(bic.wlift/maxlift); //Math.pow(bic.wlift/maxlift,1.2);
				//System.out.println("===> "+bic.wlift+" || "+maxlift);
				discBics.add(bic);
			}
		}
		return discBics;
	}

	/*************************
	 **** E: BICLUSTERING **** 
	 *************************/
	
	private Biclusters mineBiclusters(double support, Dataset data, PatternType coherence) throws Exception {

		/** E1: parameterize BicPAM **/
		Biclusters bics = new Biclusters();
		BiclusterMiner bicminer = null; 
    	for(PatternType type : Arrays.asList(coherence)){
		   	if(type.equals(PatternType.OrderPreserving)){
				SequentialPM pminer = new SequentialPM();
			    pminer.algorithm = SequentialImplementation.PrefixSpan;
			    pminer.setSupport(support);
	    		Biclusterizer posthandler = new Biclusterizer(new BiclusterMerger(1,Order.Area));
				bicminer = new OrderPreservingBiclusterMiner(data,pminer,posthandler,orientation);		
			} else {
				ClosedFIM pminer = new ClosedFIM();//MaximalFIM()
			    pminer.setSupport(support);
	    		Biclusterizer posthandler = new Biclusterizer(new BiclusterMerger(.7,Order.Area));
				if(type.equals(PatternType.Additive)){
					bicminer = new AdditiveBiclusterMiner(data,pminer,posthandler,orientation);
				} else if(type.equals(PatternType.Constant)){
					bicminer = new ConstantBiclusterMiner(data,pminer,posthandler,orientation); 
				} else if(type.equals(PatternType.Symmetric)){
					bicminer = new SymmetricBiclusterMiner(data,pminer,posthandler,orientation); 
				} else bicminer = new MultiplicativeBiclusterMiner(data,pminer,posthandler,orientation); 
			}
			
			/** E2: Run BicPAM **/		
			List<List<Integer>> originalIndexes = CopyUtils.copyIntList(data.indexes);
			List<List<Integer>> originalScores = CopyUtils.copyIntList(data.intscores);
			double removePercentage = 0.4;
			for(int i=0, nrIterations=1; i<nrIterations; i++){
				Biclusters iBics = bicminer.mineBiclusters();
				if(iBics.size()==0) break;
				if(i+1<nrIterations) {
					data.remove(iBics.getElementCounts(),removePercentage);
					bicminer.setData(data);
				}
				bics.addAll(iBics);
			}
			data.indexes = originalIndexes;
			data.intscores = originalScores;
			
			/** E3: Compute pattern */
			if(type.equals(PatternType.Constant)){
	    		for(Bicluster bic : bics.getBiclusters()) {
	    			bic.items = new ArrayList<Integer>();
    				for(Integer col : bic.columns) { 
	    				List<Integer> values = new ArrayList<Integer>();
		    			for(Integer row : bic.rows)
	    					values.add(data.getElement(row,col));
	    				bic.items.add(BicMath.mode(values));
	    			}
	    		}
	    	}
    	}
		return bics;
	}

	/**********************
	 **** F: AUXILIARY **** 
	 **********************/
	
	private double[] discretizeInstance(Instance instance) {
		if(mean==null) return instance.toDoubleArray();
		double[] cutPoints = Discretizer.breakingPoints[nrLabels];
		double[] result = new double[instance.numAttributes()-1];
		for(int i=0, cindex=0; i<instance.numAttributes(); i++) { 
			if(i==instance.classIndex()) cindex=1;
			else {
				if(attributes.get(i).isNumeric()) {
					double value = (instance.value(i)-mean.get(i-cindex))/std.get(i-cindex);
					for(int k=cutPoints.length-1; k>=0; k--) {
						if(value >= cutPoints[k]) {
							result[i-cindex]=k;
							break;
						}
					}
				} else result[i-cindex]=(int)instance.value(i);
			}
		}
		return result;
	}
	
	public String rulesToString() {
		return learner.rulesToString();
	}
	
	public String rulesToString(List<String> rows, List<String> columns) {
		return learner.rulesToString(rows, columns);
	}
	
	public String toString() { 
		return "BClassifier |L|="+nrLabels+" Learner="+learningFunction+" Tester={"+tester.toString()+"} Coherencies="+coherencies; 
	}
	
	public Capabilities getCapabilities() { return null; }
}
