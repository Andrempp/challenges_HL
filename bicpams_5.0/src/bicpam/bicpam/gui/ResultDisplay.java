package bicpam.gui;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.List;

import domain.Bicluster;
import domain.Biclusters;

public class ResultDisplay {
	
	public static String toSummaryTable(Biclusters bics) {
		return bics.size()+" biclusters with sizes:<br>{"+bics.toShortString().replace("\n", "<br>")+"}";
	}
	
	public static String toIndexesTable(Biclusters bics) {
		//String disc = bics.size()>100 ? "displaying the larger 100 biclusters (out of "+bics.size()+")": "";
		StringBuffer res = new StringBuffer("<table>"); //"#"+biclusters.size()+"\r\n"
		int i=0;
		for(Bicluster bic : bics.getBiclusters()){
			//if(++i>100) break;
			res.append("<tr><td><b>("+bic.columns.size()+","+bic.rows.size()+")</b></td><td>");
			res.append("<b>X</b>="+bic.rows+"<br>"+"<b>Y</b>="+bic.columns+"<br><br></td></tr>");
		}
		return res.toString()+"</table>";
	}
	
	public static String toNamesTable(List<String> rows, List<String> columns, Biclusters bics) {
		//String disc = bics.size()>100 ? "displaying the larger 100 biclusters (out of "+bics.size()+")": "";
		StringBuffer res = new StringBuffer("<table>"); //"#"+biclusters.size()+"\r\n"
		int k=0;
		for(Bicluster bic : bics.getBiclusters()){
			//if(++k>100) break;
			res.append("<tr><td><b>("+bic.columns.size()+","+bic.rows.size()+")</b></td><td><b>X</b>=[");
			for(int i : bic.rows) res.append(rows.get(i)+", ");
			res.append("]<br>"+"<b>Y</b>=[");
			for(int i : bic.columns) res.append(columns.get(i)+", ");
			res.append("]<br><br></td></tr>");
		}
		return res.toString().replace(", ]","]")+"</table>";
	}
	
	public static void writeFile(String name, String content, String path) throws Exception {
		FileWriter fstream = new FileWriter(path+".htm");
		BufferedWriter out = new BufferedWriter(fstream);
		out.write(content);
		out.close();
	}
}
