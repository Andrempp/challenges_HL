package tests.bicpam;

import generator.BicMatrixGenerator.PatternType;
import java.io.File;
import java.util.List;
import domain.Bicluster;
import domain.Biclusters;
import domain.Dataset;
import utils.BicReader;
import utils.BicResult;
import utils.others.CopyUtils;
import bicpam.bicminer.BiclusterMiner;
import bicpam.bicminer.BiclusterMiner.Orientation;
import bicpam.bicminer.coherent.AdditiveBiclusterMiner;
import bicpam.bicminer.coherent.MultiplicativeBiclusterMiner;
import bicpam.bicminer.coherent.SymmetricBiclusterMiner;
import bicpam.bicminer.constant.ConstantBiclusterMiner;
import bicpam.bicminer.constant.ConstantOverallBiclusterMiner;
import bicpam.bicminer.order.OrderPreservingBiclusterMiner;
import bicpam.closing.BiclusterFilter;
import bicpam.closing.BiclusterMerger;
import bicpam.closing.Biclusterizer;
import bicpam.mapping.Itemizer;
import bicpam.mapping.Itemizer.DiscretizationCriteria;
import bicpam.mapping.Itemizer.FillingCriteria;
import bicpam.mapping.Itemizer.NoiseRelaxation;
import bicpam.mapping.Itemizer.NormalizationCriteria;
import bicpam.pminer.fim.MaximalFIM;
import bicpam.pminer.spm.SequentialPM;
import bicpam.pminer.spm.SequentialPM.SequentialImplementation;


/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class BicRealTests {

	private static String dataset; 

    public static void main(String[] args) throws Exception{
    	
		/** A: Read Dataset **/
    	String delimeter = "\t";
    	dataset = "/data/arff/gyeast.arff";
    	dataset = new File("").getAbsolutePath()+dataset;
		Dataset data = dataset.contains(".arff") ?
				new Dataset(BicReader.getInstances(dataset)) :
				new Dataset(BicReader.getConds(dataset,1,delimeter),BicReader.getGenes(dataset,delimeter),BicReader.getTable(dataset,1,delimeter));

		/** B: Define Homogeneity **/
		PatternType type = PatternType.Constant; /*coherence assumption*/
		Orientation orientation = Orientation.PatternOnRows;
		
		/** C: Define Stopping Criteria **/
		int minNrBicsBeforeMerging = 40; //choose high number of biclusters
		int minNrColumns = 3;
		int nrIterations = 2; //increase for more even space exploration 

		/** D: Define Itemizer for Mapping **/
		int nrLabels = 5;
		boolean symmetries = false;
		data = Itemizer.run(data, nrLabels, symmetries, 
			NormalizationCriteria.Row, 
			DiscretizationCriteria.NormalDist,
			NoiseRelaxation.None, /* multi-item assignments */
			FillingCriteria.RemoveValue);
						
		/** E: Define Biclusterizer for Closing **/
		double minOverlapMerging = 0.7;
		double minSimilarity = 0.5;
		Biclusterizer posthandler = new Biclusterizer(new BiclusterMerger(minOverlapMerging),
				new BiclusterFilter(minSimilarity));

		/** F: PMiner **/
		BiclusterMiner bicminer = null; 
	   	if(type.equals(PatternType.OrderPreserving)){
			SequentialPM pminer = new SequentialPM();
		    pminer.algorithm = SequentialImplementation.PrefixSpan;
			pminer.inputMinNrBics(minNrBicsBeforeMerging);
			pminer.inputMinColumns(minNrColumns);
			bicminer = new OrderPreservingBiclusterMiner(data,pminer,posthandler,orientation);		
		} else {
			MaximalFIM pminer = new MaximalFIM(); //new ClosedFIM();
			pminer.inputMinNrBics(minNrBicsBeforeMerging);
			pminer.inputMinColumns(minNrColumns);
			if(type.equals(PatternType.Additive)){
				bicminer = new AdditiveBiclusterMiner(data,pminer,posthandler,orientation);
			} else if(type.equals(PatternType.Constant)){
				bicminer = new ConstantBiclusterMiner(data,pminer,posthandler,orientation); 
			} else if(type.equals(PatternType.Symmetric)){
				bicminer = new SymmetricBiclusterMiner(data,pminer,posthandler,orientation); 
			} else if(type.equals(PatternType.ConstantOverall)){
				bicminer = new ConstantOverallBiclusterMiner(data,pminer,posthandler,orientation); 
			} else {
				bicminer = new MultiplicativeBiclusterMiner(data,pminer,posthandler,orientation); 
			}
		}
		
		/** G: Run BicPAM **/		
		long time = System.currentTimeMillis();
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
		time = System.currentTimeMillis() - time;

		/** H: Output and Evaluation **/		
		System.out.println("Time:"+((double)time/(double)1000)+"ms");
		BicResult.println("FOUND BICS:" + bics.toString(data.rows,data.columns));
		for(Bicluster bic : bics.getBiclusters()) BicResult.println(bic.toString(data)+"\n\n");
	}
}