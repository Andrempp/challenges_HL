package bicpam.pminer.fim.apriori;

/**
 * This class is for creating a triangular matrix of integers.
 * All the elements in the matrix are initialized to zero.
 * For example: 
 * 0: [0, 0, 0, 0]
 * 1: [0, 0, 0]
 * 2: [0, 0]
 * 3: [0]
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
public class TriangularMatrix {
	
	private int[][] matrix;
	private int elementCount;

	public TriangularMatrix(int elementCount){
		this.elementCount = elementCount;
		matrix = new int[elementCount-1][]; // -1 cause we want it shorter of 1 element
		for(int i=0; i< elementCount-1; i++){ // -1 cause we want it shorter of 1 element
		   // allocate an array for each row
			matrix[i] = new int[elementCount - i -1];
		}
	}
	
	public int get(int i, int j){
		return matrix[i][j];
	}
	
	/**
	 * for testing!
	 */
	public static void main(String[] args) {
		TriangularMatrix a = new TriangularMatrix(5);

		System.out.println(a.toString());
		// AB, AD, AE, BD, BE, DE
		a.incrementCount(1, 0);
		System.out.println(a.toString());
		a.incrementCount(1, 4);
		a.incrementCount(1, 3);
		a.incrementCount(2, 4);
		a.incrementCount(2, 4);
		a.incrementCount(4, 3);
		System.out.println(a.toString());
		a.incrementCount(0, 2);
		a.incrementCount(0, 3);
		a.incrementCount(0, 4);
		System.out.println(a.toString());
	}
	
	/**
	 * for testing!
	 */
	public String toString() {
		System.out.println("Element count = " + elementCount);
		StringBuffer temp = new StringBuffer();
		for (int i = 0; i < matrix.length; i++) {
			temp.append(i);
			temp.append(": ");
			for (int j = 0; j < matrix[i].length; j++) {
				temp.append(matrix[i][j]);
				temp.append(" ");
			}
			temp.append("\n");
		}
		return temp.toString();
	}

	public void incrementCount(int id, int id2) {
		if(id2 < id){
			incrementCount(id2, id);  // so that id is always smaller than id2
		}else{
			matrix[elementCount - id2 -1][id]++;
		}
		
	}
	
	public int getSupportForItems(int id, int id2){
		if(id2 < id){
			return getSupportForItems(id2, id);  // so that id is always smaller than id2
		}else{
			return matrix[elementCount - id2 -1][id];
		}
	}
}
