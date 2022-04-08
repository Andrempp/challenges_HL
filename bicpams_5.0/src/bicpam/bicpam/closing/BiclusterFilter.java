package bicpam.closing;

import java.util.SortedSet;
import java.util.TreeSet;
import domain.Bicluster;
import domain.Biclusters;
import domain.Biclusters.Order;


/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class BiclusterFilter {
	
	public double overlapMinExcess;
	public enum FilteringCriteria { Overall, Rows, Columns }
	public enum SortingCriteria { Overall, Rows, PValue }
	public FilteringCriteria criteria = FilteringCriteria.Overall; 
	public Order sort = Order.PValue; 
	
	public Biclusters run(Biclusters bics){
		bics.order(sort);
		//System.out.println("Filtering...");
		SortedSet<Integer> rembics = new TreeSet<Integer>();
		for(int i1=0, l=bics.size(); i1<l; i1++){
			if(rembics.contains(i1)) continue;
			Bicluster newbic = bics.getBiclusters().get(i1);
			
			switch(criteria){
				case Overall:
					for(int i2=i1+1; i2<l; i2++){
						if(newbic.filter(bics.getBiclusters().get(i2),overlapMinExcess))
							rembics.add(i2);
					}
					break;
				case Rows:
					for(int i2=i1+1; i2<l; i2++){
						if(newbic.filterRows(bics.getBiclusters().get(i2),overlapMinExcess))
							rembics.add(i2);
					}
					break;
				case Columns:
					for(int i2=i1+1; i2<l; i2++){
						if(newbic.filterColumns(bics.getBiclusters().get(i2),overlapMinExcess))
							rembics.add(i2);
					}
					break;
			}
		}
		while(!rembics.isEmpty()){
			Integer i = rembics.last();
			bics.remove(i);
			rembics.remove(i);
		}
		return bics;		
	}
	
	public BiclusterFilter(){
		overlapMinExcess=0.6;
	}
	public BiclusterFilter(double error){
		overlapMinExcess=error;
	}
	public BiclusterFilter(FilteringCriteria _criteria, double _dissimilarity) {
		criteria = _criteria;
		overlapMinExcess = _dissimilarity;
	}
	public BiclusterFilter(FilteringCriteria _criteria, Order _sort, double _dissimilarity) {
		criteria = _criteria;
		sort = _sort;
		overlapMinExcess = _dissimilarity;
	}
}
