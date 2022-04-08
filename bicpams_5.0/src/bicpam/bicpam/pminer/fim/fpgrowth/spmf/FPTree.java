package bicpam.pminer.fim.fpgrowth.spmf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * This is an implementation of a FPTree.
 *
 * Copyright (c) 2008-2012 Philippe Fournier-Viger
 * 
 * This file is part of the SPMF DATA MINING SOFTWARE
 * (http://www.philippe-fournier-viger.com/spmf).
 *
 * SPMF is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SPMF is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SPMF.  If not, see <http://www.gnu.org/licenses/>.
 */
public class FPTree {
	
	List<Integer> headerList = null; // List of items in the header table
	Map<Integer, FPNodeTID> mapItemNodes = new HashMap<Integer, FPNodeTID>(); // Pairs (item, frequency) of the header table
	FPNodeTID root = new FPNodeTID(); // root of the tree

	FPTree(){}

	/**
	 * Method for adding a transaction to the fp-tree (for the initial construction of the FP-Tree).
	 * @param transaction
	 */
	public void addTransaction(Itemset transaction, int tid) {
		if(transaction.size()==0) return;
		FPNodeTID currentNode = root;
		Integer lastItem = transaction.get(transaction.size()-1);
		for(Integer item : transaction.getItems()){
			
			FPNodeTID child = currentNode.getChildWithID(item); 
			if(child == null){ // there is no node, we create a new one
				FPNodeTID newNode = new FPNodeTID();
				newNode.itemID = item;
				newNode.parent = currentNode;
				
				currentNode.childs.add(newNode); // we link the new node to its parrent
				currentNode = newNode; // take this node as the current node for iteration
				
				// We update the header table
				FPNodeTID headernode = mapItemNodes.get(item);
				if(headernode == null) mapItemNodes.put(item, newNode);
				else { 
					// we find the last node with this id
					while(headernode.nodeLink != null)
						headernode = headernode.nodeLink;
					headernode.nodeLink  = newNode;
				}	
			} else { 
				child.counter++; // there is a node already, we update it
				currentNode = child;
			}
			
			if(item == lastItem) currentNode.transactions.add(tid); 
		}
	}
	
	/**
	 * Method for adding a prefixpath to a fp-tree.
	 * @param prefixPath  The prefix path
	 * @param mapSupportBeta  The frequencies of items in the prefixpaths
	 * @param relativeMinsupp
	 */
	public void addPrefixPath(List<FPNodeTID> prefixPath, Map<Integer, Integer> mapSupportBeta, int relativeMinsupp) {
		int pathCount = prefixPath.get(0).counter; //support
		FPNodeTID currentNode = root;
		
		for(int i= prefixPath.size()-1; i >=1; i--){ // item in backward order (ignore 1st element) 
			FPNodeTID pathItem = prefixPath.get(i);
			//System.out.print(pathItem.itemID+",");
			// if item is not frequent we skip it
			if(mapSupportBeta.get(pathItem.itemID) < relativeMinsupp) continue;
			
			FPNodeTID child = currentNode.getChildWithID(pathItem.itemID); // node already in the FP-Tree?
			if(child == null){ // there is no node, we create a new one
				FPNodeTID newNode = new FPNodeTID();
				newNode.itemID = pathItem.itemID;
				newNode.parent = currentNode;
				newNode.counter = pathCount;  // SPECIAL 
				currentNode.childs.add(newNode);
				currentNode = newNode;
				
				// We update the header table.
				FPNodeTID headernode = mapItemNodes.get(pathItem.itemID);
				if(headernode == null) mapItemNodes.put(pathItem.itemID, newNode);
				else {
					while(headernode.nodeLink != null)
						headernode = headernode.nodeLink;
					headernode.nodeLink  = newNode;
				}	
			} else { 
				child.counter += pathCount;
				//child.transactions.addAll(prefixPath.get(0).transactions);
				currentNode = child;
			}
		}
		//System.out.println();
		currentNode.transactions.addAll(prefixPath.get(0).transactions);
	}

	/**
	 * Method for creating the list of items in the header table, in descending order of frequency.
	 * @param mapSupport the frequencies of each item.
	 */
	public void createHeaderList(final Map<Integer, Integer> mapSupport) {
		headerList =  new ArrayList<Integer>(mapItemNodes.keySet());
		Collections.sort(headerList, new Comparator<Integer>(){
			public int compare(Integer id1, Integer id2){
				int compare = mapSupport.get(id2) - mapSupport.get(id1);
				if(compare ==0){ // if the same frequency, we check the lexical ordering!
					return (id1 - id2);
				}
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
	
	
	public String toString(){
		return nodeToString(root,0); 
	}

	private String nodeToString(FPNodeTID node,int counter) {
		StringBuffer result = new StringBuffer("");
		int j=counter;
		while(j-->0) result.append("-");
		result.append("("+node.itemID+":"+node.counter+")"+node.transactions.toString()+"->");
		FPNodeTID xnode = node.nodeLink; 
		while(xnode!=null){
			result.append(xnode.itemID+",");
			xnode = xnode.nodeLink;
		}
		result.append("\n");
		for(FPNodeTID inode : node.childs)
			result.append(nodeToString(inode,counter+1));
		return result.toString();
	}

}
