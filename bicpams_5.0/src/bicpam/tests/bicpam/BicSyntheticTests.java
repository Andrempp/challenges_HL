package tests.bicpam;

import utils.BicPrinting;
import utils.BicResult;
import generator.BicMatrixGenerator;
import generator.BicMatrixGenerator.PatternType;
import generator.BicMatrixGenerator.PlaidCoherency;
import domain.Biclusters;
import domain.Dataset;
import bicpam.bicminer.BiclusterMiner;
import bicpam.bicminer.BiclusterMiner.Orientation;
import bicpam.bicminer.coherent.AdditiveBiclusterMiner;
import bicpam.bicminer.coherent.MultiplicativeBiclusterMiner;
import bicpam.bicminer.coherent.SymmetricBiclusterMiner;
import bicpam.bicminer.constant.ConstantBiclusterMiner;
import bicpam.bicminer.constant.ConstantOverallBiclusterMiner;
import bicpam.bicminer.order.OrderPreservingBiclusterMiner;
import bicpam.closing.BiclusterMerger;
import bicpam.closing.Biclusterizer;
import bicpam.evaluation.MatchMetrics;
import bicpam.mapping.Discretizer;
import bicpam.mapping.Itemizer;
import bicpam.mapping.Itemizer.NoiseRelaxation;
import bicpam.mapping.MissingsHandler;
import bicpam.mapping.Normalizer;
import bicpam.mapping.Itemizer.DiscretizationCriteria;
import bicpam.mapping.Itemizer.FillingCriteria;
import bicpam.mapping.Itemizer.NormalizationCriteria;
import bicpam.pminer.PM;
import bicpam.pminer.fim.ClosedFIM;
import bicpam.pminer.fim.MaximalFIM;
import bicpam.pminer.fim.SimpleFIM;
import bicpam.pminer.fim.SimpleFIM.SimpleImplementation;
import bicpam.pminer.spm.SequentialPM;
import bicpam.pminer.spm.SequentialPM.SequentialImplementation;

/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class BicSyntheticTests {

	static String background = "random";
	static PM[] pminers = new PM[]{new ClosedFIM()};
	static Biclusterizer bichandler = new Biclusterizer();
    static int[] minRowsSup, minColsSup;

	//Generator properties
    static boolean defaultdataset, symmetries;
	static int[] numRows, numColumns, numBics, alphabets;
	static String distRowsBics = "Uniform", distColsBics = "Uniform";
    static int[] minNrBics, minRowsBics, minColsBics, maxRowsBics, maxColsBics;
    static double[] minArea;
	
    public static void main(String[] args) throws Exception{
		
		/** DATA VARIABLES **/
		alphabets = new int[]{8}; 
		defaultdataset = true; /*500x70*/
		symmetries = true;
		numRows = new int[]{100,500,1000};
		numColumns = new int[]{40,70,100};
		numBics = new int[]{2,5,10};
	    minRowsBics = new int[]{14,25,30}; 
	    maxRowsBics = new int[]{20,30,40};
	    minColsBics = new int[]{7,8,9};
	    maxColsBics = new int[]{7,9,10};
		
		/** PARAMETERS **/
		minNrBics = new int[]{100,400,800}; //option 1 
		minColsSup = new int[]{6,7,8}; //option 1,2,3
		minArea = new double[]{0.1,0.04,0.03}; //option 2
	    minRowsSup = new int[]{10,15,20}; //option 3
	    
	    bichandler.setMerger(new BiclusterMerger(0.6));
	    
		/** RUN BICPAM WITHOUT CLOSING OPTIONS (<<1min for all options) **/

	    run(PatternType.Constant);
	    //run(PatternType.Symmetric);
	    
	    /* fix odd nr of items (|L|%2!=0) when using symmetries with the additive model
	     * to guarantee the presence of the 0 item (validity of shifts) */
		//alphabets = new int[]{9}; 
	    //run(PatternType.Additive);

	    /* fix even nr of items (|L|%2==0) when using symmetries with the multiplicative model
	     * 0 has no meaning when using shifting factors */
	    //alphabets = new int[]{8}; 
	    //run(PatternType.Multiplicative);
	    
		//runOrderPreserving();
    }

	private static void run(PatternType type) throws Exception {
		
	    for(int alphabet : alphabets){
	    	BicResult.println("ALPHABET::"+alphabet);
	    	int i = defaultdataset ? 1 : 0;
	    	int l = defaultdataset ? 2 : numRows.length;
	    		    	
			for(; i<l; i++){
				
				/** GENERATION OF THE SYNTHETIC DATASET **/
				//use alternative generation procedures for further customization (e.g. prevent overlapping or add plaid effects)
				
				BicResult.println("["+numRows[i]+","+numColumns[i]+"]("+numBics[i]+")");
				BicMatrixGenerator generator = new BicMatrixGenerator(numRows[i],numColumns[i],numBics[i],background,alphabet,symmetries);
				Biclusters trueBics = generator.generateKBiclusters(type,
						distRowsBics, minRowsBics[i], maxRowsBics[i], 
						distColsBics, minColsBics[i], maxColsBics[i], false, false, false);
				
				int[][] dataset = generator.getSymbolicExpressionMatrix();
				//double[][] newdataset = generator.putNoise(dataset,0.01);
				//newdataset = generator.putMissings(newdataset,0.01);
				
				Dataset data = new Dataset(dataset);
				data.nrLabels = alphabet;
				
				/*for(Bicluster bic : trueBics.getBiclusters())
					BicResult.println(BicPrinting.plot(data.getRealValues(bic.rows,bic.columns)));
				BicResult.println(BicPrinting.plot(data.getItems()));*/

				
				/** PARAMETERIZE AND RUN BICPAM **/
				for(PM pminer : pminers){
					pminer.inputMinArea(minArea[i]);
					pminer.inputMinNrBics(minNrBics[i]);
					pminer.inputParams(minColsSup[i],minRowsSup[i]);
					
					BiclusterMiner bicminer = null;
					Orientation orientation = Orientation.PatternOnRows; //constant on columns/across rows
					if(type.equals(PatternType.Constant)) 
						bicminer = new ConstantBiclusterMiner(data,pminer,bichandler,orientation);
					else if(type.equals(PatternType.Additive))
						bicminer = new AdditiveBiclusterMiner(data,pminer,bichandler,orientation);
					else if(type.equals(PatternType.Multiplicative))
						bicminer = new MultiplicativeBiclusterMiner(data,pminer,bichandler,orientation);
					else if(type.equals(PatternType.Symmetric))
						bicminer = new SymmetricBiclusterMiner(data,pminer,bichandler,orientation);
					else if(type.equals(PatternType.ConstantOverall))
						bicminer = new ConstantOverallBiclusterMiner(data,pminer,bichandler,orientation); 
					else bicminer = new OrderPreservingBiclusterMiner(data,(SequentialPM)pminer,bichandler,orientation);
					core(bicminer,trueBics);
				}
			}
		}
	}

	private static void core(BiclusterMiner bicminer, Biclusters trueBics) throws Exception {
		
		long time = System.currentTimeMillis();
		Biclusters bics = bicminer.mineBiclusters();
		time = System.currentTimeMillis() - time;
		
		/** PRINTING (>> output/results.txt <<) and EVALUATION **/
		BicResult.println("TRUE BICS: " + trueBics.toString());
		BicResult.println("FOUND BICS: " + bics.toString());
		BicResult.println("TIME: "+((double)time/(double)1000)+"s");
		BicResult.println(MatchMetrics.run(bics, trueBics));
		//FabiaConsensus requires Munkres algorithm: can be expensive for large solutions
		//BicResult.println(MatchMetrics.runFabiaConsensus(bics, trueBics));
	}
	
	public static void runOrderPreserving() throws Exception {
		defaultdataset = true; /*500x70*/
		alphabets = new int[]{200};
		numRows = new int[]{100,500,1000};
		numColumns = new int[]{40,70,100};
		
		numBics = new int[]{3,4,6};
	    minRowsBics = new int[]{14,40,65}; 
	    maxRowsBics = new int[]{16,45,70};
	    minColsBics = new int[]{6,6,8};
	    maxColsBics = new int[]{6,8,10};
		
		/** PARAMETERS **/
		minNrBics = new int[]{5,10,20}; //option 1 
		minColsSup = new int[]{6,6,6}; //option 1,2,3
		minArea = new double[]{0.1,0.04,0.03}; //option 2
	    minRowsSup = new int[]{12,35,60}; //option 3

		SequentialPM spm = new SequentialPM();
		spm.algorithm = SequentialImplementation.IndexSpan;
	    spm.setOverlap(1); 
		pminers = new PM[]{spm};

	    run(PatternType.OrderPreserving);
	}
}