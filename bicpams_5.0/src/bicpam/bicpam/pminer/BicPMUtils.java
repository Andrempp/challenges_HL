package bicpam.pminer;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import bicpam.pminer.fim.apriori.Itemset;
import bicpam.pminer.fim.apriori.Itemsets;
import bicpam.pminer.spm.algo.Sequence;
import bicpam.pminer.spm.algo.Sequences;
import domain.Bicluster;
import domain.Biclusters;


public class BicPMUtils {

	
	/****************************/
	/** A: FREQ ITEMSET MINING **/
	/****************************/

	private static Bicluster decode(Collection<Integer> items, BitSet trans, int nrLabels) {
		SortedSet<Integer> itemsB = new TreeSet<Integer>();
		for(Integer item : items) itemsB.add(item/nrLabels);
		SortedSet<Integer> transetB = new TreeSet<Integer>();
		for(int i=trans.nextSetBit(0); i>=0; i=trans.nextSetBit(i+1)) transetB.add(i);
		return new Bicluster(transetB,itemsB);
	}
	/*if(disc!=null) {
		disc.compute(bic,itemset.getItems());
		if(bic.wlift>disc.wlift) bicItemsets.add(bic);
	} else bicItemsets.add(bic);*/

	public static Biclusters toBicItemsets(bicpam.pminer.fim.eclat.Itemsets frequentItemsets, int nrLabels){
		//System.out.println("A");
		Biclusters bicItemsets = new Biclusters();
		for(List<bicpam.pminer.fim.eclat.Itemset> itemsetl : frequentItemsets.getLevels()) {
			for(bicpam.pminer.fim.eclat.Itemset itemset : itemsetl){
				SortedSet<Integer> itemsB = new TreeSet<Integer>();
				for(Integer item : itemset.getItems()) itemsB.add(item/nrLabels);
				bicItemsets.add(new Bicluster(new TreeSet<Integer>(itemset.getTransactionsIds()),itemsB));
			}
		}
	    return bicItemsets;
	}

	public static Biclusters toBicItemsets(Itemsets itemsets, int nrLabels){
		//System.out.println("A");
		Biclusters bicItemsets = new Biclusters();
		for(List<Itemset> itemsetl : itemsets.getLevels()) {
			for(Itemset itemset : itemsetl){
				if(!itemset.maximal) continue;
				bicItemsets.add(decode(itemset.items,itemset.transactionsIds,nrLabels));
			}
		}
	    return bicItemsets;
	}
	
	public static Biclusters toBicItemsets(bicpam.pminer.fim.charm.Itemsets itemsets, int nrLabels) {
		Biclusters bicItemsets = new Biclusters();
		for(List<bicpam.pminer.fim.charm.Itemset> itemsetl : itemsets.getLevels()) {
			for(bicpam.pminer.fim.charm.Itemset itemset : itemsetl){
				SortedSet<Integer> items = new TreeSet<Integer>();
				for(Integer item : itemset.itemset) items.add(item);
				bicItemsets.add(decode(items,itemset.getTransactionsIds(),nrLabels));
			}
		}
	    return bicItemsets;
	}

	public static Biclusters toValidItemsets(Itemsets itemsets, int minColumns, int maxColumns, int maxRows, int nrLabels) {
		Biclusters bicItemsets = new Biclusters();
		int originalMinCols = minColumns;
		if(minColumns == -1) minColumns = 1;
		for(int nrLevels = itemsets.getLevels().size(); minColumns < nrLevels; minColumns++){
			for(Itemset itemset : itemsets.getLevels().get(minColumns)){
				if(!itemset.maximal) continue;
				SortedSet<Integer> itemsetB = new TreeSet<Integer>();
				//SortedSet<Integer> patternB = new TreeSet<Integer>();
				for(Integer item : itemset.items){
					itemsetB.add(item/nrLabels);
					//patternB.add(item%nrLabels);
				}
				if(itemsetB.size()<originalMinCols) continue;
				//if(patternB.size()>1) continue;
				SortedSet<Integer> transetB = new TreeSet<Integer>();
				BitSet trans = itemset.transactionsIds;
				for(int i=trans.nextSetBit(0); i>=0; i=trans.nextSetBit(i+1)) transetB.add(i);
				bicItemsets.add(new Bicluster(transetB,itemsetB));
			}
		}
		//System.out.println("#"+itemsets.getItemsetsCount()+"/"+bicItemsets.size());
		return bicItemsets;
	}
	
	public static Biclusters toValidItemsets(bicpam.pminer.fim.charm.Itemsets itemsets, int minColumns, int maxColumns, int maxRows, int nrLabels) {
		//System.out.println("Here!"+itemsets.getLevels().get(0));
		//if(itemsets.getLevels().size()>1) System.out.println("Here!"+itemsets.getLevels().get(1));
		Biclusters bicItemsets = new Biclusters();
		int originalMinCols = minColumns;
		if(minColumns == -1) minColumns = 1;
		for(int nrLevels = itemsets.getLevels().size(); minColumns < nrLevels; minColumns++){
			for(bicpam.pminer.fim.charm.Itemset itemset : itemsets.getLevels().get(minColumns)){
				SortedSet<Integer> itemsetB = new TreeSet<Integer>();
				for(int i=0, l=itemset.itemset.length; i<l; i++)
					itemsetB.add(itemset.get(i)/nrLabels);
				if(itemsetB.size()<originalMinCols) continue;
				SortedSet<Integer> transetB = new TreeSet<Integer>();
				BitSet trans = itemset.getTransactionsIds();
				for(int i=trans.nextSetBit(0); i>=0; i=trans.nextSetBit(i+1)) transetB.add(i);
				bicItemsets.add(new Bicluster(transetB,itemsetB));
			}
		}
		return bicItemsets;
	}
		
	/**********************************/
	/** B: SEQUENTIAL PATTERN MINING **/
	/**********************************/
	
	public static Biclusters toBicItemsets(Sequences seqs, int nrLabels){
		Biclusters bicItemsets = new Biclusters();
		for(List<Sequence> seql : seqs.getLevels())
			for(Sequence seq : seql){
				List<Integer> orders = new ArrayList<Integer>();
				SortedSet<Integer> itemset = new TreeSet<Integer>();
				SortedSet<Integer> transet = new TreeSet<Integer>();
				transet.addAll(seq.getSequencesID());
				for(bicpam.pminer.spm.algo.Itemset items : seq.getItemsets()) {
					SortedSet<Integer> order = new TreeSet<Integer>(items.getItems());
					orders.addAll(order);
					itemset.addAll(order);
				}
				Bicluster bic = new Bicluster(transet,itemset);
				bic.orders = orders;
				bicItemsets.add(bic);
			}
	    return bicItemsets;
	}

	public static Biclusters toValidItemsets(Sequences seqs, int minColumns, int maxColumns, int maxRows, int nrLabels) {
		Biclusters bicItemsets = new Biclusters();
		for(List<Sequence> seql : seqs.getLevels())
			for(Sequence seq : seql){
				if(seq.getItemOccurencesTotalCount()<minColumns) continue;
				List<Integer> orders = new ArrayList<Integer>();
				SortedSet<Integer> itemset = new TreeSet<Integer>();
				for(bicpam.pminer.spm.algo.Itemset items : seq.getItemsets()) {
					SortedSet<Integer> order = new TreeSet<Integer>(items.getItems());
					orders.addAll(order);
					itemset.addAll(order);
				}
				if(itemset.size()<minColumns) continue;
				SortedSet<Integer> transet = new TreeSet<Integer>();
				transet.addAll(seq.getSequencesID());
				Bicluster bic = new Bicluster(transet,itemset);
				bic.orders = orders;
				bicItemsets.add(bic);
			}
	    return bicItemsets;
	}


}
