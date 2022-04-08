package generator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import utils.BicMath;
import utils.BicResult;
import utils.GOTermReader;
import domain.Biclusters;

/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public final class Annotation {

	public static List<List<Integer>> run(int nrRows, Biclusters bics) {
		return run(nrRows,100,10,bics);
	}		
	
	public static List<List<Integer>> run(int nrRows, int avgNrRowsPerAnnotation, int avgNrAnnotationsPerRow, Biclusters bics) {
		Random r = new Random();
		List<List<Integer>> result = new ArrayList<List<Integer>>();
		for(int i=0; i<nrRows; i++) result.add(new ArrayList<Integer>());
		
		int nrAnnotations = avgNrAnnotationsPerRow*(nrRows/avgNrRowsPerAnnotation);
		
		for(int i=0; i<nrAnnotations; i++){
			int supportPerAnnotation = (int)(r.nextGaussian()*20+avgNrRowsPerAnnotation-10);
			Set<Integer> rows = new HashSet<Integer>();
			while(rows.size()<supportPerAnnotation) rows.add(r.nextInt(nrRows));
			for(Integer row : rows)	result.get(row).add(i);
		}
		
		for(int i=0, l=bics.size(); i<l; i++)
			for(Integer row : bics.getBiclusters().get(i).rows)
				result.get(row).add(nrAnnotations+i);
		
		BicResult.println(result+"");
		return result;
	}

	public static List<List<String>> run(List<String> rows, boolean human) throws Exception {
		List<List<String>> result = new ArrayList<List<String>>();
		Map<String,Set<String>> goTerms = GOTermReader.getTerms(human);
		Map<String,String> names = GOTermReader.getNames(human);
		BicResult.println(goTerms+"");
		BicResult.println(names+"");
		for(String gene : rows){
			BicResult.println(gene+">"+goTerms.get(names.get(gene)));
			List<String> terms = new ArrayList<String>();
			Set<String> termsSet = goTerms.get(names.get(gene)); 
			if(termsSet!=null) terms.addAll(termsSet);
			result.add(terms);
		}
		return result;
	}

	public static List<List<Integer>> convertToNumericAnnotations(List<List<String>> annotations) throws Exception {
    	Set<String> terms = new HashSet<String>();
    	List<List<Integer>> result = new ArrayList<List<Integer>>();
    	Map<String,Integer> indexOfTerms = new HashMap<String,Integer>();
    	
    	int i=0;
    	for(List<String> annot : annotations) if(annot!=null) terms.addAll(annot);
    	for(String term : terms) indexOfTerms.put(term,i++);
    	for(List<String> annot : annotations){
    		List<Integer> entry = new ArrayList<Integer>();
    		for(String an : annot) entry.add(indexOfTerms.get(an));
    		result.add(entry);
    	}
    	
    	boolean printStatistics = true;
    	if(printStatistics){
        	List<Integer> counts1 = new ArrayList<Integer>(), counts2 = new ArrayList<Integer>();
        	for(String term : terms){
        		int count=0;
        		for(List<String> annot : annotations) if(annot!=null && annot.contains(term)) count++;
        		counts1.add(count);
        	}
    		for(List<String> annot : annotations) 
    			if(annot!=null) counts2.add(annot.size());
    		Collections.sort(counts1);
    		Collections.sort(counts2);
    		System.out.println("Counts1:"+counts1);
    		System.out.println("Counts2:"+counts2);
        	System.out.println("Rows/term="+BicMath.average(counts1)+" +- "+BicMath.standardVariation(counts1));
        	System.out.println("Terms/row="+BicMath.average(counts2)+" +- "+BicMath.standardVariation(counts2));
    	}
    	
    	return result;
	}
}
