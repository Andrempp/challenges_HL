package bicpam.pminer.fim.fpgrowth.spmf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import domain.constraint.Constraint;

/**
 * Copyright (c) 2014 Rui Henriques
 * Based on SPMF implementation (Philippe Fournier-Viger)
 */
public class SFPGrowthTID {

	protected Itemsets frequentItemsets = new Itemsets("FREQUENT ITEMSETS");
	protected List<List<Integer>> dataset;
	public int relativeMinsup;
	protected List<Itemset> itemSets; 
	protected List<List<Integer>> transSets; 
	
	protected boolean constrained = true;
	
	protected long mem = 0;
	private long startTimestamp; // for stats
	private long endTime; // for stats

	public SFPGrowthTID() {}
	public SFPGrowthTID(List<List<Integer>> _dataset) {
		dataset = _dataset;
	    transSets = new ArrayList<List<Integer>>(_dataset.size());
	    itemSets = new ArrayList<Itemset>();
	}
	public void setData(List<List<Integer>> dataI) {
		dataset = dataI;
	    transSets = new ArrayList<List<Integer>>(dataI.size());
	    itemSets = new ArrayList<Itemset>();
	}
	public void reset() {
		frequentItemsets = new Itemsets("FREQUENT ITEMSETS");
	    transSets = new ArrayList<List<Integer>>();
	    itemSets = new ArrayList<Itemset>();
	}
	public void addConstraints(List<Constraint> _constraints) {}
	
	public void runAlgorithm(double minsupp) {
		startTimestamp = System.currentTimeMillis();
		this.relativeMinsup = (int) Math.ceil(minsupp*dataset.size());
		System.out.println("sup:"+relativeMinsup);

		// STEP 1:
		final Map<Integer, Integer> mapSupport = new HashMap<Integer, Integer>();
		List<Itemset> itemsets = new ArrayList<Itemset>();

		for(List<Integer> dataI : dataset){ //frequency of each item
			for(Integer item : dataI)
				if(!mapSupport.containsKey(item)) mapSupport.put(item,1);
				else mapSupport.put(item, mapSupport.get(item)+1);
		}
		for(List<Integer> dataI : dataset){ //remove infrequent items
			Itemset itemset = new Itemset();
			for(Integer item : dataI) 
				if(mapSupport.get(item) >= relativeMinsup) itemset.addItem(item);
			itemsets.add(itemset);
		}
		
		// (3) PREPROCESSING: Sort items in each transaction in a descending order according to their frequency in the database.
		for(Itemset itemset : itemsets){
			Collections.sort(itemset.getItems(), new Comparator<Integer>(){
				public int compare(Integer item1, Integer item2){
					int compare = mapSupport.get(item2) - mapSupport.get(item1);
					if(compare ==0) return (item1 - item2); //lexical ordering if equal
					return compare;
				}
			});
		}
	
		// (4) Build the initial FP-TREE
		FPTree tree = new FPTree();
		for(int i=0, l=itemsets.size(); i<l; i++) tree.addTransaction(itemsets.get(i),i);
		tree.createHeaderList(mapSupport); // We create the header table for the tree
		
		// (5) We start to mine the FP-Tree by calling the recursive method.
		Itemset prefixAlpha = new Itemset(); // Initially, the prefix alpha is empty.
		prefixAlpha.setTransactioncount(dataset.size());		
		fpgrowth(tree, prefixAlpha, mapSupport, 0);
		
		mem = Math.max(mem,Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory());
		endTime= System.currentTimeMillis();
		//return frequentItemsets; 
	}

	private List<Itemset> alphaReduction(List<Itemset> itemsets, Map<Integer, Integer> mapSupport) {
		List<Itemset> newitemsets = new ArrayList<Itemset>();
		for(Itemset itemset : itemsets){
			Itemset newitemset = new Itemset();
			for(Integer item : itemset.getItems()) 
				if(mapSupport.get(item) >= relativeMinsup) itemset.addItem(item);
			newitemsets.add(newitemset);
		}
		return newitemsets;
	}
	
	/**
	 * This method mines pattern from a Prefix-Tree recursively
	 * @param tree  The Prefix Tree
	 * @param prefix  The current prefix "alpha"
	 * @param mapSupport The frequency of each item in the prefix tree.
	 */
	private void fpgrowth(FPTree tree, Itemset prefixAlpha, Map<Integer, Integer> mapSupport, int counter) {
		// FP-BONSAI optimization: // pruning(tree, prefixAlpha.getAbsoluteSupport(), mapSupport);
	    //System.out.println("INDEX:"+tree.headerList.toString());
	    //System.out.println(tree.toString()+"\n");
		
		if(tree.headerList.size() == 1){// if there is a single path in the prefix tree or not.
			FPNodeTID node = tree.mapItemNodes.get(tree.headerList.get(0));
			if(node.nodeLink == null) // single path: add all combinations of this path concatenated with the prefix "alpha"
				addAllCombinationsForPathAndPrefix(node, prefixAlpha); 
			else fpgrowthMoreThanOnePath(tree, prefixAlpha, mapSupport,counter); 
		} else fpgrowthMoreThanOnePath(tree, prefixAlpha, mapSupport,counter);
		mem = Math.max(mem,Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory());
	}
	
	/**
	 * Mine an FP-Tree having more than one path.
	 * @param tree  the FP-tree
	 * @param prefix  the current prefix, named "alpha"
	 * @param mapSupport the frequency of items in the FP-Tree
	 */
	private void fpgrowthMoreThanOnePath(FPTree tree, Itemset prefixAlpha, Map<Integer, Integer> mapSupport, int counter) {
		// process each frequent item in the header in reverse order
		for(int i= tree.headerList.size()-1; i>=0; i--){
			Integer item = tree.headerList.get(i);
			int support = mapSupport.get(item);
			//System.out.println("::("+counter+")"+item);
			if(support < relativeMinsup){
				FPNodeTID node = tree.mapItemNodes.get(item);
				while(node!=null){
					node.parent.transactions.addAll(node.transactions);
					node.transactions = null;
					node = node.nodeLink;
				}
				continue;
			}

			// create Beta by concatening Alpha with the current item
			Itemset beta = prefixAlpha.cloneItemset();
			beta.addItem(item);
			if(prefixAlpha.getAbsoluteSupport() < support){
				beta.setTransactioncount(prefixAlpha.getAbsoluteSupport());
			} else beta.setTransactioncount(support);
			sortItemset(beta);
			frequentItemsets.addItemset(beta, beta.size());
			FPNodeTID no = tree.mapItemNodes.get(item);
			List<Integer> trans = new ArrayList<Integer>();
			while(no!=null){
				no.parent.transactions.addAll(no.transactions);
				trans.addAll(no.transactions);
				no = no.nodeLink;
			}
			itemSets.add(beta);
			transSets.add(trans);
			//System.out.println(">"+beta.toString()+trans.toString());
			
			// Construct beta's conditional pattern base (set of prefix paths in the FP-tree co-occuring with the suffix pattern)
			List<List<FPNodeTID>> prefixPaths = new ArrayList<List<FPNodeTID>>();
			FPNodeTID path = tree.mapItemNodes.get(item);
			while(path != null){
				if(path.parent.itemID != -1){ // if the path is not just the root node
					List<FPNodeTID> prefixPath = new ArrayList<FPNodeTID>();
					prefixPath.add(path);   // we add it just to keep its support
					FPNodeTID parent = path.parent; // recursively add all the parents of this node.
					while(parent.itemID != -1){
						prefixPath.add(parent);
						parent = parent.parent;
					}
					prefixPaths.add(prefixPath);
				}
				path = path.nodeLink;
			}
			
			// Calculate the frequency of each item in the prefixpath
			Map<Integer, Integer> mapSupportBeta = new HashMap<Integer, Integer>();
			for(List<FPNodeTID> prefixPath : prefixPaths){
				int pathCount = prefixPath.get(0).counter; // support is the support of its first node.  
				for(int j=1; j<prefixPath.size(); j++){  // for each node, except the first one, we count the frequency
					FPNodeTID node = prefixPath.get(j);
					if(mapSupportBeta.get(node.itemID) == null) mapSupportBeta.put(node.itemID, pathCount);
					else mapSupportBeta.put(node.itemID, mapSupportBeta.get(node.itemID) + pathCount);
				}
			}
			
			// Construct beta's conditional FP-Tree
			FPTree treeBeta = new FPTree();
			for(List<FPNodeTID> prefixPath : prefixPaths)
				treeBeta.addPrefixPath(prefixPath, mapSupportBeta, relativeMinsup); 
			treeBeta.createHeaderList(mapSupportBeta,tree.headerList); 
			
			if(treeBeta.root.childs.size() > 0) fpgrowth(treeBeta, beta, mapSupportBeta,counter+1);
		}
		
	}

	/**
	 * Adds recursively all combinations of nodes in a path, concatenated with a given prefix
	 * @param nodeLink the first node of the path
	 * @param prefix  the prefix
	 * @param minsupportForNode the support of this path.
	 */
	private void addAllCombinationsForPathAndPrefix(FPNodeTID node, Itemset prefix) {
		Itemset itemset = prefix.cloneItemset(); // We add the node to the prefix
		itemset.addItem(node.itemID);
		itemset.setTransactioncount(node.counter); 
		sortItemset(itemset);
		frequentItemsets.addItemset(itemset, itemset.size());
		itemSets.add(itemset);
		transSets.add(node.transactions);
		//System.out.println("==("+node.itemID+")\n");
		//System.out.println(">"+itemset.toString()+node.transactions.toString());
		
		if(node.nodeLink != null){// recursive call if there is a node link
			addAllCombinationsForPathAndPrefix(node.nodeLink, prefix);
			addAllCombinationsForPathAndPrefix(node.nodeLink, itemset);
		}
		//System.out.println("==("+node.itemID+")");
	}
	
	public void sortItemset(Itemset itemset){
		Collections.sort(itemset.getItems(), new Comparator<Integer>() {
			public int compare(Integer o1,Integer o2) {
				return o1 - o2;
			}
		});
	}

	public void printStats() {
		System.out.println("=============  FP-GROWTH - STATS =============");
		System.out.println(" Transactions count from database : " + dataset.size());
		System.out.println(" Frequent itemsets count : " + frequentItemsets.getItemsetsCount()); 
		System.out.println(" Total time ~ " + (endTime - startTimestamp) + " ms");
		System.out.println("===================================================");
	}
	
	public Itemsets getItemsets() {
		return frequentItemsets;
	}
}
