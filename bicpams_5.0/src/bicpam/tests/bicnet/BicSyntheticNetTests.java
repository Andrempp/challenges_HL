package tests.bicnet;

import utils.BicResult;
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
import bicpam.pminer.fim.ClosedFIM;
import bicpam.pminer.spm.SequentialPM;
import domain.Bicluster;
import domain.Biclusters;
import domain.Dataset;
import generator.BicMatrixGenerator.PatternType;
import generator.BicNetsGenerator;


/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class BicSyntheticNetTests {
	
	/** Run BicNET over synthetic network data **/				
	public static void main(String[] args) throws Exception {

		/** Part 1: parameterize properties of synthetic data **/				
       	int alphabet = 8; /*coherence strength*/
        int[] numNodes = new int[]{100,200,500,1000,2000,10000};
    	double density = 0.2; //0.1,0.3
		boolean defaultdataset = true; /*1000 nodes*/

        int[] numBics = new int[]{3,6,10,15,20,30};

       	PatternType[] types = new PatternType[]{PatternType.Multiplicative};
			//PatternType.Constant,PatternType.Symmetric,PatternType.OrderPreserving
    	double[] numBicRowsMin = new double[]{ 8,10,15,25,40,50}, numBicColsMin = new double[]{5,5,6,8,10,16}; 
    	double[] numBicRowsMax = new double[]{10,15,20,30,50,70}, numBicColsMax = new double[]{5,6,8,10,12,20};

		/** Part 2: parameterize BicNET stopping criteria **/
	  	int minNrModules = 20;
	  	int minNodesI = 7, minNodesJ;
	  	double minOverlapMerging = 0.7;
    	
	    for(PatternType type : types){
	    	int i = defaultdataset ? 1 : 0;
		    int l = defaultdataset ? 2 : numNodes.length;
			for(; i<l; i++){
				if(type.equals(PatternType.OrderPreserving)) alphabet=1000;
				else if(alphabet%2==0 && type.equals(PatternType.Additive)) alphabet++;

				/** Part 3: generate net and modules **/				
				BicNetsGenerator generator = new BicNetsGenerator(numNodes[i],density,numBics[i],alphabet,true);
			  	Biclusters trueBics = generator.generateKBiclusters(type,
			  		"uniform", numBicRowsMin[i], numBicRowsMax[i],  //rows
			  		"uniform", numBicColsMin[i], numBicColsMax[i]); //columns
			  	//generator.plantNoisyElements(noise);
			  	//generator.putMissings(missings);
			  	Dataset dataset = generator.getDataset();
			  	BicResult.println(trueBics.toString());
			  	//BicResult.println(dataset.toString());
			  	
				/**************************/				
				/********* BicNET *********/				
				/**************************/				
			  	
				/** Part 4: instantiate BicNET **/
			  	minNodesJ = (int)numBicColsMin[i];
				BiclusterMiner bicminer;
				Orientation orientation = Orientation.PatternOnRows;
				Biclusterizer posthandler = new Biclusterizer(new BiclusterMerger(minOverlapMerging));
				if(type.equals(PatternType.OrderPreserving)){
				  	SequentialPM spminer = new SequentialPM(); /*order-preserving modules*/
					spminer.inputMinNrBics(minNrModules);
					spminer.inputParams(minNodesJ, minNodesI);
					bicminer = new OrderPreservingBiclusterMiner(dataset,spminer,posthandler,orientation);
				} else {
				  	ClosedFIM pminer = new ClosedFIM(); /*others: maximal and simple FIM*/
					pminer.inputMinNrBics(minNrModules);
					pminer.inputParams(minNodesJ, minNodesI);
					if(type.equals(PatternType.Symmetric)) bicminer = new SymmetricBiclusterMiner(dataset,pminer,posthandler,orientation);
					else if(type.equals(PatternType.Additive)) bicminer = new AdditiveBiclusterMiner(dataset,pminer,posthandler,orientation);
					else if(type.equals(PatternType.Multiplicative)) bicminer = new MultiplicativeBiclusterMiner(dataset,pminer,posthandler,orientation);
					else if(type.equals(PatternType.ConstantOverall)) bicminer = new ConstantOverallBiclusterMiner(dataset,pminer,posthandler,orientation); 
					else bicminer = new ConstantBiclusterMiner(dataset,pminer,posthandler,orientation);
				}

				/** Part 5: run BicNET **/
				long time = System.currentTimeMillis();
				Biclusters bics = bicminer.mineBiclusters();
				time = System.currentTimeMillis() - time;
				
				/** Part 6: output results in file: output/result.txt **/
				System.out.println("Time:"+((double)time/(double)1000)+"ms");
				BicResult.println("FOUND BICS: " + bics.toString());
				BicResult.println(MatchMetrics.run(bics, trueBics));
			}
	    }
	}
}