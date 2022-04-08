package utils.others;

import java.util.Comparator;

public class IntArrayIndexComparator implements Comparator<Integer>{
		
    private final int[] array;

    public IntArrayIndexComparator(int[] values){
        this.array = values;
    }
	    
    public Integer[] createIndexArray(){
        Integer[] indexes = new Integer[array.length];
        for (int i = 0; i < array.length; i++) indexes[i] = i; // Autoboxing
        return indexes;
    }

    public int compare(Integer index1, Integer index2) {
        if(array[index1] == array[index2]) return 0;
        else if(array[index1] > array[index2]) return 1;
        else return -1;
    }
}
