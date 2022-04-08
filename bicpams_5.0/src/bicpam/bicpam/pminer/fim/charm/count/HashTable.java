package bicpam.pminer.fim.charm.count;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Set;

/**
 * This class represents an HashTable for storing itemsets found by the Charm
 * algorithm to perform the closeness check.
 * @see AlgoCharm_Bitset
 * @see TriangularMatrix
 * @see TransactionDatabase
 * @see Itemset
 * @see Itemsets
 * @author Philippe Fournier-Viger
 */
public class HashTable {

	// the internal array for the hash table
	private List<Itemset>[] table;

	/**
	 * Construtor.
	 * @param size size of the internal array for the hash table.
	 */
	public HashTable(int size) {
		table = new ArrayList[size];
	}

	/**
	 * Check if the hash table contains a superset of a given itemset.
	 * @param itemset the given itemset
	 * @param hashcode the hashcode of the itemset (need to be calculated before by using the
	 *  provided hashcode() method.
	 * @return true if the hash table contains at least one superset, otherwise false.
	 */
	public boolean containsSupersetOf(Itemset itemset, int hashcode) {
		// If the position in the array that is given by the hashcode is empty,
		// then return false.
		if (table[hashcode] == null) {
			return false;
		}
		// For each itemset X at that hashcode position
		for (Object object : table[hashcode]) {
			Itemset itemsetX = (Itemset) object;
			// if the support of X is the same as the given itemset and X contains
			// the given itemset
			if (itemsetX.getAbsoluteSupport() == itemset.getAbsoluteSupport()
					&& itemsetX.containsAll(itemset)) {
				// then return true
				return true;
			}
		}
		// Otherwise no superset is in the hashtable, so return false
		return false;
	}

	/**
	 * Add an itemset to the hash table.
	 * @param itemset the itemset to be added to the hashtable
	 * @param hashcode the hashcode of the itemset (need to be calculated before by using the
	 *  provided hashcode() method.
	 */
	public void put(Itemset itemset, int hashcode) {
		// if the position in the array is empty create a new array list
		// for that position
		if (table[hashcode] == null) {
			table[hashcode] = new ArrayList<Itemset>();
		}
		// store the itemset in the arraylist of that position
		table[hashcode].add(itemset);
	}

	/**
	 * Calculate the hashcode of an itemset as the sum of the tids of its tidset,
	 * modulo the internal array length.
	 * @param tidset the tidset of the itemset
	 * @return the hashcode (an integer)
	 */
	public int hashCode(BitSet tidset) {
		int hashcode = 0;
		// for each tid in the tidset
		for (int tid = tidset.nextSetBit(0); tid >= 0; tid = tidset.nextSetBit(tid+1)) {
			// make the sum
			hashcode += tid;
		}
		// If an integer overflow occurs and the hashcode is negative,
		// then we make it positive.
		if(hashcode < 0){
			hashcode = 0 - hashcode;
		}
		// Finally the hashcode is obtained by performing the modulo 
		// operation using the size of the internal array.
		return (hashcode % table.length);
	}
	
	/**
	 * Calculate the hashcode of an itemset as the sum of the tids of its tidset,
	 * modulo the internal array length.
	 * @param tidset the tidset of the itemset
	 * @return the hashcode (an integer)
	 */
	public int hashCode(Set<Integer> tidset) {
		int hashcode = 0;
		// for each tid in the tidset
		for (int tid : tidset) {
			// make the sum
			hashcode += tid;
		}
		// If an integer overflow occurs and the hashcode is negative,
		// then we make it positive.
		if(hashcode < 0){
			hashcode = 0 - hashcode;
		}
		// Finally the hashcode is obtained by performing the modulo 
		// operation using the size of the internal array.
		return (hashcode % table.length);
	}

}
