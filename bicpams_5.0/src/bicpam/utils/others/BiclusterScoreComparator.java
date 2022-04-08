package utils.others;

import java.util.Comparator;

import domain.Bicluster;

public class BiclusterScoreComparator implements Comparator<Bicluster>{
	
	private int asc = 1;
	
	public BiclusterScoreComparator(boolean ascendent){
		asc = ascendent ? 1 : -1;
	}

    public int compare(Bicluster bic1, Bicluster bic2) {
        if(bic1.score == bic2.score) return 0;
        else if(bic1.score < bic2.score) return 1*asc;
        else return -1*asc;
    }
}