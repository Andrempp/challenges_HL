package bicpam.gui;

import java.util.List;

import utils.BicMath;
import domain.Bicluster;
import domain.Biclusters;
import domain.Dataset;

public class ExportDisplay {

	public static String bicsToBicOverlapper(Biclusters bics, List<String> rows, List<String> columns) {
		StringBuffer result = new StringBuffer(bics.size()+"\nBicPAMS\n");
		int k=0;
		for(Bicluster bic : bics.getBiclusters()){
			result.append("B"+(k++)+":"+bic.numRows()+"\t"+bic.numColumns()+"\n");
			for(Integer row : bic.rows) result.append(rows.get(row)+"\t");
			result.setCharAt(result.length()-1, '\n');
			for(Integer col : bic.columns) result.append(columns.get(col)+"\t");
			result.setCharAt(result.length()-1, '\n');
		}
		return result.toString();
	}

	public static String bicsToBicat(Biclusters bics, List<String> rows, List<String> columns) {
		StringBuffer result = new StringBuffer();
		for(Bicluster bic : bics.getBiclusters()){
			result.append(bic.numRows()+" "+bic.numColumns()+"\n");
			for(Integer row : bic.rows) result.append(rows.get(row)+" ");
			result.setCharAt(result.length()-1, '\n');
			for(Integer col : bic.columns) result.append(columns.get(col)+" ");
			result.setCharAt(result.length()-1, '\n');
		}
		return result.toString();
	}

	public static String bicsToExpander(Biclusters bics, Dataset data, List<String> columns) {
		StringBuffer result = new StringBuffer("[Bick]\n");
		int k=0;
		for(Bicluster bic : bics.getBiclusters()){
			double[][] matrix = data.getRealBicluster(bic.columns, bic.rows);
			result.append("Bicluster_"+(k++)+"\t"+BicMath.mean(matrix)+"\n");
		}
		result.append("[Bicd]\n");
		k=0;
		for(Bicluster bic : bics.getBiclusters()){
			for(Integer row : bic.rows) result.append("Bicluster_"+k+"\t1\t"+data.rows.get(row)+"\n");
			for(Integer col : bic.columns) result.append("Bicluster_"+k+"\t0\t"+columns.get(col)+"\n");
			k++;
		}
		return result.toString();	
	}
}
