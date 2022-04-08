package bicpam.pminer.fim.apriori;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Set;

/**
 * This class represents an itemset (a set of items)
 */
public class Itemset{

	public boolean maximal = true;
	public List<Integer> items = new ArrayList<Integer>(); // ordered
	public BitSet transactionsIds = new BitSet();
	int cardinality =0;
	
	public Itemset(){}

	public Itemset(int[] is) {
		for(int i=0; i<is.length; i++) items.add(is[i]);
	}
	public Itemset(List<Integer> is) {
		for(Integer i : is) items.add(i);
	}
	public Itemset(Set<Integer> is) {
		for(Integer i : is) items.add(i);
	}

	public Itemset(Set<Integer> itemset, BitSet tidset, int _card) {
		items.addAll(itemset);
		transactionsIds = tidset;
		cardinality = _card;
	}

	public boolean includedIn(Itemset itemset2) {
		return itemset2.items.containsAll(items);
	}

	public double getRelativeSupport(int nbObject) {
		return ((double)cardinality) / ((double) nbObject);
	}
	
	public String getSupportRelatifFormatted(int nbObject) {
		double frequence = ((double)cardinality) / ((double) nbObject);
		DecimalFormat format = new DecimalFormat();
		format.setMinimumFractionDigits(0); 
		format.setMaximumFractionDigits(2); 
		return format.format(frequence);
	}
	
	public int getAbsoluteSupport(){
		return transactionsIds.cardinality();
	}

	public void addItem(Integer value){
			items.add(value);
	}

	
	public List<Integer> getItems(){
		return items;
	}
	
	public Integer get(int index){
		return items.get(index);
	}
	
	public void print(){
		System.out.print(toString());
	}
	
	public void setTransactioncount(BitSet listTransactionIds, int cardinality) {
		this.transactionsIds = listTransactionIds;
		this.cardinality = cardinality;
	}
	
	public int size(){
		return items.size();
	}

	public BitSet getTransactionsIds() {
		return transactionsIds;
	}
	
	public Itemset union(Itemset itemset) {
		Itemset union = new Itemset();
		union.getItems().addAll(items);
		union.getItems().addAll(itemset.getItems());
		return union;
	}

	public void setTransactioncount(Set<Integer> listTransactionIds) {
		transactionsIds = new BitSet();
		for(Integer i : listTransactionIds)
			transactionsIds.set(i);
	}

	public String toString(){
		StringBuffer r = new StringBuffer ();
		for(Integer attribute : items){
			r.append(attribute.toString());
			r.append(' ');
		}
		return r.toString();
	}
}
