package bicpam.pminer.spm.algo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * This is an implementation of the PrefixSpan algorithm by Pei et al. 2001
 * This implementation is part of the SPMF framework.
 * This implemtation uses pseudo-projection as suggested by Pei et al. 2001
 */

public class AlgoIndexSpan3 {
	
	private Sequences patterns = null;
	private long startTime;
	private long endTime;
	private int minsuppRelative;
	private int minColumns;
	protected SequenceDatabase database;
	protected double mergingOverlap = -1;
			
	public void setDataset(List<List<Integer>> dataset, int overlap) {
		database = new SequenceDatabase(dataset);
	}
	public void setMergingOverlap(double _mergingOverlap) {
		mergingOverlap = _mergingOverlap;
	}

	/**
	 * Run the algorithm
	 * @param database : a sequence database
	 * @param minsupPercent : the minimum support as a percentage (e.g. 50%)
	 * @return sequential patterns
	 */
	public Sequences runAlgorithm(double minsupPercent) {
		patterns = new Sequences("FREQUENT SEQUENTIAL PATTERNS");
		this.minsuppRelative = (int) Math.ceil(minsupPercent*database.size());
		System.out.println("Support:"+minsuppRelative);
		if(this.minsuppRelative == 0) this.minsuppRelative = 1;
		startTime = System.currentTimeMillis();
		prefixSpan();
		endTime = System.currentTimeMillis();
		return patterns;
	}
	
	/**
	 * Run the algorithm
	 * @param database : a sequence database
	 * @param minSupRelative  :  the minimum support as a number (e.g. 2 sequences)
	 * @return sequential patterns
	 */
	public Sequences runAlgorithm(int minSupRelative, int minCols) {
		System.out.println("Support:"+minSupRelative);
		patterns = new Sequences("FREQUENT SEQUENTIAL PATTERNS");
		minColumns = minCols;
		this.minsuppRelative = minSupRelative;
		startTime = System.currentTimeMillis();
		prefixSpan();
		endTime = System.currentTimeMillis();
		return patterns;
	}
	
	/**
	 * @param contexte The initial context.
	 */
	private void prefixSpan(){
		//System.out.println(database.toString());
		int[] upIndexes = new int[database.size()];
		for(int i=0, l=database.size(); i<l; i++) upIndexes[i]=-1;
		Set<Integer> upSeqsID = database.getSequenceIDs();
		
		Set<Integer> items = new HashSet<Integer>();
		for(Itemset itemset : database.getSequences().get(0).getItemsets())
			items.addAll(itemset.getItems());
		
		Map<Integer, Set<Integer>> mapSequenceID = findSequencesContainingItems(database);
		for(Entry<Integer, Set<Integer>> entry : mapSequenceID.entrySet()){
			Integer item = entry.getKey();
			
			Sequence prefix = new Sequence(0);  
			prefix.addItemset(new Itemset(item));
			prefix.setSequencesID(database.getSequenceIDs());
			//patterns.addSequence(prefix, 1);
			items.remove(item);
			OptProjDb projectedDb = buildProjectedContext(upIndexes.clone(),upSeqsID,-1,item,1);
			recursion(prefix, 2, projectedDb.indexes, projectedDb.seqsID, items, item);
			items.add(item);
		}		
	}

	private void recursion(Sequence prefix, int k, int[] upIndexes, Set<Integer> upSeqsID, Set<Integer> items, int upitem) {
		Set<Pair> pairs = new HashSet<Pair>();
		for(Integer item : items){
			Pair p1 = new Pair(true,item);
			Pair p2 = new Pair(false,item);
			for(Integer seqID : upSeqsID){
				Sequence seq = database.getSequences().get(seqID);
				if(seq.indexes.get(item)>seq.indexes.get(upitem)){
					if(seq.left.get(item)>=minColumns-k)
						p2.getSequencesID().add(seqID);
				}
				else if(item>upitem && seq.indexes.get(item)==seq.indexes.get(upitem)){
					if(seq.left.get(item)>=minColumns-k) 
						p1.getSequencesID().add(seqID);
				}
			}
			if(p1.getCount() >= minsuppRelative) pairs.add(p1);
			if(p2.getCount() >= minsuppRelative) pairs.add(p2);
		}
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

	public void projprint(OptProjDb projectedDb){
		for(int i=0; i<projectedDb.indexes.length; i++) System.out.print(projectedDb.indexes[i]+",");
		System.out.println(" "+projectedDb.seqsID);
	}
	
	/**
	 * Create a projected database by pseudo-projection
	 * @param currentItem 
	 * @param nrItems 
	 * @param item The item to use to make the pseudo-projection
	 * @param context The current database.
	 * @param inSuffix This boolean indicates if the item "item" is part of a suffix or not.
	 * @return the projected database.
	 */
	private OptProjDb buildProjectedContext(int[] upIndexes, Set<Integer> upSeqsID, int upItem, int currentItem, int nrItems) {
		Set<Integer> newSeqs = new HashSet<Integer>();
		for(Integer id : upSeqsID){
			Sequence sequence = database.getSequences().get(id);
			//System.out.print("ID"+id);
			if(sequence.indexes.get(currentItem) > upIndexes[id]){
				//System.out.print("a");
				upIndexes[id]=sequence.indexes.get(currentItem);
				if(sequence.left.get(currentItem)>=minColumns-nrItems) 
					newSeqs.add(id);
			} else if(currentItem>upItem && sequence.indexes.get(currentItem)==upIndexes[id]){
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

	public void printStatistics(int size) {
		StringBuffer r = new StringBuffer(200);
		r.append("=============  Algorithm - STATISTICS =============\n Total time ~ ");
		r.append(endTime - startTime);
		r.append(" ms\n");
		r.append(" Frequent sequences count : ");
		r.append(patterns.sequenceCount);
		r.append('\n');
		r.append(patterns.toString());
		r.append("===================================================\n");
		System.out.println(r.toString());
	}

}
