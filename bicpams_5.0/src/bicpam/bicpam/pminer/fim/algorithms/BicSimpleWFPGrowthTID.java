package bicpam.pminer.fim.algorithms;

import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import bicpam.pminer.BicPM;
import bicpam.pminer.fim.fpgrowth.weka.WFPGrowthTID;
import domain.Bicluster;
import domain.Biclusters;
import weka.associations.BinaryItem;
import weka.core.Instances;

/**@author Rui Henriques
 * @version 1.0
 */
public class BicSimpleWFPGrowthTID extends WFPGrowthTID implements BicPM {
  
  private static final long serialVersionUID = 1L;
  
  public BicSimpleWFPGrowthTID(Instances data){
	  super(data);
  }
  //public void setData(List<Set<Integer>> dataI) {}
  public void reset(){
	super.reset();
  }
  
  public Biclusters run(int minColumns,	int minRows, int maxColumns, int maxRows, int nrLabels) throws Exception {
	  buildAssociations(((double)minRows)/(double)source.numInstances());
	  Biclusters itemsets = new Biclusters();
	  //System.out.println("#rows:" + source.numInstances() + " #columns:" + source.numAttributes() + " #minrows:" + minRows + " #mincolumns:" + minColumns + " #maxrows" + maxRows + " #maxcolumns" + maxColumns);
	  //System.out.println("#itemsets:" + itemsets.size() + "\nsupport:" + ((double)minRows)/(double)source.numInstances());
	  
	  for(int i=0; i<m_largeItemSets.size(); i++){
		  int nrItems = m_largeItemSets.getItemSet(i).getItems().size();
		  // ||  nrItems > maxColumns || nrTrans > maxRows
		  if(nrItems < minColumns) continue;
		  SortedSet<Integer> itemset = new TreeSet<Integer>();
		  Iterator<BinaryItem> binList = m_largeItemSets.getItemSet(i).getItems().iterator();
		  while(binList.hasNext()) itemset.add(binList.next().getAttribute().index()/nrLabels);
		  SortedSet<Integer> transet = new TreeSet<Integer>();
		  transet.addAll(m_TransactionSets.get(i));
		  itemsets.add(new Bicluster(transet,itemset));//
	  }
	  return itemsets;
  }

  public Biclusters run(double support, int nrLabels) throws Exception {
	  buildAssociations(support);
	  Biclusters itemsets = new Biclusters();
	  for(int i=0; i<m_largeItemSets.size(); i++){
		  SortedSet<Integer> itemset = new TreeSet<Integer>();
		  Iterator<BinaryItem> binList = m_largeItemSets.getItemSet(i).getItems().iterator();
		  while(binList.hasNext()) itemset.add(binList.next().getAttribute().index()/nrLabels);
		  SortedSet<Integer> transet = new TreeSet<Integer>();
		  transet.addAll(m_TransactionSets.get(i));
		  itemsets.add(new Bicluster(transet,itemset));//
	  }
	  return itemsets;
  }
  
  protected void printItemsets() throws Exception {
	  for(int i=0; i<m_largeItemSets.size(); i++) 
		  System.out.println(m_largeItemSets.getItemSet(i).toString());
  }

  public long getMemory() { return -1; }

}
