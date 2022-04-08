package tests.others.constraints;

import generator.BicMatrixGenerator;
import generator.BicMatrixGenerator.PatternType;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import domain.Bicluster;
import domain.Biclusters;
import domain.Dataset;
import domain.constraint.AntiMonotoneConstraint;
import domain.constraint.Constraint.Aggregator;
import domain.constraint.Constraint.AntiMonotoneOperation;
import domain.constraint.Constraint.MonotoneOperation;
import domain.constraint.Constraint.SuccinctOperation;
import domain.constraint.Constraint.ValueType;
import domain.constraint.Constraints;
import domain.constraint.MonotoneConstraint;
import domain.constraint.SequentialConstraint;
import domain.constraint.SuccinctConstraint;
import utils.BicPrinting;
import utils.BicReader;
import utils.BicResult;
import utils.NetMatrixMapper;
import bicpam.bicminer.BiclusterMiner;
import bicpam.bicminer.BiclusterMiner.Orientation;
import bicpam.bicminer.constant.ConstantBiclusterMiner;
import bicpam.bicminer.order.OrderPreservingBiclusterMiner;
import bicpam.closing.BiclusterMerger;
import bicpam.closing.Biclusterizer;
import bicpam.mapping.ItemMapper;
import bicpam.mapping.Itemizer;
import bicpam.mapping.Itemizer.DiscretizationCriteria;
import bicpam.mapping.Itemizer.FillingCriteria;
import bicpam.mapping.Itemizer.NoiseRelaxation;
import bicpam.mapping.Itemizer.NormalizationCriteria;
import bicpam.pminer.PM;
import bicpam.pminer.fim.SimpleFIM;
import bicpam.pminer.fim.SimpleFIM.SimpleImplementation;
import bicpam.pminer.spm.SequentialPM;
import bicpam.pminer.spm.SequentialPM.SequentialImplementation;


/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class ConstraintSyntheticTests {

	static Dataset data = null;
	static boolean realdata = true;
	static boolean functional = false;
	static int minNrColumns = 3;
	static String path = new File("").getAbsolutePath();
	static List<Integer> removals = new ArrayList<Integer>();
	static PatternType type = PatternType.Constant; 
	static Constraints constraints = null;
	
    public static void main(String[] args) throws Exception{
    	
    	/** 1: Efficiency in the presence of succinct constraints (constant coherency) */
    	//syntheticSuccinct();
    	
    	/** 2: Efficiency with anti-monotone, monotone and convertible constraints (constant coherency) */ 
    	//syntheticAntiMonotone();
    	//syntheticMonotone();
    	
    	/** 3: Efficiency with sequence constraints (order-preserving coherency) */ 
    	//syntheticOrders();
    }
    
	public static void syntheticOrders() throws Exception {
		realdata=false;
		int numRows = 1000, numColumns = 100;
		int minRowsBics = 100, maxRowsBics = 150;
		int minColsBics = 8, maxColsBics = 10;
		/*int numRows = 20, numColumns = 10;
		int minRowsBics = 10, maxRowsBics = 10;
		int minColsBics = 4, maxColsBics = 4;*/
		int nrLabels = 10;
	    type = PatternType.OrderPreserving;

		BicMatrixGenerator generator = new BicMatrixGenerator(
				numRows,numColumns,2 /*|bics|*/,"random",nrLabels /*labels*/,true);
		Biclusters trueBics = generator.generateKBiclusters(type,
				"uniform", minRowsBics, maxRowsBics, 
				"uniform", minColsBics, maxColsBics, false, false, false);
		int[][] dataset = generator.getSymbolicExpressionMatrix();
		//BicResult.println(BicPrinting.plot(dataset));
		//BicResult.println(trueBics.toString());
		minNrColumns = 5;
		data = new Dataset(dataset);
		data.nrLabels = nrLabels;
		
    	/*List<List<Integer>> removalsSet = Arrays.asList(Arrays.asList(0,1,2),new ArrayList<Integer>());//,Arrays.asList(0));
    	for(List<Integer> removalsI : removalsSet){
    		removals = removalsI;
    		run();
    	}*/

		constraints = new Constraints(data.nrLabels,true);
		constraints.add(new SequentialConstraint(Arrays.asList(Arrays.asList(1,2)),false));//,Arrays.asList(0)
		run();    	
    }
	
	public static void syntheticMonotone() throws Exception {
		realdata=false;
		int nrLabels = 5;
		int numRows = 1000, numColumns = 100;
		int minRowsBics = 100, maxRowsBics = 150, minColsBics = 8, maxColsBics = 10;
	    type = PatternType.Constant;
		BicMatrixGenerator generator = new BicMatrixGenerator(numRows,numColumns,5 /*|bics|*/,"random",nrLabels /*labels*/,true);
		Biclusters trueBics = generator.generateKBiclusters(type, "uniform", minRowsBics, maxRowsBics, "uniform", minColsBics, maxColsBics, false, false, false);
		int[][] dataset = generator.getSymbolicExpressionMatrix();
		data = new Dataset(dataset);
		data.nrLabels = nrLabels;
		
		constraints = new Constraints(data.nrLabels,true);
		constraints.add(new MonotoneConstraint(Aggregator.Sum, MonotoneOperation.Greater, 15));
		run();    	
	}
	
	public static void syntheticAntiMonotone() throws Exception {
		realdata=false;
		int nrLabels = 5;
		int numRows = 1000, numColumns = 100;
		int minRowsBics = 100, maxRowsBics = 150, minColsBics = 8, maxColsBics = 10;
	    type = PatternType.Constant;
		BicMatrixGenerator generator = new BicMatrixGenerator(numRows,numColumns,10 /*|bics|*/,"random",nrLabels /*labels*/,false);
		Biclusters trueBics = generator.generateKBiclusters(type, "uniform", minRowsBics, maxRowsBics, "uniform", minColsBics, maxColsBics, false, false, false);
		int[][] dataset = generator.getSymbolicExpressionMatrix();
		data = new Dataset(dataset);
		data.nrLabels = nrLabels;
		minNrColumns = 5;
		run();    			
		
		data = new Dataset(dataset);
		data.nrLabels = nrLabels;
		minNrColumns = 5;
		constraints = new Constraints(data.nrLabels,true);
		constraints.add(new AntiMonotoneConstraint(Aggregator.Sum, AntiMonotoneOperation.Less, 2));
		run();    			
	}
	
	public static void syntheticSuccinct() throws Exception {
		realdata=false;
		minNrColumns=5;
		int nrLabels = 5;
		int numRows = 1000, numColumns = 100;
		int minRowsBics = 100, maxRowsBics = 150, minColsBics = 8, maxColsBics = 10;
	    type = PatternType.Constant;
		BicMatrixGenerator generator = new BicMatrixGenerator(numRows,numColumns,5 /*|bics|*/,"random",nrLabels /*labels*/,false);
		Biclusters trueBics = generator.generateKBiclusters(type, "uniform", minRowsBics, maxRowsBics, "uniform", minColsBics, maxColsBics, false, false, false);
		int[][] dataset = generator.getSymbolicExpressionMatrix();
		data = new Dataset(dataset);
		data.nrLabels = nrLabels;
		run();    	
		
		data = new Dataset(dataset);
		data.nrLabels = nrLabels;
		constraints = new Constraints(data.nrLabels,true);
		constraints.add(new SuccinctConstraint(SuccinctOperation.Includes, Arrays.asList(1,2), ValueType.Values));
		run();    	
	}

	public static void realRemovals() throws Exception {	
		minNrColumns = 3;
		List<String> datasets = new ArrayList<String>();
		//datasets.add("/data/networks/4932.scerevisae.protein.links.v10.txt");
		//datasets.add("/data/networks/ecoliB.protein.links.v10.txt");
		//datasets.add("/data/networks/protein.hsapiens.links.v10.txt");
		//datasets.add("/data/arff/hughes.arff");
    	//datasets.add("/data/arff/dlblc.arff");
    	datasets.add("/data/arff/yeast80.arff");
    	//datasets.add("/data/arff/gyeast.arff");
    	//String dataset1 = "/data/arff/simple.arff";
    	List<List<Integer>> removalsSet = Arrays.asList(Arrays.asList(0,1,2),new ArrayList<Integer>());//,Arrays.asList(0));
    	
    	for(String dataset : datasets){
	    	for(List<Integer> removalsI : removalsSet){
				removals = removalsI;
				if(dataset.contains("network")) data = NetMatrixMapper.getTable(path+dataset,0,1,2," ",true);
				else data = new Dataset(BicReader.getInstances(path+dataset));
				data.nrLabels=4;
				run();
	    	}
    	}
	}
	
	public static void syntheticRemovals() throws Exception {		
		realdata=false;
		int[] numRows = new int[]{500,1000,2000}, numColumns = new int[]{80,100,200};
		int[] minRowsBics = new int[]{80,100,170}; 
		int[] maxRowsBics = new int[]{100,120,200};
		int[] minColsBics = new int[]{8,9,10};
		int[] maxColsBics = new int[]{9,10,12};
		int nrLabels = 7;
	    type = PatternType.Constant;
    	List<List<Integer>> removalsSet = Arrays.asList(Arrays.asList(0,1,2),new ArrayList<Integer>());//,Arrays.asList(0));

		for(int i=2, l=numRows.length; i<l; i++){
	    	for(List<Integer> removalsI : removalsSet){
				removals = removalsI;
				System.out.println("["+numRows[i]+","+numColumns[i]+"]");
				BicMatrixGenerator generator = new BicMatrixGenerator(
						numRows[i],numColumns[i],10 /*|bics|*/,"random",nrLabels /*labels*/,true);
				Biclusters trueBics = generator.generateKBiclusters(type,
						"uniform", minRowsBics[i], maxRowsBics[i], 
						"uniform", minColsBics[i], maxColsBics[i], false, false, false);
				
				data = new Dataset(generator.getSymbolicExpressionMatrix());
				data.nrLabels = nrLabels;
				run();
	    	}
		}
	}
	
	private static void run() throws Exception {
		
		/** A: Additional Parameters **/
		double minOverlapMerging = 0.8;
		int minNrBicsBeforeMerging = 1; 
		boolean symmetries = false;

		/** B: Mapping and Closing **/
		Biclusterizer posthandler = new Biclusterizer(new BiclusterMerger(minOverlapMerging));
		if(realdata){
			data = Itemizer.run(data, data.nrLabels, symmetries, 
				NormalizationCriteria.Row, DiscretizationCriteria.NormalDist,
				NoiseRelaxation.None, FillingCriteria.RemoveValue);
		} else {
			data = Itemizer.run(data, data.nrLabels, symmetries, 
				NormalizationCriteria.None, DiscretizationCriteria.None,
				NoiseRelaxation.None, FillingCriteria.None);
		}
		data = ItemMapper.remove(data,removals);
		
		/** C: Define Pattern Miner **/
		PM pminer = null;
	   	if(type.equals(PatternType.OrderPreserving)){
			pminer = new SequentialPM();
		    if(constraints==null) ((SequentialPM)pminer).algorithm = SequentialImplementation.IndexSpan;
		    else ((SequentialPM)pminer).algorithm = SequentialImplementation.IndexSpanConstraint;
		} else {
			pminer = new SimpleFIM(); 
			if(constraints==null) ((SimpleFIM)pminer).algorithm = SimpleImplementation.F2G;//F2G;
			else ((SimpleFIM)pminer).algorithm = SimpleImplementation.F2GConstraint;
		}
	   	pminer.setSupport(0.1);
	   	//pminer.inputMinNrBics(minNrBicsBeforeMerging);
		pminer.inputMinColumns(minNrColumns);
	   	pminer.setConstraints(constraints);
		
		BiclusterMiner bicminer = null; 
	   	if(type.equals(PatternType.OrderPreserving)){
	   		bicminer = new OrderPreservingBiclusterMiner(data,(SequentialPM)pminer,posthandler,Orientation.PatternOnRows);		
	   	} else bicminer = new ConstantBiclusterMiner(data,pminer,posthandler,Orientation.PatternOnRows); 
		
		/** D: Run BicPAM **/		
		long time = System.currentTimeMillis();
		Biclusters bics = bicminer.mineBiclusters();
		time = System.currentTimeMillis() - time;
		
		bics.order();
		System.out.println("Time:"+((double)time/(double)1000)+"ms");
		BicResult.println("FOUND BICS:" + bics.toString(data.rows,data.columns));
		for(Bicluster bic : bics.getBiclusters()) BicResult.println(bic.toString(data)+"\n\n");
	}
}