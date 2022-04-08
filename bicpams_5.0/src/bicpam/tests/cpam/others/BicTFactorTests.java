package tests.cpam.others;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import performance.significance.BSignificance;
import bicpam.bicminer.BiclusterMiner;
import bicpam.bicminer.BiclusterMiner.Orientation;
import bicpam.bicminer.constant.ConstantBiclusterMiner;
import bicpam.closing.BiclusterMerger;
import bicpam.closing.Biclusterizer;
import bicpam.pminer.fim.ClosedFIM;
import domain.Bicluster;
import domain.Biclusters;
import domain.Dataset;
import utils.BicResult;


/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class BicTFactorTests {
	
  static public String datafile = "/data/EnhancerPromoter2.txt";
  
  public static void main(String[] args) throws Exception {
	  
	/** Part 1: read input network **/
	Dataset data = readData(datafile,"\t");
	data.nrLabels = 2; /*coherence strength for binary data*/
	//PatternType type = PatternType.Constant; /*coherence assumption*/

	/** Part 2: parameterize coherence and other expectations **/
	int minNrBiclusters = 1000;
	int minColsModule = 3;
	double minOverlapMerging = 0.45;

	/** Part 3: no need for mapping step **/
	
	/** Part 4: instantiate BicNET **/
	ClosedFIM pminer = new ClosedFIM(); //new ClosedFIM();
	pminer.inputMinNrBics(minNrBiclusters);
	pminer.inputMinColumns(minColsModule);
	
	int nrIterations = 1;
	double removePercentage = 0.3;
	Biclusters bics = new Biclusters();
	List<List<Integer>> originalIndexes = copyList(data.indexes);
	List<List<Integer>> originalScores = copyList(data.intscores);
	for(int i=0; i<nrIterations; i++){
		
		BiclusterMiner bicminer = new ConstantBiclusterMiner(data,pminer,new Biclusterizer(new BiclusterMerger(minOverlapMerging)),Orientation.PatternOnRows); 

		/** Part 4: run BicNET **/
		long time = System.currentTimeMillis();
		Biclusters iBics = bicminer.mineBiclusters();
		time = System.currentTimeMillis() - time;
		System.out.println("Time:"+((double)time/(double)1000)+"ms");
		data.remove(iBics.getElementCounts(),removePercentage);
		bics.addAll(iBics);
	}	
	data.indexes = originalIndexes;
	data.intscores = originalScores;
	
	/** Part 5: output and evaluation **/		
	bics.order();
	//bics.shiftColumns(0);
	BSignificance.run(data,bics);
	BicResult.println("FOUND BICS:" + bics.toString(data.rows,data.columns));
	for(Bicluster bic : bics.getBiclusters()) BicResult.println(bic.toString(data)+"\n\n");
  }
  
  private static List<List<Integer>> copyList(List<List<Integer>> indexes) {
	List<List<Integer>> result = new ArrayList<List<Integer>>();
	for(List<Integer> index : indexes){
		List<Integer> newIndex = new ArrayList<Integer>();
		newIndex.addAll(index);
		result.add(newIndex);
	}
	return result;
  }

  private static Dataset readData(String dataname, String delimeter) throws IOException {
	List<String[]> table = new ArrayList<String[]>();
	BufferedReader br = new BufferedReader(new FileReader(new File("").getAbsolutePath() + dataname));
    String line;
    String[] features = br.readLine().split(delimeter);
    while((line=br.readLine())!=null && line.contains(delimeter)) 
       	table.add(line.split(delimeter));
    br.close();
    String[][] matrix = new String[table.size()][]; 
   	for(int i=0, l1=table.size(); i<l1; i++) matrix[i]=table.get(i);
        
    List<String> nodesX = new ArrayList<String>(), nodesY = new ArrayList<String>();
    List<List<Integer>> indexes = new ArrayList<List<Integer>>();
    List<List<Integer>> scores = new ArrayList<List<Integer>>();
    for(int i=1; i<features.length; i++) nodesY.add(features[i]);    
    for(int i=0, l1=matrix.length; i<l1; i++){
    	nodesX.add(matrix[i][0]);
    	List<Integer> indexing = new ArrayList<Integer>();
    	List<Integer> scoring = new ArrayList<Integer>();
	    for(int j=1, l2=features.length; j<l2; j++){
	    	int value = Integer.valueOf(matrix[i][j]);
	    	if(value==0) continue;
		    indexing.add(j-1);
	    	scoring.add(1);
	    }
   		indexes.add(indexing);
   		scores.add(scoring);
    }
    System.out.println("Dataset created!");
    return new Dataset(nodesX,nodesY,indexes,scores);
  }

}