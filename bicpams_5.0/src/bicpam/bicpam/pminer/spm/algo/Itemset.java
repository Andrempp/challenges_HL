package bicpam.pminer.spm.algo;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class represents an itemset from a sequence from a sequence database.
 * The itemset can thus have a timestamp.
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
public class Itemset{

	private final List<Integer> items = new ArrayList<Integer>(); // ordered list.
	
	public Itemset(Integer item, long timestamp){
		addItem(item);
	}
	
	public Itemset(Integer item){
		addItem(item);
	}

	public Itemset(){
	}

	public void addItem(Integer value){
			items.add(value);
	}
	
	public List<Integer> getItems(){
		return items;
	}
	
	public Integer get(int index){
		return items.get(index);
	}
	
	public void print(){
		System.out.print(toString());
	}
	
	public String toString(){
		StringBuffer r = new StringBuffer ();
		for(Integer attribute : items){
			r.append(attribute.toString());
			r.append(' ');
		}
		return r.toString();
	}

	public Itemset cloneItemSetMinusItems(Map<Integer, Set<Integer>> mapSequenceID, double minsuppRelatif) {
		Itemset itemset = new Itemset();
		for(Integer item : items){
			if(mapSequenceID.get(item).size() >= minsuppRelatif){
				itemset.addItem(item);
			}
		}
		return itemset;
	}
	
	public Itemset cloneItemSet(){
		Itemset itemset = new Itemset();
		itemset.getItems().addAll(items);
		return itemset;
	}
	
	public int size(){
		return items.size();
	}
}
