package tests.bicnet;

import java.io.File;
import java.util.Arrays;
import java.util.List;

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
import bicpam.mapping.Discretizer;
import bicpam.mapping.ItemMapper;
import bicpam.mapping.Itemizer.DiscretizationCriteria;
import bicpam.mapping.Itemizer.NoiseRelaxation;
import bicpam.mapping.Itemizer.NormalizationCriteria;
import bicpam.mapping.Normalizer;
import bicpam.pminer.fim.MaximalFIM;
import bicpam.pminer.spm.SequentialPM;
import bicpam.pminer.spm.SequentialPM.SequentialImplementation;
import domain.Bicluster;
import domain.Biclusters;
import domain.Dataset;
import generator.BicMatrixGenerator.PatternType;
import utils.BicResult;
import utils.NetMatrixMapper;
import utils.others.CopyUtils;

/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class BicRealNetTests {
	
  static public String path = "/data/networks/";
  static public String ecoli = "ecoliB.protein.links.v10.txt";
  static public String ppirandom = "386585.protein.links.detailed.v9.1.txt";
  static public String scerevisae = "4932.scerevisae.protein.links.v10.txt";
  static public String human = "protein.hsapiens.links.v10.txt";
  //static public String human = "psample.hsapiens.links.v10.txt";
  static public String drygin = "DRYGIN_sgadata.txt";
  
  public static void main(String[] args) throws Exception {
	  
	/** Part 1: read input network **/
	path = new File("").getAbsolutePath()+path;
	//Dataset data = NetMatrixMapper.getTable(path+drygin, 0/*node A*/, 2/*node B*/, 4/*score*/, "\t"/*delimeter*/, true);
	Dataset data = NetMatrixMapper.getTable(path+scerevisae,0,1,2," ",true);
	//Dataset data = NetMatrixMapper.getTable(path+ecoli,0,1,2," ",true);
	//Dataset data = NetMatrixMapper.getTable(path+human,0,1,2," ",true);

	/** B: Define Stopping Criteria **/
	int minNrBicsBeforeMerging = 10; //choose high number of biclusters
	int minNrColumns = 4;
	int nrIterations = 1; //increase for more even space exploration 

	/** Part 2: parameterize coherence and other expectations **/
	double minOverlapMerging = 0.8, filteringThreshold = 0.5;
	PatternType type = PatternType.Constant; /*coherence assumption*/
	data.nrLabels = 4; /*coherence strength (e.g. 4->{0,1,2,3})*/
	List<Integer> removals = Arrays.asList(0,1,2); /*non-relevant ranges of values*/

	/** Part 3: mapping step **/
	if(type.equals(PatternType.OrderPreserving)) data.nrLabels = 20;
	data = Normalizer.run(data,NormalizationCriteria.Row);
	data = Discretizer.run(data,DiscretizationCriteria.NormalDist,NoiseRelaxation.None,data.nrLabels);
	data = ItemMapper.remove(data,removals);
	//BicResult.println("Network values:\n"+data.intscores);
			
	/** Part 4: instantiate BicNET **/
	BiclusterMiner bicminer = null;
	Orientation orientation = Orientation.PatternOnRows;
	Biclusterizer posthandler = new Biclusterizer(
			new BiclusterMerger(minOverlapMerging),
			new BiclusterFilter(filteringThreshold));
	
   	if(type.equals(PatternType.OrderPreserving)){
		SequentialPM pminer = new SequentialPM();
	    pminer.algorithm = SequentialImplementation.PrefixSpan;
		pminer.inputMinNrBics(minNrBicsBeforeMerging);
		pminer.inputMinColumns(minNrColumns);
		bicminer = new OrderPreservingBiclusterMiner(data,pminer,posthandler,orientation);		
	} else {
		MaximalFIM pminer = new MaximalFIM(); //new ClosedFIM();
		pminer.inputMinNrBics(minNrBicsBeforeMerging);
		pminer.inputMinColumns(minNrColumns);
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
			
	/** Part 4: run BicNET **/
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
	time = System.currentTimeMillis() - time;
	
	/** Part 5: output and evaluation **/
	System.out.println("Time:"+((double)time/(double)1000)+"ms");
	BicResult.println("FOUND BICS:" + bics.toString(data.rows,data.columns));
	for(Bicluster bic : bics.getBiclusters()) BicResult.println(bic.toString(data)+"\n\n");
  }
}