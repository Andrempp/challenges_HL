package bicpam.pminer.fim.charm;

import java.util.BitSet;

/**
 * This class represents an itemset (a set of items) where the itemset is an array of integers 
 * sorted by lexical order, an item should not appear more than once, 
 * the ids of transactions/sequences containing this itemset is represented
 * as a bitset.
 * @see AbstractOrderedItemset
 * @author Philippe Fournier-Viger
 */
public class Itemset extends AbstractOrderedItemset{
	
	/** The list of items contained in this itemset, ordered by 
	 lexical order */
	public int[] itemset; // the array of items
	
	/** The list of transactions/sequences containing this itemset **/
	private BitSet transactionsIds;
	public int cardinality =0;  // the cardinality of the above bitset
	
	/**
	 * Constructor of an empty itemset
	 */
	public Itemset(){
		transactionsIds = new BitSet();
	}
	
	/**
	 * Constructor of an empty itemset
	 * @param itemset the itemset
	 * @param the tidset of the itemset
	 * @param the tidset cardinality (support)
	 */
	public Itemset(int[] itemset, BitSet bitset, int support){
		this.transactionsIds = bitset;
		this.itemset = itemset;
		this.cardinality = support;
	}
	
	/**
	 * Constructor 
	 * @param item an item that should be added to the new itemset
	 */
	public Itemset(int item){
		itemset = new int[]{item};
	}
	
	/**
	 * Constructor 
	 * @param items an array of items that should be added to the new itemset
	 */
	public Itemset(int [] items){
		this.itemset = items;
	}

	/**
	 * Get the support of this itemset
	 * @return the support of this itemset
	 */
	public int getAbsoluteSupport(){
		return cardinality;
	}

	/**
	 * Get the list of items in this itemset
	 * @return the list of items
	 */
	public int[] getItems(){
		return itemset;
	}
	
	/**
	 * Get the item at a given position of this itemset
	 * @param index the position of the item to be returned
	 * @return the item
	 */
	public Integer get(int index){
		return itemset[index];
	}

	/**
	 * Set the list of transaction/sequence ids containing this itemset.
	 * @param listTransactionIds the list of transaction/sequence ids.
	 * @param cardinality the cardinality of the list.
	 */
	public void setTIDs(BitSet listTransactionIds, int cardinality) {
		this.transactionsIds = listTransactionIds;
		this.cardinality = cardinality;
	}

	/**
	 * Get the size of this itemset.
	 */
	public int size(){
		return itemset.length;
	}

	/**
	 * Get the list of transactions/sequences containing this itemset.
	 * @return the list as a bitset.
	 */
	public BitSet getTransactionsIds() {
		return transactionsIds;
	}


}
