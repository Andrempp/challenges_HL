package bicpam.pminer.fim.apriori;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import utils.BicMath;

/**
 * This is an implementation of the AprioriTID algorithm.
 * The AprioriTID algorithm finds all the frequents itemsets and their support in a binary context.
 * AprioriTID is usually faster than Apriori and produce the same result.
 * This version is very fast because it uses bit vector for representing TID SETS.
 * 
 * @author Rui Henriques (based on SPMF)
 * @version 1.0
 */
public class AlgoAprioriTID_Bitset {

	protected Itemsets frequentItemsets = new Itemsets("FREQUENT ITEMSETS");
	protected Map<Integer, BitSet> mapItemTIDS = new HashMap<Integer, BitSet>();
	protected int k; // level
	protected int minSuppRelative;
	protected int maxItemsetSize = Integer.MAX_VALUE;
	protected long startTimestamp = 0;
	protected long endTimestamp = 0;
	protected long mem = 0;
	protected List<List<Integer>> dataset;

	public AlgoAprioriTID_Bitset(List<List<Integer>> _dataset) { dataset=_dataset; }
	public void setData(List<List<Integer>> dataI) { dataset = dataI; }
	public void reset() {
		frequentItemsets = new Itemsets("FREQUENT ITEMSETS");
		mapItemTIDS = new HashMap<Integer, BitSet>();
	}

	public Itemsets runAlgorithm(double minsupp) {
		startTimestamp = System.currentTimeMillis();
		mapItemTIDS = new HashMap<Integer, BitSet>(); // id item, count
		int tidcount = 0;
		this.minSuppRelative = (int) Math.ceil(minsupp*dataset.size());
		if (this.minSuppRelative == 0) this.minSuppRelative = 1;
		System.out.println("sup:"+minSuppRelative);

		// (1) count the tid set of each item in the database in one database pass

		for (int j = 0; j < dataset.size(); j++) {
			Itemset transaction = new Itemset(dataset.get(j));
			for (int i = 0; i < transaction.size(); i++) {
				int item = transaction.get(i);
				BitSet tids = mapItemTIDS.get(item);
				if (tids == null) {
					tids = new BitSet();
					mapItemTIDS.put(item, tids);
				}
				tids.set(tidcount);
			}
			tidcount++;
		}

		// We scan the database one time to calculate the support of each candidate.
		k = 1;
		List<Itemset> level = new ArrayList<Itemset>();
		Iterator<Entry<Integer, BitSet>> iterator = mapItemTIDS.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<Integer, BitSet> entry = (Map.Entry<Integer, BitSet>) iterator.next();
			int cardinality = entry.getValue().cardinality();
			if (cardinality >= minSuppRelative) { // if frequent
				Integer item = entry.getKey();
				Itemset itemset = new Itemset();
				itemset.addItem(item);
				itemset.setTransactioncount(mapItemTIDS.get(item), cardinality);
				level.add(itemset);
				frequentItemsets.addItemset(itemset, 1);
			} else iterator.remove(); // if the item is not frequent
		}

		// sort itemsets of size 1 according to lexicographical order.
		Collections.sort(level, new Comparator<Itemset>() {
			public int compare(Itemset o1, Itemset o2) {
				return o1.get(0) - o2.get(0);
			}
		});

		k = 2; // Generate candidates with size k = 1 (all itemsets of size 1)
		while (!level.isEmpty() && k <= maxItemsetSize) {
			// level k+1 with all the candidates with minsup
			level = generateCandidateSizeK(level);
			k++;
		}
		mem = Math.max(mem,Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory());
		endTimestamp = System.currentTimeMillis();
		return frequentItemsets;
	}

	/* Based on the description of Pasquier 99: "Efficient mining..."
	protected List<Itemset> generateCandidateSize1() {
		List<Itemset> candidates = new ArrayList<Itemset>(); // liste d'itemsets
		int nrLabels = BicMath.max(dataset);
		for (int item=1; item<=nrLabels; item++) {
			Itemset itemset = new Itemset();
			itemset.addItem(item);
			candidates.add(itemset);
		}
		mem = Math.max(mem,Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory());
		return candidates;
	}*/

	protected List<Itemset> generateCandidateSizeK(List<Itemset> levelK_1) {
		List<Itemset> candidates = new ArrayList<Itemset>();

		// For each itemset I1 and I2 of level k-1
		loop1: for (int i = 0; i < levelK_1.size(); i++) {
			Itemset itemset1 = levelK_1.get(i);
			loop2: for (int j = i + 1; j < levelK_1.size(); j++) {
				Itemset itemset2 = levelK_1.get(j);

				// Compare and combine items of itemset1 and itemset2.
				// If they have all the same k-1 items and the last item of itemset1 is smaller
				for (int k = 0; k < itemset1.size(); k++) {
					if (k == itemset1.size() - 1) {
						if (itemset1.getItems().get(k) >= itemset2.get(k)) continue loop1;
					}
					// if they are not the last items, and
					else if (itemset1.getItems().get(k) < itemset2.get(k)) continue loop2; // we continue searching
					else if (itemset1.getItems().get(k) > itemset2.get(k)) continue loop1; // we stop searching
				}
				Integer missing = itemset2.get(itemset2.size() - 1);

				// create list of common tids
				BitSet list = (BitSet) itemset1.getTransactionsIds().clone();
				list.and(itemset2.getTransactionsIds());
				int cardinality = list.cardinality();

				if (cardinality >= minSuppRelative) {
					Itemset candidate = new Itemset();
					for (int k = 0; k < itemset1.size(); k++) candidate.addItem(itemset1.get(k));
					candidate.addItem(missing);
					candidate.setTransactioncount(list, cardinality);
					candidates.add(candidate);
					frequentItemsets.addItemset(candidate, k);
				}
				mem = Math.max(mem,Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory());
			}
		}
		return candidates;
	}

	public Itemsets getItemsets() {
		return frequentItemsets;
	}

	public void setMaxItemsetSize(int maxItemsetSize) {
		this.maxItemsetSize = maxItemsetSize;
	}

	public void printStats() {
		System.out.println("=============  APRIORI - STATS =============");
		long temps = endTimestamp - startTimestamp;
		// System.out.println(" Total time ~ " + temps + " ms");
		System.out.println(" Transactions count from database : " + dataset.size());
		System.out.println(" Frequent itemsets count : "+ frequentItemsets.getItemsetsCount());
		frequentItemsets.printItemsets(dataset.size());
		System.out.println(" Total time ~ " + temps + " ms");
		System.out.println("===================================================");
	}

}
