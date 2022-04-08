package bicpam.pminer.fim.fpgrowth.spmf;

import java.util.ArrayList;
import java.util.List;

import weka.associations.BinaryItem;
import bicpam.pminer.fim.fpgrowth.weka.WFPGrowthTID.FPTreeNode;

public class FPNodeTID {
	
	int itemID = -1;  // item id
	int counter = 1;  // frequency counter
	FPNodeTID nodeLink = null; // link to next node with the same item id (for the header table).
	FPNodeTID parent = null; 
	List<FPNodeTID> childs = new ArrayList<FPNodeTID>();
    List<Integer> transactions = new ArrayList<Integer>();
	
	FPNodeTID(){}

	/**Return the immmediate child of this node having a given ID.
	 * If there is no such child, return null;
	 */
	public FPNodeTID getChildWithID(int id) {
		for(FPNodeTID child : childs) 
			if(child.itemID == id) return child;
		return null;
	}
	
    public List<Integer> getItemLeafs(int recursion, int cursor, List<Integer> items){
    	//for(int i=0;i<recursion;i++) System.out.print(" | ");
    	//System.out.println(m_item.getAttribute().index() + " cursor:" + items.get(cursor).getAttribute().index()); 
		List<Integer> trans = new ArrayList<Integer>();
    	if(items.get(cursor).equals(itemID)) cursor++;
        if(cursor < items.size()){
        	//System.out.println("NO" + cursor + " " + items.size());
        	if(childs.isEmpty()) return trans; 
        	for(FPNodeTID child : childs) trans.addAll(child.getItemLeafs(recursion+1,cursor,items));
  	        /*if(!trans.isEmpty()){
  	        	System.out.println("NOOOO");
  	        	trans.addAll(m_transactions);
  	        }*/
   	    	return trans; 
        }
        else return addAllTransactions();
    }    
    public List<Integer> addAllTransactions(){
		List<Integer> trans = new ArrayList<Integer>();
    	trans.addAll(transactions);
    	for(FPNodeTID child : childs) trans.addAll(child.addAllTransactions());
    	return trans;
    }	
}
