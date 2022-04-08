package bicpam.bicminer.coherent;

import java.util.List;

import domain.Biclusters;
import domain.Dataset;
import bicpam.closing.BiclusterMerger;
import bicpam.closing.Biclusterizer;
import bicpam.mapping.ItemMapper;
import bicpam.mapping.Itemizer;
import bicpam.pminer.PM;

/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class SymmetricBiclusterMiner extends CoherentBiclusterMiner {
	
	public Biclusters mineItemsets() throws Exception {
		Biclusters itemsets = new Biclusters();
		pminer.setNrLabels(data.nrLabels);
		for(int j=0, l=data.columns.size(); j<l; j++){//
			System.out.println("M"+j);
			//BicResult.println("Itemized\n"+BicPrinting.plot(itemizer.itemize()));
			List<List<Integer>> intscores = alignBy(j,data,CoherenceCriteria.Constant);
			pminer.setDataset(ItemMapper.itemize(intscores,data.indexes,data.nrLabels,data.symmetry,null));
			itemsets.addAll(pminer.findFrequentItemSets());
		}
		return itemsets;
	}

	public SymmetricBiclusterMiner() {}
	
	public SymmetricBiclusterMiner(Dataset data, PM pminer, Biclusterizer bichandler, Orientation orientation) {
		super(data,pminer,bichandler,orientation);
	}
}
