package bicpam.pminer.fim.eclat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * This class represents an ITNode
 * 
 * Copyright (c) 2008-2012 Philippe Fournier-Viger
 * 
 * This file is part of the SPMF DATA MINING SOFTWARE
 * (http://www.philippe-fournier-viger.com/spmf).
 * 
 * SPMF is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * SPMF is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * SPMF. If not, see <http://www.gnu.org/licenses/>.
 */
public class ITNode {

	private Itemset itemset;
	private Set<Integer> tidset;

	private ITNode parent = null;
	private List<ITNode> childNodes = new ArrayList<ITNode>();

	public ITNode(Itemset itemset) {
		this.itemset = itemset;
	}

	public Itemset getItemset() {
		return itemset;
	}

	public void setItemset(Itemset itemset) {
		this.itemset = itemset;
	}

	public Set<Integer> getTidset() {
		return tidset;
	}

	public void setTidset(Set<Integer> tidset) {
		this.tidset = tidset;
	}

	public List<ITNode> getChildNodes() {
		return childNodes;
	}

	public void setChildNodes(List<ITNode> childNodes) {
		this.childNodes = childNodes;
	}

	public ITNode getParent() {
		return parent;
	}

	public void setParent(ITNode parent) {
		this.parent = parent;
	}

	// for charm
	public void replaceInChildren(Itemset replacement) {
		for (ITNode node : getChildNodes()) {
			Itemset itemset = node.getItemset();
			// could be optimized... not very efficient..
			// in particular, instead of using a list in itemset, we could use a
			// set.
			for (Integer item : replacement.getItems()) {
				if (!itemset.getItems().contains(item)) {
					itemset.addItem(item);
				}
			}
			node.replaceInChildren(replacement);
		}
	}

}
