package bicpam.pminer.fim.eclat;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;


/**
 * This is an implementation of the ECLAT algorithm as proposed by ZAKI (2000).
 * 
 * This implementation may not be fully optimized. In particular, Zaki proposed
 * various extensions that I have not implemented.
 * 
 * Copyright (c) 2008-2012 Philippe Fournier-Viger
 * 
 * This file is part of the SPMF DATA MINING SOFTWARE
 * (http://www.philippe-fournier-viger.com/spmf).
 * 
 * SPMF is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * SPMF is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * SPMF. If not, see <http://www.gnu.org/licenses/>.
 */
public class AlgoEclat {

	protected Itemsets frequentItemsets = new Itemsets("FREQUENT ITEMSETS");
	protected int[][] dataset;
	protected long mem = 0;
	
	private long startTimestamp; // for stats
	private long endTimestamp; // for stats
	private int minsupRelative;

	// for optimization with a triangular matrix for counting itemsets of size 2.
	private TriangularMatrix matrix;
	private boolean useTriangularMatrixOptimization;

	public AlgoEclat(List<List<Integer>> _dataset) {}
	public void setData(List<List<Integer>> dataI) {}
	public void reset() {
		frequentItemsets = new Itemsets("FREQUENT ITEMSETS");
	}

	/**
	 * This algorithm has two parameters
	 * @param minsupp the minimum support
	 * @param itemCount
	 * @return
	 */
	public Itemsets runAlgorithm(double minsupp, boolean useTriangularMatrixOptimization) {
		startTimestamp = System.currentTimeMillis();
		this.minsupRelative = (int) Math.ceil(minsupp * dataset.length);
		this.useTriangularMatrixOptimization = useTriangularMatrixOptimization;

		Set<Integer> allTIDS = new HashSet<Integer>();

		// (1) First database pass : calculate tidsets of each item.
		int maxItemId = 0;
		final Map<Integer, Set<Integer>> mapItemCount = new HashMap<Integer, Set<Integer>>();
		for (int i = 0; i < dataset.length; i++) {
			allTIDS.add(i);
			for (int j = 0, l = dataset[i].length; j < l; j++) {
				int item = dataset[i][j];
				Set<Integer> set = mapItemCount.get(item);
				if (set == null) {
					set = new HashSet<Integer>();
					mapItemCount.put(item, set);
					if (item > maxItemId) {
						maxItemId = item;
					}
				}
				set.add(i); // add tid
			}
		}

		// (1.b) create the triangular matrix for counting the support of
		// itemsets of size 2
		// for optimization purposes.
		if (useTriangularMatrixOptimization) {
			matrix = new TriangularMatrix(maxItemId + 1);
			// for each transaction, take each itemset of size 2,
			// and update the triangular matrix.
			for(int k=0, l1=dataset.length; k<l1; k++){
				int[] array = dataset[k];
				for (int i = 0, l2=dataset[i].length; i < l2; i++) {
					Integer itemI = (Integer) array[i];
					for (int j = i + 1; j < l2; j++) {
						Integer itemJ = (Integer) array[j];
						// update the matrix
						matrix.incrementCount(itemI, itemJ);
					}
				}
			}
		}

		// (2) create ITSearchTree with root node
		ITSearchTree tree = new ITSearchTree();
		ITNode root = new ITNode(new Itemset());
		root.setTidset(allTIDS);
		tree.setRoot(root);
		System.out.println(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory());

		// (3) create childs of the root node.
		for (Entry<Integer, Set<Integer>> entry : mapItemCount.entrySet()) {
			// we only add nodes for items that are frequents
			if (entry.getValue().size() >= minsupRelative) {
				// create the new node
				Itemset itemset = new Itemset();
				itemset.addItem(entry.getKey());
				ITNode newNode = new ITNode(itemset);
				newNode.setTidset(entry.getValue());
				newNode.setParent(root);
				// add the new node as child of the root node
				root.getChildNodes().add(newNode);
			}
		}

		// save root node
		save(root);

		// for optimization
		sortChildren(root);

		while (root.getChildNodes().size() > 0) {
			ITNode child = root.getChildNodes().get(0);
			extend(child);
			save(child);
			delete(child);
		}

		mem = Math.max(mem,Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory());
		endTimestamp = System.currentTimeMillis();

		return frequentItemsets; // Return all frequent itemsets found!
	}

	private void extend(ITNode currNode) {
		// loop over the brothers
		for (ITNode brother : currNode.getParent().getChildNodes()) {
			//mem = Math.max(mem,Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory());
			if (brother != currNode) {
				ITNode candidate = getCandidate(currNode, brother);
				if (candidate != null) {
					currNode.getChildNodes().add(candidate);
					candidate.setParent(currNode);
				}
			}
		}

		sortChildren(currNode);

		mem = Math.max(mem,Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory());
		while (currNode.getChildNodes().size() > 0) {
			ITNode child = currNode.getChildNodes().get(0);
			extend(child);
			save(child);
			delete(child);
		}
	}

	private ITNode getCandidate(ITNode currNode, ITNode brother) {
		// optimization: if these node are itemsets of size 1, we just check the
		// triangular matrix to know their support. If they are not frequent,
		// then we don't need to calculate the list of common tids.
		if (useTriangularMatrixOptimization
				&& currNode.getItemset().size() == 1) {
			int support = matrix.getSupportForItems((Integer) currNode
					.getItemset().getItems().toArray()[0], (Integer) brother
					.getItemset().getItems().toArray()[0]);
			if (support < minsupRelative) {
				return null;
			}
		}

		// create list of common tids.
		Set<Integer> commonTids = new HashSet<Integer>();
		for (Integer tid : currNode.getTidset()) {
			if (brother.getTidset().contains(tid)) {
				commonTids.add(tid);
			}
		}

		// (2) check if the two itemsets have enough common tids
		// if not, we don't need to generate a rule for them.
		if (commonTids.size() >= minsupRelative) {
			Itemset union = currNode.getItemset().union(brother.getItemset());
			ITNode node = new ITNode(union);
			node.setTidset(commonTids);
			return node;
		}

		return null;
	}

	private void delete(ITNode child) {
		child.getParent().getChildNodes().remove(child);
	}

	private void save(ITNode node) {
		Itemset itemset = node.getItemset();
		itemset.setTransactioncount(node.getTidset());
		frequentItemsets.addItemset(itemset, itemset.size());
	}

	private void sortChildren(ITNode node) {
		// sort children of the node according to the support.
		Collections.sort(node.getChildNodes(), new Comparator<ITNode>() {
			// Returns a negative integer, zero, or a positive integer as
			// the first argument is less than, equal to, or greater than the
			// second.
			public int compare(ITNode o1, ITNode o2) {
				return o1.getTidset().size() - o2.getTidset().size();
			}
		});
	}

	public void printStats() {
		System.out.println("=============  ECLAT - STATS =============");
		long temps = endTimestamp - startTimestamp;
		System.out.println(" Transactions count from database : " + dataset.length);
		System.out.println(" Frequent itemsets count : " + frequentItemsets.getItemsetsCount());
		frequentItemsets.printItemsets(dataset.length);
		System.out.println(" Total time ~ " + temps + " ms");
		System.out.println("===================================================");
	}

	public Itemsets getItemsets() {
		return frequentItemsets;
	}
}
