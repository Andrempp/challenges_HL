package bicpam.pminer.spm.algo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class represents a projected database.
 * A projected database is a list of pseudoSequences (projected sequences).
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
public class PseudoSequenceDatabase {
	
	private List<PseudoSequence> pseudoSequences = new ArrayList<PseudoSequence>();

	
	public List<PseudoSequence> getPseudoSequences(){
		return pseudoSequences;
	}
	
	//----
	
	public void printContext(){
		System.out.println(toString());
	}
	
	public String toString(){
		StringBuffer r = new StringBuffer("==  CONTEXT ==\n");
		for(PseudoSequence sequence : pseudoSequences){ // pour chaque objet
			r.append(sequence.getId());
			r.append(":  ");
			r.append(sequence.toString());
			r.append('\n');
		}
		return r.toString();
	}
	
	public int size(){
		return pseudoSequences.size();
	}

	public Set<Integer> getSequenceIDs() {
		Set<Integer> ensemble = new HashSet<Integer>();
		for(PseudoSequence sequence : getPseudoSequences()){
			ensemble.add(sequence.getId());
		}
		return ensemble;
	}
	
	public void addSequence(PseudoSequence newSequence) {
		pseudoSequences.add(newSequence);
		
	}
}
