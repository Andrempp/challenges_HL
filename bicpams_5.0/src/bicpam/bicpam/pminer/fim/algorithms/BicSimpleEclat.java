package bicpam.pminer.fim.algorithms;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import bicpam.pminer.BicPM;
import bicpam.pminer.BicPMUtils;
import bicpam.pminer.fim.eclat.AlgoEclat;
import bicpam.pminer.fim.eclat.Itemset;
import domain.Bicluster;
import domain.Biclusters;

/**
 * @author Rui Henriques
 * @version 1.0
 */
public class BicSimpleEclat extends AlgoEclat implements BicPM {

	public BicSimpleEclat(List<List<Integer>> dataset) {
		super(dataset);
	}	
    public void setData(List<List<Integer>> dataI) {
    	super.setData(dataI);
	}
    public void reset(){
	  super.reset();
    }

	public Biclusters run(int minColumns, int minRows, int maxColumns, int maxRows, int nrLabels) throws Exception {
	  double support = ((double)minRows)/(double)dataset.length;
	  //System.out.println("SUP:"+support+" Length:"+dataset.length);
	  //System.out.println("Prior itemsets:"+freqItemsets.size());
	  super.runAlgorithm(support,false);
	  Biclusters itemsets = new Biclusters();
	  for(List<Itemset> itemsetsL : frequentItemsets.getLevels())
		  for(Itemset itemset : itemsetsL){
			  // ||  nrItems > maxColumns || nrTrans > maxRows
			  if(itemset.size() < minColumns) continue;
			  SortedSet<Integer> itemsetB = new TreeSet<Integer>();
			  for(Integer item : itemset.getItems()) itemsetB.add(item/nrLabels);
			  SortedSet<Integer> transetB = new TreeSet<Integer>();
			  transetB.addAll(itemset.getTransactionsIds());
			  itemsets.add(new Bicluster(transetB,itemsetB));//
		  }
	  return itemsets;
  }

  public Biclusters run(double support, int nrLabels) throws Exception {
	  super.runAlgorithm(support,false);
	  Biclusters itemsets = BicPMUtils.toBicItemsets(frequentItemsets, nrLabels);
	  return itemsets;
  }
  
  public long getMemory() { return mem; }
}
