package bicpam.bicminer.constant;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import domain.Biclusters;
import domain.Dataset;
import bicpam.bicminer.BiclusterMiner;
import bicpam.closing.Biclusterizer;
import bicpam.mapping.ItemMapper;
import bicpam.pminer.PM;


/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class ConstantOverallBiclusterMiner extends BiclusterMiner {
	
	public Biclusters mineItemsets() throws Exception {
		Biclusters itemsets = new Biclusters();
		List<List<Integer>> originalIndexes = data.indexes, originalScores = data.intscores;
		long time = System.currentTimeMillis();
		if(pminer.getMinNrBics()>0)
			pminer.inputMinNrBics(Math.max(1, pminer.getMinNrBics()/data.nrLabels));

		for(int k=0; k<data.nrLabels; k++){
			List<List<Integer>> newIndexes = new ArrayList<List<Integer>>(), newScores = new ArrayList<List<Integer>>();
			for(int i=0, l1=originalScores.size(); i<l1; i++){ 
				List<Integer> newIndexesRow = new ArrayList<Integer>(), newScoresRow = new ArrayList<Integer>(); 
				for(int j=0, l2=originalScores.get(i).size(); j<l2; j++){ 
					if(originalScores.get(i).get(j)==k){
						newIndexesRow.add(originalIndexes.get(i).get(j));
						newScoresRow.add(originalScores.get(i).get(j));
					}
				}
				newIndexes.add(newIndexesRow);
				newScores.add(newScoresRow);
			}
			System.out.println("===K:"+k+"===");
			data.indexes=newIndexes;
			data.intscores=newScores;
			//System.out.println(data.toIntString());
			List<List<Integer>> dataset = ItemMapper.itemize(data);
			//System.out.println(dataset);
			pminer.setNrLabels(data.nrLabels);
			pminer.setDataset(dataset);
			Biclusters bics = pminer.findFrequentItemSets();
			//System.out.println(bics.toString());
			itemsets.addAll(bics);
		}
		data.indexes=originalIndexes;
		data.intscores=originalScores;
		time = System.currentTimeMillis() - time;
		return itemsets;
	}

	public ConstantOverallBiclusterMiner() {}
	
	public ConstantOverallBiclusterMiner(Dataset data, PM pminer, Biclusterizer bichandler, Orientation orientation) {
		super(data,pminer,bichandler,orientation);
	}
}
