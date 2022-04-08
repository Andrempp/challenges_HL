package bicpam.pminer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import domain.Bicluster;
import domain.Biclusters;
import utils.BicResult;

/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public final class PartitioningUtils {

	public static boolean print = false;
	
	public static List<List<List<Integer>>> partition(List<List<Integer>> dataset, int nrLabels, int limitColumns) {
		int nrColumns = 0;
		if(print) BicResult.println("Data:"+dataset);
		for(Collection<Integer> row : dataset)
			if(row.isEmpty()) continue;
			else nrColumns=Math.max(nrColumns,row.size()-1);
		nrColumns = nrColumns/nrLabels + 1;
		int partitions = nrColumns/limitColumns + 1;
		List<List<List<Integer>>> datasets = new ArrayList<List<List<Integer>>>();
		int colsPartition = nrColumns/partitions;
		if(print) BicResult.println("Partitions: "+partitions+"\nCols per partition: "+colsPartition+"\nNr labels: "+nrLabels);
		for(int k=0; k<partitions; k++){
			List<List<Integer>> entry = new ArrayList<List<Integer>>();
			for(int i=0, l=dataset.size(); i<l; i++) entry.add(new ArrayList<Integer>());
			int min=k*colsPartition*nrLabels;
			int max=(k+1==partitions) ? nrColumns*nrLabels+1 : (k+1)*colsPartition*nrLabels;
			if(print) BicResult.println("k="+k+" ("+min+","+max+")");
			for(int i=0, l=dataset.size(); i<l; i++){
				for(Integer val : dataset.get(i)) 
					if(val>=min && val<max) entry.get(i).add(val);
			}
			if(print) BicResult.println(">"+entry);
			datasets.add(entry);
		}
		return datasets;
	}

	public static Biclusters merge(Biclusters biclusters, int minrows) {
		if(print) BicResult.println(biclusters.toString());
		if(!biclusters.hasBiclusters()) return biclusters;			
		return mergeSimpleBiclusters(biclusters,minrows);
	}
	public static Biclusters mergeSimpleBiclusters(Biclusters bics, int minsup) {
		minsup = (int)(0.7*minsup);
		//System.out.println("MINSUP:"+minsup);
		int j=0;
		while(j++<1){
			Biclusters newbics = new Biclusters();
			for(int i1=0, l=bics.size(); i1<l; i1++){
				Bicluster bicA = bics.getBiclusters().get(i1);
				for(int i2=i1+1; i2<l; i2++){
					Bicluster bicB = bics.getBiclusters().get(i2);
					if(bicA.partition==bicB.partition) continue;
					SortedSet<Integer> rows = overlap(bicA.rows.iterator(),bicB.rows.iterator());
					if(rows.size()<minsup) continue;
					//if(rows.size()==0) System.out.println("WARNING:\n>"+bicA.rows+"\n>"+bicB.rows);
					SortedSet<Integer> cols = new TreeSet<Integer>();
					cols.addAll(bicA.columns);
					cols.addAll(bicB.columns);
					newbics.add(new Bicluster(rows,cols));
				}
			}
			bics.addAll(newbics);
		}
		return bics;
	}
	public static SortedSet<Integer> overlap(Iterator<Integer> it1, Iterator<Integer> it2) {
		SortedSet<Integer> over = new TreeSet<Integer>();
		//if(!it1.hasNext() || !it2.hasNext()) return over;
		int i1 = it1.next(), i2 = it2.next();
		while(true){
			if(i1<i2){
				if(it1.hasNext()) i1=it1.next();
				else break;
			} else if(i2<i1){
				if(it2.hasNext()) i2=it2.next();
				else break;
			} else {
				over.add(i1);
				if(!it1.hasNext()) break;
				else if(!it2.hasNext()) break;
				i1=it1.next();
				i2=it2.next();
			}
		}
		return over;
	}

}