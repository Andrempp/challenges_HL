package bicpam.pminer.fim.eclat;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a set of itemsets.
 * They are ordered by size. For example, level 1 means itemsets of size 1 (that contains 1 item).
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
public class Itemsets {
	private final List<List<Itemset>> levels = new ArrayList<List<Itemset>>();  // itemset class par taille
	private int itemsetsCount=0;
	private final String name;
	
	public Itemsets(String name){
		this.name = name;
		levels.add(new ArrayList<Itemset>()); // We create an empty level 0 by default.
	}
	
	public void printItemsets(int nbObject){
		System.out.println(" ------- " + name + " -------");
		int patternCount=0;
		int levelCount=0;
		for(List<Itemset> level : levels){
			System.out.println("  L" + levelCount + " ");
			for(Itemset itemset : level){
				System.out.print(" Maximal?" + itemset.maximal);
				System.out.print("  pattern " + patternCount + ":  ");
				itemset.print();
				System.out.print("support :  " + itemset.getSupportRelatifFormatted(nbObject));
				System.out.print("  (" + itemset.getAbsoluteSupport() + "/" + nbObject + " transactions)");
				patternCount++;
				System.out.println("");
			}
			levelCount++;
		}
		System.out.println(" --------------------------------");
	}
		
	public void addItemset(Itemset itemset, int k){
		while(levels.size() <= k){
			levels.add(new ArrayList<Itemset>());
		}
		levels.get(k).add(itemset);
		itemsetsCount++;
	}

	public List<List<Itemset>> getLevels() {
		return levels;
	}

	public int getItemsetsCount() {
		return itemsetsCount;
	}

	public void saveToFile(String filepath, int databaseSize) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(filepath)); 
		for(List<Itemset> itemsetLevel : getLevels()){
			for(Itemset itemset : itemsetLevel){
				StringBuffer temp = new StringBuffer();
				temp.append(itemset.toString());
				temp.append(" ");
				temp.append(itemset.getAbsoluteSupport());
				temp.append(" ");
				temp.append(itemset.getRelativeSupport(databaseSize));
				temp.append("\n");
				writer.write(temp.toString());
			}
		}
			
		writer.flush();
		writer.close();
	}
	
}
