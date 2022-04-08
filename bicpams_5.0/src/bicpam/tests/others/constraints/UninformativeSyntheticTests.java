package tests.others.constraints;

import generator.BicMatrixGenerator;
import generator.BicMatrixGenerator.PatternType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import domain.Bicluster;
import domain.Biclusters;
import domain.Dataset;
import utils.BicResult;
import bicpam.bicminer.BiclusterMiner;
import bicpam.bicminer.BiclusterMiner.Orientation;
import bicpam.bicminer.constant.ConstantBiclusterMiner;
import bicpam.closing.BiclusterMerger;
import bicpam.closing.Biclusterizer;
import bicpam.mapping.ItemMapper;
import bicpam.mapping.Itemizer;
import bicpam.mapping.Itemizer.DiscretizationCriteria;
import bicpam.mapping.Itemizer.FillingCriteria;
import bicpam.mapping.Itemizer.NoiseRelaxation;
import bicpam.mapping.Itemizer.NormalizationCriteria;
import bicpam.pminer.PM;
import bicpam.pminer.fim.ClosedFIM;


/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class UninformativeSyntheticTests {

	List<Integer> removals = new ArrayList<Integer>();
	
    public static void main(String[] args) throws Exception{
    	
		int nrLabels = 7;
		PatternType type = PatternType.Constant;
		int[] numRows = new int[]{500,1000,2000};
		int[] numColumns = new int[]{80,100,200};
		int[] minRowsBics = new int[]{80,100,170}; 
		int[] maxRowsBics = new int[]{100,120,200};
		int[] minColsBics = new int[]{8,9,10};
		int[] maxColsBics = new int[]{9,10,12};
		
    	List<List<Integer>> removalsSet = Arrays.asList(Arrays.asList(0,1,2),new ArrayList<Integer>());//,Arrays.asList(0));

		for(int i=2, l=numRows.length; i<l; i++){
	    	for(List<Integer> removals : removalsSet){
				System.out.println("["+numRows[i]+","+numColumns[i]+"]");
				BicMatrixGenerator generator = new BicMatrixGenerator(
						numRows[i],numColumns[i],10 /*|bics|*/,"random",nrLabels /*labels*/,true);
				Biclusters trueBics = generator.generateKBiclusters(type,
						"uniform", minRowsBics[i], maxRowsBics[i], 
						"uniform", minColsBics[i], maxColsBics[i], false, false, false);
				
				Dataset data = new Dataset(generator.getSymbolicExpressionMatrix());
				data.nrLabels = nrLabels;
				run(data,removals);
	    	}
		}
	}
	
	private static void run(Dataset data, List<Integer> removals) throws Exception {
		
		/** A: Additional Parameters **/
		int minNrColumns = 3;
		double minOverlapMerging = 0.8;
		double minSupport = 0.1;
		int minNrBicsBeforeMerging = 1; 
		boolean symmetries = false;

		/** B: Mapping and Closing **/
		Biclusterizer posthandler = new Biclusterizer(new BiclusterMerger(minOverlapMerging));
		data = Itemizer.run(data, data.nrLabels, symmetries, 
				NormalizationCriteria.None, DiscretizationCriteria.None,
				NoiseRelaxation.None, FillingCriteria.None);
		data = ItemMapper.remove(data,removals);
		
		/** C: Define Pattern Miner **/
		PM pminer = new ClosedFIM(); 
	   	pminer.setSupport(minSupport);
	   	//pminer.inputMinNrBics(minNrBicsBeforeMerging);
		pminer.inputMinColumns(minNrColumns);
		
		BiclusterMiner bicminer = new ConstantBiclusterMiner(data,pminer,posthandler,Orientation.PatternOnRows); 
		
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