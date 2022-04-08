package utils;

import java.util.ArrayList; 

/** Class for debugging the application (by logging)
 *  @author Rui Henriques
 *  @version 1.0
 */
public class BicResult {

	private static BicWriter writer = new BicWriter("output/result.txt");
	
	public static void filename(ArrayList<String> strs)	{
		String r = "";
		for (String i: strs) {
			r = r + "_" + i;
		}
		writer = new BicWriter("output/result"+r+".txt");
	}
	
	public static void print(String str) {
		writer.print(str);
	}

	public static void enter(String str){
		writer.enter(str);
	}
	
	public static void back(String str){
		writer.back(str);
	}
	
	public static void println(String str){
		writer.println(str);
	}
}
