package bicpam.mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import domain.Dataset;


/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public final class ItemMapper {

	public static List<List<Integer>> itemize(Dataset dataset){
		return itemize(dataset.intscores,dataset.indexes,dataset.nrLabels,dataset.symmetry,dataset.annotations);
	}
	public static List<List<Integer>> itemize(List<List<Integer>> data, List<List<Integer>> indexes, int nrLabels, boolean symmetry, List<List<Integer>> annotations){
		//System.out.println("ITEMIZE!");
		List<List<Integer>> result = new ArrayList<List<Integer>>();
		int shift = symmetry ? nrLabels/2 : 0;
		int adjustpos = (symmetry && (nrLabels%2==0)) ? -1 : 0;
		int nrRows = 0;
		for(int i=0, l1=indexes.size(); i<l1; i++){
			List<Integer> entry = new ArrayList<Integer>();
			int l2=indexes.get(i).size();
			for(int j=0; j<l2; j++){
				int index = indexes.get(i).get(j);
				nrRows = Math.max(nrRows, index+1);
				int value = data.get(i).get(j);
				entry.add(index*nrLabels+value+shift+(value>0 ? adjustpos : 0));
			}
			result.add(entry);
		}
		if(annotations!=null){
			for(int i=0, l1=indexes.size(); i<l1; i++){
				for(Integer annot : annotations.get(i)){
					indexes.get(i).add(nrRows);
					result.get(i).add(nrRows*nrLabels+annot);
				}
			}
		}
		//System.out.println("L0"+result.get(0));
		//System.out.println("L1"+result.get(1));
		return result;
	}
	
	public static int[] itemize(int[] values, int nrLabels, boolean symmetry) {
		int shift = symmetry ? nrLabels/2 : 0;
		int[] result = new int[values.length];
		int adjustpos = (symmetry && (nrLabels%2==0)) ? -1 : 0;
		for(int j=0, l=values.length; j<l; j++)
			result[j]=j*nrLabels+values[j]+shift+(values[j]>0 ? adjustpos : 0);
		return result;
	}

	public static List<List<Integer>> itemizeWithContext(Dataset dataset){
		
		/** A: GET VARIABLES */
		List<List<Integer>> result = new ArrayList<List<Integer>>();
		List<List<Integer>>  data = dataset.intscores,  cdata = dataset.context, indexes = dataset.indexes;
		
		int nrLabels = dataset.nrLabels * dataset.nrLabelsContext;
		System.out.println(nrLabels);
		
		/** B: APPLY MAPPING */
		for(int i=0; i<indexes.size(); i++){
			List<Integer> row = new ArrayList<Integer>(); 
			for(int j=0; j<indexes.get(i).size(); j++){
				int value = data.get(i).get(j), col = indexes.get(i).get(j);
				if(cdata.get(i).size()<=j) {
					j = indexes.get(i).indexOf(col);
				}
				int cvalue = cdata.get(i).get(j);
				row.add(col*nrLabels+value+cvalue*dataset.nrLabels);
				//if(i==1) System.out.println(col+":"+value+"|"+cvalue+"|"+res);
			}
			result.add(row);
		}
		return result;
	}

	public static Dataset remove(Dataset data, List<Integer> removals) {
		for(int i=0, l1=data.indexes.size(); i<l1; i++){
			//System.out.println(data.indexes.size()+"|"+data.scores.size());
			for(int j=data.indexes.get(i).size()-1; j>=0; j--){
				if(removals.contains(data.intscores.get(i).get(j))){
					data.indexes.get(i).remove(j);
					//if(data.scores!=null) data.scores.get(i).remove(j);
					data.intscores.get(i).remove(j);
				}
			}
		}
		return data;
	}

	public static Dataset preremove(Dataset data, List<Double> removals) {
		for(int i=0, l1=data.indexes.size(); i<l1; i++){
			for(int j=data.indexes.get(i).size()-1; j>=0; j--){
				if(removals.contains(data.scores.get(i).get(j))){
						data.indexes.get(i).remove(j);
						data.scores.get(i).remove(j);
				}
			}
		}
		return data;
	}

}
