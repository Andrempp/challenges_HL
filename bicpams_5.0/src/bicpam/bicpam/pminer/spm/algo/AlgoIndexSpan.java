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
public class AlgoIndexSpan{
	
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
	}
	public Sequences runAlgorithm(int minSupRelative, int minCols) {
		minColumns = minCols;
		minsup = minSupRelative;
		System.out.println("sup:"+minSupRelative);
		time = System.currentTimeMillis();
		prefixSpan();
		time = time-System.currentTimeMillis();
		return patterns;
	}
	
	private void prefixSpan(){
		int[] upIndexes = new int[database.size()];
		for(int i=0, l=database.size(); i<l; i++) upIndexes[i]=-1;
		for(int i=0, l=database.size(); i<l; i++) database.seqIDs.add(i);


		/** A: Get seq IDs and items **/
		Set<Integer> upSeqsID = database.getSequenceIDs();
		Map<Integer, Set<Integer>> mapSequenceID = findSequencesContainingItems(database);
		Set<Integer> items = new HashSet<Integer>();
		for(Entry<Integer, Set<Integer>> entry : mapSequenceID.entrySet())
			if(entry.getValue().size()>=minsup) items.add(entry.getKey());
		
		/** B: Grow each item (1-prefix) **/
		for(Entry<Integer, Set<Integer>> entry : mapSequenceID.entrySet()){
			if(entry.getValue().size()<minsup) continue; 
			Integer item = entry.getKey();
			Sequence prefix = new Sequence(0);  
			prefix.addItemset(new Itemset(item));
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
			}
			else newPrefix.addItemset(new Itemset(paire.getItem()));
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
		//System.out.println("("+BicPrinting.plot(upIndexes)+"),"+upSeqsID.toString()+","+currentItem+"|"+upItem+" #"+nrItems);
		
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
		//System.out.println(BicPrinting.plot(upIndexes));
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
	
	public void projprint(OptProjDb projectedDb){
		for(int i=0; i<projectedDb.indexes.length; i++) System.out.print(projectedDb.indexes[i]+",");
		System.out.println(" "+projectedDb.seqsID);
	}
	public void printStatistics(int size) {
		System.out.println("Statistics:\n Total time ~ "+time+"ms\n"+patterns.toString());
	}
}
