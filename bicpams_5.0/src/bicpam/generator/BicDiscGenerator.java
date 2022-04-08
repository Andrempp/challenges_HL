package generator;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;

import generator.BicMatrixGenerator.PatternType;

import org.apache.commons.math3.distribution.NormalDistribution;

import domain.Bicluster;
import domain.Biclusters;
import utils.BicException;
import utils.BicMath;
import utils.BicResult;

public class BicDiscGenerator {

  public int[][] symbolicMatrix;
  public int numRows, numColumns, numClasses;
  public int[] sumRows, numBiclusters;
  public double[][] areaBics, rowBics, colBics, discBics;
  public PatternType type;
  public int[] alphabet;

  public Biclusters biclusters;
  public String background = "random"; //no,random,normal

  public BicDiscGenerator(int[] sumRows, int numColumns, int[] numBiclusters, double[][] areaBics, double[][] discBics, int nrLabels, PatternType type) {
	this.numClasses = numBiclusters.length;
	this.numRows = sumRows[sumRows.length-1];
	this.numColumns = numColumns;
	this.sumRows = sumRows;
	this.numBiclusters = numBiclusters;
	this.areaBics = areaBics;
	this.discBics = discBics;
    this.alphabet = new int[nrLabels];
    this.biclusters = new Biclusters();
    this.type = type;
    for(int i=0; i<nrLabels; i++) this.alphabet[i]=i; 
    switch(background){
    	case "random" : symbolicMatrix = this.initializeSymbolicMatrixWithRandomExpressionSymbols(); break;
    	case "normal" : symbolicMatrix = this.initializeSymbolicMatrixWithNormalExpressionSymbols(); break;
    	default : symbolicMatrix = this.initializeSymbolicMatrixWithNoExpressionSymbol();
    }
  }
  public BicDiscGenerator(int[] sumRows, int numColumns, int[] numBiclusters, double[][] rowBics, double[][] colBics, double[][] discBics, int nrLabels, PatternType type) {
	this(sumRows, numColumns, numBiclusters, null, discBics, nrLabels, type);
	this.rowBics = rowBics;
	this.colBics = colBics;
  }
  public BicDiscGenerator(int[] sumRows, int numColumns, int[] numBiclusters, double[][] rowBics, double[][] colBics, double[][] discBics, int nrLabels, PatternType type, int[][] background) {
	this(sumRows, numColumns, numBiclusters, null, discBics, nrLabels, type);
	this.rowBics = rowBics;
	this.colBics = colBics;
	this.symbolicMatrix = background;
  }

  /* =============================
     ======= CORE METHODS ========
     ============================= */

  public int[][] generateKDiscBiclusters() throws BicException {
    
    //1. GENERATE NR OF ROWS AND COLUMNS
    int[][][] rowsNrBics = new int[numClasses][][];
    int[][] colsNrBics = new int[numClasses][];
    for(int k=0, l1=numClasses; k<l1; k++){
    	rowsNrBics[k] = new int[numBiclusters[k]][numClasses];
    	colsNrBics[k] = new int[numBiclusters[k]];
    	if(areaBics != null){
	        for(int i=0, l2=numBiclusters[k]; i<l2; i++){
	        	int nrElements = (int) (areaBics[k][i]*((sumRows[k+1]-sumRows[k])*numColumns)); 
	        	colsNrBics[k][i] = (int) Math.sqrt(nrElements*numColumns/numRows);
	            for(int j=0; j<numClasses; j++){
	            	rowsNrBics[k][i][j] = (int) Math.sqrt(nrElements*numRows/numColumns);
	            	if(j!=k) rowsNrBics[k][i][j] = (int) (rowsNrBics[k][i][j]*(1-discBics[k][i]));
	            }
	        }
    	} else {
            for(int i=0, l2=numBiclusters[k]; i<l2; i++){
            	colsNrBics[k][i] = (int) (colBics[k][i]*numColumns); 
                for(int j=0; j<numClasses; j++){
                	rowsNrBics[k][i][j] = (int) (rowBics[k][i]*(sumRows[k+1]-sumRows[k]));
                	if(j!=k) rowsNrBics[k][i][j] = (int) (rowsNrBics[k][i][j]*(1-discBics[k][i]));
                }
            }
    	}
    }

    //2. GENERATE THE EXPRESSION SYMBOL FOR EACH COLUMN
    Random r = new Random();
    int[][][] colsPatternBics = new int[numClasses][][];
    int alphabetLength = alphabet.length;
    if(type.equals(PatternType.Additive)) alphabetLength = (int) Math.floor(alphabetLength/2.0);
    else if(type.equals(PatternType.Multiplicative)) alphabetLength = (int) Math.floor(Math.sqrt(alphabetLength));
    
    for(int k=0, l1=numClasses; k<l1; k++){
    	colsPatternBics[k] = new int[numBiclusters[k]][];
        for(int i=0, l2=numBiclusters[k]; i<l2; i++){
        	colsPatternBics[k][i] = new int[colsNrBics[k][i]];
        	for (int j=0, l3=colsNrBics[k][i]; j<l3; j++)
        		colsPatternBics[k][i][j] = alphabet[r.nextInt(alphabetLength)];
        }
    }
    
    //3. GENERATE ROWS AND COLUMNS
    int[][][] biclustersRows = new int[numClasses][][];
    int[][][] biclustersColumns = new int[numClasses][][];
    for(int k=0, l1=numClasses; k<l1; k++){
        biclustersRows[k] = new int[numBiclusters[k]][];
        biclustersColumns[k] = new int[numBiclusters[k]][];
        for(int i=0, l2=numBiclusters[k]; i<l2; i++){
        	
        	//A. Generate Rows
            SortedSet<Integer> rowsSet = new TreeSet<Integer>();
            int nrRows = (int) BicMath.sum(rowsNrBics[k][i]);
            biclustersRows[k][i] = new int[nrRows];
            for(int k2=0, row=-1, index=0; k2<numClasses; k2++) {
            	int min=sumRows[k2], max=sumRows[k2+1];
                for(int j=0, l3=rowsNrBics[k][i][k2]; j<l3; j++) {
	                do{ row = r.nextInt(max-min)+min; }
	                while (rowsSet.contains(new Integer(row)));
	                rowsSet.add(new Integer(row));
	                biclustersRows[k][i][index++] = row;
	            }
            	//BicResult.println("R:"+k2+" "+rowsSet);
            }
            
        	//B. Generate Columns
            SortedSet<Integer> colsSet = new TreeSet<Integer>();
            biclustersColumns[k][i] = new int[colsNrBics[k][i]];
            for(int j=0, col=-1, l=colsNrBics[k][i]; j<l; j++) {
                do{ col = r.nextInt(this.numColumns); }
                while (colsSet.contains(new Integer(col)));
                colsSet.add(new Integer(col));
                biclustersColumns[k][i][j] = col;
            }
        }
    }
 
    //3. GENERATE BICLUSTERS
    int additiveBicAlphabet = (int) Math.floor(alphabet.length/2.0);
    int multiplicativeBicAlphabet = (int) Math.floor(Math.sqrt(alphabet.length));

    for(int k=0; k<numClasses; k++){
      for(int i=0, l1=biclustersRows[k].length; i<l1; i++){
    	SortedSet<Integer> rows = BicMath.getSet(biclustersRows[k][i]);
    	SortedSet<Integer> cols = BicMath.getSet(biclustersColumns[k][i]);
    	List<Integer> items = BicMath.getList(colsPatternBics[k][i]);
    	biclusters.add(new Bicluster(rows,cols,items,type));
    	for(Integer row : rows){
    		int index = 0;
    		if(type.equals(PatternType.Constant))
    			for(Integer col : cols) symbolicMatrix[row][col] = items.get(index++);
    		else if(type.equals(PatternType.OrderPreserving)){
				int[] vector = new int[cols.size()];
			    for (int j=0, l=cols.size(); j<l; j++) vector[j] = alphabet[r.nextInt(alphabet.length)];
				Arrays.sort(vector);
    			for(Integer col : cols) symbolicMatrix[row][col] = vector[index++];
    		} else if(type.equals(PatternType.Additive)){
	    		int rowContribution = r.nextInt(additiveBicAlphabet);
    			for(Integer col : cols) symbolicMatrix[row][col] = items.get(index++)+rowContribution;
    		} else if(type.equals(PatternType.Multiplicative)){
	    		int rowContribution = r.nextInt(multiplicativeBicAlphabet)+1;
    			for(Integer col : cols) symbolicMatrix[row][col] = items.get(index++)+rowContribution;
    		}
    	}
      }
      if(type.equals(PatternType.Multiplicative)){
	      for(int i=0; i<numRows; i++)
		      for(int j=0; j<numColumns; j++) symbolicMatrix[i][j]++;
      }
    }
    BicResult.println(biclusters.toString());
    return symbolicMatrix;
  }

    
  /* =============================
     ===== PRIVATE METHODS =======
     ============================= */

  /** Initializes the symbolic expression matrix with the no expression symbol. */
  private int[][] initializeSymbolicMatrixWithNoExpressionSymbol() {
    int[][] symbolicMatrix = new int[numRows][numColumns];
    for (int i = 0; i < this.numRows; i++) 
      for (int j = 0; j < this.numColumns; j++)
        symbolicMatrix[i][j] = -1;
    return symbolicMatrix;
  }

  /** initializes the symbolic expression matrix with random expressin symbols from the set */
  private int[][] initializeSymbolicMatrixWithRandomExpressionSymbols() {
    int[][] symbolicMatrix = new int[numRows][numColumns];
    Random r = new Random();
    for (int i = 0; i < this.numRows; i++) 
      for (int j = 0; j < this.numColumns; j++) 
        symbolicMatrix[i][j] = this.alphabet[r.nextInt(this.alphabet.length)];
    return symbolicMatrix;
  }
  
  /** initializes the symbolic expression matrix with Gaussian expressin symbols from the set */
  private int[][] initializeSymbolicMatrixWithNormalExpressionSymbols() {
    int[][] symbolicMatrix = new int[numRows][numColumns];
    NormalDistribution r = new NormalDistribution(this.alphabet.length/2,this.alphabet.length/5);
    for (int i = 0; i < this.numRows; i++) {
   	  double[] vals = r.sample(this.numColumns);
      for (int j = 0; j < this.numColumns; j++) {
    	if(vals[j]<0) vals[j]=0;
    	else if(vals[j]>this.alphabet.length-1) vals[j]=this.alphabet.length-1;
        symbolicMatrix[i][j] = this.alphabet[(int)vals[j]];
      }
    }
    return symbolicMatrix;
  }
  
  public int[][] annotateSymbolicMatrix() {
	int[][] symbolicMatrix = new int[numRows][numColumns+1];
	for (int i=0, index=0; i<numRows; i++){
	   	if(i>=sumRows[index+1]) index++;
	   	symbolicMatrix[i][numColumns] = index; 
	}
	return symbolicMatrix;
  }
  
  public int getNumLabels() {
	if(type.equals(PatternType.Multiplicative)) return BicMath.max(symbolicMatrix);
	return alphabet.length;
  }
}