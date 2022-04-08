package cpam.others;

import generator.BicMatrixGenerator.PatternType;
import java.util.Arrays;
import java.util.List;
import cpam.BClassifier;
import cpam.cmar.PlugableCMAR;
import bicpam.bicminer.BiclusterMiner;
import bicpam.bicminer.BiclusterMiner.Orientation;
import bicpam.bicminer.coherent.AdditiveBiclusterMiner;
import bicpam.bicminer.coherent.MultiplicativeBiclusterMiner;
import bicpam.bicminer.coherent.SymmetricBiclusterMiner;
import bicpam.bicminer.constant.ConstantBiclusterMiner;
import bicpam.bicminer.order.OrderPreservingBiclusterMiner;
import bicpam.closing.BiclusterMerger;
import bicpam.closing.Biclusterizer;
import bicpam.mapping.ItemMapper;
import bicpam.pminer.fim.MaximalFIM;
import bicpam.pminer.spm.SequentialPM;
import bicpam.pminer.spm.SequentialPM.SequentialImplementation;
import domain.Biclusters;
import domain.Dataset;
import utils.BicMath;
import utils.others.CopyUtils;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;

public class BicPAMClassifier extends BClassifier {
	
	PlugableCMAR cmar;
	
	public int minBiclusters=100;
	public int minDiscBiclusters=1;
	public double support=0.5;
	public int minColumns=3;
	public int nrIterations = 2;	
	private int nrLabels;
	private boolean symmetry = false;

	public BicPAMClassifier(){}

	public void buildClassifier(Instances dataset) throws Exception {
		nrLabels = dataset.attribute(0).numValues();
		//cmar = new PlugableCMAR(dataset.numInstances(),dataset.numClasses());
        System.out.println("Computing frequent patterns");
    	Dataset data = new Dataset(dataset);
    	data.roundValues();
        for(int i=0, l=dataset.numClasses(); i<l; i++){
        	Dataset dataI = data.getPartition(i);
        	dataI.nrLabels = nrLabels;
        	Biclusters ipatterns = mineBiclusters(dataI);//new CoherentBMiner(dataI).run((int)(support*dataI.getNrTrans()),minCols);
        	System.out.println("#patterns["+i+"]="+ipatterns.size());
            //cmar.addRules(ipatterns, null, Arrays.asList(i), -1, nrLabels); //data.getTransactions(), i, dataI.getNrTrans(), nitems);
        }
        //System.out.println("\n==== CMAR Tree ====\n"+cmar.toString());
    }    
	
	private Biclusters mineBiclusters(Dataset data) throws Exception {

		/** Default Parameterizations **/
		PatternType type = PatternType.Constant; /*coherence assumption*/
		double minOverlapMerging = 0.7;
		Biclusterizer posthandler = new Biclusterizer(new BiclusterMerger(minOverlapMerging));

		/** Define PMiner **/
		BiclusterMiner bicminer = null; 
	   	if(type.equals(PatternType.OrderPreserving)){
			SequentialPM pminer = new SequentialPM();
		    pminer.algorithm = SequentialImplementation.PrefixSpan;
			pminer.inputMinNrBics(minBiclusters);
			pminer.inputMinColumns(minColumns);
			bicminer = new OrderPreservingBiclusterMiner(data,pminer,posthandler,Orientation.PatternOnRows);		
		} else {
			MaximalFIM pminer = new MaximalFIM(); //new ClosedFIM();
			pminer.inputMinNrBics(minBiclusters);
			pminer.inputMinColumns(minColumns);
			if(type.equals(PatternType.Additive)){
				bicminer = new AdditiveBiclusterMiner(data,pminer,posthandler,Orientation.PatternOnRows);
			} else if(type.equals(PatternType.Constant)){
				bicminer = new ConstantBiclusterMiner(data,pminer,posthandler,Orientation.PatternOnRows); //TODO: false parameter
			} else if(type.equals(PatternType.Symmetric)){
				bicminer = new SymmetricBiclusterMiner(data,pminer,posthandler,Orientation.PatternOnRows); 
			} else bicminer = new MultiplicativeBiclusterMiner(data,pminer,posthandler,Orientation.PatternOnRows); 
		}
		
		/** Run BicPAM **/		
		Biclusters bics = new Biclusters();
		List<List<Integer>> originalIndexes = CopyUtils.copyIntList(data.indexes);
		List<List<Integer>> originalScores = CopyUtils.copyIntList(data.intscores);
		double removePercentage = 0.3;
		for(int i=0; i<nrIterations; i++){
			Biclusters iBics = bicminer.mineBiclusters();
			data.remove(iBics.getElementCounts(),removePercentage);
			bicminer.setData(data);
			bics.addAll(iBics);
		}
		data.indexes = originalIndexes;
		data.intscores = originalScores;
		
		/** Output and Evaluation **/		
		bics.order();
		//BicResult.println("FOUND BICS:" + bics.toString(data.rows,data.columns));
		//for(Bicluster bic : bics.getBiclusters()) BicResult.println(bic.toString(data)+"\n\n");
		return bics;
	}

	public double classifyInstance(Instance instance) throws Exception {
		int[] values = new int[instance.numAttributes()];
		for(int i=0, l=instance.numAttributes(); i<l; i++)
			values[i] = (short) instance.value(i);
		int[] itemizedvalues = ItemMapper.itemize(values,nrLabels,symmetry);
		//System.out.println("Inst:"+BPrinting.plot(values));
		return BicMath.maxindex(null);//cmar.run(itemizedvalues));
	}
	
	public double[] distributionForInstance(Instance instance) throws Exception {
		int[] values = new int[instance.numAttributes()];
		for(int i=0, l=instance.numAttributes(); i<l; i++)
			values[i] = (short) instance.value(i);
		int[] itemizedvalues = ItemMapper.itemize(values,nrLabels,symmetry);
		return null;//cmar.run(itemizedvalues);
	}
	
	public String getModelName() {
		return "SPMClassifier";
	}

	public Capabilities getCapabilities() {
		return null;
	}
}
