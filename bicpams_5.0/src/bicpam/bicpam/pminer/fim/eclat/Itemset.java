package bicpam.pminer.fim.eclat;

import java.text.DecimalFormat;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;

/**
 * This class represents an itemset (a set of items)
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
public class Itemset {
	private final Set<Integer> items = new HashSet<Integer>(); // ordered
	private Set<Integer> tidset = new HashSet<Integer>();
	boolean maximal = true;

	public Itemset() {
	}

	public double getRelativeSupport(int nbObject) {
		return ((double) tidset.size()) / ((double) nbObject);
	}

	public String getSupportRelatifFormatted(int nbObject) {
		double frequence = getRelativeSupport(nbObject);
		DecimalFormat format = new DecimalFormat();
		format.setMinimumFractionDigits(0);
		format.setMaximumFractionDigits(2);
		return format.format(frequence);
	}

	public int getAbsoluteSupport() {
		return tidset.size();
	}

	// APRIORI
	public void increaseTransactionCount(int transactionId) {
		tidset.add(transactionId);
	}

	public void addItem(Integer value) {
		items.add(value);
	}

	public Set<Integer> getItems() {
		return items;
	}

	public void print() {
		System.out.print(toString());
	}

	public String toString() {
		StringBuffer r = new StringBuffer();
		for (Integer attribute : items) {

			r.append(attribute.toString());
			
			r.append(' ');
		}
		if (maximal) {
			r.append(" M ");
		}
		// r.append("[ <transactionIDs: " );
		// for(Integer id : transactionsIds){
		// r.append(" " + id );
		// }
		// r.append("]  ");
		return r.toString();
	}

	public boolean isEqualTo(Itemset itemset2) {
		if (items.size() != itemset2.items.size()) {
			return false;
		}
		return items.containsAll(itemset2.items);
	}

	public void setTransactioncount(Set<Integer> listTransactionIds) {
		this.tidset = listTransactionIds;
	}

	public int size() {
		return items.size();
	}

	public Itemset union(Itemset itemset) {

		Itemset union = new Itemset();
		union.getItems().addAll(items);
		union.getItems().addAll(itemset.getItems());

		return union;
	}

	public Set<Integer> getTransactionsIds() {
		return tidset;
	}
}
