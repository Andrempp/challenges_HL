package bicpam.pminer.fim.algorithms;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import bicpam.pminer.BicPM;
import bicpam.pminer.fim.f2g.F2GConstraint;
import domain.Bicluster;
import domain.Biclusters;
import domain.constraint.Constraints;

/**@author Rui Henriques
 * @version 1.0
 */
public class BicSimpleF2GConstraint extends F2GConstraint implements BicPM {
  
  public BicSimpleF2GConstraint(List<List<Integer>> dataset){
	  super(dataset);
  }
  public void setData(List<List<Integer>> dataI) {
  	super.setData(dataI);
  }
  public void reset(){
	super.reset();
  }
  public void addConstraints(Constraints constraints) {
	super.addConstraints(constraints);
  }
  public Biclusters run(int minColumns,	int minRows, int maxColumns, int maxRows, int nrLabels) throws Exception {
	  super.runAlgorithm(((double)minRows)/(double)dataset.size());
	  Biclusters itemsets = new Biclusters();
	  //System.out.println("#rows:" + source.numInstances() + " #columns:" + source.numAttributes() + " #minrows:" + minRows + " #mincolumns:" + minColumns + " #maxrows" + maxRows + " #maxcolumns" + maxColumns);
	  //System.out.println("#itemsets:" + itemsets.size() + "\nsupport:" + ((double)minRows)/(double)source.numInstances());
	  
	  for(int i=0; i<itemSets.size(); i++){
		  int nrItems = itemSets.get(i).size();
		  if(nrItems < minColumns) continue;
		  SortedSet<Integer> itemset = new TreeSet<Integer>();
		  SortedSet<Integer> transet = new TreeSet<Integer>();
		  for(Integer item : itemSets.get(i)) itemset.add(item/nrLabels);
		  transet.addAll(transSets.get(i));
		  itemsets.add(new Bicluster(transet,itemset));
	  }
	  return itemsets;
  }

  public Biclusters run(double support, int nrLabels) throws Exception {	  
	  super.runAlgorithm(support);
	  Biclusters itemsets = new Biclusters();
	  for(int i=0; i<itemSets.size(); i++){
		  SortedSet<Integer> itemset = new TreeSet<Integer>();
		  SortedSet<Integer> transet = new TreeSet<Integer>();
		  if(itemSets.get(i).size()<3) continue;
		  for(Integer item : itemSets.get(i)) itemset.add(item/nrLabels);
		  transet.addAll(transSets.get(i));
		  itemsets.add(new Bicluster(transet,itemset));
	  }
	  return itemsets;
  }

  public long getMemory() { return mem; }
}
