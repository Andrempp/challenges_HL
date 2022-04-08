package bicpam.pminer.fim.algorithms;

import java.util.List;

import bicpam.pminer.BicPM;
import bicpam.pminer.BicPMUtils;
import bicpam.pminer.fim.apriori.AlgoCharm_Bitset;
import bicpam.pminer.fim.apriori.Itemsets;
import domain.Biclusters;

/**
 * @author Rui Henriques
 * @version 1.0
 */
public class BicClosedCharm extends AlgoCharm_Bitset implements BicPM {

	public BicClosedCharm(List<List<Integer>> dataset) {
		super(dataset);
	}
    public void setData(List<List<Integer>> dataI) {
    	super.setData(dataI);
	}
	public void reset(){
		super.reset();
    }

	public Biclusters run(int minColumns, int minRows, int maxColumns, int maxRows, int nrLabels) throws Exception {
	  Itemsets itemsets = super.runAlgorithm((double)minRows/(double)dataset.size(),minColumns);
	  //System.out.println("#ALL:"+itemsets.getItemsetsCount());
	  //List<FrequentItemset> freqItemsets = BicFIMUtils.toBicItemsets(itemsets);
	  //for(FrequentItemset itemset : freqItemsets) System.out.print(itemset.nrItems+"|"+itemset.nrTrans+",");
	  return BicPMUtils.toValidItemsets(itemsets, minColumns, maxColumns, maxRows, nrLabels);
  }

  public Biclusters run(double support, int nrLabels) throws Exception {
	  Itemsets itemsets = super.runAlgorithm(support);
	  return BicPMUtils.toBicItemsets(itemsets, nrLabels);
  }

  public long getMemory() { return -1; }
}
