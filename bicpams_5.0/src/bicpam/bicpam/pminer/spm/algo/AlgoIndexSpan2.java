package bicpam.pminer.spm.algo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import utils.BicPrinting;

/**
 * Copyright (c) 2014 Rui Henriques
 * Based on SPMF implementation (Philippe Fournier-Viger)
 */
public class AlgoIndexSpan2{
	
	private Sequences patterns = new Sequences("");
	private long time;
	private int minsup;
	private int minColumns;
	protected SequenceDatabase database;
	protected double mergingOverlap = -1;

	
	/***************************
	 ****** MAIN METHODS *******
	 ***************************/

	public void setDataset(List<List<Integer>> dataset, int overlap) {
		database = new SequenceDatabase(dataset);
		//System.out.println(database.toString());
	}
	public Sequences runAlgorithm(int minSupRelative, int minCols) {
		minColumns = minCols;
		minsup = minSupRelative;
		System.out.println("Support:"+minSupRelative);
		time = System.currentTimeMillis();
		prefixSpan();
		time = time-System.currentTimeMillis();
		return patterns;
	}
	public Sequences runAlgorithm(double minsupPercent) {
		return runAlgorithm((int) Math.ceil(minsupPercent*database.size()),-1);
	}
	
	private void prefixSpan(){
		int[] upIndexes = new int[database.size()];
		for(int i=0, l=database.size(); i<l; i++) upIndexes[i]=-1;

		/** A: Get seq IDs and items **/
		Set<Integer> upSeqsID = database.getSequenceIDs();
		Set<Integer> items = new HashSet<Integer>();
		for(Itemset itemset : database.getSequences().get(0).getItemsets())
			items.addAll(itemset.getItems());
		Map<Integer, Set<Integer>> mapSequenceID = findSequencesContainingItems(database);
		
		/** B: Grow each item (1-prefix) **/
		for(Entry<Integer, Set<Integer>> entry : mapSequenceID.entrySet()){
			Integer item = entry.getKey();
			Sequence prefix = new Sequence(0);  
			prefix.addItemset(new Itemset(item));
			prefix.setSequencesID(database.getSequenceIDs());
			
			items.remove(item);
			OptProjDb projectedDb = buildProjectedContext(upIndexes.clone(),upSeqsID,-1,item,1);
			recursion(prefix, 2, projectedDb.indexes, projectedDb.seqsID, items, item);
			items.add(item);
		}		
	}

	
	/************************************
	 ****** PROJECTION AND GROWTH *******
	 ************************************/

	private void recursion(Sequence prefix, int k, int[] upIndexes, Set<Integer> upSeqsID, Set<Integer> items, int upitem) {
		
		/** B: Grow each item (1-prefix) **/
		Set<Pair> pairs = new HashSet<Pair>();
		for(Integer item : items){
			Pair p1 = new Pair(true,item);
			Pair p2 = new Pair(false,item);
			for(Integer seqID : upSeqsID){
				Sequence seq = database.getSequences().get(seqID);
				if(!seq.indexes.containsKey(item) || !seq.indexes.containsKey(upitem)) continue;//
				if(seq.indexes.get(item)>seq.indexes.get(upitem)){
					if(seq.left.get(item)>=minColumns-k)
						p2.getSequencesID().add(seqID);
				}
				else if(item>upitem && seq.indexes.get(item)==seq.indexes.get(upitem)){
					if(seq.left.get(item)>=minColumns-k) 
						p1.getSequencesID().add(seqID);
				}
			}
			if(p1.getCount() >= minsup) pairs.add(p1);
			if(p2.getCount() >= minsup) pairs.add(p2);
		}

		/** B: Grow each item (1-prefix) **/
		boolean merged = false;
		int index=-1;
		if(prefix.size()>=minColumns) index=patterns.getLevel(prefix.size()).indexOf(prefix);
		
		for(Pair paire : pairs){
			Sequence newPrefix;
			if(paire.isPostfix()) newPrefix = appendItemToPrefixOfSequence(prefix, paire.getItem()); // is =<is, (deltaT,i)>
			else newPrefix = appendItemToSequence(prefix, paire.getItem());
			
			newPrefix.setSequencesID(paire.getSequencesID());
			//System.out.println("Pattern::"+newPrefix);
			
			if(k>=minColumns){
				if(mergingOverlap==-1 || k==minColumns || index==-1) patterns.addSequence(newPrefix, newPrefix.size());
				else {
					System.out.println(">"+prefix.size()+","+newPrefix.getAbsoluteSupport()+","+k+","+prefix.getAbsoluteSupport());
					double area = ((double)(prefix.size()*newPrefix.getAbsoluteSupport()))/((double)(k*prefix.getAbsoluteSupport()));
					if(area<mergingOverlap) patterns.addSequence(newPrefix, newPrefix.size());
					else {
						Sequence mergedPredix = newPrefix.cloneSequence();
						mergedPredix.setSequencesID(prefix.getSequencesID());
						patterns.addSequence(mergedPredix, mergedPredix.size());
						merged = true;
					}
				}
			}
			
			items.remove(paire.getItem());
			//System.out.println("\nK:"+k+" ITEM:"+currentItem+items);
			OptProjDb projectedDb = buildProjectedContext(upIndexes.clone(),paire.getSequencesID(),upitem,paire.getItem(),k);
			//projprint(projectedDb);
			recursion(newPrefix, k+1, projectedDb.indexes, projectedDb.seqsID ,items, paire.getItem());
			items.add(paire.getItem());
		}
		if(merged) patterns.getLevel(prefix.size()).remove(index);
	}
	
	/** Create a projected database by pseudo-projection */
	private OptProjDb buildProjectedContext(int[] upIndexes, Set<Integer> upSeqsID, int upItem, int currentItem, int nrItems) {
		
		/** A: Checks if prefix preceeds new item **/
		Set<Integer> newSeqs = new HashSet<Integer>();
		for(Integer id : upSeqsID){
			Sequence sequence = database.getSequences().get(id);
			int index = sequence.indexes.containsKey(currentItem) ? sequence.indexes.get(currentItem) : -10;  
			if(index > upIndexes[id]){
				//System.out.print("a");
				upIndexes[id]=index;
				if(sequence.left.get(currentItem)>=minColumns-nrItems) 
					newSeqs.add(id);
			} else if(currentItem>upItem && index==upIndexes[id]){
				//System.out.print("b["+currentItem+","+upItem+"]");
				if(sequence.left.get(currentItem)>=minColumns-nrItems) 
					newSeqs.add(id);
			} else{
				//System.out.print("c");
				upIndexes[id]=1000;
			}
		}
		//System.out.println();
		return new OptProjDb(upIndexes,newSeqs);
	}
	
	// This method takes as parameters : a sequence, an item, and the item support.
	// It creates a copy of the sequence and add the item to the sequence. It sets the 
	// support of the sequence as the support of the item.
	private Sequence appendItemToSequence(Sequence prefix, Integer item) {
		Sequence newPrefix = prefix.cloneSequence();  // isSuffix
		newPrefix.addItemset(new Itemset(item));  // cr un nouvel itemset   + decalage
		return newPrefix;
	}
	
	// This method takes as parameters : a sequence, an item, and the item support.
	// It creates a copy of the sequence and add the item to the last itemset of the sequence. 
	// It sets the support of the sequence as the support of the item.
	private Sequence appendItemToPrefixOfSequence(Sequence prefix, Integer item) {
		Sequence newPrefix = prefix.cloneSequence();
		Itemset itemset = newPrefix.get(newPrefix.size()-1);  // ajoute au dernier itemset
		itemset.addItem(item);   
		return newPrefix;
	}

	
	/***************************************
	 ***************** OTHERS **************
	 ***************************************/

	private Map<Integer, Set<Integer>> findSequencesContainingItems(SequenceDatabase contexte) {
		Set<Integer> alreadyCounted = new HashSet<Integer>(); // il faut compter un item qu'une fois par s�quence.
		Sequence lastSequence = null;
		Map<Integer, Set<Integer>> mapSequenceID = new HashMap<Integer, Set<Integer>>(); // pour conserver les ID des s�quences: <Id Item, Set d'id de s�quences>
		for(Sequence sequence : contexte.getSequences()){
			if(lastSequence == null || lastSequence.getId() != sequence.getId()){ // FIX
				alreadyCounted.clear(); 
				lastSequence = sequence;
			}
			for(Itemset itemset : sequence.getItemsets()){
				for(Integer item : itemset.getItems()){
					if(!alreadyCounted.contains(item)){
						Set<Integer> sequenceIDs = mapSequenceID.get(item);
						if(sequenceIDs == null){
							sequenceIDs = new HashSet<Integer>();
							mapSequenceID.put(item, sequenceIDs);
						}
						sequenceIDs.add(sequence.getId());
						alreadyCounted.add(item); 
					}
				}
			}
		}
		return mapSequenceID;
	}

	public void reset() { patterns = new Sequences(""); }
	
	public void projprint(OptProjDb projectedDb){
		for(int i=0; i<projectedDb.indexes.length; i++) System.out.print(projectedDb.indexes[i]+",");
		System.out.println(" "+projectedDb.seqsID);
	}
	public void printStatistics(int size) {
		System.out.println("Statistics:\n Total time ~ "+time+"ms\n"+patterns.toString());
	}
}
