package tests.others;

import generator.BicMatrixGenerator.PatternType;
import performance.significance.BSignificance;
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
import bicpam.pminer.fim.ClosedFIM;
import bicpam.pminer.spm.SequentialPM;
import bicpam.pminer.spm.SequentialPM.SequentialImplementation;
import java.util.ArrayList; 


/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */


public class BicAndreData {

	private static String dataset; 

    public static void main(String[] args) throws Exception{
    	
    	ArrayList<String> params = new ArrayList<String>();
    	
		/** A: Read Dataset **/
    	int classIndex = -1;
    	dataset = "/data/andre.csv";
    	dataset = new File("").getAbsolutePath()+dataset;
		Dataset data = dataset.contains(".arff") ?
				new Dataset(BicReader.getInstances(dataset),classIndex) :
				new Dataset(BicReader.getConds(dataset,1,","),BicReader.getGenes(dataset,","),BicReader.getTable(dataset,1,","),classIndex);
		//BicResult.println(data.toString(false));

		/** B: Define Homogeneity **/
		PatternType type = PatternType.Constant; /*coherence assumption*/
		Orientation orientation = Orientation.PatternOnRows;
		/** C: Define Stopping Criteria **/
		int minNrBicsBeforeMerging = 100; //choose high number of biclusters
		int minNrColumns = 3;
		int nrIterations = 3; //increase for more even space exploration 
		double minlift = 1.25;

		/** D: Define Itemizer for Mapping **/
		int nrLabels = 4;
		boolean symmetries = false;
		data = Itemizer.run(data, nrLabels, symmetries, 
			NormalizationCriteria.Column, 
			DiscretizationCriteria.NormalDist,
			NoiseRelaxation.OptionalItem, /* multi-item assignments */
			FillingCriteria.RemoveValue);
		
		
		
		/** Extra: Define name of result file**/
		params.add(type.toString());
		params.add(Integer.toString(nrIterations));
		params.add(Double.toString(minlift % 1).substring(2));
		BicResult.filename(params);
		
		

						
		/** E: Define Biclusterizer for Closing **/
		double minOverlapMerging = 0.8;
		double minSimilarity = 0.5;
		Biclusterizer posthandler = new Biclusterizer(
				new BiclusterMerger(minOverlapMerging),
				new BiclusterFilter(minSimilarity));

		/** F: PMiner **/
		BiclusterMiner bicminer = null; 
	   	if(type.equals(PatternType.OrderPreserving)){
			SequentialPM pminer = new SequentialPM();
		    pminer.algorithm = SequentialImplementation.PrefixSpan;
			pminer.inputMinNrBics(minNrBicsBeforeMerging);
			pminer.inputMinColumns(minNrColumns);
			pminer.setClass(data.classValues, minlift);
			bicminer = new OrderPreservingBiclusterMiner(data,pminer,posthandler,orientation);		
		} else {
			ClosedFIM pminer = new ClosedFIM();
			pminer.inputMinNrBics(minNrBicsBeforeMerging);
			pminer.inputMinColumns(minNrColumns);
			pminer.setClass(data.classValues, minlift);
			//pminer.setTargetClass(targetClass,classSuperiority);
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
		bics.computePatterns(data, orientation);
		BSignificance.run(data, bics);
		bics.orderPValue();
		time = System.currentTimeMillis() - time;

		/** H: Output and Evaluation **/
		System.out.println("Time:"+((double)time/(double)1000)+"ms");
		BicResult.println("FOUND BICS:" + bics.toString(data.rows,data.columns));
		for(Bicluster bic : bics.getBiclusters()) BicResult.println(bic.toString(data)+"\n\n");
	}
}