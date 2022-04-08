package bicpam.mapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import utils.BicMath;
import utils.BicPrinting;
import bicpam.bicminer.BiclusterMiner.Orientation;
import bicpam.mapping.TFilter.FilterCriteria;
import domain.Dataset;


/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class TFilter {
	
	public enum FilterCriteria { HighStd, HighDifferential } 
	public enum RemoveCriteria { Percentage, CutoffValue } 

	public static Dataset run(Dataset data, FilterCriteria highdifferential) {
		return run(data,Orientation.PatternOnRows,highdifferential);
	}
	public static Dataset run(Dataset data, Orientation orientation, FilterCriteria highdifferential) {
		double perc = findPercentage(orientation.equals(Orientation.PatternOnRows) ? data.columns.size() : data.rows.size());
		System.out.println(">> "+data.rows.size()+" , "+data.columns.size());
		Dataset mydata = run(data,orientation,highdifferential,RemoveCriteria.Percentage,perc); 
		System.out.println(">> "+data.rows.size()+" , "+data.columns.size());
		return mydata;
	}

	private static double findPercentage(int size) {
		double a=((((double)size)-20.0)/80.0);
		if(size<20) return Math.min(1,10.0/(double)size);
		if(size<100) return 0.5-((((double)size)-20.0)/80.0)*0.3;
		if(size<300) return 0.2-((((double)size)-100.0)/200.0)*0.1;
		if(size<1000) return 0.1-((((double)size)-300.0)/700.0)*0.05;
		return 200.0/(double)size;
	}

	public static Dataset run(Dataset dataset, Orientation orientation, FilterCriteria filterCriteria, RemoveCriteria remCriteria, double remValue) {
		if(remValue==1) return dataset;
		remValue=1.0-remValue;
		int nrColumns = orientation.equals(Orientation.PatternOnRows) ? dataset.columns.size() : dataset.rows.size();
		double[] result = new double[nrColumns];
		
		switch(filterCriteria) {
			case HighStd :
				for(int j=0; j<nrColumns; j++){
					List<Double> values = new ArrayList<Double>();

					if(orientation.equals(Orientation.PatternOnRows)){
						for(int i=0, l=dataset.rows.size(); i<l; i++)
							if(dataset.indexes.get(i).contains(j)) values.add(dataset.scores.get(i).get(dataset.indexes.get(i).indexOf(j)));
					} else {
						for(int i=0, l=dataset.columns.size(); i<l; i++)
							if(dataset.indexes.get(j).contains(i)) values.add(dataset.scores.get(j).get(dataset.indexes.get(j).indexOf(i)));
					}
					result[j] = BicMath.std(values);
				}
				break;
			case HighDifferential :
				for(int j=0; j<nrColumns; j++){
					double sum = 0;
					
					if(orientation.equals(Orientation.PatternOnRows)){
						for(int i=0, l=dataset.rows.size(); i<l; i++)
							if(dataset.indexes.get(i).contains(j)) sum+=Math.abs(dataset.scores.get(i).get(dataset.indexes.get(i).indexOf(j)));
					} else {
						for(int i=0, l=dataset.columns.size(); i<l; i++)
							if(dataset.indexes.get(j).contains(i)) sum+=Math.abs(dataset.scores.get(j).get(dataset.indexes.get(j).indexOf(i)));
					}
					result[j] = sum;
				}
				//System.out.println("Score sum:"+BicPrinting.plot(result));
				break;
		}
		List<Integer> removals = new ArrayList<Integer>();
		if(remCriteria.equals(RemoveCriteria.Percentage)) {
			double[] sorted = Arrays.copyOf(result, result.length);
			Arrays.sort(sorted);
			remValue = sorted[(int)(remValue*(double)result.length)];
		} 
		for(int j=nrColumns-1; j>=0; j--) if(result[j] < remValue) removals.add(j);
		//System.out.println("Removals:"+removals);
		
		List<String> cnew = new ArrayList<String>();
		if(orientation.equals(Orientation.PatternOnRows)){
			for(int j=0; j<nrColumns; j++) 
				if(!removals.contains(j)) cnew.add(dataset.columns.get(j));
			dataset.originalColumns = dataset.columns;
			dataset.columns = cnew;
			System.out.println(remValue+"% #originalCols:"+dataset.originalColumns.size()+" #finalCols:"+dataset.columns.size());
		} else {
			for(int j=0; j<nrColumns; j++) 
				if(!removals.contains(j)) cnew.add(dataset.rows.get(j));
			dataset.rows = cnew;
			System.out.println(remValue+"% #originalRows:"+nrColumns+" #finalRows:"+dataset.rows.size());
		}

		/*System.out.println("Indexes 0:"+dataset.indexes.get(0));
		System.out.println("Scores 0:"+dataset.scores.get(0));
		System.out.println("Indexes 1:"+dataset.indexes.get(1));
		System.out.println("Scores 1:"+dataset.scores.get(1));*/
		if(orientation.equals(Orientation.PatternOnRows)){
			for(Integer rem : removals){
				for(int i=0, l=dataset.rows.size(); i<l; i++){
					if(dataset.indexes.get(i).contains(rem)){
						int index=dataset.indexes.get(i).indexOf(rem);
						dataset.indexes.get(i).remove(index);
						dataset.scores.get(i).remove(index);
					}
				}
			} 
		} else {
			//System.out.println("Removals="+removals);
			for(Integer index : removals){
				dataset.indexes.remove((int)index);
				dataset.scores.remove((int)index);
			}
		}
		/*System.out.println("Indexes 0:"+dataset.indexes.get(0));
		System.out.println("Scores 0:"+dataset.scores.get(0));
		System.out.println("Indexes 1:"+dataset.indexes.get(1));
		System.out.println("Scores 1:"+dataset.scores.get(1));*/
		return dataset;
	}

}
