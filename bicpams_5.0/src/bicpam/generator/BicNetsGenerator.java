package generator;

import generator.BicMatrixGenerator.PatternType;
import generator.BicMatrixGenerator.PlaidCoherency;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;

import utils.BicException;
import utils.BicMath;
import domain.Bicluster;
import domain.Biclusters;
import domain.Dataset;


public class BicNetsGenerator {

  protected int numNodes, numInteractions, numBics;
  protected boolean symmetries;
  protected int[] alphabet;

  public List<List<Integer>> indexes, intscores;
  public List<List<Double>> scores;
  protected double maxM, minM;
  protected Biclusters plantedBics;

  protected boolean overlapEl = true;
  protected HashSet<String> elements = new HashSet<String>();

  public BicNetsGenerator(int nNodes, double density, int nBics, double minValue, double maxValue, double strength) {
	init(nNodes,density,nBics,(int)((maxValue-minValue)/strength),maxValue==-minValue);
	maxM=maxValue;
	minM=minValue;
	scores = new ArrayList<List<Double>>(numNodes);
	for(int i=0; i<numNodes; i++) scores.add(new ArrayList<Double>());
	initNet();
  }
  public BicNetsGenerator(int nNodes, double density, int nBics, int alphabetL, boolean symmetric) {
	init(nNodes, density, nBics, alphabetL, symmetric);
	intscores = new ArrayList<List<Integer>>(numNodes);
	for(int i=0; i<numNodes; i++) intscores.add(new ArrayList<Integer>());
	initNet();
  }
  private void init(int nNodes, double density, int nBics, int alphabetL, boolean symmetric) {
	numNodes=nNodes;
	indexes = new ArrayList<List<Integer>>(numNodes);
	for(int i=0; i<numNodes; i++) indexes.add(new ArrayList<Integer>());
	numInteractions=(int)(density*(double)(numNodes*numNodes));
	numBics=nBics;
	symmetries=symmetric;
    plantedBics = new Biclusters();
    alphabet = new int[alphabetL];
    int val = symmetries ? -(alphabetL/2) : 0; 
    for(int i=0; i<alphabetL; i++, val++) alphabet[i]=val;
    if(symmetries && alphabetL%2==0) 
      for(int i=alphabetL/2; i<alphabetL; i++) alphabet[i]++;
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
   * @param cols1 Either u from N(u,s) or minimum number of columns of a bicluster min(|J|)
   * @param cols2 Either s from N(u,s) or maximum number of columns of a bicluster max(|J|)
   * @return The generated biclusters
   */
  public Biclusters generateKBiclusters(PatternType type,
	      String distRowsBics, double rowsParam1, double rowsParam2, 
	      String distColsBics, double colsParam1, double colsParam2) {
	  return generateKBiclusters(type, distRowsBics, rowsParam1, rowsParam2,
			  distColsBics, colsParam1, colsParam2, new ArrayList<Integer>());
  }
  public Biclusters generateKBiclusters(PatternType type,
	      String distRowsBics, double rowsParam1, double rowsParam2, 
	      String distColsBics, double colsParam1, double colsParam2, List<Integer> itemsToRemove) {
	  
	  if(type.equals(PatternType.Multiple)){
		  numBics=numBics/2;
		  Biclusters bics = new Biclusters();
		  bics.addAll(generatePlaidBiclusters(PatternType.Constant, 
				  distRowsBics, (int) rowsParam1, (int) rowsParam2, 
				  distColsBics, (int) colsParam1, (int) colsParam2, 
				  PlaidCoherency.None, -1, -1, itemsToRemove));
		  plantedBics = new Biclusters();
		  bics.addAll(generatePlaidBiclusters(PatternType.Additive, 
				  distRowsBics, (int) rowsParam1*1.1, (int) rowsParam2*1.1, 
				  distColsBics, (int) colsParam1*1.1, (int) colsParam2*1.1, 
				  PlaidCoherency.None, -1, -1, itemsToRemove));
		  plantedBics = bics;
	  } else 
		  generatePlaidBiclusters(type,  
				  distRowsBics, (int) rowsParam1, (int) rowsParam2,
				  distColsBics, (int) colsParam1, (int) colsParam2, 
				  PlaidCoherency.None, -1, -1, itemsToRemove);		  
	  return plantedBics;
  }
  
  /**
   * Generate K biclusters with plaid effects
   * This method is organized in 6 major parts: see comments along the code 
   * @param type Bicluster coherency: Constant, Symmetric, Additive, Multiplicative, Order-Preserving
   * @param distRowsBics Distribution of the number of rows in biclusters: normal or uniform
   * @param rows1 Either u from N(u,s) or minimum number of rows of a bicluster min(|I|)
   * @param rows2 Either s from N(u,s) or maximum number of rows of a bicluster max(|I|)
   * @param distColsBics Distribution of the number of columns in biclusters: normal or uniform
   * @param cols1 Either u from N(u,s) or minimum number of columns of a bicluster min(|J|)
   * @param cols2 Either s from N(u,s) or maximum number of columns of a bicluster max(|J|)
   * @param plaidcoherency Overlapping coherency: Additive, Multiplicative, Interpoled, None
   * @param overlappingDegree Expected percentage of overlapping interactions
   * @param nrOverlappingBics Expectations on the n-wise overlapping relations (e.g. 2 if pairwise only)
   * @return The generated biclusters
   */
  public Biclusters generatePlaidBiclusters(PatternType type,  
		 String distRowsBics, double rows1, double rows2, String distColsBics, double cols1, double cols2, 
		 PlaidCoherency plaidcoherency, double overlappingDegree, int nrOverlappingBics, List<Integer> itemsToRemove) {
	
    Random r = new Random();
    int numRowsBics, numColsBics;
    int[][] bicsRows = new int[numBics][1], bicsCols = new int[numBics][1];
    int[][] symbols = new int[numBics][1], indexSymbols = new int[numBics][1];
    int[] maxRowContribution = new int[numBics], patternRange = new int[numBics];
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
	  symbols[k] = new int[numColsBics];
	  indexSymbols[k] = new int[numColsBics];
	  if(type.equals(PatternType.Multiplicative)) {
		  int shift = symmetries ? (int)Math.ceil(((double)alphabet.length)/2.0) : 1;
    	  for(int j=0; j<numColsBics; j++){
    		  int signal = symmetries ? (r.nextBoolean() ? 1 : -1) : 1;
    		  symbols[k][j] = signal*alphabet[r.nextInt(patternRange[k])+shift];
    	  }
      } else if(type.equals(PatternType.Additive)) 
    	  for(int j=0; j<numColsBics; j++){
    		  indexSymbols[k][j] = r.nextInt(patternRange[k]);
    		  symbols[k][j] = alphabet[indexSymbols[k][j]];
    	  }
      else if(type.equals(PatternType.OrderPreserving))
    	  for(int j=0; j<numColsBics; j++) symbols[k][j] = alphabet[r.nextInt(patternRange[k])];
      else {
    	  for(int j=0; j<numColsBics; j++) {
        	  do { symbols[k][j] = alphabet[r.nextInt(patternRange[k])]; }
        	  while(itemsToRemove.contains(symbols[k][j]));
    	  }
      }
      
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
      bicsCols[k] = generate(numColsBics,numNodes,overlappingDegree,bicsCols,bicsWithOverlap,bicsExcluded);
      if(overlapEl) bicsRows[k] = generate(numRowsBics,numNodes,overlappingDegree,bicsRows,bicsWithOverlap,bicsExcluded);
      else bicsRows[k]=generateNonOverlapping(numRowsBics,numNodes,bicsCols[k],elements);

      for(int i=0; i<numRowsBics; i++)
    	  for(int j=0; j<numColsBics; j++)
    		  elements.add(bicsRows[k][i]+":"+bicsCols[k][j]);

      	
      Bicluster bicK = new Bicluster(BicMath.getSet(bicsRows[k]), BicMath.getSet(bicsCols[k]), BicMath.getList(symbols[k]), PatternType.Constant);
	  plantedBics.add(bicK);
    }
    //System.out.println("BicsRows:"+BicPrinting.plot(bicsRows));
    //System.out.println("BicsCols:"+BicPrinting.plot(bicsCols));
    
	for (int k=0; k<numBics; k++){

	  /** PART VI: generate biclusters coherencies **/
	  int[][] bicsymbols = new int[bicsRows[k].length][bicsCols[k].length];
      if(type.equals(PatternType.Additive)) 
    	  maxRowContribution[k] = alphabet.length-patternRange[k]+1;
      else if(type.equals(PatternType.Multiplicative)){
    	  int maxval=Math.max(BicMath.max(symbols[k]),-BicMath.min(symbols[k]));
    	  int maxoriginal=Math.max(BicMath.max(alphabet),-BicMath.min(alphabet));
    	  maxRowContribution[k] = maxoriginal/maxval;
      }
	  //System.out.println("PatternRange"+patternRange[k]);    
	  //System.out.println("MaxRowCont"+maxRowContribution[k]);    
	  //System.out.println("Symbols:"+BicPrinting.plot(symbols[k]));
	  
      List<Integer> rowContributions = new ArrayList<Integer>();
	  for (int i=0, rowContribution=0; i<bicsRows[k].length; i++){
		if(type.equals(PatternType.OrderPreserving)){
			rowContributions.add(0);
	        int[] vector = new int[bicsCols[k].length];
	        for(int j=0; j<bicsCols[k].length; j++) vector[j] = this.alphabet[r.nextInt(this.alphabet.length)];
			Arrays.sort(vector);
		    for(int j=0; j<bicsCols[k].length; j++) bicsymbols[i][j] = vector[j];
		} else if(type.equals(PatternType.Constant)){
			rowContributions.add(0);
	       	for(int j=0; j<bicsCols[k].length; j++) bicsymbols[i][j] = symbols[k][j];
		} else if(type.equals(PatternType.Symmetric)){
			int symmetry = r.nextBoolean() ? 1 : -1;
			rowContributions.add(symmetry);
	       	for(int j=0; j<bicsCols[k].length; j++) bicsymbols[i][j] = symbols[k][j]*symmetry;
		} else if(type.equals(PatternType.Additive)){
		    rowContribution = r.nextInt(maxRowContribution[k]);
			rowContributions.add(rowContribution);
	    	for(int j=0; j<bicsCols[k].length; j++) bicsymbols[i][j] = alphabet[indexSymbols[k][j]+rowContribution];
	    } else if(type.equals(PatternType.Multiplicative)){
	       	rowContribution = r.nextInt(maxRowContribution[k])+1; 
			rowContributions.add(rowContribution);
			int symmetry = 1;//symmetries ? (r.nextBoolean() ? 1 : -1) : 1;
      		for(int j=0; j<bicsCols[k].length; j++) bicsymbols[i][j] = symbols[k][j]*rowContribution*symmetry;
	    } 
	  }
	  plantedBics.getBiclusters().get(k).rowFactors = rowContributions;
	  //System.out.println("K"+k+" BicSymbols:\n"+BicPrinting.plot(bicsymbols));		

	  
	  /** Part VI: generate the layers according to plaid type and put them in the background **/
	  for (int i=0; i<bicsRows[k].length; i++){
		int row = bicsRows[k][i];
		for (int j=0; j<bicsCols[k].length; j++){
		  if(scores==null){
 			if(indexes.get(row).contains(bicsCols[k][j])){
 				int index = indexes.get(row).indexOf(bicsCols[k][j]);
 	 			switch(plaidcoherency){
 	 			  case Additive : intscores.get(row).set(index,intscores.get(i).get(j)+bicsymbols[i][j]); break;
 	 			  default : intscores.get(row).set(index,bicsymbols[i][j]); break;
 	 			}
 			} else {
 				indexes.get(row).add(bicsCols[k][j]);
 				intscores.get(row).add(bicsymbols[i][j]);
 			}
   		  } else {
	      	double adjustment = 0;
			if(symmetries && (alphabet.length%2==0)) adjustment = bicsymbols[i][j]<0 ? 0.5 : -0.5;
	      	double normalizedValue = ((double)bicsymbols[i][j]+adjustment)/(double)alphabet.length;
 	  		if(indexes.get(row).contains(bicsCols[k][j])){
 	  			int index = indexes.get(row).indexOf(bicsCols[k][j]);
 	  			switch(plaidcoherency){
 	  			  case Additive : scores.get(row).set(index,scores.get(i).get(j)+normalizedValue*(maxM-minM)); break;
 	  			  default : scores.get(row).set(index,normalizedValue*(maxM-minM)); break;
 	  			}
 	  		} else {
 	  			indexes.get(row).add(bicsCols[k][j]);
 	  			scores.get(row).add(normalizedValue*(maxM-minM));
 	  		}
 	  	  }
 		}
	  }
	}
	
	/** Part VII: plant background data **/
	//int newInteractions=numInteractions;
	//for(List<Integer> indexList : indexes) newInteractions-=indexList.size();
	//System.out.println("All interactions="+numInteractions+"\nFree interactions="+newInteractions);
	
    return plantedBics;
  }
  

  /*********************************
   ******** PUBLIC METHODS *********
   *********************************/

  public int getNumLabels(){ return alphabet.length; }
  public Biclusters getPlantedBiclusters(){ return plantedBics; }
  public int countInteractions(){
	  int result = 0;
	  for(List<Integer> indexL : indexes) result += indexL.size();
	  return result;
  }

  public Dataset getDataset(){
	  List<String> nodes = new ArrayList<String>();
	  for(int i=0; i<numNodes; i++) nodes.add("N"+i);
	  Dataset dataset = new Dataset(nodes,indexes,scores);
	  if(intscores!=null) dataset.itemize(intscores, alphabet.length);
	  dataset.symmetry = symmetries;
	  return dataset;
  }

  /**
   * Plant a Missing Label in |X|x|Y|xPercentageMissing elements in the matrix
   * @param percMissing Percentage of missing values
   */
  public void putMissings(double percMissing) {
	int nrMissings = (int)(countInteractions()*percMissing);
	Random r = new Random(); 
	//System.out.println("Before Missings:"+countInteractions());
    for(int k=0; k<nrMissings; k++) {
       while(true){ 
     	  int row = r.nextInt(this.numNodes);
     	  if(indexes.get(row).isEmpty()) continue;
     	  int col = r.nextInt(indexes.get(row).size());
     	  indexes.get(row).remove(col);
     	  intscores.get(row).remove(col);
     	  break;
       }
    }
	//System.out.println("After Missings:"+countInteractions());
  }
  
  public void putBicMissings(double percMissing) {
	//System.out.println("Before Missings:"+countInteractions());
	for(Bicluster bic : plantedBics.getBiclusters()){
		List<Integer> cols = new ArrayList<Integer>();
		List<Integer> rows = new ArrayList<Integer>();
		cols.addAll(bic.columns);
		rows.addAll(bic.rows);
		int numCols=cols.size(), numRows=rows.size();
		int nrMissings = (int)(bic.numColumns()*bic.numRows()*percMissing);
		Random r = new Random();
		TreeSet<String> set = new TreeSet<String>();
	    for(int k=0; k<nrMissings; k++) {
	       while(true){ 
	     	  int col = cols.get(r.nextInt(numCols));
	     	  int row = rows.get(r.nextInt(numRows));
	     	  if(set.contains(row+","+col)) continue;
	     	  set.add(row+","+col);
	     	  int index = indexes.get(row).indexOf(col);
	     	  indexes.get(row).remove(index);
	     	  intscores.get(row).remove(index);
	     	  break;
	       }
	    }
		//System.out.println("After Missings:"+countInteractions());
	}
  }

  public void plantNoisyElements(double percNoise) {
		int nrNoisy = (int)(countInteractions()*percNoise);
		Random r = new Random(); 
		//System.out.println("Before Missings:"+countInteractions());
	    for(int k=0; k<nrNoisy; k++) {
	       while(true){ 
	     	  int row = r.nextInt(this.numNodes);
	     	  if(indexes.get(row).isEmpty()) continue;
	     	  int col = r.nextInt(indexes.get(row).size());
	     	  if(scores == null){
	     		  int val = alphabet[r.nextInt(alphabet.length)];
	     		  intscores.get(row).set(col, val);
	     	  } else {
	     		 double val = r.nextDouble()*(maxM-minM)+minM;
	     		 scores.get(row).set(col, val);
	     	  }
	     	  break;
	       }
    }
  }
  public void putNoise(double noise) {	
	Random r = new Random(); 
	for(int i=0, l1=indexes.size(); i<l1; i++){
		for(int j=0, l2=indexes.get(i).size(); j<l2; j++){
		   int symmetry = r.nextBoolean() ? 1 : -1;
	       if(intscores==null) {
	    	   double val = scores.get(i).get(j) + r.nextDouble()*(maxM-minM)*noise*symmetry;
	    	   if(val<minM) val=minM+(r.nextDouble()/10.0);	    	   
	    	   if(val>maxM) val=maxM-(r.nextDouble()/10.0);
	    	   scores.get(i).set(j,val);
	       } 
		}
	}
  }  

  
  /*********************************
   ******* PRIVATE METHODS *********
   *********************************/

  private void initNet() {
	Random r = new Random();
	for(int i=0; i<numNodes; i++){
	   for(int j=0; j<numNodes; j++){ 
	   	  while(numInteractions>0){
	   		  int row=r.nextInt(numNodes),col=r.nextInt(numNodes);
	   		  if(!indexes.get(row).contains(col)){
	   			  indexes.get(row).add(col);
	   			  if(scores==null) intscores.get(row).add(alphabet[r.nextInt(this.alphabet.length)]);
	   			  else scores.get(row).add((r.nextDouble()*(maxM-minM))+minM);
	   			  numInteractions--;
	   		  }
	   	  }
	   }
	}
  }

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
  
  private int[] generateNonOverlapping(int nBicDim, int nDim, int[] bicCols, HashSet<String> elements) {
	  Random r = new Random(); 
      int[] result = new int[nBicDim];
      SortedSet<Integer> set = new TreeSet<Integer>();
      for(int i=0, val=-1; i<nBicDim; i++) {
         do{ val = r.nextInt(nDim); }
         while(set.contains(new Integer(val)) || isOverlap(val,bicCols,elements));
         set.add(new Integer(val));
         result[i] = val;
      }
      return result;
  }

  private boolean isOverlap(int val, int[] bicCols, HashSet<String> elements) {
	for(int j=0, l=bicCols.length; j<l; j++) if(elements.contains(val+":"+bicCols[j])) return true;
	return false;
  }
}