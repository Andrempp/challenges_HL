package bicpam.pminer.fim.fpgrowth.weka;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import utils.BicResult;
import weka.associations.*;
import weka.core.Instances;

/**Class adapting the FP-growth algorithm to enable the retrieval of transactions and only maxima itemsets.
 * @author Rui Henriques
 * @version 1.0
 */
public class MaximalFPGrowthTID extends WFPGrowthTID {

	private static final long serialVersionUID = 1L;

	public void setData(List<List<Integer>> dataI) {super.setData(dataI);}

  /**Find Maximal item sets in the FP-tree.
   * @param tree the root of the tree to mine
   * @param largeItemSets holds the large item sets found
   * @param recursionLevel the recursion level for the current projected counts
   * @param conditionalItems the current set of items that the current (projected) tree is conditional on
   * @param minSupport the minimum acceptable support
   */
  protected void mineTree(FPTreeRoot tree, FrequentItemSets maximalItemSets, 
      int recursionLevel, FrequentBinaryItemSet conditionalItems, int minSupport) {
    //System.out.print("yes");
    if(recursionLevel==0) BicResult.println(tree.toString(0));
    if (!tree.isEmpty(recursionLevel)) {
      if (m_maxItems > 0 && recursionLevel >= m_maxItems) return;
     
      Map<BinaryItem, FPTreeRoot.Header> headerTable = tree.getHeaderTable();
      Set<BinaryItem> keys = headerTable.keySet();
      Iterator<BinaryItem> i = keys.iterator();
      //boolean growing = false;
      while (i.hasNext()) {
        BinaryItem item = i.next();
        FPTreeRoot.Header itemHeader = headerTable.get(item);
        
        int support = itemHeader.getProjectedCounts().getCount(recursionLevel);// check for minimum support at this level
        if (support >= minSupport) {
          //growing = true;
          for(int j=0;j<recursionLevel;j++) BicResult.print(" |");
          BicResult.println(""+item.getAttribute().index()+"("+support+")");
        	
          for (FPTreeNode n : itemHeader.getHeaderList()) {// process header list at this recursion level
            int currentCount = n.getProjectedCount(recursionLevel);// push count up path to root
            if (currentCount > 0) {                            
              FPTreeNode temp = n.getParent();
              while (temp != tree) {
                temp.increaseProjectedCount(recursionLevel + 1, currentCount); // set/increase for the node
                headerTable.get(temp.getItem()).getProjectedCounts().increaseCount(recursionLevel + 1, currentCount);
                temp = temp.getParent();
              }
            }
          }
          FrequentBinaryItemSet newConditional = (FrequentBinaryItemSet) conditionalItems.clone();
          newConditional.addItem(item); // this item gets added to the conditional items
          newConditional.setSupport(support);
                  
          // now recursively process the new tree
          mineTree(tree, maximalItemSets, recursionLevel + 1, newConditional, minSupport);
          
          // reverse the propagated counts
          for (FPTreeNode n : itemHeader.getHeaderList()) {
            FPTreeNode temp = n.getParent();
            while (temp != tree) {
              temp.removeProjectedCount(recursionLevel + 1);
              temp = temp.getParent();
            }
          }
          for (FPTreeRoot.Header h : headerTable.values())
            h.getProjectedCounts().removeCount(recursionLevel + 1);
        		  
        } else {
            for(int j=0;j<recursionLevel;j++) BicResult.print(" |");
            BicResult.println("no"+item.getAttribute().index()+"("+support+")");
        }

      }
    } else maximalItemSets.addItemSet((FrequentBinaryItemSet) conditionalItems.clone()); 
  }
  
  public MaximalFPGrowthTID(Instances data) {
    super(data);
  }
}
