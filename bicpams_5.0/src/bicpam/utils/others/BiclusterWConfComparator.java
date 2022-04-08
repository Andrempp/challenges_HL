package utils.others;

import java.util.Comparator;

import domain.Bicluster;

public class BiclusterWConfComparator implements Comparator<Bicluster>{
	
	private int asc = 1;
	
	public BiclusterWConfComparator(boolean ascendent){
		asc = ascendent ? 1 : -1;
	}

    public int compare(Bicluster bic1, Bicluster bic2) {
        if(bic1.wconf == bic2.wconf){
        	int area1=bic1.area(), area2=bic2.area();
            if(area1 == area2) return 0;
            else if(area1 > area2) return 1*asc;
            else return -1*asc;
        }
        else if(bic1.wconf > bic2.wconf) return 1*asc;
        else return -1*asc;
    }
}