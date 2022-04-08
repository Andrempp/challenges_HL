package bicpam.closing;

import utils.BicResult;
import domain.Biclusters;

/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class Biclusterizer {

	private BiclusterExtender extender;
	private BiclusterMerger merger;
	private BiclusterFilter filter;
	private String urls;
	
	public Biclusters run(Biclusters biclusters){
		//System.out.println("Postprocessing!");
		//System.out.println("#"+biclusters.size());
		if(merger!=null) biclusters = merger.run(biclusters);
		//System.out.println("#"+biclusters.size());
		if(filter!=null) biclusters = filter.run(biclusters);
		//System.out.println("#"+biclusters.size());
		//System.out.println("Postprocessed!");
		return biclusters;
	}

	/*private Biclusters biclusterize(FrequentItemsets itemsets) {
		Biclusters biclusters = new Biclusters();
		for(FrequentItemset itemset : itemsets.itemsets){
			System.out.println(itemset.toString());
			Bicluster bic = new Bicluster(itemset);
			if(data.getOrientation()==OrientationCriteria.Row) bic.invert();
			biclusters.add(bic);
		}
		return biclusters;
	}*/

	public BiclusterExtender getExtender(){ return extender; }
	public BiclusterMerger getMerger(){ return merger; }
	public BiclusterFilter getFilter(){ return filter; }
	public String getUrls(){ return urls; }
	public void setUrls(String _urls){ urls=_urls; }	
	public void setExtender(BiclusterExtender _extender){ extender = _extender; }
	public void setMerger(BiclusterMerger _merger){ merger = _merger; }
	public void setFilter(BiclusterFilter _filter){ filter = _filter; }
	
	public Biclusterizer(){}
	public Biclusterizer(BiclusterMerger _merger){ merger = _merger; }
	public Biclusterizer(BiclusterExtender _extender, BiclusterFilter _filter, BiclusterMerger _merger){
		extender = _extender;
		filter = _filter;
		merger = _merger;
	}
	public Biclusterizer(BiclusterMerger _merger, BiclusterFilter _filter) {
		filter = _filter;
		merger = _merger;
	}
}
