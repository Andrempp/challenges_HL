package bicpam.pminer.fim.charm;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import bicpam.pminer.fim.charm.count.HashTable;

/**
 * This is an implementation of the CHARM-MFI algorithm (thesis of L. Szathmary, 2006) 
 * that is a simple extension that takes the output of CHARM as input and 
 * do post-processing to keep only maximal itemsets. <br/><br/>
 * 
 * But it was found that the Charm-MFI algorithm is actually incorrect so
 * this implementation was modified to fix the original algorithm and 
 * generate the correct result. <br/><br/>
 * 
 * Note that this algorithm is not very efficient because the maximal itemsets 
 * are generated by post-processing.<br/><br/>
 * 
 * Also, note that this version can save the result to a file
 * or keep it into memory if no output path is provided
 * by the user to the runAlgorithm() method.
 * 
 * @see TriangularMatrix
 * @see TransactionDatabase
 * @see Itemset
 * @see Itemsets
 * @see HashTable
 * @see AlgoCharm_Bitset
 * @author Philippe Fournier-Viger
 */
public class AlgoRCharmMFI {
	
	/**  start time of the last execution */
	private long startTimestamp;
	/** end  time of the last execution */
	private long endTimestamp; 

	/** 
	 The  patterns that are found 
	 (if the user want to keep them into memory) */
	protected Itemsets maximalItemsets;
	/** object to write the output file */
	BufferedWriter writer = null; 
	
	/**
	 * Default constructor
	 */
	public AlgoRCharmMFI() {}

	/**
	 * Run the CHARM-MFI algorithm.
	 * @param output an output file path or null if the user want to keep the result in memory.
	 * @param frequentClosed a set of frequent closed itemsets
	 * @return the set of maximal itemsets (if the user chose to keep the result in memory.
	 * @throws IOException An exception if an error occurs while writting the output to a file.
	 */
	public Itemsets runAlgorithm(String output, Itemsets frequentClosed) throws IOException {
		
		// if the user want to keep the result into memory
		if(output == null){
			writer = null;
	    }else{ // if the user want to save the result to a file
			writer = new BufferedWriter(new FileWriter(output)); 
		}

		// Initialize the structure to store maximal itemsets
		maximalItemsets =  frequentClosed;
		maximalItemsets.setName("FREQUENT MAXIMAL ITEMSETS");
		
		// record the start time of the algorithm
		startTimestamp = System.currentTimeMillis();

		// get the size of the largest closed itemset.
		int maxItemsetLength = frequentClosed.getLevels().size();
		
		// IMPORTANT NOTE : THE ORIGINAL ALGORITHM IS INCORRECT (IT
		// DOES NOT PRODUCE THE SET OF MAXIMAL ITEMSETS IN SOME CASES BECAUSE
		// IT ONLY COMPARES CLOSED ITEMSETS OF SIZE I WITH THOSE OF SIZE I+1. 
		// HOWEVER, IT IS POSSIBLE THAT AN ITEMSET IS NOT MAXIMAL BECAUSE
		// OF A CLOSED ITEMSET OF A SIZE LARGER THAN I+1. ):
		// TO FIX IT THE ALGORITHM, WE HAVE MODIFIED IT AS FOLLOWS:
		
		// For closed itemsets of size i=1 to the largest size
		for (int i = 1; i < maxItemsetLength - 1; i++) {
			// Get the itemsets of size i
			List<Itemset> ti = frequentClosed.getLevels().get(i);
			// For closed itemsets of size j = i+1 to the largest size
			for (int j = i+1; j < maxItemsetLength; j++) {
				// get itemsets of size j
				List<Itemset> tip1 = frequentClosed.getLevels().get(j);
				
				// Check which itemsets are maximals by comparing itemsets
				// of size i and i+1
				findMaximal(ti, tip1, frequentClosed);
			}
		}
		
		// If the user chose to save the output to a file
		if(writer != null){
			// For itemsets of size i = 1 to the maximum itemset length
			for(List<Itemset> level : maximalItemsets.getLevels()){
				// For each itemset of length i
				for(int i=0; i < level.size(); i++){
					Itemset itemset = level.get(i);
					// save the itemset an its support
					writer.write(itemset.toString() + " #SUP: "	+ itemset.getAbsoluteSupport());
					writer.newLine();
				}
			}
			writer.close();
		}

		endTimestamp = System.currentTimeMillis();
		// Return all frequent maximal itemsets found!
		return maximalItemsets; 
	}

	/**
	 * Check if itemsets of size i are closed by comparing them with
	 * itemsets of size j where j > i.
	 * @param ti  itemsets of size i
	 * @param tip1 itemsets of size j
	 * @param maximalItemsets the current set of maximal itemsets
	 */
	private void findMaximal(List<Itemset> ti, List<Itemset> tip1, Itemsets maximalItemsets) {
		// for each itemset of j
		for (Itemset itemsetJ : tip1) {
			// iterates over the itemsets of size i
			Iterator<Itemset> iter = ti.iterator();
			while (iter.hasNext()) {
				Itemset itemsetI = (Itemset) iter.next();
				// if the current itemset of size i is contained
				// in the current itemset of size J
				if (itemsetJ.containsAll(itemsetI) ) {
					// Then, it means that the itemset of size I is not maximal so we remove it
					iter.remove();
					// We decrease the current number of maximal itemsets.
					maximalItemsets.decreaseItemsetCount();
					// NOTE: IF WE WOULD LIKE TO MAKE THIS MORE EFFICIENT
					// WE COULD USE LINKED-LIST TO STORE ITEMSETS 
					// INSTEAD OF ARRAY LISTS.....
					// THE COST FOR THE REMOVE OPERATION MAY BE SMALLER...
				}	
			}
		}
	}

	/**
	 * Print statistics about the algorithm execution to System.out.
	 */
	public void printStats(int transactionCount) {
		System.out.println("=============  CHARM-MFI - STATS =============");
		long temps = endTimestamp - startTimestamp;
		System.out.println(" Transactions count from database : "
				+ transactionCount);
		System.out.println(" Frequent maximal itemsets count : "
				+ maximalItemsets.getItemsetsCount());
		System.out.println(" Total time ~ " + temps + " ms");
		System.out.println("===================================================");
	}

	/**
	 * Get the set of maximal itemsets found by Charm-MFI
	 * @return the set of maximal itemsets
	 */
	public Itemsets getItemsets() {
		return maximalItemsets;
	}
}
