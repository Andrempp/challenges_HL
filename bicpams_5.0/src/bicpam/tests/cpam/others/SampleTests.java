package tests.cpam.others;

import generator.BicMatrixGenerator.PatternType;
import java.util.List;
import domain.Bicluster;
import domain.Biclusters;
import domain.Dataset;
import utils.BicReader;
import utils.BicResult;
import utils.NetMatrixMapper;
import utils.others.CopyUtils;
import bicpam.bicminer.BiclusterMiner;
import bicpam.bicminer.BiclusterMiner.Orientation;
import bicpam.bicminer.coherent.AdditiveBiclusterMiner;
import bicpam.bicminer.coherent.MultiplicativeBiclusterMiner;
import bicpam.bicminer.coherent.SymmetricBiclusterMiner;
import bicpam.bicminer.constant.ConstantBiclusterMiner;
import bicpam.bicminer.order.OrderPreservingBiclusterMiner;
import bicpam.closing.BiclusterMerger;
import bicpam.closing.Biclusterizer;
import bicpam.mapping.Itemizer;
import bicpam.mapping.Itemizer.DiscretizationCriteria;
import bicpam.mapping.Itemizer.FillingCriteria;
import bicpam.mapping.Itemizer.NoiseRelaxation;
import bicpam.mapping.Itemizer.NormalizationCriteria;
import bicpam.pminer.fim.ClosedFIM;
import bicpam.pminer.spm.SequentialPM;
import bicpam.pminer.spm.SequentialPM.SequentialImplementation;


/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class SampleTests {

	private static String dataset; 

    public static void main(String[] args) throws Exception{
    	
		/** A: Read Dataset **/
    	dataset = "/data/sample/test2.txt";
		//Dataset data = new Dataset(BicReader.getConds(dataset,1),BicReader.getGenes(dataset),BicReader.getTable(dataset,1));
		Dataset data = NetMatrixMapper.getTable(dataset,0,1,2,"\t",false);
		System.out.println("Indexes: "+data.indexes);
		System.out.println("Scores: "+data.scores);
		

		/** B: Define Stopping Criteria **/
		int minNrBicsBeforeMerging = 1; //choose high number of biclusters
		int minNrColumns = 2;
		int nrIterations = 1; //increase for more even space exploration 

		/** C: Define Itemizer for Mapping **/
		int nrLabels = 4;
		boolean symmetries = false;
		data = Itemizer.run(data, nrLabels, symmetries, 
			NormalizationCriteria.None, 
			DiscretizationCriteria.SimpleRange,
			NoiseRelaxation.None, /* multi-item assignments */
			FillingCriteria.Replace);
						
		/** D: Define Biclusterizer for Closing **/
		double minOverlapMerging = 0.7;
		Biclusterizer posthandler = new Biclusterizer(new BiclusterMerger(minOverlapMerging));

		/** E: Define PMiner **/
		PatternType type = PatternType.Constant; /*coherence assumption*/
		BiclusterMiner bicminer = null; 
	   	if(type.equals(PatternType.OrderPreserving)){
			SequentialPM pminer = new SequentialPM();
		    pminer.algorithm = SequentialImplementation.PrefixSpan;
			pminer.inputMinNrBics(minNrBicsBeforeMerging);
			pminer.inputMinColumns(minNrColumns);
			bicminer = new OrderPreservingBiclusterMiner(data,pminer,posthandler,Orientation.PatternOnRows);		
		} else {
			ClosedFIM pminer = new ClosedFIM(); //;
			pminer.inputMinNrBics(minNrBicsBeforeMerging);
			pminer.inputMinColumns(minNrColumns);
			if(type.equals(PatternType.Additive)){
				bicminer = new AdditiveBiclusterMiner(data,pminer,posthandler,Orientation.PatternOnRows);
			} else if(type.equals(PatternType.Constant)){
				bicminer = new ConstantBiclusterMiner(data,pminer,posthandler,Orientation.PatternOnRows); 
			} else if(type.equals(PatternType.Symmetric)){
				bicminer = new SymmetricBiclusterMiner(data,pminer,posthandler,Orientation.PatternOnRows); 
			} else {
				bicminer = new MultiplicativeBiclusterMiner(data,pminer,posthandler,Orientation.PatternOnRows); 
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
		bics.order();
		BicResult.println("FOUND BICS:" + bics.toString(data.rows,data.columns));
		for(Bicluster bic : bics.getBiclusters()) BicResult.println(bic.toString(data)+"\n\n");
	}
}