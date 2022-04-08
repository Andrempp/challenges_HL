package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import domain.Bicluster;
import domain.Biclusters;
import domain.Dataset;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public final class BicReader {
	
  public static Instances getInstances(BufferedReader in) throws IOException {
	  return new Instances(in);
  }
  public static Instances getInstances(String name) throws Exception {
	Instances data = new Instances(new BufferedReader(new FileReader(name)));
	/*System.out.println(data.getAttributes());
	for(int i=0; i<100; i++)
		if(data.attribute(i).isNominal())
			System.out.println(i+" => "+data.attribute(i).name()+" "+data.attributeStats(i).toString());
		else System.out.println(i+" => "+data.attribute(i).name()+" "+data.attributeStats(i).toString());*/
	//int[] removals = BicMath.generateVector(1,100);
	//int[] removals = new int[] {41,44,52,53,54,55,56,57,58,60,62,64,66,67,68,69,70,71,73,74,75,76,77,78,79,80,81,82,87,88,89,90,91,92,93,94,95,96,97,98,99};
	//52-58,60,62,64,66-71,73-82,87-99
	Remove removeFilter = new Remove();
	removeFilter.setAttributeIndicesArray(removals);
	removeFilter.setInvertSelection(false);
	removeFilter.setInputFormat(data);
	Instances newData = Filter.useFilter(data, removeFilter);
	System.out.println(newData.getAttributes());
	//System.out.println(BicPrinting.printInstances(newData));
	return newData;
  }
  public static double[][] getTable(String file) {
		List<String[]> table = new ArrayList<String[]>();
		BufferedReader br;
        String line;
	    try {
			br = new BufferedReader(new FileReader(new File("").getAbsolutePath() + file));
	        while((line=br.readLine())!=null && line.contains(",")){
        		table.add(line.split(","));
			}
	        br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	    double[][] result = new double[table.size()][];
	    for(int i=0, l=table.size(); i<l; i++){
	    	result[i] = new double[table.get(i).length];
		    for(int j=0, s=table.get(i).length; j<s; j++)
		    	result[i][j] = Double.valueOf(table.get(i)[j]);
	    }
	    return result;
  }

  public static double[][] getFabiaTable(String file) {
		List<String[]> table = new ArrayList<String[]>();
		BufferedReader br;
        String line;
	    try {
			br = new BufferedReader(new FileReader(new File("").getAbsolutePath() + file));
	        br.readLine();
	        while ((line=br.readLine())!=null && line.contains("\t")){
        		table.add(line.split("\t"));
			}
	        br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	    double[][] result = new double[table.size()][];
	    for(int i=0, l=table.size(); i<l; i++){
	    	result[i] = new double[table.get(i).length-1];
		    for(int j=0, s=table.get(i).length-1; j<s; j++)
		    	result[i][j] = Double.valueOf(table.get(i)[j+1]);
	    }
	    return result;
  }

  public static int[][] getTransactions(String file) {
		List<String[]> table = new ArrayList<String[]>();
		BufferedReader br;
        String line;
	    try {
			br = new BufferedReader(new FileReader(new File("").getAbsolutePath() + file));
	        while (!br.readLine().startsWith("@data"));
	        while ((line=br.readLine())!=null && line.contains(",")){
        		table.add(line.split(","));
			}
	        br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	    int[][] result = new int[table.size()][];
	    for(int i=0, l=table.size(); i<l; i++){
	    	result[i] = new int[table.get(i).length];
		    for(int j=0, s=table.get(i).length; j<s; j++)
		    	result[i][j] = Integer.valueOf(table.get(i)[j]);
	    }
	    return result;
  }

  @SuppressWarnings("resource")
  public static int[] getClasses(String file, String del) throws Exception {
	  List<String> classes = new ArrayList<String>();
	  BufferedReader br = new BufferedReader(new FileReader(new File("").getAbsolutePath() + file));
	  String line;
      while ((line=br.readLine())!=null && line.contains(del)) 
    	  classes.add(line.substring(line.lastIndexOf(del)));
      return getVector(classes);
  }
  @SuppressWarnings("resource")
  public static int[] getClasses(String file, String del, int att) throws Exception {
	  List<String> classes = new ArrayList<String>();
	  BufferedReader br = new BufferedReader(new FileReader(new File("").getAbsolutePath() + file));
	  String line;
      while ((line=br.readLine())!=null && line.contains(del)) 
    	  classes.add(line.split(del)[att]);
      return getVector(classes);
  }
  private static int[] getVector(List<String> classes) {
	  SortedSet<String> set = new TreeSet<String>();
	  set.addAll(classes);
	  List<String> classIndexes = new ArrayList<String>();
	  classIndexes.addAll(set);
	  int[] result = new int[classes.size()];
	  for(int i=0, l=result.length; i<l; i++) result[i]=classIndexes.indexOf(classes.get(i));
	  return result;
  }
  public static double[][] getTable(String file, String del, List<Integer> atts) throws Exception {
		List<String[]> table = new ArrayList<String[]>();
		BufferedReader br = new BufferedReader(new FileReader(new File("").getAbsolutePath() + file));
		String line;
        while ((line=br.readLine())!=null && line.contains(del)) table.add(line.split(del));
        br.close();
	    double[][] result = new double[table.size()][atts.size()];
	    for(int i=0, l=table.size(); i<l; i++){
	    	int index = 0;
		    for(Integer att : atts){
		    	if(table.get(i)[att].equals("?")||table.get(i)[att].equals("")) result[i][index++]=Dataset.MISSING;
		    	else result[i][index++]=((double)((int)(Double.valueOf(table.get(i)[att])*100)))/100.0;
		    }
	    }
	    return result;
  }

  public static double[][] getTable(BufferedReader br, int removals, String delimiter) throws IOException {
 	    List<String[]> table = new ArrayList<String[]>();
        String line;
		br.readLine();
	    while ((line=br.readLine())!=null && line.contains(delimiter)){
	    	String[] values = line.split(delimiter);
	    	//for(int i=1, l=values.length; i<l; i++) if(values[i].contains(".")) values[i]=values[i].substring(0, values[i].indexOf('.')+2);
	    	table.add(values);
	    }
	    double[][] result = new double[table.size()][];
	    for(int i=0, l=table.size(); i<l; i++){
	    	result[i] = new double[table.get(i).length-removals];
		    for(int j=removals, s=table.get(i).length; j<s; j++){
		    	if(table.get(i)[j].isEmpty() || table.get(i)[j].equals("?")) result[i][j-removals] = Dataset.MISSING;
		    	else result[i][j-removals] = ((double)((int)(Double.valueOf(table.get(i)[j])*100)))/100.0;
		    }
	    }
	    return result;
  }  
  public static double[][] getTable(String file, int removals, String delimiter) throws IOException {
	BufferedReader br = new BufferedReader(new FileReader(file));
	double[][] result = getTable(br, removals, delimiter);
	br.close();
	return result;//new File("").getAbsolutePath() +
  }
  public static List<String> getGenes(BufferedReader br, String delimiter) throws IOException {
	List<String> result = new ArrayList<String>();
    String line;
    br.readLine();
    while ((line=br.readLine())!=null && line.contains(delimiter))
   		result.add(line.split(delimiter)[0]);
    return result;
  }

  public static List<String> getGenes(String file, String delimiter) throws FileNotFoundException, IOException {
	BufferedReader br = new BufferedReader(new FileReader(file));
	List<String> result = getGenes(br,delimiter);
	br.close();
	return result;//new File("").getAbsolutePath() +
  }

  public static List<String> getConds(BufferedReader br, int removals, String delimiter) throws IOException {
	List<String> result = new ArrayList<String>();
    String[] res = br.readLine().split(delimiter);
    for(int j=removals, s=res.length; j<s; j++) result.add(res[j]);
    return result;
  }
  public static List<String> getConds(String file, int removals, String delimiter) throws IOException {
	BufferedReader br = new BufferedReader(new FileReader(file));
	List<String> result = getConds(br,removals,delimiter);
	br.close();
	return result;//new File("").getAbsolutePath() +
  }

  public static String detectDelimeter(BufferedReader br) throws IOException {
	int tab=0, comma=0, space=0;
	String line = br.readLine(); 
    tab = line.split("\t").length;
    comma = line.split(",").length;
    space = line.split(" ").length;
    if(tab>=comma){
    	if(tab>=space) return "\t";
    	else return " ";
    }
    else if(comma>=space) return ",";
    else return " ";
  }
  public static String detectDelimeter(String file) throws IOException {
	BufferedReader br = new BufferedReader(new FileReader(file));
	String result = detectDelimeter(br);
	br.close();
  	return result;//new File("").getAbsolutePath() +
  }
  
  public static Biclusters readBicPAMBics(String file, boolean absolute) {
		BufferedReader br;
	    Biclusters bics = new Biclusters();
	    try { 
	        String line;
	        if(!absolute) file = new File("").getAbsolutePath()+file;
			br = new BufferedReader(new FileReader(file));
			br.readLine();
	        while((line=br.readLine())!=null && line.contains("(")){
	        	String[] parts = line.split("\\[");
	        	String[] colEls = parts[1].split("\\]")[0].split(",");
	        	String[] rowEls = parts[2].split("\\]")[0].split(",");
	    	    SortedSet<Integer> cols = new TreeSet<Integer>();
	    	    SortedSet<Integer> rows = new TreeSet<Integer>();
	        	for(int i=0; i<colEls.length; i++) cols.add(Integer.valueOf(colEls[i]));
	        	for(int i=0; i<rowEls.length; i++) rows.add(Integer.valueOf(rowEls[i]));
	    		bics.add(new Bicluster(rows,cols));
			}
	        br.close();
	    } catch (Exception e) {    
	    	e.printStackTrace();
	    }
	    return bics;
  }
  
  public static Biclusters readBics(String bicsRows, String bicsCols) {
		BufferedReader br;
	    Biclusters bics = new Biclusters();
	    List<SortedSet<Integer>> rows = new ArrayList<SortedSet<Integer>>();
	    List<SortedSet<Integer>> cols = new ArrayList<SortedSet<Integer>>();
	    try { 
	        String line;
			br = new BufferedReader(new FileReader(new File("").getAbsolutePath() + bicsRows));
	        while ((line=br.readLine())!=null){
	        	String[] els = line.split("\t");
	        	SortedSet<Integer> row = new TreeSet<Integer>();
	        	for(int i=0, l=els.length; i<l; i++){
	        		int val = Integer.valueOf(els[i]);
	        		if(val!=0) row.add(val);
	        	}
	        	rows.add(row);
			}
	        br.close();
			br = new BufferedReader(new FileReader(new File("").getAbsolutePath() + bicsCols));
	        while ((line=br.readLine())!=null){
	        	String[] els = line.split("\t");
	        	SortedSet<Integer> col = new TreeSet<Integer>();
	        	for(int i=0, l=els.length; i<l; i++){
	        		int val = Integer.valueOf(els[i]);
	        		if(val!=0) col.add(val);
	        	}
	        	cols.add(col);
			}
	        br.close();
	    } catch (Exception e) {    
	    	e.printStackTrace();
	    }
	    
    	for(int i=0, l=rows.size(); i<l; i++) 
    		bics.add(new Bicluster(rows.get(i),cols.get(i)));
    	
	    return bics;
  }
  public static List<String[]> getStringTable(String file, String del) throws IOException {
	  List<String[]> table = new ArrayList<String[]>();
	  BufferedReader br = new BufferedReader(new FileReader(file));
	  String line;
      while ((line=br.readLine())!=null && line.contains(del)) table.add(line.split(del));
      br.close();
	  return table;
  }

}
