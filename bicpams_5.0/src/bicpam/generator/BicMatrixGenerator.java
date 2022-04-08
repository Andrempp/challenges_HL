package generator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import domain.Bicluster;
import domain.Biclusters;
import domain.Dataset;
import utils.BicMath;
import utils.BicPrinting;
import utils.BicException;

import org.apache.commons.math3.distribution.NormalDistribution;

public class BicMatrixGenerator {

  public enum PatternType { ConstantOverall, Constant, Additive, Multiplicative, OrderPreserving, Symmetric, Multiple };
  public enum PlaidCoherency { Additive , Multiplicative , Interpoled , None};
  public enum CoherencyConstraint { Exact , InBetween , Relaxed };
  
  protected int numRows, numCols, numBics;
  protected int[] alphabet;
  protected boolean symmetries;

  protected int[][] symbolicMatrix;
  protected double[][] realMatrix;
  protected double maxM, minM;
  protected Biclusters plantedBics;

  public boolean overlap = false;
  protected HashSet<String> elements = new HashSet<String>();

  /**
   * @param nRows Number of rows in the expression matrix (|X|)
   * @param nCols Number of columns in the expression matrix (|Y|)
   * @param numBics Number of Biclusters (K)
   * @param background Background of the symbolic expression matrix: random, normal or null
   * @param alphabet Number of items |L| in the matrix
   */
  public BicMatrixGenerator(int nRows, int nCols, int nBics, String background, int alphabetL, boolean symmetric) {
	this(nRows,nCols,nBics,background,alphabetL,symmetric,false);
  }
  public BicMatrixGenerator(int nRows, int nCols, int nBics, String background, int alphabetL, boolean symmetric, boolean realvalue) {
	this(nRows,nCols,nBics,alphabetL,symmetric);
    if(!realvalue) symbolicMatrix = initSymbolicMatrix(background);
    else {
    	minM=alphabet[0]-0.2;
    	maxM=alphabet[alphabetL-1]+0.2;
    	realMatrix = initRealMatrix(background);
    }
  }

  /**
   * @param nRows Number of rows in the expression matrix (|X|)
   * @param nCols Number of columns in the expression matrix (|Y|)
   * @param numBics Number of Biclusters (K)
   * @param background Background of the symbolic expression matrix: random, normal or null
   * @param alphabet Number of items |L| in the matrix
   */
  public BicMatrixGenerator(int nRows, int nCols, int nBics, String background, double minValue, double maxValue, double strength) {
	this(nRows,nCols,nBics,(int)((maxValue-minValue)/strength),maxValue==-minValue);
	maxM=maxValue;
	minM=minValue;
    realMatrix = initRealMatrix(background);
  }

  private BicMatrixGenerator(int nRows, int nCols, int nBics, int alphabetL, boolean symmetric) {
	numRows=nRows;
	numCols=nCols;
	numBics=nBics;
	symmetries=symmetric;
    plantedBics = new Biclusters();
    alphabet = new int[alphabetL];
    int val = symmetries ? -(alphabetL/2) : 0; 
    for(int i=0; i<alphabetL; i++, val++) alphabet[i]=val;
    if(symmetries && alphabetL%2==0) 
    	for(int i=alphabetL/2; i<alphabetL; i++) alphabet[i]++;
    System.out.println("Alphabet:"+BicPrinting.plot(alphabet));
  }

  /*********************************
   ********* CORE METHODS **********
   *********************************/

  /**
   * Generate K biclusters with parameterizable coherency and structure
   * @param type Bicluster coherency: Constant, Symmetric, Additive, Multiplicative, Order-Preserving
   * @param distRowsBics Distribution of the number of rows in biclusters: normal or uniform
   * @param rows1 Either u from N(u,s) or minimum number of rows of a bicluster min(|I|)
   * @param rows2 Either s from N(u,s) or maximum number of rows of a bicluster max(|I|)
   * @param distColsBics Distribution of the number of columns in biclusters: normal or uniform
 * @param coherencyTranspose 
   * @param cols1 Either u from N(u,s) or minimum number of columns of a bicluster min(|J|)
   * @param cols2 Either s from N(u,s) or maximum number of columns of a bicluster max(|J|)
   * @return The generated biclusters
   */
  public Biclusters generateKBiclusters(PatternType type,
	      String distRowsBics, double rowsParam1, double rowsParam2, 
	      String distColsBics, double colsParam1, double colsParam2, boolean contiguous, boolean differential, boolean coherencyTranspose) {
	  return generatePlaidBiclusters(type, PlaidCoherency.None, distRowsBics, (int) rowsParam1, (int) rowsParam2,
			  distColsBics, (int) colsParam1, (int) colsParam2, contiguous, differential, coherencyTranspose, -1, -1, -1);
  }

  public Biclusters generateKBiclusters(PatternType type,
	      String distRowsBics, double rowsParam1, double rowsParam2, 
	      String distColsBics, double colsParam1, double colsParam2, 
	      boolean contiguous, boolean differential, boolean orientationColumns, List<Integer> itemsToRemove) {
	  
	  System.out.println("Orientation Cols:"+orientationColumns);
	  if(type.equals(PatternType.Multiple)){
		  numBics=numBics/3;
		  Biclusters bics = new Biclusters();
		  bics.addAll(generatePlaidBiclusters(PatternType.Constant, PlaidCoherency.None, distRowsBics, (int) rowsParam1, (int) rowsParam2, distColsBics, (int) colsParam1, (int) colsParam2, contiguous, differential, orientationColumns, -1, -1, -1, itemsToRemove));
		  plantedBics = new Biclusters();
		  bics.addAll(generatePlaidBiclusters(PatternType.Additive, PlaidCoherency.None, distRowsBics, (int) rowsParam1, (int) rowsParam2, distColsBics, (int) colsParam1, (int) colsParam2, contiguous, differential, orientationColumns, -1, -1, -1, itemsToRemove));
		  plantedBics = new Biclusters();		  
		  bics.addAll(generatePlaidBiclusters(PatternType.Multiplicative, PlaidCoherency.None, distRowsBics, (int) rowsParam1, (int) rowsParam2, distColsBics, (int) colsParam1, (int) colsParam2, contiguous, differential, orientationColumns, -1, -1, -1, itemsToRemove));
		  plantedBics = bics;
	  }
	  else generatePlaidBiclusters(type, PlaidCoherency.None, distRowsBics, (int) rowsParam1, (int) rowsParam2,
			  distColsBics, (int) colsParam1, (int) colsParam2, contiguous, differential, orientationColumns, -1, -1, -1, itemsToRemove);
	  return plantedBics;
	}

  public Biclusters generatePlaidBiclusters(PatternType type, PlaidCoherency plaidcoherency, 
			 String distRowsBics, double rows1, double rows2, String distColsBics, double cols1, double cols2, 
			 boolean contiguous, boolean differential, boolean coherencyTranspose,  
			 double overlappingRows, double overlappingCols, int nrOverlappingBics) {
	  return generatePlaidBiclusters(type, plaidcoherency, distRowsBics, rows1, rows2, distColsBics, cols1, cols2, contiguous, differential, coherencyTranspose, overlappingRows, overlappingCols, nrOverlappingBics, new ArrayList<Integer>());
  }

  /**
   * Generate K biclusters with plaid effects
   * This method is organized in 6 major parts: see comments along the code 
   * @param type Bicluster coherency: Constant, Symmetric, Additive, Multiplicative, Order-Preserving
   * @param plaidcoherency Overlapping coherency: Additive, Multiplicative, Interpoled, None
   * @param distRowsBics Distribution of the number of rows in biclusters: normal or uniform
   * @param rows1 Either u from N(u,s) or minimum number of rows of a bicluster min(|I|)
   * @param rows2 Either s from N(u,s) or maximum number of rows of a bicluster max(|I|)
   * @param distColsBics Distribution of the number of columns in biclusters: normal or uniform
   * @param cols1 Either u from N(u,s) or minimum number of columns of a bicluster min(|J|)
   * @param cols2 Either s from N(u,s) or maximum number of columns of a bicluster max(|J|)
   * @param overlappingRows Expectations on the extent of overlapping among rows
   * @param overlappingCols Expectations on the extent of overlapping among columns
   * @param nrOverlappingBics Expectations on the n-wise overlapping relations (e.g. 2 if pairwise only)
   * @return The generated biclusters
   */
  public Biclusters generatePlaidBiclusters(PatternType type, PlaidCoherency plaidcoherency, 
		 String distRowsBics, double rows1, double rows2, String distColsBics, double cols1, double cols2, 
		 boolean contiguous, boolean differential, boolean orientationCols,
		 double overlappingRows, double overlappingCols, int nrOverlappingBics, List<Integer> itemsToRemove) {
	
    Random r = new Random();
    int numRowsBics, numColsBics;
    int[][] bicsRows = new int[numBics][], bicsCols = new int[numBics][];
    int[][] symbols = new int[numBics][], indexSymbols = new int[numBics][];
    int[] maxContribution = new int[numBics], patternRange = new int[numBics];
    Set<Integer> chosenCols = new HashSet<Integer>();
    
    if(type.equals(PatternType.Symmetric) && alphabet[0]>=0) throw new BicException("Symmetric coherency cannot be used with positive range!");
    
    
    /** PART I: generate pattern ranges **/
    // Additive and multiplicative patterns with varying degree of shift/scale factors
    if(type.equals(PatternType.Additive)||type.equals(PatternType.Multiplicative)){
    	int alphaL = type.equals(PatternType.Additive) ? alphabet.length-1 : 
    		(symmetries ? alphabet.length/3 : alphabet.length/2); //Multiplicative
    	if(numBics>alphaL){
    		int joints = (int) Math.ceil(((double)numBics)/(double)alphaL);
    		for(int k=0, c=0; k<numBics; c++) 
    			for(int it=0; it<joints && k<numBics; it++, k++) patternRange[k]=c+1;
    	} else {
    		int skips = (int) Math.floor(alphaL/numBics);
    		for(int k=0; k<numBics; k++) patternRange[k]=k*skips+1;
    	}
    } else for(int k=0; k<numBics; k++) patternRange[k]=alphabet.length;
    
    for (int k=0; k<numBics; k++){
    	
      /** PART II: select number of rows and columns according to distribution U(min,max) or N(mean,std) **/
      if (distRowsBics.toLowerCase().matches("uniform")) numRowsBics = (int) rows1+ (rows1==rows2 ? 0 : r.nextInt((int)(rows2-rows1)));
      else numRowsBics = (int) Math.round(r.nextGaussian()*rows2 + rows1);
      if (distColsBics.toLowerCase().matches("uniform")) numColsBics = (int) cols1+ (cols1==cols2 ? 0 : r.nextInt((int)(cols2-cols1))); 
      else numColsBics = (int) Math.round(r.nextGaussian()*cols2 + cols1);

      /** PART III: generate bicluster pattern **/
      if(orientationCols){
	      symbols[k] = new int[numRowsBics];
	      indexSymbols[k] = new int[numRowsBics];
      } else {
	      symbols[k] = new int[numColsBics];
	      indexSymbols[k] = new int[numColsBics];
      }
      if(type.equals(PatternType.Multiplicative)) {
		  int shift = symmetries ? (int)Math.ceil(((double)alphabet.length)/2.0) : 1;
    	  for(int j=0, l=symbols[k].length; j<l; j++){
    		  int signal = symmetries ? (r.nextBoolean() ? 1 : -1) : 1;
    		  symbols[k][j] = signal*alphabet[r.nextInt(patternRange[k])+shift];
    	  }
      } else if(type.equals(PatternType.Additive)){ 
    	  for(int j=0, l=symbols[k].length; j<l; j++){
    		  indexSymbols[k][j] = r.nextInt(patternRange[k]);
    		  symbols[k][j] = alphabet[indexSymbols[k][j]];
    	  }
      } else if(type.equals(PatternType.ConstantOverall)){
    	  int symbol;
    	  do { symbol = alphabet[r.nextInt(patternRange[k])]; }
    	  while(itemsToRemove.contains(symbol));
    	  for(int j=0, l=symbols[k].length; j<l; j++) symbols[k][j] = symbol;
      }
      else for(int j=0, l=symbols[k].length; j<l; j++){
    	  do { symbols[k][j] = alphabet[r.nextInt(patternRange[k])]; }
    	  while(itemsToRemove.contains(symbols[k][j]));
      }
      //System.out.println("Removals:"+itemsToRemove+"\nPattern:"+BicPrinting.plot(symbols[k]));
      
      /** PART IV: select biclusters with (non-)overlapping elements **/
      int[] bicsWithOverlap = null, bicsExcluded = null;
      if(nrOverlappingBics > 0){ 
	      boolean dispersed = true;
	      if(dispersed){
		      if((k+1)%nrOverlappingBics==0){
		  		bicsWithOverlap=new int[Math.min(k, nrOverlappingBics-1)];
		  		for(int i=k-1, j=0; i>=0 && j<nrOverlappingBics-1; i--, j++) bicsWithOverlap[j]=i;
			  }
	      } else if(k%nrOverlappingBics!=0) bicsWithOverlap = new int[]{k-1};
	
	      int l=(k/nrOverlappingBics)*nrOverlappingBics;
	      bicsExcluded = new int[l];
	   	  for(int i=0; i<l; i++) bicsExcluded[i]=i;
	      //System.out.println("K"+k+":"+(bicsWithOverlap!=null?BicPrinting.plot(bicsWithOverlap):"-")+" >> "+BicPrinting.plot(bicsExc));
      }
      
   	  /** PART V: generate rows and columns using overlapping constraints **/
      if(contiguous) bicsCols[k] = generateContiguous(numColsBics,numCols,overlappingCols,bicsCols,bicsWithOverlap,bicsExcluded);
      else {
    	  if(overlap) bicsCols[k] = generate(numColsBics,numCols,overlappingCols,bicsCols,bicsWithOverlap,bicsExcluded);
    	  else {
    		  bicsCols[k]=generateNonOverlappingCols(numColsBics,numCols,chosenCols);
    		  for(Integer c : bicsCols[k]) chosenCols.add(c);
    	  }
      }
      
      if(overlap) bicsRows[k]=generate(numRowsBics,numRows,overlappingRows,bicsRows,bicsWithOverlap,bicsExcluded);
      else bicsRows[k]=generateNonOverlappingRows(numRowsBics,numRows,bicsCols[k],elements);
      
      
      for(int i=0; i<numRowsBics; i++)
    	  for(int j=0; j<numColsBics; j++)
    		  elements.add(bicsRows[k][i]+":"+bicsCols[k][j]);
      
      List<Integer> items = orientationCols ? getOrderedList(symbols[k],bicsRows[k]) : getOrderedList(symbols[k],bicsCols[k]);  
      Bicluster bicK = new Bicluster(BicMath.getSet(bicsRows[k]), BicMath.getSet(bicsCols[k]), items, type);
	  plantedBics.add(bicK);
    }
    //System.out.println("BicsRows:"+BicPrinting.plot(bicsRows));
    //System.out.println("BicsCols:"+BicPrinting.plot(bicsCols));
    
    int[][] plaid = initSymbolicMatrix("missing");
	for (int k=0; k<numBics; k++){

	  /** PART VI: generate biclusters coherencies **/
	  int[][] bicsymbols = new int[bicsRows[k].length][bicsCols[k].length];
      if(type.equals(PatternType.Additive)) 
    	  maxContribution[k] = alphabet.length-patternRange[k]+1;
      else if(type.equals(PatternType.Multiplicative)){
    	  int maxval=Math.max(BicMath.max(symbols[k]),-BicMath.min(symbols[k]));
    	  int maxoriginal=Math.max(BicMath.max(alphabet),-BicMath.min(alphabet));
    	  maxContribution[k] = maxoriginal/maxval;
      }
	  //System.out.println("PatternRange"+patternRange[k]);    
	  //System.out.println("MaxRowCont"+maxRowContribution[k]);    
	  //System.out.println("Symbols:"+BicPrinting.plot(symbols[k]));
	  
      
      
      List<Integer> contributions = new ArrayList<Integer>();
      if(orientationCols){
		  for(int i=0, colContribution=0; i<bicsCols[k].length; i++){
			if(type.equals(PatternType.OrderPreserving)){
				contributions.add(0);
		        int[] vector = new int[bicsRows[k].length];
		        for(int j=0; j<bicsRows[k].length; j++) vector[j] = this.alphabet[r.nextInt(this.alphabet.length)];
				Arrays.sort(vector);
			    for(int j=0; j<bicsRows[k].length; j++) bicsymbols[j][i] = vector[j];
			} else if(type.equals(PatternType.Constant)||type.equals(PatternType.ConstantOverall)){
				contributions.add(0);
		       	for(int j=0; j<bicsRows[k].length; j++) bicsymbols[j][i] = symbols[k][j];
			} else if(type.equals(PatternType.Symmetric)){
				int symmetry = r.nextBoolean() ? 1 : -1;
				contributions.add(symmetry);
		       	for(int j=0; j<bicsRows[k].length; j++) bicsymbols[j][i] = symbols[k][j]*symmetry;
			} else if(type.equals(PatternType.Additive)){
			    colContribution = r.nextInt(maxContribution[k]);
				contributions.add(colContribution);
		    	for(int j=0; j<bicsRows[k].length; j++) bicsymbols[j][i] = alphabet[indexSymbols[k][j]+colContribution];
		    } else if(type.equals(PatternType.Multiplicative)){
		       	colContribution = r.nextInt(maxContribution[k])+1; 
				contributions.add(colContribution);
				int symmetry = 1;//symmetries ? (r.nextBoolean() ? 1 : -1) : 1;
	      		for(int j=0; j<bicsRows[k].length; j++) bicsymbols[j][i] = symbols[k][j]*colContribution*symmetry;
		    } 
		  }
      } else {
		  for (int i=0, rowContribution=0; i<bicsRows[k].length; i++){
			if(type.equals(PatternType.OrderPreserving)){
				contributions.add(0);
		        int[] vector = new int[bicsCols[k].length];
		        for(int j=0; j<bicsCols[k].length; j++) vector[j] = this.alphabet[r.nextInt(this.alphabet.length)];
				Arrays.sort(vector);
			    for(int j=0; j<bicsCols[k].length; j++) bicsymbols[i][j] = vector[j];
			} else if(type.equals(PatternType.Constant)||type.equals(PatternType.ConstantOverall)){
				contributions.add(0);
		       	for(int j=0; j<bicsCols[k].length; j++) bicsymbols[i][j] = symbols[k][j];
			} else if(type.equals(PatternType.Symmetric)){
				int symmetry = r.nextBoolean() ? 1 : -1;
				contributions.add(symmetry);
		       	for(int j=0; j<bicsCols[k].length; j++) bicsymbols[i][j] = symbols[k][j]*symmetry;
			} else if(type.equals(PatternType.Additive)){
			    rowContribution = r.nextInt(maxContribution[k]);
				contributions.add(rowContribution);
		    	for(int j=0; j<bicsCols[k].length; j++) bicsymbols[i][j] = alphabet[indexSymbols[k][j]+rowContribution];
		    } else if(type.equals(PatternType.Multiplicative)){
		       	rowContribution = r.nextInt(maxContribution[k])+1; 
				contributions.add(rowContribution);
				int symmetry = 1;//symmetries ? (r.nextBoolean() ? 1 : -1) : 1;
	      		for(int j=0; j<bicsCols[k].length; j++) bicsymbols[i][j] = symbols[k][j]*rowContribution*symmetry;
		    } 
		  }
      }
	  plantedBics.getBiclusters().get(k).rowFactors = contributions;
	  //System.out.println("K"+k+" BicSymbols:\n"+BicPrinting.plot(bicsymbols));		
	  
	  /** Part VII: generate the layers according to plaid type and put them in the background **/
	  for (int i=0; i<bicsRows[k].length; i++){
 		for (int j=0; j<bicsCols[k].length; j++){ 
 			if(plaid[bicsRows[k][i]][bicsCols[k][j]]==Integer.MIN_VALUE){
 				plaid[bicsRows[k][i]][bicsCols[k][j]]=bicsymbols[i][j];
 			} else switch(plaidcoherency){
 			  case Additive : plaid[bicsRows[k][i]][bicsCols[k][j]]+=bicsymbols[i][j]; break;
 			  case Multiplicative : plaid[bicsRows[k][i]][bicsCols[k][j]]*=bicsymbols[i][j]; break; 
 			  case Interpoled : plaid[bicsRows[k][i]][bicsCols[k][j]]=(plaid[bicsRows[k][i]][bicsCols[k][j]]+bicsymbols[i][j])/2; break;
 			  default: break;
 			}
 		}
	  }
	}
	
	/** Part VIII: plant biclusters against background data **/
	if(type.equals(PatternType.Multiplicative) && !symmetries){ 
	    for (int i=0; i<numRows; i++)
	        for (int j=0; j<numCols; j++) 		
	        	if(symbolicMatrix!=null) symbolicMatrix[i][j]++;
	        	else realMatrix[i][j]++;
	}

	double[][] test = new double[numRows][numCols];
    for (int i=0; i<numRows; i++){
      for (int j=0; j<numCols; j++){ 
      	if(plaid[i][j]==Integer.MIN_VALUE) continue;
      	else if(symbolicMatrix!=null) symbolicMatrix[i][j]=plaid[i][j];
      	else {
      		double adjustment = 0;
			if(symmetries && (alphabet.length%2==0)) adjustment = plaid[i][j]<0 ? 0.5 : -0.5;
      		double normalizedValue = ((double)plaid[i][j]+adjustment)/(double)alphabet.length;
      		realMatrix[i][j]=normalizedValue*(maxM-minM);
      		test[i][j]=normalizedValue*(maxM-minM);
      	}
      }
    }
	//System.out.println(BicPrinting.plot(realMatrix[0]));		
	//System.out.println(BicPrinting.plot(realMatrix[1]));		
    return plantedBics;
  }
  


  /*********************************
   ******** PUBLIC METHODS *********
   *********************************/

  public int getNumLabels(){ return alphabet.length; }
  public int[][] getSymbolicExpressionMatrix(){ return symbolicMatrix; }
  public double[][] getRealExpressionMatrix() { return realMatrix; }
  public Biclusters getPlantedBiclusters(){ return plantedBics; }

  /**
   * Plant a Missing Label in |X|x|Y|xPercentageMissing elements in the matrix
   * @param percMissing Percentage of missing values
   */
  public void putMissings(double percMissing) {
	int nrMissings = (int)(numRows*numCols*percMissing);
	Random r = new Random(); 
    SortedSet<String> cellsSet = new TreeSet<String>();
    int row=-1, col=-1;
    for(int k=0; k<nrMissings; k++) {
       do { 
     	  row = r.nextInt(this.numRows);
     	  col = r.nextInt(this.numCols);
       } while (cellsSet.contains(row+","+col));
       cellsSet.add(new String(row+","+col));
       if(symbolicMatrix==null) realMatrix[row][col] = Dataset.MISSING;
       else symbolicMatrix[row][col] = (int) Dataset.MISSING;
    }
  }

  public void plantNoisyElements(double percNoise) {
	int nrNoisy = (int)(numRows*numCols*percNoise);
	//System.out.println("NR NOISY:"+nrNoisy);
	Random r = new Random(); 
    SortedSet<String> cellsSet = new TreeSet<String>();
    for(int k=0, row=-1, col=-1; k < nrNoisy; k++) {
       do { 
     	  row = r.nextInt(this.numRows);
     	  col = r.nextInt(this.numCols);
       } while (cellsSet.contains(row+","+col));
       cellsSet.add(new String(row+","+col));
       if(symbolicMatrix==null) realMatrix[row][col] = r.nextDouble()*(maxM-minM)+minM;
       else symbolicMatrix[row][col] = alphabet[r.nextInt(alphabet.length)];
    }
  }

  public void putNoise(double noise) {	
	Random r = new Random(); 
	if(realMatrix==null) realMatrix = new double[numRows][numCols];
	for(int i=0; i<numRows; i++)
		for(int j=0; j<numCols; j++){
		   int symmetry = r.nextBoolean() ? 1 : -1;
	       if(symbolicMatrix==null) {
	    	   realMatrix[i][j] += r.nextDouble()*(maxM-minM)*noise*symmetry;
	    	   if(realMatrix[i][j]<minM) realMatrix[i][j]=minM+(r.nextDouble()/10.0);	    	   
	    	   if(realMatrix[i][j]>maxM) realMatrix[i][j]=maxM-(r.nextDouble()/10.0);	    	   
	       } else realMatrix[i][j] = symbolicMatrix[i][j] + r.nextDouble()*alphabet.length*noise*symmetry;
		}
  }  
  public void putNoise(double noise, String dist) {
	if(dist.equals("uniform")) putNoise(noise);
	Random r = new Random(); 
	if(realMatrix==null) realMatrix = new double[numRows][numCols];
	for(int i=0; i<numRows; i++)
		for(int j=0; j<numCols; j++){
	       if(symbolicMatrix==null) {
	    	   if(realMatrix[i][j]==Dataset.MISSING) continue;
	    	   double adjustment = (r.nextGaussian()/3)*(maxM-minM)*noise;
	    	   realMatrix[i][j] += adjustment;
	    	   //if(i%100==0) System.out.println(adjustment);
	    	   if(realMatrix[i][j]<minM) realMatrix[i][j]=minM+(r.nextDouble()/10.0);	    	   
	    	   if(realMatrix[i][j]>maxM) realMatrix[i][j]=maxM-(r.nextDouble()/10.0);	    	   
	       } else realMatrix[i][j] = symbolicMatrix[i][j] + (r.nextGaussian()/3)*alphabet.length*noise;
		}
  }  
  
  public double[][] divideBy(double[][] dataset, int divisor) {
	double[][] result = new double[dataset.length][dataset[0].length];
	for(int i=0, l1=dataset.length; i<l1; i++)
		for(int j=0, l2=dataset[i].length; j<l2; j++)
			result[i][j] = dataset[i][j]/divisor;
	return result;
  }

  public double[][] createSymmetry(double[][] dataset) {	
	double[][] result = new double[dataset.length][dataset[0].length];
	int shift = alphabet.length/2;
	for(int i=0, l1=dataset.length; i<l1; i++)
		for(int j=0, l2=dataset[i].length; j<l2; j++)
			result[i][j] = dataset[i][j]-shift;
	return result;
  }  
  
  /*********************************
   ******* PRIVATE METHODS *********
   *********************************/

  /** Generate rows and columns for a bicluster using overlapping constraints */
  private int[] generate(int nBicDim, int nDim, double overlap, int[][] vecsL, int[] overlapVecs, int[] vecsExc) throws BicException {
	  Random r = new Random(); 
      int[] result = new int[nBicDim];
      SortedSet<Integer> set = new TreeSet<Integer>();

      if(overlap<0){ //no need for plaid calculus
          if(nBicDim==nDim) for(int i=0; i<nBicDim; i++) result[i]=i;
          else for(int i=0, val=-1; i<nBicDim; i++) {
              do{ val = r.nextInt(nDim); }
              while(set.contains(new Integer(val)));
              set.add(new Integer(val));
              result[i] = val;
          }
          return result;
      }
      
      SortedSet<Integer> setExc = new TreeSet<Integer>();
      for(int i=0, l1=vecsExc.length; i<l1; i++) 
    	  for(int j=0, l2=vecsL[vecsExc[i]].length; j<l2; j++) 
    		  setExc.add(vecsL[vecsExc[i]][j]);

      if(nBicDim==nDim) for(int i=0; i<nBicDim; i++) result[i]=i;
      else {
    	int i=0;
    	if(overlapVecs!=null){
	    	for(Integer vecID : overlapVecs){
	    		for(int j=0, l=vecsL[vecID].length; j<l; j++) setExc.add(vecsL[vecID][j]);
	    		int nrOverlapVals = (int) (((double)vecsL[vecID].length)*overlap);
	    		for(int j=0, val=-1; j<nrOverlapVals && i<nBicDim; j++){
	    			val = vecsL[vecID][j];
	    			if(set.contains(val)) continue;
	            	set.add(val);
	            	result[i++] = val;
	    		}
	    	}
    	}
    	if(setExc.size()+(nBicDim-i)>nDim) throw new BicException("Not enough number of rows or columns to satisfy the biclusters' constraints!");
    	for (int val=-1; i<nBicDim; i++) {
          do val = r.nextInt(nDim); while (set.contains(val)||setExc.contains(val));
          set.add(val);
          result[i] = val;
        }
      }
      return result;
  }

  private int[] generateContiguous(int nBicDim, int nDim, double overlap, int[][] vecsL, int[] overlapVecs, int[] vecsExc) {
	  Random r = new Random(); 
      int[] result = new int[nBicDim];
      int posInicial = r.nextInt(nDim-nBicDim);
      for(int i=0; i<nBicDim; i++) result[i]=posInicial+i;
  	  return result;
  }

  private int[] generateNonOverlappingCols(int nBicDim, int nDim, Set<Integer> chosenCols) {
	  Random r = new Random(); 
      int[] result = new int[nBicDim];
      SortedSet<Integer> set = new TreeSet<Integer>();
      long k=0, limit=nDim*nDim;
      for(int i=0, val=-1; i<nBicDim; i++) {
         do{ val = r.nextInt(nDim); }
         while((set.contains(new Integer(val)) || chosenCols.contains(new Integer(val))) && k++<limit);
         if(k>limit){
             do{ val = r.nextInt(nDim); }
             while(set.contains(new Integer(val)));
         }
         set.add(new Integer(val));
         result[i] = val;
      }
      return result;
  }

  private int[] generateNonOverlappingRows(int nBicDim, int nDim, int[] bicCols, HashSet<String> elements) {
	  Random r = new Random(); 
      int[] result = new int[nBicDim];
      SortedSet<Integer> set = new TreeSet<Integer>();
      long k=0, limit=nDim*nDim*100;
      for(int i=0, val=-1; i<nBicDim; i++) {
         do{ val = r.nextInt(nDim); }
         while((set.contains(new Integer(val)) || isOverlap(val,bicCols,elements)) && k++<limit);
         
         if(k>limit){
             do{ val = r.nextInt(nDim); }
             while(set.contains(new Integer(val)));
             System.out.println("IMPT: Not able to meet the non-overlapping row criteria for the generate sets of columns!\nSuggestions: try again OR increase the matrix size OR decrease the size of bics!");
         }
         
         set.add(new Integer(val));
         result[i] = val;
      }
      return result;
  }
  
  public List<Integer> getOrderedList(int[] array, int[] order){
	  Map<Integer, Integer> indices = new HashMap<Integer, Integer>();
	  for(int i=0; i<order.length; i++) indices.put(order[i],i);
	  int[] sorted = Arrays.copyOf(order, order.length);
	  Arrays.sort(sorted);
	  List<Integer> result = new ArrayList<Integer>();
	  for(int i=0; i<array.length; i++) result.add(array[indices.get(sorted[i])]);
	  return result;
  }
  
  private boolean isOverlap(int val, int[] bicCols, HashSet<String> elements) {
	for(int j=0, l=bicCols.length; j<l; j++) if(elements.contains(val+":"+bicCols[j])) return true;
	return false;
  }

  /** Initializes the symbolic expression matrix */
  private int[][] initSymbolicMatrix(String background) {
	int[][] result = new int[numRows][numCols];
	for(int i=0; i<numRows; i++) 
	  if(background.equals("random")){
	  	  Random r = new Random();
		  for(int j=0; j<numCols; j++) result[i][j]=this.alphabet[r.nextInt(this.alphabet.length)];
	   } else if(background.equals("normal")){
		  NormalDistribution n = new NormalDistribution(this.alphabet.length/2,this.alphabet.length/5);
		  double[] vals = n.sample(this.numCols);
		  for (int j=0, l=alphabet.length-1; j<numCols; j++) 
			  if(vals[j]<0) vals[j]=0;
			  else if(vals[j]>l) vals[j]=l;
		  for (int j=0; j<numCols; j++) result[i][j] = this.alphabet[(int)vals[j]];
	   } else if(background.equals("missing")) {
		   for(int j=0; j<numCols; j++) result[i][j]=Integer.MIN_VALUE;
	   } else for(int j=0; j<numCols; j++) result[i][j]=-1; /*null*/
	return result;
  }
  
  /** Initializes the real expression matrix */
  private double[][] initRealMatrix(String background) {
	double[][] result = new double[numRows][numCols];
	for(int i=0; i<numRows; i++) 
	  if(background.equals("random")){
	  	  Random r = new Random();
		  for(int j=0; j<numCols; j++) result[i][j]=(r.nextDouble()*(maxM-minM))+minM;
	   } else if(background.equals("normal")){
		  NormalDistribution n = new NormalDistribution((maxM+minM)/2,(maxM-minM)/5);
		  double[] vals = n.sample(this.numCols);
		  for (int j=0; j<numCols; j++) 
			  if(vals[j]<minM) vals[j]=minM;
			  else if(vals[j]>maxM) vals[j]=maxM;
		  for (int j=0; j<numCols; j++) result[i][j] = vals[j];
	   } else for(int j=0; j<numCols; j++) result[i][j]=-1; /*null*/
	return result;
  }

}