package domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import bicpam.bicminer.BiclusterMiner.Orientation;
import utils.others.BiclusterComparator;
import utils.others.BiclusterPValueComparator;
import utils.others.BiclusterRowComparator;
import utils.others.BiclusterScoreComparator;
import utils.others.BiclusterWConfComparator;
import utils.others.BiclusterWLiftComparator;


/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class Biclusters {
	
	private List<Bicluster> biclusters;
	public enum Order { WLift, WConf, Area, Rows, PValue, Default, Score };

	public Biclusters(){
		biclusters = new ArrayList<Bicluster>();
	}
	public Biclusters(List<Bicluster> bics){
		biclusters = bics;
	}
	public void add(Bicluster bicluster) {
		biclusters.add(bicluster);
	}
	public Bicluster get(int i) {
		return biclusters.get(i);
	}
	public Bicluster remove(int i) {
		return biclusters.remove(i);
	}
	public Biclusters removeAll(Biclusters removals) {
		biclusters.removeAll(removals.getBiclusters());
		return this;
	}
	public Bicluster removeMin() {
		return biclusters.remove(0);
	}
	public boolean hasBiclusters() {
		return !biclusters.isEmpty();
	}
	public List<Bicluster> getBiclusters() {
		return biclusters;
	}
	public int size() {
		if(biclusters==null) return 0;
		return biclusters.size();
	}
	
	public void order(){ Collections.sort(biclusters, new BiclusterComparator(false)); }
	public void orderPValue() { Collections.sort(biclusters, new BiclusterPValueComparator(true)); }
	public void orderRows() { Collections.sort(biclusters, new BiclusterRowComparator(false)); }
	public void orderWLift() { Collections.sort(biclusters, new BiclusterWLiftComparator(false)); }
	public void orderWConf() { Collections.sort(biclusters, new BiclusterWConfComparator(false)); }
	public void orderScore() { Collections.sort(biclusters, new BiclusterScoreComparator(true)); }
	
	public Biclusters order(Order order) {
		switch(order){
			case Area : order(); break;
			case WConf : orderWConf(); break;
			case WLift : orderWLift(); break;
			case Rows : orderRows(); break;
			case PValue : orderPValue(); break;
			case Score : orderScore(); break;
			default: break;
		}
		return this;
	}

	public Biclusters copy() {
		Biclusters bics = new Biclusters();
		bics.biclusters = new ArrayList<Bicluster>();
		for(Bicluster bic : biclusters) bics.add(bic);
		return bics;
	}
	public Biclusters addAll(Biclusters result) {
		biclusters.addAll(result.biclusters);
		return this;
	}
	public void invert() {
		for(int i=0; i<biclusters.size(); i++)
			biclusters.set(i, biclusters.get(i).invert());
	}
	public void setPartition(int i) {
		for(Bicluster bic : biclusters) bic.partition=i;
	}
	public void shiftRows(int i) {
		for(Bicluster bic : biclusters) bic.shiftRows(i);
	}
	public void shiftColumns(int i) {
		for(Bicluster bic : biclusters) bic.shiftColumns(i);
	}
	public Map<String,Integer> getElementCounts() {
    	HashMap<String,Integer> posCount = new HashMap<String,Integer>();
    	for(Bicluster bic : biclusters){
    		for(Integer row : bic.rows)
    			for(Integer col : bic.columns){
    				Integer val = posCount.get(row+","+col); 
    				if(val == null) val = 0;
    				posCount.put(row+","+col,val+1);
    			}
    	}
		return posCount;
	}
	public Biclusters aboveLift(int[] classValues, int[] supports, int nrows, double lift, int targetClass, boolean liftSuperiority) {
		Biclusters bics = new Biclusters();
		for(Bicluster bic : biclusters) {
			double maxLift = bic.computeLifts(classValues,supports,nrows,targetClass,liftSuperiority);
			if(maxLift>=lift) bics.add(bic);
		}
		return bics;
	}
	public String toShortString() {
		if(biclusters.isEmpty()) return "";
		StringBuffer res = new StringBuffer();
		for(Bicluster bic : biclusters) res.append(", "+bic.toShortString());
		return res.toString().substring(2);
	}
	public void computePatterns(Dataset data, Orientation orientation) {
		for(Bicluster bic : biclusters) bic.computePattern(data,orientation);
	}
	public String toString() {
		StringBuffer res = new StringBuffer("#"+biclusters.size()+"\r\n");
		for(Bicluster bic : biclusters) res.append(bic.toString()+"\r\n");
		return res.toString().replace(",]","]");
	}
	public String toString(List<String> nodesX, List<String> nodesY) {
		StringBuffer res = new StringBuffer("#"+biclusters.size()+"\r\n");
		for(Bicluster bic : biclusters) res.append(bic.toString(nodesX,nodesY)+"\r\n");
		return res.toString().replace(",]","]");
	}
	/*public String toString(List<String> rows, List<String> datacols, List<String>) {
		StringBuffer res = new StringBuffer("#"+biclusters.size()+"\r\n");
		for(Bicluster bic : biclusters) res.append(bic.toString(nodesX,nodesY)+"\r\n");
		return res.toString().replace(",]","]");
	}*/
	public boolean isEmpty() {
		return biclusters.isEmpty();
	}
}
