package bicpam.pminer.fim.f2g;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class F2GTree {
	
	List<Integer> headerList = null; // List of items in the header table
	Map<Integer, F2GNodeTID> mapItemNodes = new HashMap<Integer, F2GNodeTID>(); // Pairs (item, frequency) of the header table
	F2GNodeTID root = new F2GNodeTID(); // root of the tree

	
	/**********************/
	/*** ADD TREE PATHS ***/
	/**********************/	
	
	/** Add transaction to the fp-tree (initial construction) */
	public void addTransaction(List<Integer> transaction, int tid) {
		if(transaction.size() == 0) return;
		F2GNodeTID currentNode = root;
		Integer lastItem = transaction.get(transaction.size()-1);
		
		for(Integer item : transaction){			
			F2GNodeTID child = null;
			for(F2GNodeTID childI : currentNode.childs) if(childI.itemID == item) child = childI; 
			if(child == null){ // create a new one
				F2GNodeTID newNode = new F2GNodeTID();
				newNode.itemID = item;
				newNode.parent = currentNode;
				currentNode.childs.add(newNode); // link node to its parrent
				currentNode = newNode; // take this node as current node
				
				// update the header table
				F2GNodeTID headernode = mapItemNodes.get(item);
				if(headernode != null){ //find last node 
					while(headernode.nodeLink != null) headernode = headernode.nodeLink;
					headernode.nodeLink  = newNode;					
				} else mapItemNodes.put(item, newNode);
			} else { 
				child.counter++; //update
				currentNode = child;
			}
			if(item == lastItem) currentNode.transactions.add(tid); 
		}
	}
	
	/** Add prefixpath to fp-tree */
	public void addPrefixPath(List<F2GNodeTID> prefixPath, Map<Integer, Integer> mapSupportBeta, int minsup) {
		int pathCount = prefixPath.get(0).counter; //support
		F2GNodeTID currentNode = root;
		
		for(int i=prefixPath.size()-1; i >=1; i--){ // item in backward order (ignore 1st element) 
			F2GNodeTID pathItem = prefixPath.get(i);
			if(mapSupportBeta.get(pathItem.itemID) < minsup) continue;
			
			F2GNodeTID child = null;
			for(F2GNodeTID childI : currentNode.childs) if(childI.itemID == pathItem.itemID) child = childI; 
			if(child == null){ // there is no node, we create a new one
				F2GNodeTID newNode = new F2GNodeTID();
				newNode.itemID = pathItem.itemID;
				newNode.parent = currentNode;
				newNode.counter = pathCount;  // SPECIAL 
				currentNode.childs.add(newNode);
				currentNode = newNode;
				
				// update the header table
				F2GNodeTID headernode = mapItemNodes.get(pathItem.itemID);
				if(headernode != null){
					while(headernode.nodeLink != null) headernode = headernode.nodeLink;
					headernode.nodeLink  = newNode;
				} else mapItemNodes.put(pathItem.itemID, newNode);
			} else { 
				child.counter += pathCount;
				currentNode = child; //child.transactions.addAll(prefixPath.get(0).transactions);
			}
		}
		currentNode.transactions.addAll(prefixPath.get(0).transactions);
	}

	
	/*********************/
	/*** CREATE HEADER ***/
	/*********************/	

	/** Create list of items in the header table */
	public void createHeaderList(final Map<Integer, Integer> mapSupport) {
		headerList =  new ArrayList<Integer>(mapItemNodes.keySet());
		Collections.sort(headerList, new Comparator<Integer>(){
			public int compare(Integer id1, Integer id2){
				int compare = mapSupport.get(id2) - mapSupport.get(id1);
				if(compare ==0) return (id1 - id2); // lexical ordering
				return compare;
			}
		});
	}
	public void createHeaderList(final Map<Integer, Integer> mapSupport, final List<Integer> headerList2) {
		headerList =  new ArrayList<Integer>(mapItemNodes.keySet());
		Collections.sort(headerList, new Comparator<Integer>(){
			public int compare(Integer id1, Integer id2){
				return (headerList2.indexOf(id1) - headerList2.indexOf(id2));
			}
		});
	}

	
	/*******************/
	/*** AUX METHODS ***/
	/*******************/	

	public String toString(){
		return nodeToString(root,0); 
	}
	private String nodeToString(F2GNodeTID node,int counter) {
		StringBuffer result = new StringBuffer("");
		int j=counter;
		while(j-->0) result.append("-");
		result.append("("+node.itemID+":"+node.counter+")"+node.transactions.toString()+"->");
		F2GNodeTID xnode = node.nodeLink; 
		while(xnode!=null){
			result.append(xnode.itemID+",");
			xnode = xnode.nodeLink;
		}
		result.append("\n");
		for(F2GNodeTID inode : node.childs) result.append(nodeToString(inode,counter+1));
		return result.toString();
	}
}
