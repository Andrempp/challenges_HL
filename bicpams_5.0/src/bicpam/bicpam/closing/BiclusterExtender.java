package bicpam.closing;

import domain.Biclusters;
import bicpam.pminer.PM.ItemsetType;


/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class BiclusterExtender {
	
	protected double overallError=-1;
	protected double rowError=-1;
	protected double columnError=-1;
	private double missingTolerance=-1;
	
	//gene by gene based on error / Fisher test
	public Biclusters run(Biclusters biclusters, double[][] data, ItemsetType itemsetType, boolean inverted){
		switch(itemsetType){
			case Maximal: ;
			case Closed: ;
			case Simple: ;
			default : break;
		}
		return biclusters;
	}
	
	/*private Bicluster recursiveAddValidTrans(FrequentItemset itemset, Bicluster bicluster) {
		if(1-(itemset.nrTrans/(itemset.nrTrans+1)) > rowError) return bicluster;							
		for(Integer i : itemset.getLargerTrans()){
			bicluster.add(itemsets.itemsets.get(i));
			bicluster = recursiveAddValidItems(itemsets.itemsets.get(i),bicluster);
		}
		return bicluster;
	}

	private Bicluster recursiveAddValidItems(FrequentItemset itemset, Bicluster bicluster) {
		if(1-(itemset.nrItems/(itemset.nrItems+1)) > columnError) return bicluster;							
		for(Integer i : itemset.getLargerItems()){
			bicluster.add(itemsets.itemsets.get(i));
			bicluster = recursiveAddValidItems(itemsets.itemsets.get(i),bicluster);
		}
		return bicluster;
	}*/

	public BiclusterExtender(){
		columnError=0.25;
		rowError=0.25;
	}
	public BiclusterExtender(double error){
		overallError=error;
		columnError=error;
		rowError=error;
		
	}
	public BiclusterExtender(double _colerror, double _rowerror){
		columnError=_colerror;
		rowError=_rowerror;
	}
	
	public double getMissingTolerance(){ return missingTolerance; }
	public void setMissingTolerance(double tolerance){ missingTolerance = tolerance; }
}
