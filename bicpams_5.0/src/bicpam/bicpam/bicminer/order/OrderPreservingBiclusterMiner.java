package bicpam.bicminer.order;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import domain.Biclusters;
import domain.Dataset;
import bicpam.bicminer.BiclusterMiner;
import bicpam.closing.BiclusterMerger;
import bicpam.closing.Biclusterizer;
import bicpam.pminer.spm.SequentialPM;


/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class OrderPreservingBiclusterMiner extends BiclusterMiner {

	public enum OrderCriteria { Simple, Strict, Advanced }
	public int overlap;
	
	public Biclusters mineItemsets() throws Exception {
		//BicResult.println(BicPrinting.plot(data.getItems()));
		pminer.setDataset(orderData(data.indexes,data.intscores));
		//BicResult.println("\n\n\nORDER DATASET:\n"+pminer.getDataset());
		pminer.setNrLabels(data.nrLabels);
		((SequentialPM)pminer).setOverlap(overlap); //choose the level of relaxation
		Biclusters bics = pminer.findFrequentItemSets();
		//System.out.println(bics.toString());
		return bics;
	}
	
	public List<List<Integer>> orderData(List<List<Integer>> indexes, List<List<Integer>> scores){
		List<List<Integer>> result = new ArrayList<List<Integer>>();
		for(int i=0, l1=scores.size(); i<l1; i++){
			List<Integer> sorted = new ArrayList<Integer>();
			List<Integer> orders = new ArrayList<Integer>();
			List<Integer> scoresI = new ArrayList<Integer>();
			sorted.addAll(scores.get(i));
			scoresI.addAll(scores.get(i));
			Collections.sort(sorted);
			int index = -1;
			for(Integer val : sorted){
				if(index != scoresI.indexOf(val)) orders.add(-1);
				else scoresI.set(index,-9999);
				index = scoresI.indexOf(val);
				orders.add(indexes.get(i).get(index));
			}
			if(orders.size()>0) orders.remove(0);
			//else System.out.println("WEIRD:"+i);
			result.add(orders);
		}
		//System.out.println(indexes);
		//System.out.println(scores);
		//System.out.println(result);
		return result;
	}
	
	public void setOverlap(int i){
		overlap = i;
	}
	public OrderPreservingBiclusterMiner(Dataset data, SequentialPM pminer, Biclusterizer bichandler, Orientation orientation) {
		super(data,pminer,bichandler,orientation);
	}
	public OrderPreservingBiclusterMiner(Dataset data, SequentialPM pminer, Biclusterizer bichandler, Orientation orientation, int overlapping) {
		this(data,pminer,bichandler,orientation);
		overlap = overlapping;
	}
}
