package utils.others;

import java.util.Comparator;

import domain.Bicluster;

public class BiclusterWLiftComparator implements Comparator<Bicluster>{
	
	private int asc = 1;
	
	public BiclusterWLiftComparator(boolean ascendent){
		asc = ascendent ? 1 : -1;
	}

    public int compare(Bicluster bic1, Bicluster bic2) {
        if(bic1.wlift == bic2.wlift){
        	int area1=bic1.area(), area2=bic2.area();
            if(area1 == area2) return 0;
            else if(area1 > area2) return 1*asc;
            else return -1*asc;
        }
        else if(bic1.wlift > bic2.wlift) return 1*asc;
        else return -1*asc;
    }
}