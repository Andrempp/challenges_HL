package bicpam.pminer.spm.algo;

import java.util.HashSet;
import java.util.Set;

/**
 * This class is used by PrefixSpanItemIntervals. It represents, based on Hirate & Yamana
 * a pair of an (1) Item  and (2) a time interval. It is used for calculating the support
 * of item in a database. It contains some other information too.
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
class Pair{
	private final boolean postfix; // is cut at left
	private final boolean prefix; // is cut at right
	private final Integer item;
	
	// List of the its of all the patterns that contains this one.
	private Set<Integer> sequencesID = new HashSet<Integer>();
	
	Pair(long timestamp, boolean prefix, boolean postfix, Integer item){
		this.postfix = postfix;
		this.prefix  = prefix;
		this.item = item;
	}
	
	Pair(boolean postfix, Integer item){
		this.postfix = postfix;
		this.prefix = true;
		this.item = item;
	}

	// for prefixspan
	Pair( boolean prefix, boolean postfix, Integer item){
		this.postfix = postfix;
		this.prefix = prefix;
		this.item = item;
	}
	
	public boolean equals(Object object){
		Pair paire = (Pair) object;
		if((paire.postfix == this.postfix) 
				&& (paire.prefix == this.prefix)
				&& (paire.item.equals(this.item))){
			return true;
		}
		return false;
	}
	
	public int hashCode()
	{// Ex: 127333,P,X,1  127333,N,Z,2
		StringBuffer r = new StringBuffer();
		r.append((postfix ? 'P' : 'N')); // the letters here have no meanings. they are just used for the hashcode
		r.append((prefix ? 'X' : 'Z')); // the letters here have no meanings. they are just used for the hashcode
		r.append(item);
		return r.toString().hashCode();
	}

	public boolean isPostfix() {
		return postfix;
	}

	public Integer getItem() {
		return item;
	}

	public int getCount() {
		return sequencesID.size();
	}		

	public Set<Integer> getSequencesID() {
		return sequencesID;
	}

	public void setSequencesID(Set<Integer> sequencesID) {
		this.sequencesID = sequencesID;
	}

	public boolean isPrefix() {
		return prefix;
	}
}