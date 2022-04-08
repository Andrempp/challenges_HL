package utils.others;

import java.util.Comparator;

import domain.Bicluster;

public class BiclusterPValueComparator implements Comparator<Bicluster>{
	
	private int asc = 1;
	
	public BiclusterPValueComparator(boolean ascendent){
		asc = ascendent ? 1 : -1;
	}

    public int compare(Bicluster bic1, Bicluster bic2) {
        if(bic1.pvalue == bic2.pvalue) return 0;
        else if(bic1.pvalue > bic2.pvalue) return 1*asc;
        else return -1*asc;
    }
}