package bicpam.pminer.fim.algorithms;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import bicpam.pminer.BicPM;
import bicpam.pminer.fim.fpgrowth.spmf.SFPGrowthTID;
import domain.Bicluster;
import domain.Biclusters;
import domain.constraint.Constraint;

/**@author Rui Henriques
 * @version 1.0
 */
public class BicSimpleSFPGrowthTID extends SFPGrowthTID implements BicPM {
  
  public BicSimpleSFPGrowthTID(List<List<Integer>> dataset){
	  super(dataset);
  }
  public void setData(List<List<Integer>> dataI) {
  	super.setData(dataI);
  }
  public void reset(){
	super.reset();
  }
  public void addConstraints(List<Constraint> constraints) {
	super.addConstraints(constraints);
  }
  public Biclusters run(int minColumns,	int minRows, int maxColumns, int maxRows, int nrLabels) throws Exception {
	  super.runAlgorithm(((double)minRows)/(double)dataset.size());
	  Biclusters itemsets = new Biclusters();
	  //System.out.println("#rows:" + source.numInstances() + " #columns:" + source.numAttributes() + " #minrows:" + minRows + " #mincolumns:" + minColumns + " #maxrows" + maxRows + " #maxcolumns" + maxColumns);
	  //System.out.println("#itemsets:" + itemsets.size() + "\nsupport:" + ((double)minRows)/(double)source.numInstances());
	  
	  for(int i=0; i<itemSets.size(); i++){
		  int nrItems = itemSets.get(i).size();
		  // ||  nrItems > maxColumns || nrTrans > maxRows
		  if(nrItems < minColumns) continue;
		  SortedSet<Integer> itemset = new TreeSet<Integer>();
		  SortedSet<Integer> transet = new TreeSet<Integer>();
		  for(Integer item : itemSets.get(i).getItems()) itemset.add(item/nrLabels);
		  transet.addAll(transSets.get(i));
		  itemsets.add(new Bicluster(transet,itemset));
	  }
	  return itemsets;
  }

  public Biclusters run(double support, int nrLabels) throws Exception {	  
	  super.runAlgorithm(support);
	  System.out.println(frequentItemsets.getItemsetsCount());
	  Biclusters itemsets = new Biclusters();
	  for(int i=0; i<itemSets.size(); i++){
		  SortedSet<Integer> itemset = new TreeSet<Integer>();
		  SortedSet<Integer> transet = new TreeSet<Integer>();
		  for(Integer item : itemSets.get(i).getItems()) itemset.add(item/nrLabels);
		  transet.addAll(transSets.get(i));
		  itemsets.add(new Bicluster(transet,itemset));
	  }
	  return itemsets;
  }

  public long getMemory() { return mem; }
}
