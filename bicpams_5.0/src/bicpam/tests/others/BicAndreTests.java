package tests.others;

import java.io.BufferedReader;
import java.io.InputStreamReader;
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
public class BicAndreTests {

	private static String dataset; 

    public static void main(String[] args) throws Exception{
    	
    	ArrayList<String> params = new ArrayList<String>();
    	
		/** A: Read Dataset **/
    	int classIndex = -1;
    	dataset = "/data/result_temp.csv";
    	dataset = new File("").getAbsolutePath()+dataset;
		Dataset data = dataset.contains(".arff") ?
				new Dataset(BicReader.getInstances(dataset),classIndex) :
				new Dataset(BicReader.getConds(dataset,1,","),BicReader.getGenes(dataset,","),BicReader.getTable(dataset,1,","),classIndex);
		//BicResult.println(data.toString(false));

		/** B: Define Homogeneity **/
		PatternType type = PatternType.Constant; /*coherence assumption*/
		Orientation orientation = Orientation.PatternOnRows;
		/** C: Define Stopping Criteria **/
		boolean useSup = false;
		int minNrBicsBeforeMerging = 100; //choose high number of biclusters
		double minSupport = 0.4;
		int minNrColumns = 3;
		int nrIterations = 3; //increase for more even space exploration 
		double minlift = 1.15;
		int train = 0;
		int nrLabels = 4;
		boolean filter_by_lift = false;

		boolean from_line = true;
		boolean running = true;
		
		if (from_line==true) {
			System.out.println("Input number bics before merging ; number iterations; min lift; train (1) or test (2); number labels");
			minNrBicsBeforeMerging = Integer.parseInt(args[0]);
			nrIterations = Integer.parseInt(args[1]);
			minlift = Double.parseDouble(args[2]);
			train = Integer.parseInt(args[3]);
			nrLabels = Integer.parseInt(args[4]);
			filter_by_lift = Boolean.parseBoolean(args[5]);
			System.out.println(filter_by_lift);

		}
		
		/** D: Define Itemizer for Mapping **/
		boolean symmetries = false;
		data = Itemizer.run(data, nrLabels, symmetries, 
			NormalizationCriteria.Column, 
			DiscretizationCriteria.NormalDist,
			NoiseRelaxation.OptionalItem, /* multi-item assignments */
			FillingCriteria.RemoveValue);
		
		/** Extra: Define name of result file**/
		if (running == false) {
			params.add(type.toString());
			params.add(Integer.toString(nrIterations));
			params.add(Double.toString(minlift % 1).substring(2));
			if (useSup) {params.add("s" + Double.toString(minSupport%1).substring(2));}
			if (nrLabels != 4) {params.add("l" + Integer.toString(nrLabels));}
			if (minNrBicsBeforeMerging != 100) {params.add("it" + Integer.toString(minNrBicsBeforeMerging));}
		}else {
			params.add("temp" + train);
		}
		BicResult.filename(params);
		BicResult.println(data.toString(false));

		if (train != 2) {
			
			/** E: Define Biclusterizer for Closing **/
			double minOverlapMerging = 0.8;
			double minSimilarity = 0.5;
			BiclusterMerger bicMerger = null;
			BiclusterFilter bicFilter = new BiclusterFilter(minSimilarity);
			
			if (!filter_by_lift) {
				bicMerger  = new BiclusterMerger(minOverlapMerging);
				//bicFilter = new BiclusterFilter(minSimilarity);
				System.out.println("NOT filtering by lift");

			}
			else {
				System.out.println("Filtering by lift");
			}
			Biclusterizer posthandler = new Biclusterizer(
					bicMerger,
					bicFilter
					);
			
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
				if (useSup) {
					pminer.setSupport(minSupport);
					System.out.println("using support");
				}else {
					pminer.inputMinNrBics(minNrBicsBeforeMerging);
				}
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
}