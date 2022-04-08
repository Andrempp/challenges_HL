package tests.others.constraints;

import generator.BicMatrixGenerator.PatternType;
import java.io.File;
import domain.Bicluster;
import domain.Biclusters;
import domain.Dataset;
import domain.constraint.Constraints;
import utils.BicResult;
import bicpam.bicminer.BiclusterMiner;
import bicpam.bicminer.BiclusterMiner.Orientation;
import bicpam.bicminer.constant.ConstantBiclusterMiner;
import bicpam.bicminer.order.OrderPreservingBiclusterMiner;
import bicpam.closing.BiclusterMerger;
import bicpam.closing.Biclusterizer;
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
public class ConstraintRealTests {

	static String path = new File("").getAbsolutePath();
	
    public static void main(String[] args) throws Exception{
    	
    	/** 1: Efficiency with anti-monotone, monotone and convertible constraints for gasch dataset (constant coherency) */
    	//realSuccinct();
    	//realMonotone();
    	//realAntiMonotone();
    	
    	/** 2: Biological relevance multiple profiles of expression */

    }
    
	public static void realMonotone() throws Exception {}
	public static void realAntiMonotone() throws Exception {}
	public static void realSuccinct() throws Exception {}
		
	public static void run(Dataset data, Constraints constraints) throws Exception {
		
		/** A: Additional Parameters **/
		PatternType type = PatternType.Constant;
		double minOverlapMerging = 0.8;
		int minNrColumns = 3;
		int minNrBicsBeforeMerging = 10; 
		boolean symmetries = false;

		/** B: Mapping and Closing **/
		Biclusterizer posthandler = new Biclusterizer(new BiclusterMerger(minOverlapMerging));
		data = Itemizer.run(data, data.nrLabels, symmetries, 
				NormalizationCriteria.Row, DiscretizationCriteria.NormalDist,
				NoiseRelaxation.None, FillingCriteria.RemoveValue);
		
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
	   	//pminer.setSupport(0.1);
	   	pminer.inputMinNrBics(minNrBicsBeforeMerging);
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