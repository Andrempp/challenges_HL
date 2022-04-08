package utils.others;

import java.util.Comparator;

import domain.Bicluster;

public class BiclusterRowComparator implements Comparator<Bicluster>{
	
	private int asc = 1;
	
	public BiclusterRowComparator(boolean ascendent){
		asc = ascendent ? 1 : -1;
	}

    public int compare(Bicluster bic1, Bicluster bic2) {
        if(bic1.numRows() == bic2.numRows()){
        	if(bic1.numColumns() == bic2.numColumns()) return 0;
        	else if(bic1.numColumns() > bic2.numColumns()) return 1*asc;
            else return -1*asc;
        }
        else if(bic1.numRows() > bic2.numRows()) return 1*asc;
        else return -1*asc;
    }
}