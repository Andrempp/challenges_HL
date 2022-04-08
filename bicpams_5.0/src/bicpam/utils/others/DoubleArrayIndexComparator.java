package utils.others;

import java.util.Comparator;

public class DoubleArrayIndexComparator implements Comparator<Integer>{
		
    private final double[] array;
    private int asc = 1;

    public DoubleArrayIndexComparator(double[] values){
        this.array = values;
    }

    public DoubleArrayIndexComparator(double[] values, boolean desc){
        this.array = values;
        if(desc) this.asc = -1;
    }

    public Integer[] createIndexArray(){
        Integer[] indexes = new Integer[array.length];
        for (int i = 0; i < array.length; i++) indexes[i] = i; // Autoboxing
        return indexes;
    }

    public int compare(Integer index1, Integer index2) {
        if(array[index1] == array[index2]) return 0;
        else if(array[index1]*asc > array[index2]*asc) return 1;
        else return -1;
    }
}
