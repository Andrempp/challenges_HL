package bicpam.bicminer.constant;

import generator.BicMatrixGenerator.PlaidCoherency;
import domain.Biclusters;
import domain.Dataset;
import bicpam.bicminer.BiclusterMiner;
import bicpam.bicminer.constant.PlaidMiner.CoherencyConstraint;
import bicpam.closing.BiclusterMerger;
import bicpam.closing.Biclusterizer;
import bicpam.mapping.ItemMapper;
import bicpam.mapping.Itemizer;
import bicpam.pminer.PM;


/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class ConstantPlaidBiclusterMiner extends BiclusterMiner {

	PlaidCoherency criteria;
	CoherencyConstraint constraint;
	double error;
		
	public Biclusters mineItemsets() throws Exception {
		pminer.setDataset(ItemMapper.itemize(data));
		long time = System.currentTimeMillis();
		Biclusters itemsets = pminer.findFrequentItemSets();
		itemsets = super.getPlaidFromCompactSet(itemsets, criteria, constraint, error);
		time = System.currentTimeMillis() - time;
		return itemsets;
	}

	public ConstantPlaidBiclusterMiner(Dataset _data, PM _pminer, Biclusterizer _bichandler, Itemizer _itemizer, Orientation _orientation, PlaidCoherency _criteria) {
		this(_data,_pminer,_bichandler,_itemizer,_orientation,_criteria,CoherencyConstraint.InBetween, 0.9);
	}
	public ConstantPlaidBiclusterMiner(Dataset _data, PM _pminer, Biclusterizer _bichandler, Itemizer _itemizer, Orientation _orientation, PlaidCoherency _criteria, CoherencyConstraint _constraint, double _error){
		super(_data,_pminer,_bichandler,_orientation);
		criteria = _criteria;
		constraint = _constraint;
		error = _error;
	}
}
