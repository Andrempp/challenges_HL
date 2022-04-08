package tests.others.constraints;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import domain.Bicluster;
import domain.Biclusters;
import domain.Dataset;
import utils.BicReader;
import utils.BicResult;
import utils.NetMatrixMapper;
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
public class UninformativeRealTests {

	
    public static void main(String[] args) throws Exception{
    	String path = new File("").getAbsolutePath();
		List<String> datasets = new ArrayList<String>();
		//datasets.add("/data/networks/4932.scerevisae.protein.links.v10.txt");
		//datasets.add("/data/networks/ecoliB.protein.links.v10.txt");
		//datasets.add("/data/networks/protein.hsapiens.links.v10.txt");
		//datasets.add("/data/arff/hughes.arff");
    	datasets.add("/data/arff/dlblc.arff");
    	//datasets.add("/data/arff/yeast80.arff");
    	//datasets.add("/data/arff/gyeast.arff");
    	//datasets.add("/data/arff/gasch.txt");
    	
    	List<List<Integer>> removalsSet = Arrays.asList(Arrays.asList(0,1,2),new ArrayList<Integer>());//,Arrays.asList(0));
    	
    	for(String dataset : datasets){
	    	for(List<Integer> removals : removalsSet){
	    		Dataset data = null;
				if(dataset.contains("network")) data = NetMatrixMapper.getTable(path+dataset,0,1,2," ",true);
				else if(dataset.contains(".arff")) data = new Dataset(BicReader.getInstances(path+dataset));
				else data = new Dataset(BicReader.getConds(path+dataset,7,"\t"),BicReader.getGenes(path+dataset,"\t"),BicReader.getTable(path+dataset,7,"\t"));
				data.nrLabels=5;
				run(data,removals);
	    	}
    	}
	}
	
	private static void run(Dataset data, List<Integer> removals) throws Exception {
		
		/** A: Additional Parameters **/
		double minOverlapMerging = 0.8;
		int minNrBicsBeforeMerging = 10; 
		boolean symmetries = false;
		int minNrColumns = 4;

		/** B: Mapping and Closing **/
		Biclusterizer posthandler = new Biclusterizer(new BiclusterMerger(minOverlapMerging));
		data = Itemizer.run(data, data.nrLabels, symmetries, 
				NormalizationCriteria.Row, DiscretizationCriteria.NormalDist,
				NoiseRelaxation.None, FillingCriteria.RemoveValue);
		data = ItemMapper.remove(data,removals);
		
		/** C: Define Pattern Miner **/
		PM pminer = new ClosedFIM(); 
	   	//pminer.setSupport(0.1);
	   	pminer.inputMinNrBics(minNrBicsBeforeMerging);
		pminer.inputMinColumns(minNrColumns);
	   	//pminer.setConstraints(constraints);
		
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