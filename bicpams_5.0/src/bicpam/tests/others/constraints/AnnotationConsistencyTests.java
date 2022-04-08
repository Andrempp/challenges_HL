package tests.others.constraints;

import generator.BicMatrixGenerator.PatternType;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import domain.Bicluster;
import domain.Biclusters;
import domain.Dataset;
import domain.constraint.AnnotationConstraint;
import domain.constraint.Constraints;
import utils.BicMath;
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
import bicpam.pminer.fim.SimpleFIM;
import bicpam.pminer.fim.SimpleFIM.SimpleImplementation;


/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class AnnotationConsistencyTests {

	
    public static void main(String[] args) throws Exception{
    	
    	/** 1: Accuracy and efficiency for the discovery of label-consistent biclusters from data with different distributions of annotations */
    	syntheticAnnotatedDataTests();
    	
    	/** 2: Accuracy and efficiency for the discovery of GO term-consistent biclusters (annotated expression data) */
    	//realAnnotatedDataTests();
    }
    
    private static void realAnnotatedDataTests() throws Exception {
		List<String> datasets = new ArrayList<String>();
		//datasets.add("/data/networks/4932.scerevisae.protein.links.v10.txt");
		//datasets.add("/data/networks/ecoliB.protein.links.v10.txt");
    	datasets.add("/data/arff/dlblc.arff");
    	//datasets.add("/data/arff/gyeast.arff");
    	AnnotatedTestingData annotator = new AnnotatedTestingData(datasets);
    	List<Dataset> realData = annotator.getDatasets();
    	for(Dataset data : realData){
    		Biclusters res = runConsistencyBiclustering(data,true);
    		//enrichmentAnalysis(res);
    	}
	}

	private static void syntheticAnnotatedDataTests() throws Exception {
		List<Integer> avgNrRowsPerAnnotation = Arrays.asList(200,100);
		List<Integer> avgNrAnnotationsPerRow = Arrays.asList(10,4);
		AnnotatedTestingData annotator = new AnnotatedTestingData(avgNrRowsPerAnnotation,avgNrAnnotationsPerRow);
    	List<Dataset> syntheticData = annotator.getDatasets();
    	List<Biclusters> consistentBics = annotator.getConsistentBiclusters();
    	for(int i=0, l=syntheticData.size(); i<l; i++){
    		Biclusters res = runConsistencyBiclustering(syntheticData.get(i),false);
    		compare(res,consistentBics.get(i));
    	}
	}

	private static Biclusters runConsistencyBiclustering(Dataset data, boolean realdata) throws Exception {
		
		/** A: Additional Parameters **/
		int minNrColumns = 5;
		double minOverlapMerging = 0.8;
		//double minSupport = 0.1;
		boolean symmetries = false;
		int minNrBicsBeforeMerging = 1; 

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
		List<Integer> removals = new ArrayList<Integer>();
		data = ItemMapper.remove(data,removals);
		
		/** C: Define Pattern Miner **/
		PM pminer = new SimpleFIM(); 
		((SimpleFIM)pminer).algorithm = SimpleImplementation.F2GConstraint;
		
		Constraints constraints = new Constraints(data.nrLabels,true);
		constraints.add(new AnnotationConstraint(data.columns.size()));
	   	pminer.setConstraints(constraints);
		
	   	pminer.inputMinNrBics(minNrBicsBeforeMerging);
	   	//pminer.setSupport(minSupport);
		pminer.inputMinColumns(minNrColumns);
		
		ConstantBiclusterMiner bicminer = new ConstantBiclusterMiner(data,pminer,posthandler,Orientation.PatternOnRows); 
		
		/** D: Run BicPAM **/	
		long time = System.currentTimeMillis();
		Biclusters bics = bicminer.mineBiclusters();
		time = System.currentTimeMillis() - time;
		
		bics.order();
		System.out.println("Time:"+((double)time/(double)1000)+"ms");
		//BicResult.println("FOUND BICS:" + bics.toString(data.rows,data.columns));
		for(Bicluster bic : bics.getBiclusters()) BicResult.println(bic.toString());
		return bics;
	}
	
	private static void compare(Biclusters foundBics, Biclusters trueBics) {
	}
	
	/*List<Integer> list = Arrays.asList(40, 40, 41, 41, 42, 43, 45, 46, 46, 47, 48, 49, 50, 50, 50, 54, 54, 54, 56, 57, 58, 59, 61, 62, 62, 65, 72, 77, 81, 85, 86, 87, 89, 122, 136, 155, 183, 188, 206, 225, 227, 392);
	System.out.println(">>"+BicMath.sum(list));
	System.out.println(">>"+BicMath.average(list));
	System.out.println(">>"+BicMath.standardVariation(list));*/

}