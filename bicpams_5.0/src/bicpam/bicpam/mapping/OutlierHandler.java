package bicpam.mapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import domain.Dataset;
import bicpam.mapping.Itemizer.OutlierizationCriteria;

/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class OutlierHandler {

	public static Dataset run(Dataset data, OutlierizationCriteria criteria, double outlierPercentage) {
		if(outlierPercentage==0) return data;

		List<List<Integer>> outliers = new ArrayList<List<Integer>>();
		switch(criteria) {
			case Overall :
				List<Double> vector = new ArrayList<Double>();
				for(int i=0, l1=data.indexes.size(); i<l1; i++)
					for(int j=0, l2=data.indexes.get(i).size(); j<l2; j++)
						vector.add(data.scores.get(i).get(j));
				Collections.sort(vector);
				int nrOutliers = (int) (vector.size()*outlierPercentage);
				double upperThreshold = vector.get(vector.size()-nrOutliers);
				for(int i=0, l1=data.indexes.size(); i<l1; i++){
					outliers.add(new ArrayList<Integer>());
					for(int j=0, l2=data.indexes.get(i).size(); j<l2; j++)
						if(data.scores.get(i).get(j)>upperThreshold) outliers.get(i).add(data.indexes.get(i).get(j));
				}
				break;
			case Column :
				/*nrOutliers = (int) (matrix.length*outlierPercentage*0.5);
				DoubleArrayIndexComparator comparator;
				Integer[] indexes;
				System.out.println(outlierPercentage + "," + nrOutliers);
				matrix = BicMath.reshape(matrix,matrix.length,l);
				for(int j=0; j<l; j++){
					comparator = new DoubleArrayIndexComparator(matrix[j]);
					indexes = comparator.createIndexArray();
					Arrays.sort(indexes, comparator);					
					for(int k=0; k<nrOutliers; k++){
						outliers[indexes[k]][j] = true;
						outliers[indexes[indexes.length-k-1]][j] = true;
					}
				}*/					
				break;
			case Row :
				/*nrOutliers = (int) (l*outlierPercentage*0.5);
				for(int i=0; i<matrix.length; i++){
					comparator = new DoubleArrayIndexComparator(matrix[i]);
					indexes = comparator.createIndexArray();
					Arrays.sort(indexes, comparator);		
					for(int k=0; k<nrOutliers; k++){
						outliers[i][indexes[k]] = true;
						outliers[i][indexes[indexes.length-k-1]] = true;
					}
				}*/
				break;
		}
		data.outliers = outliers;
		return data;
	}
}
