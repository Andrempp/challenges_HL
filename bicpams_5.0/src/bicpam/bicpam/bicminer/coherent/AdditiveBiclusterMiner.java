package bicpam.bicminer.coherent;

import java.util.ArrayList;
import java.util.List;

import bicpam.closing.BiclusterMerger;
import bicpam.closing.Biclusterizer;
import bicpam.mapping.ItemMapper;
import bicpam.mapping.Itemizer;
import bicpam.pminer.PM;
import domain.Biclusters;
import domain.Dataset;

/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class AdditiveBiclusterMiner extends CoherentBiclusterMiner {
	
	public Biclusters mineItemsets() throws Exception {
		Biclusters itemsets = new Biclusters();
		pminer.setNrLabels(data.nrLabels*2);
		if(data.symmetry){
			int shift = data.nrLabels/2;
			int adjustpos = data.nrLabels%2==0 ? 1 : 0;
			for(int i=0, l1=data.intscores.size(); i<l1; i++){
				for(int j=0, l2=data.intscores.get(i).size(); j<l2; j++){
					int value = data.intscores.get(i).get(j);
					data.intscores.get(i).set(j,value+shift+(value<0 ? adjustpos : 0));
				}
			}
		}
		for(int j=0, l=data.columns.size(); j<l; j++){
			System.out.println("M"+j);
			List<List<Integer>> items = alignBy(j,data,CoherenceCriteria.Additive);
			pminer.setDataset(ItemMapper.itemize(items,data.indexes,data.nrLabels*2,false,null));
			itemsets.addAll(pminer.findFrequentItemSets());
		}
		return itemsets;
	}

	public AdditiveBiclusterMiner() {}
	
	public AdditiveBiclusterMiner(Dataset data, PM pminer, Biclusterizer bichandler, Orientation orientation) {
		super(data,pminer,bichandler,orientation);
	}
}
