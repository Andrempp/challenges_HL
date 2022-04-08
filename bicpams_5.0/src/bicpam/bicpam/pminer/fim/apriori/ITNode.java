package bicpam.pminer.fim.apriori;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Set;

/**
 * This class represents an ITNode
 */
public class ITNode {
	
	SetItemset itemsetObject = new SetItemset();
	private ITNode parent = null;
	private List<ITNode> childNodes = new ArrayList<ITNode>();
	
	public int size(){
		return itemsetObject.cardinality;
	}
	

	public double getRelativeSupport(int nbObject) {
		return ((double) itemsetObject.cardinality) / ((double) nbObject);
	}
	
	public ITNode(Set<Integer> itemset){
		this.itemsetObject.itemset = itemset;
	}

	public Set<Integer> getItemset() {
		return itemsetObject.itemset;
	}

	public BitSet getTidset() {
		return itemsetObject.tidset;
	}

	public void setTidset(BitSet tidset, int cardinality) {
		this.itemsetObject.tidset = tidset;
		this.itemsetObject.cardinality = cardinality;
	}

	public List<ITNode> getChildNodes() {
		return childNodes;
	}

	public ITNode getParent() {
		return parent;
	}

	public void setParent(ITNode parent) {
		this.parent = parent;
	}
	
	// for charm
	public void replaceInChildren(Set<Integer> replacement) {
		for(ITNode node : getChildNodes()){
			Set<Integer> itemset  = node.getItemset();
			for(Integer item : replacement){
				if(!itemset.contains(item)){
					itemset.add(item);
				}
			}
			node.replaceInChildren(replacement);
		}
	}

	public void setItemset(Set<Integer> union) {
		this.itemsetObject.itemset = union;
	}
}
