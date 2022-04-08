package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import domain.Biclusters;
import domain.Dataset;

/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public final class GOTermReader {

	static String humanFileGOA = "/data/go/gene_association.goa_human";
	static String yeastFileGOA = "/data/go/gene_association.goa_yeast";
	
	public static Map<String,Set<String>> getTerms(boolean human) throws Exception {
		Map<String,Set<String>> result = new HashMap<String,Set<String>>();
        String line;
    	String file = human ? humanFileGOA : yeastFileGOA;
    	BufferedReader br = new BufferedReader(new FileReader(new File("").getAbsolutePath() + file));
        while ((line=br.readLine())!=null && line.contains("UniProtKB")){
        	String[] row = line.split("\t");
        	if(!result.containsKey(row[2]))
        		result.put(row[2], new HashSet<String>());
       		result.get(row[2]).add(row[4]);
		}
        br.close();
		return result;
	}
	public static Map<String,String> getNames(boolean human) throws Exception {
		Map<String,String> result = new HashMap<String,String>();
        String line;
    	String file = human ? humanFileGOA : yeastFileGOA;
    	BufferedReader br = new BufferedReader(new FileReader(new File("").getAbsolutePath() + file));
        while ((line=br.readLine())!=null && line.contains("UniProtKB")){
        	String[] row = line.split("\t");
        	if(!result.containsKey(row[2])){
        		result.put(row[2], row[2]);
        		//System.out.println(row[10]);
        		String[] names = row[10].split(";");
        		for(int i=0; i<names.length; i++) result.put(names[i],row[2]);
        	}
		}
        br.close();
		return result;
	}
}
