package tests.bicnet;

import java.io.BufferedWriter;
import java.io.FileWriter;

import utils.BicPrinting;
import utils.BicResult;
import domain.Biclusters;
import domain.Dataset;
import generator.BicMatrixGenerator;
import generator.BicMatrixGenerator.PatternType;
import generator.BicMatrixGenerator.PlaidCoherency;
import generator.BicNetsGenerator;


/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class BicGeneratorNetTests {
	
	static public String path = "data/generator/";

	/** Generator of synthetic network data with meaningful modules **/				
	public static void main(String[] args) throws Exception {

		/** Part 1: parameterize properties of synthetic data **/				
       	int[] alphabets = new int[]{8}; /*coherence strength*/
        int[] numNodes = new int[]{100,200,500,1000,2000,10000};
    	double[] density = new double[]{0.2,0.1,0.5};

    	boolean symmetry = true;
		boolean defaultdataset = true; /*1000 nodes*/

        int[] numBics = new int[]{4,7,10,15,20,30};
		PatternType[] types = new PatternType[]{PatternType.Constant};
			//PatternType.Symmetric,PatternType.Multiplicative};

		String distRowsBics = "uniform", distColsBics = "uniform";
    	double[] numBicRowsP1 = new double[]{8,10,15,25,40,50}; 
    	double[] numBicRowsP2 = new double[]{10,15,20,30,50,70};
    	double[] numBicColsP1 = new double[]{5,5,6,8,10,16};
    	double[] numBicColsP2 = new double[]{5,6,8,10,12,20};

	  	double noise = 0.1, missings = 0.1; 

		/** Part 2: select data contexts **/				
	    for(PatternType type : types){
		  for(int alphabet : alphabets){
			  
		    int i = defaultdataset ? 4 : 0;
		    int l = defaultdataset ? 5 : numNodes.length;
		    		    	
			for(; i<l; i++){

				/** Part 3: generate net and modules, plant noise and missings **/				
				BicNetsGenerator generator = new BicNetsGenerator(numNodes[i],density[0],numBics[i],alphabet,symmetry);
	    	
			  	Biclusters trueBics = generator.generateKBiclusters(type,
			  		distRowsBics, numBicRowsP1[i], numBicRowsP2[i],  //rows
			  		distColsBics, numBicColsP1[i], numBicColsP2[i]); //columns

			  	generator.plantNoisyElements(noise);
			  	generator.putMissings(missings);
			  	//generator.putBicMissings(missings);
			  	
			  	/** Part 4: read and write data to folder data/generator/ **/
			  	Dataset dataset = generator.getDataset();
			  	//BicResult.println("Planted BICS:\n" + trueBics.toString());
			  	//BicResult.println("Dataset:\n" + dataset.toString());
			  	
			  	writeFile("bic"+type+"N"+numNodes[i]+"D"+density[0]+"N"+noise+"M"+missings+".txt",trueBics.toString());
			  	writeMatrixFromNet("net"+type+"S"+numNodes[i]+"D"+density[0]+"N"+noise+"M"+missings+".txt",dataset);
			}
		  }
	    }
	}

	
	/**************************/
	/**** NON-CORE METHODS ****/
	/**************************/
	
	/** Generator of synthetic tabular data with interacting modules **/				
	public static void alternativeNetsComplex() {
		
	  	/** Part 1: generate dataset **/
		String background = "null";
    	int numRows = 1000, numCols = 200;
    	int alphabet = 8, numBics = 4;
    	boolean symmetric = false;
		BicMatrixGenerator generator = new BicMatrixGenerator(numRows,numCols,numBics,background,alphabet,symmetric);
    	
	  	/** Part 2: plant meaningful structure of modules **/		
    	PatternType type = PatternType.Constant;
		String distRowsBics = "normal", distColsBics = "normal";
    	double meanRows = 40, stdRows = 5, meanCols = 10, stdCols = 1;
    	
    	PlaidCoherency plaidcoherency = PlaidCoherency.Additive;
		double overlappingRows = 0.1, overlappingCols = 0.1;
		int nrOverlappingBics = 3;
    	
		Biclusters trueBics = generator.generatePlaidBiclusters(type, plaidcoherency,
				distRowsBics, meanRows, stdRows, distColsBics, meanCols, stdCols, false, false, false,
				overlappingRows, overlappingCols, nrOverlappingBics);
		
	  	/** Part 3: visualize data **/		
		BicResult.println("Planted BICS:\n" + trueBics.toString());
		BicResult.println("Dataset:\n" + BicPrinting.plot(generator.getSymbolicExpressionMatrix()));
	}	

	/** Simple generator of synthetic tabular data **/				
	public static void alternativeNetsSimple() throws Exception {
		
	  	/** Part 1: generate network **/
		String background = "normal";
    	int numRows = 1000, numCols = 200, numBics = 10;
    	double min = -2, max = 2, coherenceStrength = 0.4;
		BicMatrixGenerator generator = new BicMatrixGenerator(numRows,numCols,numBics,background,min,max,coherenceStrength);
    	
	  	/** Part 2: plant biclusters **/		
    	PatternType type = PatternType.Additive;
		String distRowsBics = "uniform", distColsBics = "normal";
    	double numBicRowsP1 = 20, numBicRowsP2 = 30; 
    	double meanCols = 15, stdCols = 0.01;
    	boolean contiguousCols = false;
    	
		Biclusters trueBics = generator.generateKBiclusters(type,
				distRowsBics, numBicRowsP1, numBicRowsP2, 
				distColsBics, meanCols, stdCols, contiguousCols, false, false);
		
	  	/** Part 3: visualize data **/		
		double[][] dataset = generator.getRealExpressionMatrix();
		BicResult.println("Planted BICS:\n" + trueBics.toString());
		BicResult.println("Dataset:\n" + BicPrinting.plot(dataset));
	}

	
	/*************************/
	/**** PRIVATE METHODS ****/
	/*************************/
	
	private static void writeMatrixFromNet(String name, Dataset network) throws Exception {
		FileWriter fstream = new FileWriter(path+name);
		BufferedWriter out = new BufferedWriter(fstream);
    	int l=network.nrNodes();
    	StringBuffer result = new StringBuffer("nodes\t");
    	for(int j=0; j<l; j++, result.append("\t")) result.append(network.rows.get(j));
		out.write(result.toString()+"\n");
		for(int i=0; i<l; i++){
			String[] node = new String[l];
			for(int j=0; j<l; j++) node[j]="0";
			for(int j=0, l2=network.indexes.get(i).size(); j<l2; j++)
				node[network.indexes.get(i).get(j)]=network.intscores.get(i).get(j)+"";
			result.replace(result.length()-1, result.length(),"\n"); 
	    	result = new StringBuffer(network.rows.get(i)+"\t");
			for(int j=0; j<l; j++, result.append("\t")) result.append(node[j]);
			out.write(result.toString()+"\n");
		}
		out.close();
	}

	private static void writeFile(String name, String content) throws Exception {
	  FileWriter fstream = new FileWriter(path+name);
	  BufferedWriter out = new BufferedWriter(fstream);
	  out.write(content);
	  out.close();
	}
}