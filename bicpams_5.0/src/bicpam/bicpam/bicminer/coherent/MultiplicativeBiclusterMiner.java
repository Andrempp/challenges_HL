package bicpam.bicminer.coherent;

import java.util.List;

import utils.BicMath;
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
public class MultiplicativeBiclusterMiner extends CoherentBiclusterMiner {

	public Biclusters mineItemsets() throws Exception {
		Biclusters itemsets = new Biclusters();
		int nrLabels= data.symmetry ? data.nrLabels*2*BicMath.lcmOfLinearVector(data.nrLabels/2) 
				: data.nrLabels*BicMath.lcmOfLinearVector(data.nrLabels);
		/*if(!data.symmetry){
			for(int i=0, l1=data.intscores.size(); i<l1; i++){
				for(int j=0, l2=data.intscores.get(i).size(); j<l2; j++){
					int value = data.intscores.get(i).get(j);
					data.intscores.get(i).set(j,value+1);
				}
			}
		}*/
		//System.out.println("Nr Labels:"+nrLabels);
		for(int i=0, l=data.columns.size(); i<l; i++){
			System.out.println("M"+i);
			List<List<Integer>> items = alignBy(i,data,CoherenceCriteria.Multiplicative);
			pminer.setDataset(ItemMapper.itemize(items,data.indexes,nrLabels,data.symmetry,null));
			pminer.setNrLabels(nrLabels);
			itemsets.addAll(pminer.findFrequentItemSets());
		}
		return itemsets;
	}

	public MultiplicativeBiclusterMiner() {}
	
	public MultiplicativeBiclusterMiner(Dataset data, PM pminer, Biclusterizer bichandler, Orientation orientation) {
		super(data,pminer,bichandler,orientation);
	}
}
