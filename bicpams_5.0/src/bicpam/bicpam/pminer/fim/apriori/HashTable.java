package bicpam.pminer.fim.apriori;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a hash table
 */
public class HashTable {
	
	int size;
	List<SetItemset>[] table;
	
	@SuppressWarnings("unchecked")
	public HashTable(int size){
		this.size = size;
		table = new ArrayList[size];
	}
	
	public boolean containsSupersetOf(SetItemset itemsetObject) {
		int hashcode = hashCode(itemsetObject);
		if(table[hashcode] ==  null){
			return false;
		}
		for(Object object : table[hashcode]){
			SetItemset itemsetObject2 = (SetItemset)object;
			if(itemsetObject2.itemset.size() == itemsetObject.itemset.size() &&
					itemsetObject2.itemset.containsAll(itemsetObject.itemset)
					){  // FIXED BUG 2010-10: containsAll instead of contains.
				return true;
			}
		}
		return false;
	}
	public void put(SetItemset itemsetObject) {
		int hashcode = hashCode(itemsetObject);
		if(table[hashcode] ==  null){
			table[hashcode] = new ArrayList<SetItemset>();
		}
		table[hashcode].add(itemsetObject);
	}
	
	public int hashCode(SetItemset itemsetObject){
		int hashcode =0;
//		for (int bit = bitset.nextSetBit(0); bit >= 0; bit = bitset.nextSetBit(bit+1)) {
		for (int tid=itemsetObject.tidset.nextSetBit(0); tid >= 0; tid = itemsetObject.tidset.nextSetBit(tid+1)) {
			hashcode += tid;
	    }
		return (hashcode % size);
	}
}
