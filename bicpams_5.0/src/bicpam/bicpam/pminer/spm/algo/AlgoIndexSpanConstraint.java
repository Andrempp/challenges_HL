package bicpam.pminer.spm.algo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import utils.BicPrinting;
import domain.constraint.Constraint;
import domain.constraint.Constraints;
import domain.constraint.SequentialConstraint;

/**
 * Copyright (c) 2014 Rui Henriques
 * Based on SPMF implementation (Philippe Fournier-Viger)
 */
public class AlgoIndexSpanConstraint{
	
	private Sequences patterns = new Sequences("");
	private long time;
	private int minsup;
	private int minColumns;
	protected SequenceDatabase database;
	protected double mergingOverlap = -1;
	protected Constraints constraints;
	protected boolean constrained = true;

	
	/***************************
	 ****** MAIN METHODS *******
	 ***************************/

	public void setDataset(List<List<Integer>> dataset, int overlap) {
		database = new SequenceDatabase(dataset);
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
	
	private void prefixSpan(){
		int[] upIndexes = new int[database.size()];
		database.seqIDs = new HashSet<Integer>();
		for(int i=0, l=database.size(); i<l; i++) upIndexes[i]=-1;
		for(int i=0, l=database.size(); i<l; i++) database.seqIDs.add(i);
		
		System.out.println("Before pruning:"+database.seqIDs.size());
		prepruning();
		System.out.println("After pruning:"+database.seqIDs.size());

		/** A: Get seq IDs and items **/
		Set<Integer> upSeqsID = database.getSequenceIDs();
		//System.out.println(">>"+upSeqsID);
		Map<Integer, Set<Integer>> mapSequenceID = findSequencesContainingItems(database);
		Set<Integer> items = new HashSet<Integer>();
		for(Entry<Integer, Set<Integer>> entry : mapSequenceID.entrySet())
			if(entry.getValue().size()>=minsup) items.add(entry.getKey());

		//System.out.println(database.toString());
		
		/** B: Grow each item (1-prefix) **/
		for(Entry<Integer, Set<Integer>> entry : mapSequenceID.entrySet()){
			if(entry.getValue().size()<minsup) continue; 
			Integer item = entry.getKey();
			Sequence prefix = new Sequence(0);  
			prefix.addItemset(new Itemset(item));			
			prefix.indexes.put(item,0);
			prefix.setSequencesID(database.getSequenceIDs());
			patterns.addSequence(prefix, 1);
			
			items.remove(item);
			
			OptProjDb projectedDb = buildProjectedContext(upIndexes.clone(),upSeqsID,-1,item,1);
			//System.out.println("PSeq:"+prefix+"|"+prefix.indexes+"|"+prefix.left);
			recursion(prefix, 2, projectedDb.indexes, projectedDb.seqsID, items, item);
			
			items.add(item);
		}		
	}

	
	/************************************
	 ****** PROJECTION AND GROWTH *******
	 ************************************/

	private void recursion(Sequence prefix, int k, int[] upIndexes, Set<Integer> upSeqsID, Set<Integer> items, int upitem) {
		//System.out.println("Prefix:"+prefix.toString()+",("+BicPrinting.plot(upIndexes)+"),"+upSeqsID.toString()+","+items+"|"+upitem);
		//postpruning(prefix,upIndexes,upSeqsID);
		
		/** A: adds new items (and seq IDs) co-occur (p1) or are suffix (p2) of last prefix index **/
		Set<Pair> pairs = new HashSet<Pair>();
		for(Integer item : items){
			Pair p1 = new Pair(true,item);
			Pair p2 = new Pair(false,item);
			for(Integer seqID : upSeqsID){
				Sequence seq = database.getSequences().get(seqID);
				//System.out.println("Seq:"+seq+"|"+seq.indexes+"|"+seq.left);
				if(!seq.indexes.containsKey(item) || !seq.indexes.containsKey(upitem)) continue;
				
				if(seq.indexes.get(item)>seq.indexes.get(upitem)){
					if(seq.left.get(item)>=minColumns-k)
						p2.getSequencesID().add(seqID);
				} else if(item>upitem && seq.indexes.get(item)==seq.indexes.get(upitem)){
					if(seq.left.get(item)>=minColumns-k) 
						p1.getSequencesID().add(seqID);
				}
			}
			//System.out.println("Item:"+item+" P1:"+p1.getCount()+" P2:"+p2.getCount());
			if(p1.getCount()>=minsup) pairs.add(p1);
			if(p2.getCount()>=minsup) pairs.add(p2);
		}
		
		/** B: For each pair grow prefix and add to patterns **/
		for(Pair paire : pairs){
			Sequence newPrefix = prefix.cloneSequence();;
			if(paire.isPostfix()){
				Itemset itemset = newPrefix.get(newPrefix.size()-1); 
				itemset.addItem(paire.getItem()); // is =<is, (deltaT,i)>
				newPrefix.indexes.put(paire.getItem(), newPrefix.size()-1);
			} else {
				newPrefix.addItemset(new Itemset(paire.getItem()));
				newPrefix.indexes.put(paire.getItem(), newPrefix.size()-1);
			}
			newPrefix.setSequencesID(paire.getSequencesID());
			//System.out.println("YES:"+newPrefix.toString()+"\nk:"+k+" MinCols:"+minColumns);			
			if(k>=minColumns) patterns.addSequence(newPrefix, newPrefix.size());
			
			/** C: recursion with new prefix **/
			items.remove(paire.getItem());
			OptProjDb projectedDb = buildProjectedContext(upIndexes.clone(),paire.getSequencesID(),upitem,paire.getItem(),k);
			recursion(newPrefix, k+1, projectedDb.indexes, projectedDb.seqsID ,items, paire.getItem());
			items.add(paire.getItem());
		}
	}
	
	/** Create a projected database by pseudo-projection */
	private OptProjDb buildProjectedContext(int[] upIndexes, Set<Integer> upSeqsID, int upItem, int currentItem, int nrItems) {
		//System.out.println("+("+BicPrinting.plot(upIndexes)+"),"+upSeqsID.toString()+","+currentItem+"|"+upItem+" #"+nrItems);
		
		/** A: Sequences where prefix (current item) preceeds new item **/
		Set<Integer> newSeqs = new HashSet<Integer>();
		for(Integer id : upSeqsID){
			Sequence sequence = database.getSequences().get(id);
			
			int index = sequence.indexes.containsKey(currentItem) ? sequence.indexes.get(currentItem) : -10;  
			//if(index<0) System.out.println("AIIIII!");
			if(index > upIndexes[id]){ //preceeds
				upIndexes[id]=index;
				if(sequence.left.get(currentItem)>=minColumns-nrItems) newSeqs.add(id);
				//else System.out.println("OPSSSS!");
			} else if(currentItem>upItem && index==upIndexes[id]){ //co-occurs
				if(sequence.left.get(currentItem)>=minColumns-nrItems) newSeqs.add(id);
				//else System.out.println("OPSSSS2!");
			} else upIndexes[id]=1000; //not preceeds
		}
		//System.out.println(BicPrinting.plot(upIndexes)+" <<>> "+newSeqs);
		return new OptProjDb(upIndexes,newSeqs);
	}

	
	/****************************
	 ********* OTHERS ***********
	 ****************************/

	private Map<Integer, Set<Integer>> findSequencesContainingItems(SequenceDatabase contexte) {
		Set<Integer> alreadyCounted = new HashSet<Integer>(); // il faut compter un item qu'une fois par sequence.
		Sequence lastSequence = null;
		Map<Integer, Set<Integer>> mapSequenceID = new HashMap<Integer, Set<Integer>>(); // pour conserver les ID des sequences: <Id Item, Set d'id de sequences>
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
	public void addConstraints(Constraints _constraints) { constraints = _constraints;	}
	
	/*private void postpruning(Sequence prefix, int[] upIndexes, Set<Integer> upSeqsID) {
		//System.out.println(">>"+prefix.indexes);
		int index = -1, ci = 0, cj = 0;
		for(int l=constraint.size(); ci<l; ci++){
			Integer obj = constraint.get(ci).get(0);
			if(!prefix.indexes.containsKey(obj)) break;
			int index1 = prefix.indexes.get(obj); 
			if(index1 <= index) break;
			index = index1;
			for(cj=1; cj<constraint.get(ci).size(); cj++) {
				Integer key = constraint.get(ci).get(cj);
				if(!prefix.indexes.containsKey(key)) break;
				if(index!=prefix.indexes.get(key)) break;
			}
		}
		Set<Integer> removals = new HashSet<Integer>();
		for(Integer seqID : upSeqsID) 
			if(prune(database.sequences.get(seqID), ci, cj)) removals.add(seqID);
		upSeqsID.removeAll(removals);
	}*/
	private void prepruning() {
		for(int i=database.size()-1; i>=0; i--)
			if(prune(database.sequences.get(i))) database.seqIDs.remove(i);
			//else System.out.println(database.sequences.get(i).toString());
	}
	private boolean prune(Sequence sequence, int ci, int cj) {
		int index = -1;
		for(Constraint constraintInclude : constraints.constraints){
			List<List<Integer>> constraint = ((SequentialConstraint)constraintInclude).pattern;
			for(int l=constraint.size(); ci<l; ci++){
				int index1 = sequence.indexes.get(constraint.get(ci).get(0)); 
				if(index1 <= index) return true;
				index = index1;
				for(cj=1; cj<constraint.get(ci).size(); cj++) 
					if(index!=sequence.indexes.get(constraint.get(ci).get(cj))) return true;
			}
		}
		return false;
	}
	private boolean prune(Sequence sequence) {
		int index = -1;
		for(Constraint constraintInclude : constraints.constraints){
			List<List<Integer>> pattern = ((SequentialConstraint)constraintInclude).pattern;
			for(List<Integer> itemset : pattern){
				int index1 = sequence.indexes.get(itemset.get(0)); 
				if(index1 <= index) return true;
				index = index1;
				for(int i=1, l=itemset.size(); i<l; i++) if(index!=sequence.indexes.get(itemset.get(i))) return true;
			}
		}
		return false;
	}
	public void projprint(OptProjDb projectedDb){
		for(int i=0; i<projectedDb.indexes.length; i++) System.out.print(projectedDb.indexes[i]+",");
		System.out.println(" "+projectedDb.seqsID);
	}
	public void printStatistics(int size) {
		System.out.println("Statistics:\n Total time ~ "+time+"ms\n"+patterns.toString());
	}
}
