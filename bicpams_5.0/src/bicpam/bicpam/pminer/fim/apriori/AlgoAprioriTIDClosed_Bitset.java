package bicpam.pminer.fim.apriori;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * This is an implementation of the AprioriTID algorithm.
 * The AprioriTID algorithm finds all the frequents itemsets and their support in a binary context.
 * AprioriTID is usually faster than Apriori and produce the same result.
 * This version is very fast because it uses bit vector for representing TID SETS.
 * 
 * @author Rui Henriques (based on SPMF)
 * @version 1.0
 */
public class AlgoAprioriTIDClosed_Bitset {

	protected Itemsets frequentItemsets = new Itemsets("FREQUENT ITEMSETS");
	private Itemsets frequentClosed = new Itemsets("FREQUENT CLOSED ITEMSETS");
	protected Map<Integer, BitSet> mapItemTIDS = new HashMap<Integer, BitSet>();
	List<Integer> listFrequentsSize1 = new ArrayList<Integer>();

	protected int k; // level
	protected List<List<Integer>> dataset;
	int minSuppRelative;
	int maxItemsetSize = Integer.MAX_VALUE;
	long startTimestamp = 0;
	long endTimestamp = 0;

	public AlgoAprioriTIDClosed_Bitset(List<List<Integer>> _dataset) {
		dataset = _dataset;
	}
	public void setData(List<List<Integer>> dataI) {
		dataset = dataI;
	}
	public void reset() {
		frequentItemsets = new Itemsets("FREQUENT ITEMSETS");
		frequentClosed = new Itemsets("FREQUENT CLOSED ITEMSETS");
		mapItemTIDS = new HashMap<Integer, BitSet>();
		listFrequentsSize1 = new ArrayList<Integer>();
	}
	public Itemsets runAlgorithm(double minsupp) {
		return runAlgorithm((int) Math.ceil(minsupp*dataset.size()));
	}
	public Itemsets runAlgorithm(int minsupp) {
		System.out.println("MINSUP:" + minsupp);
		startTimestamp = System.currentTimeMillis();
		mapItemTIDS = new HashMap<Integer, BitSet>(); // id item, count
		this.minSuppRelative = minsupp;
		if (this.minSuppRelative == 0) this.minSuppRelative = 1;

		// (1) count the tid set of each item in the database in one database pass
		for (int j=0; j<dataset.size(); j++) {
			Itemset transaction = new Itemset(dataset.get(j));
			for (int i = 0; i < transaction.size(); i++) {
				int item = transaction.get(i);
				BitSet tids = mapItemTIDS.get(item);
				if (tids == null) {
					tids = new BitSet();
					mapItemTIDS.put(item, tids);
				}
				tids.set(j);
			}
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
			} else iterator.remove();
		}
		
		//System.out.println("AKI:");
		//for(Itemset i : level) System.out.println(i.toString());

		// sort itemsets of size 1 according to lexicographical order.
		Collections.sort(level, new Comparator<Itemset>() {
			public int compare(Itemset o1, Itemset o2) {
				return o1.get(0) - o2.get(0);
			}
		});
		
		k = 2;
		while (!level.isEmpty() && k <= maxItemsetSize) {
			List<Itemset> levelK = generateCandidateSizeK(level); // level k+1 with all the candidates with minsup
			checkIfItemsetsK_1AreClosed(level, levelK); // We check all sets of level k-1 for closure
			level = levelK; // We keep only the last level...
			k++;
		}
		endTimestamp = System.currentTimeMillis();
		return frequentItemsets; // Return all frequent itemsets found!
	}

	private void checkIfItemsetsK_1AreClosed(Collection<Itemset> level, List<Itemset> levelK) {
		for (Itemset itemset : level) {
			boolean isClosed = true;
			for (Itemset itemsetK : levelK) {
				if (itemsetK.getAbsoluteSupport() == itemset.getAbsoluteSupport() 
						&& itemset.includedIn(itemsetK)) {
					isClosed = false;
					break;
				}
			}
			if (isClosed) frequentClosed.addItemset(itemset, k - 1);
		}
	}

	protected List<Itemset> generateCandidateSizeK(List<Itemset> levelK_1) {
		List<Itemset> candidates = new ArrayList<Itemset>();

		// For each itemset I1 and I2 of level k-1
		loop1: for (int i = 0; i < levelK_1.size(); i++) {
			Itemset itemset1 = levelK_1.get(i);
			loop2: for (int j = i + 1; j < levelK_1.size(); j++) {
				Itemset itemset2 = levelK_1.get(j);

				// we compare items of itemset1 and itemset2.
				for (int k = 0; k < itemset1.size(); k++) {
					if (k == itemset1.size() - 1) {
						if (itemset1.getItems().get(k) >= itemset2.get(k)) continue loop1;
					}
					else if (itemset1.getItems().get(k) < itemset2.get(k)) continue loop2; // we continue searching
					else if (itemset1.getItems().get(k) > itemset2.get(k)) continue loop1; // we stop searching
				}
				Integer missing = itemset2.get(itemset2.size() - 1);

				// create list of common tids
				BitSet list = (BitSet) itemset1.getTransactionsIds().clone();
				list.and(itemset2.getTransactionsIds());

				int cardinality = list.cardinality();
				//System.out.println(itemset1 + "," + itemset2 + ":" + cardinality + ">>"+minSuppRelative);
				if (cardinality >= minSuppRelative) {
					// Create a new candidate by combining itemset1 and itemset2
					Itemset candidate = new Itemset();
					for (int k = 0; k < itemset1.size(); k++) candidate.addItem(itemset1.get(k));
					candidate.addItem(missing);
					candidate.setTransactioncount(list, cardinality);
					candidates.add(candidate);
					frequentItemsets.addItemset(candidate, k);
				}
			}
		}
		return candidates;
	}

	public Itemsets getItemsets() {
		return frequentItemsets;
	}

	public Itemsets getFrequentClosed() {
		return frequentClosed;
	}

	public void setMaxItemsetSize(int maxItemsetSize) {
		this.maxItemsetSize = maxItemsetSize;
	}

	public void printStats() {
		System.out.println("=============  APRIORI-CLOSE - STATS =============");
		long temps = endTimestamp - startTimestamp;
		// System.out.println(" Total time ~ " + temps + " ms");
		System.out.println(" Transactions count from database : "+dataset.size());
		System.out.println(" The algorithm stopped at size " + (k - 1) + ", because there is no candidate");
		System.out.println(" Frequent itemsets count : " + frequentItemsets.getItemsetsCount());
		frequentItemsets.printItemsets(dataset.size());
		System.out.println(" Total time ~ " + temps + " ms");
		System.out.println("===================================================");
	}
}
