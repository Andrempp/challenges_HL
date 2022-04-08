package performance.significance;

import generator.BicMatrixGenerator.PatternType;

import java.util.ArrayList;
import java.util.List;

import utils.BicMath;

public class SearchSpace {

	public static int searchSpaceSize(int items, int columns, boolean symmetries, PatternType type) {
		if(type.equals(PatternType.Constant)) return constantSearchSpaceSize(items,columns,symmetries);
		else if(type.equals(PatternType.Additive)) return additiveSearchSpaceSize(items,columns,symmetries);
		else if(type.equals(PatternType.Multiplicative)) return multiplicativeSearchSpaceSize(items,columns,symmetries);
		else if(type.equals(PatternType.OrderPreserving)) return orderSearchSpaceSize(items,columns);
		else return -1;
	}
	
	private static int constantSearchSpaceSize(int items, int columns, boolean symmetries) {
		double total = Math.pow(items, columns);
		if(symmetries) return (int) (total+1)/2;
		return (int) total;
	}

	public static int orderSearchSpaceSize(int items, int columns) {
		return (int) BicMath.permutation(columns);
	}

	private static int additiveSearchSpaceSize(int nritems, int columns, boolean symmetries) {
		int spaceadditive = 1;
		
		if(symmetries){
			int lowitem=-nritems/2;
			List<String> items = new ArrayList<String>();
			for(int i=lowitem, l=nritems+lowitem; i<l; i++) items.add(i+"");
			//System.out.println(items);
			List<List<Integer>> resultall = pow(items,columns);

			List<List<Integer>> result = new ArrayList<List<Integer>>();
			while(!resultall.isEmpty()){
				List<Integer> pattern = resultall.remove(0);
				for(int i=0; i<columns; i++) pattern.set(i,-pattern.get(i));
				if(!resultall.contains(pattern)) result.add(pattern);
			}
			List<List<Integer>> result2 = new ArrayList<List<Integer>>();
			for(List<Integer> res : result) if(res.contains(lowitem)||res.contains(-lowitem)) result2.add(res);
			//System.out.println(result2);
			spaceadditive=result2.size();
		} else {
			/*List<List<Integer>> result2 = new ArrayList<List<Integer>>();
			for(List<Integer> res : resultall) if(res.contains(lowitem)) result2.add(res);
			spaceadditive=result2.size();*/
			for(int i=1; i<columns; i++) spaceadditive += BicMath.combination(columns, columns-i);
			for(int d=2; d<nritems; d++){
				for(int i=1; i<columns; i++){
					int factor = 0;
					for(int j=1; j<=i; j++) factor += BicMath.combination(i, j)*Math.pow(d-1,i-j);
					factor = (int) (BicMath.combination(columns, columns-i)*factor);
					//System.out.println(i+":"+factor);
					spaceadditive += factor;
				}
			}
		}
		return spaceadditive;
	}

	private static int multiplicativeSearchSpaceSize(int nritems, int columns, boolean symmetries) {
		int space2 = 0;
		List<String> items = new ArrayList<String>();
		if(symmetries){
			for(int i=(nritems%2==0) ? 1 : 0; i<=nritems/2; i++) items.add(i+"");
			for(int i=-1; i>=-nritems/2; i--) items.add(i+"");
			//System.out.println(items);
		} else for(int i=1; i<=nritems; i++) items.add(i+"");
		List<List<Integer>> resultall = pow(items,columns);
		List<List<Integer>> result = new ArrayList<List<Integer>>();
		
		if(symmetries){
			while(!resultall.isEmpty()){
				List<Integer> pattern = resultall.remove(0);
				for(int i=0; i<columns; i++) pattern.set(i,-pattern.get(i));
				if(!resultall.contains(pattern)) result.add(pattern);
			}
		}
		else result = resultall;
		
		for(List<Integer> res : result){
			int div = gcd(res);
			//System.out.println(div+"==>"+res);
			if(div==0||div==1) space2++;
		}
		return space2;
	}

	
	/*****************************/
	/***** SEARCH SPACE PATT *****/
	/*****************************/
	
	public static List<List<Integer>> getConstantAll(int items, int columns) {
		List<String> combinations = new ArrayList<String>();
		for(int i=0; i<items; i++) combinations.add(i+"");
		return pow(combinations,columns);
	}
	
	public static List<List<Integer>> getAdditiveAll(int items, int columns) {
		List<String> combinations = new ArrayList<String>();
		for(int i=0; i<items; i++) combinations.add(i+"");
		List<List<Integer>> result1 = pow(combinations,columns);
		List<List<Integer>> result2 = new ArrayList<List<Integer>>();
		for(List<Integer> res : result1)
			if(res.contains(0)) result2.add(res);
		return result2;
	}

	public static List<List<Integer>> getMultiplicativeAll(int items, int columns) {
		List<String> combinations = new ArrayList<String>();
		for(int i=1; i<=items; i++) combinations.add(i+"");
		List<List<Integer>> result1 = pow(combinations,columns);
		List<List<Integer>> result2 = new ArrayList<List<Integer>>();
		for(List<Integer> res : result1){
			int div = gcd(res);
			if(div==1) result2.add(res);
		}
		return result2;
	}
	

	/*****************************/
	/***** AUXILIARY METHODS *****/
	/*****************************/

	public static List<List<Integer>> pow(List<String> items, int n){
		List<String> combinations = new ArrayList<String>();
		combinations.addAll(items);
		
	    for(int i=0; i<n-1; i++) {
			List<String> newcombinations = new ArrayList<String>();
		    for(String s1: combinations) 
		       for(String s2 : items)
		           newcombinations.add(s1 + "," + s2);
	        combinations = newcombinations;
	        //System.out.println(newcombinations);
	    }
	    
        //System.out.println(combinations);
	    List<List<Integer>> result = new ArrayList<List<Integer>>();
	    for(String s1: combinations){
	    	List<Integer> res = new ArrayList<Integer>();
	    	//System.out.println(s1+"=>"+s1.split(",").length);
	    	for(String s2 : s1.split(","))
	    		res.add(Integer.valueOf(s2));
	    	result.add(res);
	    }
        //System.out.println(result);
	    return result;
	}
	private static int gcd(int a, int b){
	    while(b > 0){
	        int temp = b;
	        b = a % b; // % is remainder
	        a = temp;
	    }
	    return a;
	}

	private static int gcd(List<Integer> input){
	    int result = input.get(0);
	    for(int i=1; i<input.size(); i++) 
	    	result = gcd(Math.abs(result), Math.abs(input.get(i)));
	    return result;
	}
	
	public static boolean prime(int n) {
		for (int i=2; i<=n/2; i++)
		    if(n % i == 0) return false;
		return true;
	}
	
}
