package bicpam.pminer.fim.f2g;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import domain.constraint.Constraints;

/**
 * Copyright (c) 2014 Rui Henriques
 * Based on SPMF implementation (Philippe Fournier-Viger)
 */
public class F2GConstraint2 {

	public int minsup;
	protected List<List<Integer>> dataset;
	protected List<List<Integer>> itemSets; 
	protected List<List<Integer>> transSets; 	
	protected Constraints constraints;
	protected long mem = 0;

	
	/********************/
	/*** CORE METHODS ***/
	/********************/
	
	public void runAlgorithm(double minsupp) {
		
		System.out.println(dataset.get(0));
		System.out.println(dataset.get(1));
		
		long time = System.currentTimeMillis();
		minsup = (int) Math.ceil(minsupp*dataset.size());
		System.out.println("sup:"+minsup);

		/** A: initial pruned database **/
		final Map<Integer,Integer> mapSup = new HashMap<Integer,Integer>();
		for(List<Integer> dataI : dataset){ //frequency of each item
			for(Integer item : dataI)
				if(!mapSup.containsKey(item)) mapSup.put(item,1);
				else mapSup.put(item, mapSup.get(item)+1);
		}
		List<List<Integer>> itemsets = new ArrayList<List<Integer>>();
		for(List<Integer> dataI : dataset){ //remove infrequent items
			List<Integer> itemset = new ArrayList<Integer>();
			for(Integer i : dataI) if(mapSup.get(i)>=minsup) itemset.add(i);
			itemsets.add(itemset);
		}
		
		/** C: sort items per transaction in descending freq order **/
		for(List<Integer> itemset : itemsets){
			Collections.sort(itemset, new Comparator<Integer>(){
				public int compare(Integer item1, Integer item2){
					int compare = mapSup.get(item2) - mapSup.get(item1);
					if(compare ==0) return (item1 - item2); //lexical ordering if equal
					return compare;
				}
			});
		}
	
		/** D: Build the initial FP-TREE **/
		F2GTree tree = new F2GTree();
		for(int i=0, l=itemsets.size(); i<l; i++) tree.addTransaction(itemsets.get(i),i);
		tree.createHeaderList(mapSup);
		
		/** E: mine FP-Tree **/
		List<Integer> prefixAlpha = new ArrayList<Integer>(); // Initially, the prefix alpha is empty.
		fpgrowth(tree, prefixAlpha, dataset.size(), mapSup, 0);
		
		mem = Math.max(mem,Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory());
		time= System.currentTimeMillis()-time;
	}
	
	/** This method mines pattern from a Prefix-Tree recursively **/
	private void fpgrowth(F2GTree tree, List<Integer> prefixAlpha, int prefixAlphaSup, Map<Integer, Integer> mapSup, int counter) {

		if(tree.headerList.size() == 1){ //single path in the prefix tree 
			F2GNodeTID node = tree.mapItemNodes.get(tree.headerList.get(0));
			if(node.nodeLink == null) addAllCombinationsForPathAndPrefix(node, prefixAlpha);
			return;
		}
		fpgrowthMoreThanOnePath(tree, prefixAlpha, prefixAlphaSup, mapSup,counter);
	}
	
	private void fpgrowthMoreThanOnePath(F2GTree tree, List<Integer> prefixAlpha, int prefixAlphaSup, Map<Integer, Integer> mapSup, int counter) {

		for(int i= tree.headerList.size()-1; i>=0; i--){//items in header reverse order
			Integer item = tree.headerList.get(i);

			/** A: infrequent so push up transactions **/
			int support = mapSup.get(item);
			if(support < minsup){
				F2GNodeTID node = tree.mapItemNodes.get(item);
				while(node!=null){
					node.parent.transactions.addAll(node.transactions);
					node.transactions = null;
					node = node.nodeLink;
				}
				continue;
			}

			/** B: add beta = concat alpha with current item **/
			List<Integer> beta = new ArrayList<Integer>();
			beta.addAll(prefixAlpha);
			beta.add(item);
			int betaSup = (prefixAlphaSup < support) ? prefixAlphaSup : support;
			Collections.sort(beta);
			F2GNodeTID no = tree.mapItemNodes.get(item);
			List<Integer> trans = new ArrayList<Integer>();
			while(no!=null){
				no.parent.transactions.addAll(no.transactions);
				trans.addAll(no.transactions);
				no = no.nodeLink;
			}
			itemSets.add(beta);
			transSets.add(trans);
			//System.out.println(">"+beta.toString()+trans.toString());
			
			/** C: build beta conditional pattern base **/
			List<List<F2GNodeTID>> prefixPaths = new ArrayList<List<F2GNodeTID>>();
			F2GNodeTID path = tree.mapItemNodes.get(item);
			while(path != null){
				if(path.parent.itemID != -1){ // if the path is not just the root node
					List<F2GNodeTID> prefixPath = new ArrayList<F2GNodeTID>();
					prefixPath.add(path);   // we add it just to keep its support
					F2GNodeTID parent = path.parent; // recursively add all the parents of this node.
					while(parent.itemID != -1){
						prefixPath.add(parent);
						parent = parent.parent;
					}
					prefixPaths.add(prefixPath);
				}
				path = path.nodeLink;
			}
			
			/** D: compute frequency of each item in the prefixpath **/
			Map<Integer, Integer> mapSupBeta = new HashMap<Integer, Integer>();
			for(List<F2GNodeTID> prefixPath : prefixPaths){
				int pathCount = prefixPath.get(0).counter; //sup is the sup of first node  
				for(int j=1; j<prefixPath.size(); j++){  //freq of other nodes
					F2GNodeTID node = prefixPath.get(j);
					if(mapSupBeta.get(node.itemID) == null) mapSupBeta.put(node.itemID, pathCount);
					else mapSupBeta.put(node.itemID, mapSupBeta.get(node.itemID) + pathCount);
				}
			}
			
			/** E: Construct beta's conditional FP-Tree **/
			F2GTree treeBeta = new F2GTree();
			for(List<F2GNodeTID> prefixPath : prefixPaths)
				treeBeta.addPrefixPath(prefixPath, mapSupBeta, minsup); 
			treeBeta.createHeaderList(mapSupBeta,tree.headerList);
			
			/** F: Mine beta-tree **/
			if(treeBeta.root.childs.size()>0) fpgrowth(treeBeta, beta, betaSup, mapSupBeta,counter+1);
		}
	}

	/** Adds recursively all combinations of nodes in a path, concatenated with a given prefix **/
	private void addAllCombinationsForPathAndPrefix(F2GNodeTID node, List<Integer> prefix) {
		
		/** A: Add node to prefix **/
		List<Integer> itemset = new ArrayList<Integer>();
		itemset.addAll(prefix); // We add the node to the prefix
		itemset.add(node.itemID);
		Collections.sort(itemset);
		itemSets.add(itemset);
		transSets.add(node.transactions);
		
		/** B: Recursive mining **/
		if(node.nodeLink != null){
			addAllCombinationsForPathAndPrefix(node.nodeLink, prefix);
			addAllCombinationsForPathAndPrefix(node.nodeLink, itemset);
		}
	}


	/*************************/
	/*** AUXILIARY METHODS ***/
	/*************************/
	
	public F2GConstraint2() {}
	public F2GConstraint2(List<List<Integer>> _dataset){ setData(_dataset); }
	public void addConstraints(Constraints _constraints) { constraints = _constraints;	}

	public void setData(List<List<Integer>> dataI) {
		dataset = dataI;
		reset();
	}
	public void reset() {
		itemSets = new ArrayList<List<Integer>>();
	    transSets = new ArrayList<List<Integer>>();
	}
}
