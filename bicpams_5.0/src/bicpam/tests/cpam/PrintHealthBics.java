package tests.cpam;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import utils.BicResult;

/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class PrintHealthBics {

  public static void main(String[] args) throws Exception {
	String file = "/data/others/HungarianStatlog.txt";//BreastLung
	BufferedReader br= new BufferedReader(new FileReader(new File("").getAbsolutePath() + file));
	String line, result="\\mbox{\\hspace{3cm}fold k=1} & & \\mbox{\\hspace{3cm}fold k=2}\\\\\n\\hspace{1cm}class 0 & \\hspace{1cm}class 1 & \\hspace{1cm}class 0 & \\hspace{1cm}class 1\\\\\\midrule\n";
	while ((line=br.readLine())!=null && line.contains("Y")){
		String[] tokens = line.split("\t");
		for(String token : tokens) result+=map(token)+" & ";
		result=result.substring(0,result.length()-2)+"\\\\\n";
	}
	br.close();
	BicResult.print(result+"\\bottomrule");
  }

  private static String map(String token) {
	if(token.equals("")) return "";
	String[] yxi = token.split("\\[");
	String[] Y = yxi[1].split("\\]")[0].split(",");
	String[] I = yxi[3].split("\\]")[0].split(",");
	String[] scores = yxi[3].split("\\]")[1].split("=");
	String wconf = scores[1].length()>4 ? scores[1].substring(0,4) : scores[1];
	String wlift = scores[2].length()>4 ? scores[2].substring(0,4) : scores[2];
	String result = "\\mbox{$<$";
	for(int i=0, l=Y.length; i<l; i++) result+="$y_{"+Y[i]+"}$="+I[i]+",";
	result=result.substring(0, result.length()-1)+"$>$ $|X|$="+yxi[2].split(",").length+"}";
	return result+"\\newline\\mbox{\\hspace{.3cm} $w_{conf}$="+wconf+" $w_{lift}$="+wlift+"}";
  }
}