package utils.others;

import java.util.ArrayList;
import java.util.List;

public class CopyUtils {

	public static List<List<Integer>> copyIntList(List<List<Integer>> indexes) {
		List<List<Integer>> result = new ArrayList<List<Integer>>();
		for(List<Integer> index : indexes){
			List<Integer> newIndex = new ArrayList<Integer>();
			newIndex.addAll(index);
			result.add(newIndex);
		}
		return result;
  	}

  	public static List<Integer> copyList(List<Integer> list) {
	  	List<Integer> result = new ArrayList<Integer>();
	  	result.addAll(list);
	  	return result;
  	}
	public static List<List<Double>> copyDoubleList(List<List<Double>> data) {
		List<List<Double>> result = new ArrayList<List<Double>>();
		for(List<Double> list : data){
			List<Double> newIndex = new ArrayList<Double>();
			newIndex.addAll(list);
			result.add(newIndex);
		}
		return result;
	}

}
