package tests.bicpam;

import generator.BicMatrixGenerator;
import generator.BicMatrixGenerator.PatternType;
import generator.BicMatrixGenerator.PlaidCoherency;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import domain.Biclusters;
import utils.BicPrinting;
import utils.BicResult;

/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class BicGeneratorTests {
	
	static public String path = "data/generator/rui/";

	public static void main(String[] args) throws Exception {
		//simple();
		//plaid();
		multipleTests();
		//differential();
	}

	public static void simple() throws Exception {
		String background = "normal";
    	int numRows = 1000, numCols = 200, numBics = 10;
    	double min = -2, max = 2, coherenceStrength = 0.4;
    	
		BicMatrixGenerator generator = new BicMatrixGenerator(numRows,numCols,numBics,background,min,max,coherenceStrength);
		generator.overlap = true;
    	
    	PatternType type = PatternType.Additive;
		String distRowsBics = "uniform";
    	double numBicRowsP1 = 20, numBicRowsP2 = 30; 
		String distColsBics = "normal";
    	double meanCols = 15, stdCols = 0.01;
    	boolean contiguousCols = false, differentialValues = false;
    	boolean coherencyOnColumns = false; 
    	
		Biclusters trueBics = generator.generateKBiclusters(type,
				distRowsBics, numBicRowsP1, numBicRowsP2, 
				distColsBics, meanCols, stdCols, 
				contiguousCols, differentialValues, coherencyOnColumns);
		
		double[][] dataset = generator.getRealExpressionMatrix();
		BicResult.println("Planted BICS:\n" + trueBics.toString());
		writeFile("bic"+type+"S"+numRows+background+"N0M0.txt",trueBics.toString());
		BicResult.println("Dataset:\n" + BicPrinting.plot(dataset));
		writeFile("data"+type+"S"+numRows+background+"N0M0.txt",matrixToString(dataset));
		
		//new BicExpressionChart(trueBics, dataset, coherenceStrength);
	}
	
	public static void multipleTests() throws Exception {

    	boolean symmetric = true;
		String background = "random";
		String distRowsBics = "uniform", distColsBics = "uniform";
    	boolean contiguousCols = false, differentialValues = false;
    	boolean coherencyOnColumns = false; 
		
    	PatternType[] types = new PatternType[]{
    			//PatternType.Symmetric,
    			//PatternType.OrderPreserving,
    			PatternType.Multiplicative};
    	
    	int[] alphabets = new int[]{8,4};
    	int[] numRows = new int[]{100,200,500,1000,2000,4000,10000};
    	int[] numColumns = new int[]{50,50,50,50,50,50,50};
    	int[] numBics = new int[]{4,6,8,10,14,20,30};
    	double[] numBicRowsP1 = new double[]{14,20,30,40,60,100,200}; 
    	double[] numBicRowsP2 = new double[]{20,30,50,70,100,200,300};
    	double[] numBicColsP1 = new double[]{6,7,8,9,10,11,12};
    	double[] numBicColsP2 = new double[]{9,10,11,12,13,14,15};
    	/*int[] numRows = new int[]{400,400,400,400,400,400};
    	int[] numColumns = new int[]{30,50,100,200,400,1000};
    	int[] numBics = new int[]{4,6,8,10,14,20,30};
    	double[] numBicRowsP1 = new double[]{30,30,30,30,30,30}; 
    	double[] numBicRowsP2 = new double[]{50,50,50,50,50,50};
    	double[] numBicColsP1 = new double[]{6,8,10,14,20,30};
    	double[] numBicColsP2 = new double[]{10,12,16,20,30,40};*/

    	//int[] numRows = new int[]{100,500,2000,10000};
    	//int[] numColumns = new int[]{40,80,120,200};
    	//int[] numBics = new int[]{4,8,12,20};

	    for(PatternType type : types){
	      for(int alphabet : alphabets){
	    	
			for(int i=0, l=numRows.length; i<l; i++){//
				
				/** GENERATION OF THE SYNTHETIC DATASET **/
				BicResult.println("["+numRows[i]+","+numColumns[i]+"]("+numBics[i]+")");
				BicMatrixGenerator generator = new BicMatrixGenerator(numRows[i],numColumns[i],numBics[i],
														   background,alphabet,symmetric,true);

				generator.overlap = true;
				Biclusters trueBics = generator.generateKBiclusters(type,
						distRowsBics, numBicRowsP1[i], numBicRowsP2[i], 
						distColsBics, numBicColsP1[i], numBicColsP2[i], 
						contiguousCols, differentialValues, coherencyOnColumns);
				
				List<String> rows = new ArrayList<String>(), cols = new ArrayList<String>();
				for(int k=0; k<numRows[i]; k++) rows.add("x"+k);
				for(int j=0; j<numColumns[i]; j++) cols.add("y"+j);
				BicResult.println("Planted BICS: " + trueBics.toString(rows,cols));
				
				/** POST-PROCESSING **/
				generator.putNoise(0.01);				
				double[][] dataset = generator.getRealExpressionMatrix();
				//BicResult.println("Planted BICS:\n" + trueBics.toString());
				writeFile("bicX"+numRows[i]+"Y"+numColumns[i]+"L"+alphabet+background+".txt",trueBics.toString());
				//BicResult.println("Dataset:\n" + BicPrinting.plot(dataset));
				writeFile("dataX"+numRows[i]+"Y"+numColumns[i]+"L"+alphabet+background+".txt",matrixToString(dataset));
			}
	    }
	  }
	}

	public static void plaid() {
		String background = "null";
    	int numRows = 1000, numCols = 200;
    	int alphabet = 8, numBics = 4;
    	boolean symmetric = false, coherencyOnColumns = false;
    	BicMatrixGenerator generator = new BicMatrixGenerator(numRows,numCols,numBics,background,alphabet,symmetric);
    	
    	PatternType type = PatternType.Constant;
		String distRowsBics = "normal", distColsBics = "normal";
    	double meanRows = 40, stdRows = 5, meanCols = 10, stdCols = 1;
    	
    	PlaidCoherency plaidcoherency = PlaidCoherency.Additive;
		double overlappingRows = 0.1, overlappingCols = 0.1;
		int nrOverlappingBics = 3;
    	
		Biclusters trueBics = generator.generatePlaidBiclusters(type, plaidcoherency,
				distRowsBics, meanRows, stdRows, distColsBics, meanCols, stdCols, false, false, coherencyOnColumns,
				overlappingRows, overlappingCols, nrOverlappingBics);
		
		int[][] dataset = generator.getSymbolicExpressionMatrix();
		BicResult.println("Planted BICS:\n" + trueBics.toString());
		//BicResult.println("Dataset:\n" + BicPrinting.plot(dataset));

		generator.putNoise(0);
		//new BicExpressionChart(trueBics, generator.getRealExpressionMatrix(), 1);
	}	
	
	public static void differential() throws Exception {
		String background = "null";
		int numRows = 1000, numCols = 200;
		int alphabet = 8, numBics = 4;
		boolean symmetric = true;
		BicMatrixGenerator generator = new BicMatrixGenerator(numRows,numCols,numBics,background,alphabet,symmetric);
		
		PatternType type = PatternType.Constant;
		String distRowsBics = "uniform";
		double numBicRowsP1 = 20, numBicRowsP2 = 30; 
		String distColsBics = "uniform";
		double numBicColsP1 = 10, numBicColsP2 = 15; 
		boolean contiguousCols = false, differentialValues = true, coherencyOnColumns = false;
		
		Biclusters trueBics = generator.generateKBiclusters(type,
				distRowsBics, numBicRowsP1, numBicRowsP2, 
				distColsBics, numBicColsP1, numBicColsP2, 
				contiguousCols, differentialValues, coherencyOnColumns);
		
		int[][] dataset = generator.getSymbolicExpressionMatrix();
		BicResult.println("Planted BICS:\n" + trueBics.toString());
		BicResult.println("Dataset:\n" + BicPrinting.plot(dataset));
	}
	
	public static String matrixToString(double[][] matrix) {
    	StringBuffer result = new StringBuffer();
    	result.append("X\t"); 
    	for(int j=0, l=matrix[0].length-1; j<l; j++) result.append("y"+j+"\t");
    	result.append("y"+(matrix[0].length-1)+"\n"); 
		DecimalFormat df = new DecimalFormat();
		df.setMaximumFractionDigits(2);
		for(int i=0, l1=matrix.length, l2=matrix[0].length; i<l1; i++){
			result.append("x"+i+"\t");
			for(int j=0; j<l2; j++) result.append(df.format(matrix[i][j])+"\t");
			result.replace(result.length()-1, result.length(),"\n"); 
		}
    	return result.toString();
	}
	
	public static String matrixToString(int[][] matrix) {
    	StringBuffer result = new StringBuffer();
    	result.append("X\t"); 
    	for(int j=0, l=matrix[0].length-1; j<l; j++) result.append("y"+j+"\t");
    	result.append("y"+(matrix[0].length-1)+"\n"); 
		for(int i=0, l1=matrix.length, l2=matrix[0].length; i<l1; i++){
			result.append("x"+i+"\t");
			for(int j=0; j<l2; j++) result.append(matrix[i][j]+"\t");
			result.replace(result.length()-1, result.length(),"\n"); 
		}
    	return result.toString();
	}
	
	public static void writeFile(String name, String content) throws Exception {
	  FileWriter fstream = new FileWriter(path+name);
	  BufferedWriter out = new BufferedWriter(fstream);
	  out.write(content);
	  out.close();
	}
}