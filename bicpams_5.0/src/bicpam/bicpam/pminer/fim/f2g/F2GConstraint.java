package bicpam.pminer.fim.f2g;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import domain.constraint.AnnotationConstraint;
import domain.constraint.AntiMonotoneConstraint;
import domain.constraint.Constraint;
import domain.constraint.Constraint.SuccinctOperation;
import domain.constraint.Constraint.ValueType;
import domain.constraint.Constraints;
import domain.constraint.MonotoneConstraint;
import domain.constraint.SuccinctConstraint;

/**
 * Copyright (c) 2014 Rui Henriques
 */
public class F2GConstraint {

	public int minsup;
	protected List<List<Integer>> dataset;
	protected List<List<Integer>> itemSets; 
	protected List<List<Integer>> transSets; 	
	protected Constraints constraints;
	protected boolean constrained = true;
	protected long mem = 0;

	
	/********************/
	/*** CORE METHODS ***/
	/********************/
	
	public void runAlgorithm(double minsupp) {

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
		
		//System.out.println("before:\n"+itemsets+"\n"+mapSup);
		exante(itemsets,mapSup);
		//System.out.println("\nafter:\n"+itemsets+"\n"+mapSup);
		
		/** C: sort items per transaction in descending freq order **/
		for(List<Integer> itemset : itemsets){
			Collections.sort(itemset, new Comparator<Integer>(){
				public int compare(Integer item1, Integer item2){
					//int compare = mapSup.get(item2) - mapSup.get(item1);
					int compare = item1%constraints.nrLabels - item2%constraints.nrLabels; 
					if(compare==0) return item1-item2;
					else return compare;
				}
			});
		}
	
		/** D: Build the initial FP-TREE **/
		F2KTree tree = new F2KTree();
		for(int i=0, l=itemsets.size(); i<l; i++) tree.addTransaction(itemsets.get(i),i);
		tree.createHeaderList(mapSup,constraints.nrLabels);
		
		/** E: mine FP-Tree **/
		List<Integer> prefixAlpha = new ArrayList<Integer>(); // Initially, the prefix alpha is empty.
		fpgrowth(tree, prefixAlpha, dataset.size(), mapSup, 0);
		
		mem = Math.max(mem,Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory());
		time= System.currentTimeMillis()-time;
	}
	
	/** This method mines pattern from a Prefix-Tree recursively **/
	private void fpgrowth(F2KTree tree, List<Integer> prefixAlpha, int prefixAlphaSup, Map<Integer, Integer> mapSup, int counter) {

		if(tree.headerList.size() == 1){ //single path in the prefix tree 
			F2GNodeTID node = tree.mapItemNodes.get(tree.headerList.get(0));
			if(node.nodeLink == null) addAllCombinationsForPathAndPrefix(node, prefixAlpha);
			return;
		}
		fpgrowthMoreThanOnePath(tree, prefixAlpha, prefixAlphaSup, mapSup,counter);
	}
	
	private void fpgrowthMoreThanOnePath(F2KTree tree, List<Integer> prefixAlpha, int prefixAlphaSup, Map<Integer, Integer> mapSup, int counter) {

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
			//System.out.println("B>>"+beta+" <> "+trans);
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
				//if(mapSupBeta.get(prefixPath.get(0).itemID) == null) mapSupBeta.put(prefixPath.get(0).itemID, pathCount);
				//else mapSupBeta.put(prefixPath.get(0).itemID, mapSupBeta.get(prefixPath.get(0).itemID) + pathCount);
				for(int j=0; j<prefixPath.size(); j++){  //freq of other nodes
					F2GNodeTID node = prefixPath.get(j);
					if(mapSupBeta.get(node.itemID) == null) mapSupBeta.put(node.itemID, pathCount);
					else mapSupBeta.put(node.itemID, mapSupBeta.get(node.itemID) + pathCount);
				}
			}
			//System.out.println(mapSupBeta);
			exanteFP(prefixPaths,prefixAlpha,mapSupBeta);
			
			/** E: Construct beta's conditional FP-Tree **/
			F2KTree treeBeta = new F2KTree();
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
		//System.out.println("A>>"+itemset+" <> "+node.transactions);
		itemSets.add(itemset);
		transSets.add(node.transactions);
		
		/** B: Recursive mining **/
		if(node.nodeLink != null){
			addAllCombinationsForPathAndPrefix(node.nodeLink, prefix);
			addAllCombinationsForPathAndPrefix(node.nodeLink, itemset);
		}
	}


	/***********************/
	/***** CONSTRAINTS *****/
	/***********************/

	
	private void exante(List<List<Integer>> itemsets, Map<Integer, Integer> mapSup) {
		if(!constrained) return;
		for(Constraint constraint : constraints.constraints){
			if(constraint instanceof AnnotationConstraint){
				annotationPruning(constraint,itemsets,mapSup);
			}
		}
		//succinctPruning(itemsets);
		//monotonePruning(itemsets,mapSup);
	}

	private void exanteFP(List<List<F2GNodeTID>> paths, List<Integer> prefix, Map<Integer, Integer> mapSupBeta) {
		if(!constrained) return;
		for(Constraint constraint : constraints.constraints){
			if(constraint instanceof AnnotationConstraint){
				annotationPruning(constraint,paths,prefix,mapSupBeta);
			}
		}
		//reduceSuccinct(paths,prefix);
		//reduceAntiMonotone(paths,prefix,mapSupBeta);
		//reduceMonotone(paths,prefix,mapSupBeta);
	}
	
	private void annotationPruning(Constraint c, List<List<F2GNodeTID>> paths, List<Integer> prefix, Map<Integer, Integer> mapSupBeta) {
		for(int i=paths.size()-1; i>=0; i--){
			List<Integer> itemset = new ArrayList<Integer>();
			for(F2GNodeTID node : paths.get(i)) itemset.add(node.itemID);
			if(!((AnnotationConstraint)c).satisfy(itemset, constraints.nrLabels)){
				/** CHECK REMOVALS OF PATHS IN A TREE */
				//TODO push up transactions
				//for(Integer item : itemset) mapSupBeta.put(item, mapSupBeta.get(item)-1);
				//paths.remove(i);
			}
		}
	}

	private void annotationPruning(Constraint constraint, List<List<Integer>> itemsets, Map<Integer, Integer> mapSup) {
		for(List<Integer> row : itemsets){
			if(!((AnnotationConstraint)constraint).satisfy(row, constraints.nrLabels)){
				//System.out.println("REMOVING:"+row);
				for(Integer i : row) mapSup.put(i, mapSup.get(i)-1);
				row = new ArrayList<Integer>();
			}
		}
	}

	private void succinctPruning(List<List<Integer>> itemsets) {
		for(Constraint constraint : constraints.constraints){
			if(constraint instanceof SuccinctConstraint){
				for(List<Integer> row : dataset)
					row = ((SuccinctConstraint)constraint).satisfy(row, constraints.nrLabels);
			}
		}
	}
	//monotone: sum/range/count(P)>v if false remove
	private void monotonePruning(List<List<Integer>> itemsets, Map<Integer, Integer> mapSup) {
		List<Integer> prefix = new ArrayList<Integer>();
		//System.out.println("ExANT-A with Monotone");//+itemsets
		while(true){
			int size=itemsets.size();
			for(int i=size-1; i>=0; i--)
				if(prune(itemsets.get(i),prefix,mapSup)){
					//System.out.println("Prune!");
					itemsets.set(i,new ArrayList<Integer>());
				}
			if(itemsets.size()==size) break;
			itemsets = alphaReduction(itemsets,mapSup);
			//System.out.println("New iteration!");
		}
		//System.out.println("Final:"+itemsets);
	} 


	private void reduceSuccinct(List<List<F2GNodeTID>> paths, List<Integer> prefix) {
		for(Constraint c : constraints.constraints){
			if(c instanceof SuccinctConstraint){
				if(((SuccinctConstraint) c).operator.equals(SuccinctOperation.Includes)){
					List<Integer> remainingValues = ((SuccinctConstraint) c).remaining(prefix, constraints.nrLabels);
					if(remainingValues.size()==0) return;
					else {
						for(int i=paths.size()-1; i>=0; i--){
							List<Integer> itemset = new ArrayList<Integer>();
							for(F2GNodeTID node : paths.get(i)) itemset.add(node.itemID);
							if(!((SuccinctConstraint) c).contains(itemset, remainingValues, constraints.nrLabels)){
								//System.out.println("Remove:"+prefix+">>"+itemset);
								paths.remove(i);
							}
						}						
					}
				}
			}
		}
	}
	private void reduceMonotone(List<List<F2GNodeTID>> paths, List<Integer> prefix, Map<Integer, Integer> mapSup) {
		//System.out.println("ExANT-B with Monotone with prefix:"+prefix+"\nPaths:"+paths);
		while(true){
			//muReduction: monotone: sum/range/count(P)>v if false remove
			int size=paths.size();
			//System.out.println("Paths size="+size+" => "+paths);
			for(int i=size-1; i>=0; i--){
				List<Integer> itemset = new ArrayList<Integer>();
				for(F2GNodeTID node : paths.get(i)) itemset.add(node.itemID);
				if(prune(itemset,prefix,mapSup)) paths.remove(i); //TODO push up transactions
				//else System.out.println(itemset+"|"+prefix+" YES");
			}
			if(paths.size()==size) break;
			paths = alphaReductionFP(paths,mapSup);
		}
	}
	//anti-monotone sum/range/count(P)>v if false remove
	private void reduceAntiMonotone(List<List<F2GNodeTID>> paths, List<Integer> prefix, Map<Integer, Integer> mapSup) {
		for(int i=paths.size()-1; i>=0; i--){
			List<Integer> itemset = new ArrayList<Integer>();
			for(F2GNodeTID node : paths.get(i)) itemset.add(node.itemID);
			for(Constraint c : constraints.constraints){
				if(c instanceof AntiMonotoneConstraint){
					List<Integer> removals = ((AntiMonotoneConstraint)c).satisfy(itemset,prefix,constraints.nrLabels,mapSup);
					for(Integer removal : removals){
						//F2GNodeTID node = paths.get(i).get(removal);
						//node.parent.transactions = node.transactions;
						paths.get(i).remove((int)removal); //TODO push up transactions
					}
					if(paths.get(i).size()==0) paths.remove(i);
					//System.out.println("Paths:"+paths);
				}
			}
		}
	}
	private boolean prune(List<Integer> itemset, List<Integer> prefix, Map<Integer, Integer> mapSupp) {
		for(Constraint c : constraints.constraints){
			if(c instanceof MonotoneConstraint){
				if(!((MonotoneConstraint)c).satisfy(itemset,prefix,constraints.nrLabels)){
					for(Integer item : itemset) mapSupp.put(item, mapSupp.get(item)-1);
					return true;
				}
			}
		}
		return false;
	}
	protected List<List<Integer>> alphaReduction(List<List<Integer>> itemsets, Map<Integer, Integer> mapSup) {
		List<List<Integer>> newitemsets = new ArrayList<List<Integer>>();
		for(List<Integer> itemset : itemsets){
			List<Integer> newitemset = new ArrayList<Integer>();
			for(Integer i : itemset) if(mapSup.get(i)>=minsup) newitemset.add(i);
			newitemsets.add(newitemset);
		}
		return newitemsets;
	}
	protected List<List<F2GNodeTID>> alphaReductionFP(List<List<F2GNodeTID>> itemsets, Map<Integer, Integer> mapSup) {
		List<List<F2GNodeTID>> newitemsets = new ArrayList<List<F2GNodeTID>>();
		for(List<F2GNodeTID> itemset : itemsets){
			List<F2GNodeTID> newitemset = new ArrayList<F2GNodeTID>();
			for(F2GNodeTID i : itemset){ 
				if(mapSup.get(i.itemID)>=minsup) newitemset.add(i);
			}
			newitemsets.add(newitemset);
		}
		return newitemsets;
	}
	
	/*************************/
	/*** AUXILIARY METHODS ***/
	/*************************/
	
	public F2GConstraint() {}
	public F2GConstraint(List<List<Integer>> _dataset){ setData(_dataset); }
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
