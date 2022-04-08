package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import domain.Dataset;

public class NetMatrixMapper {

	public static Dataset getTable(BufferedReader in, int n1, int n2, int n3, String delimiter, boolean bidirectional) throws IOException {
		HashMap<String,Integer> nodeIDs = new HashMap<String,Integer>();
    	List<List<Integer>> indexes = new ArrayList<List<Integer>>();
    	List<List<Double>> scores = new ArrayList<List<Double>>();
        String line;
        in.readLine();
        int c=0;
        System.out.println("Loading network...");
        while((line=in.readLine())!=null){ // && k++<limit)
        	//if(c++%10000==0) System.out.println("HERE:"+c);
        	if(!line.contains(delimiter)) continue;
        	String[] iline = line.split(delimiter);
	    	try{
	        	if(!nodeIDs.containsKey(iline[n1])){
	        		nodeIDs.put(iline[n1], c++);
		    		indexes.add(new ArrayList<Integer>());
		    		scores.add(new ArrayList<Double>());
	        	}
	        	
	        	if(!nodeIDs.containsKey(iline[n2])){
	        		nodeIDs.put(iline[n2], c++);
		    		indexes.add(new ArrayList<Integer>());
		    		scores.add(new ArrayList<Double>());
	        	}
		    	int index1=nodeIDs.get(iline[n1]), index2=nodeIDs.get(iline[n2]);
		    	double val = ((double)((int)(Double.valueOf(iline[n3])*100)))/100.0;
	    		scores.get(index1).add(val);
		    	indexes.get(index1).add(index2);
		    	if(bidirectional){
		    		scores.get(index2).add(val);
			    	indexes.get(index2).add(index1);
		    	}
	    	} catch(Exception e){
	    		System.out.println(">> input inconsistency (line "+c+"): "+BicPrinting.plot(iline));
	    	}
        }
        int interactions = 0;
        int posinteractions = 0, neginteractions = 0;
        double min=1000, max=-1000;
        for(List<Integer> indexL : indexes) interactions += indexL.size();
        for(List<Double> scoresL : scores){
        	for(Double iscore : scoresL){
        		min=Math.min(min, iscore);
        		max=Math.max(max, iscore);
        		if(iscore > 0) posinteractions++;
        		else if(iscore < 0) neginteractions++;
        	}
        }
        
        System.out.println("IDs collected!\n#Nodes="+nodeIDs.size()+"\n#Interactions="+interactions);        
        System.out.println("#PosInteractions="+posinteractions+" #NegInteractions="+neginteractions+" => %"+((double)posinteractions)/(double)interactions);        
        List<String> listNodes = new ArrayList<String>();
        listNodes.addAll(nodeIDs.keySet());
        Dataset result = new Dataset(listNodes,indexes,scores);
        result.min=min;
        result.max=max;
		return result;
	}
	public static Dataset getTable(String dataname, int x1, int x2, int score, String delimeter, boolean bidirectional) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(dataname)); 
		Dataset result = getTable(br,x1,x2,score,delimeter,bidirectional);
		br.close();
		return result;
	}

}
