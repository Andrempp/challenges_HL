package bicpam.pminer.fim.f2g;

import java.util.ArrayList;
import java.util.List;

public class F2GNodeTID {
	
	int itemID = -1;  
	int counter = 1;  // frequency
	F2GNodeTID nodeLink = null; // next node with same item
	F2GNodeTID parent = null; 
	List<F2GNodeTID> childs = new ArrayList<F2GNodeTID>();
    List<Integer> transactions = new ArrayList<Integer>();
	
	/* Return the immmediate child with a given ID (or null) 
    public F2GNodeTID getChildWithID(int id) {
		for(F2GNodeTID child : childs) if(child.itemID == id) return child;
		return null;
	}
    public List<Integer> getItemLeafs(int recursion, int cursor, List<Integer> items){
    	//for(int i=0;i<recursion;i++) System.out.print(" | ");
    	//System.out.println(m_item.getAttribute().index() + " cursor:" + items.get(cursor).getAttribute().index()); 
		List<Integer> trans = new ArrayList<Integer>();
    	if(items.get(cursor).equals(itemID)) cursor++;
        if(cursor < items.size()){
        	for(F2GNodeTID child : childs) trans.addAll(child.getItemLeafs(recursion+1,cursor,items));
   	    	return trans; 
        }
        else return getAllTransactions();
    }    
    public List<Integer> getAllTransactions(){
		List<Integer> trans = new ArrayList<Integer>();
    	trans.addAll(transactions);
    	for(F2GNodeTID child : childs) trans.addAll(child.getAllTransactions());
    	return trans;
    }*/	
    public String toString(){
    	return itemID+"";
    	/*String result = itemID+(childs.size()>0 ? "->(" : "");
    	for(F2GNodeTID node : childs) result+=node.toString();
    	return result+(childs.size()>0 ? ")" : "");//+transactions;*/
    }
}
