package bicpam.pminer.fim.apriori;

import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * This is an implementation of the CHARM algorithm that was proposed by MOHAMED ZAKI.
 * This implementation may not be fully optimized. 
 * 
 * @author Rui Henriques (based on SPMF)
 * @version 1.0
 */
public class AlgoCharm_Bitset {

	protected Itemsets frequentItemsets = new Itemsets("FREQUENT CLOSED ITEMSETS");
	Map<Integer, BitSet> mapItemTIDS = new HashMap<Integer, BitSet>();

	private long startTimestamp; // for stats
	private long endTimestamp; // for stats
	private int minsupRelative;
	private int minItems=0;
	private int tidcount=0;
	private TriangularMatrix matrix; // for optimization with a triangular matrix for counting itemsets of size 2
	private boolean useTriangularMatrixOptimization;
	private HashTable hash;
	protected List<List<Integer>> dataset;

	public AlgoCharm_Bitset(List<List<Integer>> _dataset) {
		dataset = _dataset;
		this.hash = new HashTable(100);
	}
	public void setData(List<List<Integer>> dataI) {
		dataset = dataI;
		this.hash = new HashTable(100);
	}
	public void reset() {
		frequentItemsets = new Itemsets("FREQUENT CLOSED ITEMSETS");
		mapItemTIDS = new HashMap<Integer, BitSet>();
	}
	public Itemsets runAlgorithm(double minsuppAbsolute, int minColumns) {
		minItems = minColumns;
		return runAlgorithm(minsuppAbsolute);
	}

	/**
	 * This algorithm has two parameters
	 * @param minsupp the ABSOLUTE minimum support
	 * @param itemCount
	 * @return
	 */
	public Itemsets runAlgorithm(double minsuppAbsolute, boolean useTriangularMatrixOptimization) {
		this.minsupRelative = (int) Math.ceil(minsuppAbsolute *dataset.size());
		//System.out.println("support:"+minsupRelative);
		this.useTriangularMatrixOptimization = useTriangularMatrixOptimization;
		return run();
	}
	public Itemsets runAlgorithm(double minsuppAbsolute) {
		return this.runAlgorithm(minsuppAbsolute, false);
	}

	/**
	 * This algorithm has two parameters
	 * @param minsuppthe RELATIVE minimum support
	 * @param itemCount
	 * @return
	 */
	public Itemsets runAlgorithmWithRelativeMinsup(boolean useTriangularMatrixOptimization, int minsupRelative) {
		this.minsupRelative = minsupRelative;
		this.useTriangularMatrixOptimization = useTriangularMatrixOptimization;
		return run();
	}

	/**
	 * This algorithm has two parameters
	 * @param minsupp the minimum support
	 * @param itemCount
	 * @return
	 */
	private Itemsets run() {
		startTimestamp = System.currentTimeMillis();
		Set<Integer> allTIDS = new HashSet<Integer>();

		// (1) First database pass : calculate tidsets of each item.
		int maxItemId = 0;
		mapItemTIDS = new HashMap<Integer, BitSet>(); // id item, count
		for (int i = 0; i < dataset.size(); i++) {
			allTIDS.add(i); // context.getObjects().get(i).transactionId
			for (int item : dataset.get(i)){
				BitSet tids = mapItemTIDS.get(item);
				if (tids == null) {
					tids = new BitSet();
					mapItemTIDS.put(item, tids);
				}
				tids.set(i);
			}
			tidcount++;
		}
		//BicResult.println("=>"+mapItemTIDS);

		if (useTriangularMatrixOptimization) {
			// (1.b) create the triangular matrix for counting the support of itemsets of size 2 for optimization purposes.
			matrix = new TriangularMatrix(maxItemId + 1);
			// for each transaction, take each itemset of size 2, and update the triangular matrix.
			for (int m=0; m<dataset.size(); m++) {
				int[] vec = new int[dataset.get(m).size()];
				int i=0;
				for(Integer item : dataset.get(m)) vec[i++] = item; //TODO: verify
				SetItemset itemset = new SetItemset(vec);
				Object[] array = itemset.itemset.toArray();
				for (i = 0; i < itemset.size(); i++) {
					Integer itemI = (Integer) array[i];
					for (int j = i + 1; j < itemset.size(); j++) {
						Integer itemJ = (Integer) array[j];
						matrix.incrementCount(itemI, itemJ);// update the matrix
					}
				}
			}
		}

		// (2) create ITSearchTree with root node
		ITSearchTree tree = new ITSearchTree();
		ITNode root = new ITNode(new HashSet<Integer>());
		root.setTidset(null, tidcount);
		tree.setRoot(root);

		// (3) create childs of the root node.
		for (Entry<Integer, BitSet> entry : mapItemTIDS.entrySet()) {
			int entryCardinality = entry.getValue().cardinality();
			// we only add nodes for items that are frequents
			if (entryCardinality >= minsupRelative) {
				// create the new node
				Set<Integer> itemset = new HashSet<Integer>();
				itemset.add(entry.getKey());
				ITNode newNode = new ITNode(itemset);
				newNode.setTidset(entry.getValue(), entryCardinality);
				newNode.setParent(root);
				// add the new node as child of the root node
				root.getChildNodes().add(newNode);
			}
		}
		sortChildren(root);

		while (root.getChildNodes().size() > 0) {
			ITNode child = root.getChildNodes().get(0);
			extend(child);
			save(child);
			delete(child);
		}
		
		//System.out.println(">>"+frequentItemsets.getLevels());
		
		endTimestamp = System.currentTimeMillis();
		return getClosedItemsets(); // Return all frequent itemsets found!
	}

	private void extend(ITNode currNode) {
		int i = 0;
		//System.out.print(currNode.getParent().getChildNodes().size()+",");
		while (i < currNode.getParent().getChildNodes().size()) {
			ITNode brother = currNode.getParent().getChildNodes().get(i);
			if (brother != currNode) {
				// Property 1
				if (currNode.getTidset().equals(brother.getTidset())) {
					replaceInSubtree(currNode, brother.getItemset());
					delete(brother);
				}
				// Property 2
				else if (containsAll(brother, currNode)) {
					replaceInSubtree(currNode, brother.getItemset());
					i++;
				}
				// Property 3
				else if (containsAll(currNode, brother)) {
					ITNode candidate = getCandidate(currNode, brother);
					delete(brother);
					if (candidate != null) {
						currNode.getChildNodes().add(candidate);
						candidate.setParent(currNode);
					}
				}
				// Property 4
				else if (!currNode.getTidset().equals(brother.getTidset())) {
					ITNode candidate = getCandidate(currNode, brother);
					if (candidate != null) {
						currNode.getChildNodes().add(candidate);
						candidate.setParent(currNode);
					}
					i++;
				} else i++;
			} else i++;
		}
		sortChildren(currNode);

		while (currNode.getChildNodes().size() > 0) {
			ITNode child = currNode.getChildNodes().get(0);
			extend(child);
			save(child);
			delete(child);
		}
	}

	private boolean containsAll(ITNode node1, ITNode node2) {
		BitSet newbitset = (BitSet) node2.getTidset().clone();
		newbitset.and(node1.getTidset());
		return newbitset.cardinality() == node2.size();
	}

	private void replaceInSubtree(ITNode currNode, Set<Integer> itemset) {
		// make the union
		Set<Integer> union = new HashSet<Integer>(itemset);
		union.addAll(currNode.getItemset());
		// replace for this node
		currNode.setItemset(union);
		// replace for the childs of this node
		currNode.replaceInChildren(union);
	}

	private ITNode getCandidate(ITNode currNode, ITNode brother) {

		// create list of common tids.
		BitSet commonTids = (BitSet) currNode.getTidset().clone();
		commonTids.and(brother.getTidset());
		int cardinality = commonTids.cardinality();

		// (2) check if the two itemsets have enough common tids
		// if not, we don't need to generate a rule for them.
		if (cardinality >= minsupRelative) {
			Set<Integer> union = new HashSet<Integer>(brother.getItemset());
			union.addAll(currNode.getItemset());
			ITNode node = new ITNode(union);
			node.setTidset(commonTids, cardinality);
			return node;
		}
		return null;
	}

	private void delete(ITNode child) {
		child.getParent().getChildNodes().remove(child);
	}

	private void save(ITNode node) {
		if(node.itemsetObject.size()<minItems) return;
		SetItemset itemset = node.itemsetObject;
		if(!hash.containsSupersetOf(itemset)){
			frequentItemsets.addItemset(new Itemset(itemset.itemset,itemset.tidset,itemset.cardinality), itemset.size());
			hash.put(itemset);
		}
	}

	private void sortChildren(ITNode node) {
		Collections.sort(node.getChildNodes(), new Comparator<ITNode>() {
			public int compare(ITNode o1, ITNode o2) {
				return o1.getTidset().size() - o2.getTidset().size();
			}
		});
	}

	public void printStats() {
		System.out.println("=============  CHARM - STATS =============");
		long temps = endTimestamp - startTimestamp;
		System.out.println(" Transactions count from database : " + dataset.size());
		System.out.println(" Frequent itemsets count : " + frequentItemsets.getItemsetsCount());
		frequentItemsets.printItemsets(dataset.size());
		System.out.println(" Total time ~ " + temps + " ms");
		System.out.println("===================================================");
	}

	public Itemsets getClosedItemsets() {
		/*for (List<SetItemset> hashE : hash.table) {
			if (hashE != null) for (SetItemset itemset : hashE) 
					System.out.println(itemsetObject.toString() + " Support: " + itemsetObject.cardinality 
							+ " / " + tidcount	+ " = "	+ itemsetObject.getRelativeSupport(tidcount));
		}*/
		return frequentItemsets;
	}

	public HashTable getHashTable() {
		return hash;
	}
}
