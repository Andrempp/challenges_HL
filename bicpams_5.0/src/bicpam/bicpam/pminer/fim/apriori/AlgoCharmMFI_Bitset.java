package bicpam.pminer.fim.apriori;

import java.util.ArrayList;
import java.util.List;

/**This is an implementation of the CHARM-MFI algorithm (szathmary06) that is a
 * simple extension that take as input the ouptput of CHARM algorithm by Zaki.
 * But event if it is called Charm-MFI it could be used with AprioriClose or any
 * other algorithms for mining frequent closed itemsets.
 * 
 * @author Rui Henriques (based on SPMF)
 * @version 1.0
 */
public class AlgoCharmMFI_Bitset extends AlgoCharm_Bitset {

	public AlgoCharmMFI_Bitset(List<List<Integer>> dataset) {
		super(dataset);
	}
	public void setData(List<List<Integer>> dataI) {
		super.setData(dataI);
	}

	protected Itemsets maximalItemsets;
	private long startTimestamp; // for stats
	private long endTimestamp; // for stats

	public void reset() {
		super.reset();
		maximalItemsets = null;
	}

	public Itemsets runAlgorithm(double support) {
		maximalItemsets = super.runAlgorithm(support);
		startTimestamp = System.currentTimeMillis();

		int maxItemsetLength = maximalItemsets.getLevels().size();
		for (int i = 1; i < maxItemsetLength - 1; i++) {
			List<Itemset> ti = maximalItemsets.getLevels().get(i);
			List<Itemset> tip1 = maximalItemsets.getLevels().get(i + 1);
			findMaximal(ti, tip1);
		}
		endTimestamp = System.currentTimeMillis();
		
		return maximalItemsets;
	}

	private void findMaximal(List<Itemset> ti, List<Itemset> tip1) {
		for (Itemset sup : tip1)
			for (Itemset sub : subset(ti, sup)) 
				sub.maximal = false;
	}

	// in a set of itemsets finds the subsets of a given itemset
	private List<Itemset> subset(List<Itemset> ti, Itemset sup) {
		List<Itemset> result = new ArrayList<Itemset>();
		for (Itemset itemset : ti) 
			if (sup.getItems().containsAll(itemset.getItems())) result.add(itemset);
		return result;
	}

	public void printStats(int transactionCount) {
		System.out.println("=============  CHARM-MFI - STATS =============");
		long temps = endTimestamp - startTimestamp;
		System.out.println(" Transactions count from database : "+ transactionCount);
		System.out.println(" Frequent itemsets count : "+ maximalItemsets.getItemsetsCount());
		maximalItemsets.printItemsets(transactionCount);
		System.out.println(" Total time ~ " + temps + " ms");
		System.out.println("===================================================");
	}

	public Itemsets getItemsets() {
		return maximalItemsets;
	}
}
