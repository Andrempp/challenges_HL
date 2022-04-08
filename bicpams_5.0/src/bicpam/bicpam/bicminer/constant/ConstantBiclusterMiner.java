package bicpam.bicminer.constant;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import domain.Biclusters;
import domain.Dataset;
import utils.BicResult;
import bicpam.bicminer.BiclusterMiner;
import bicpam.closing.BiclusterMerger;
import bicpam.closing.Biclusterizer;
import bicpam.mapping.ItemMapper;
import bicpam.mapping.Itemizer;
import bicpam.pminer.PM;

/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class ConstantBiclusterMiner extends BiclusterMiner {
	
	boolean vertical = false;
	
	public Biclusters mineItemsets() throws Exception {
		long time = System.currentTimeMillis();
		Biclusters itemsets = new Biclusters();
		if(vertical) itemsets = verticalSearch();
		else {
			boolean hasContext = data.context!=null;
			List<List<Integer>> dataset = hasContext ? ItemMapper.itemizeWithContext(data): ItemMapper.itemize(data); 
			if(hasContext) pminer.setNrLabels(data.nrLabels*data.nrLabelsContext);
			else pminer.setNrLabels(data.nrLabels);
	
			//BicResult.println("A> "+dataset.toString());
			//BicResult.println("B> "+context.toIntString());
			//BicResult.println("C> "+dataset_new.toString());
			pminer.setDataset(dataset);
			itemsets = pminer.findFrequentItemSets();
		}
		time = System.currentTimeMillis() - time;
		return itemsets;
	}
	
	public Biclusters verticalSearch() {
		Biclusters itemsets = new Biclusters();
		System.out.println("Vertical search\n"+data.indexes.size()+"\n"+data.rows.size());
		/* TODO: vertical search 
		pminer.setNrLabels(1);
		List<List<Integer>> valuesT = new ArrayList<List<Integer>>(), indexesT = new ArrayList<List<Integer>>();
		for(int i=0, l=data.columns.size(); i<l; i++){
			valuesT.add(new ArrayList<Integer>());
			indexesT.add(new ArrayList<Integer>());
		}
		for(int i=0, l1=data.indexes.size(); i<l1; i++){
			List<Integer> indexes = data.indexes.get(i);
			List<Integer> values = data.intscores.get(i);
			for(int j=0, l2=indexes.size(); j<l2; j++){
				valuesT.get(indexes.get(j)).add(values.get(j));
				indexesT.get(indexes.get(j)).add(i);
			}
		}
		for(int k=0, l=data.indexes.size(); k<l; k++){
			List<List<Integer>> aligned = new ArrayList<List<Integer>>();
			for(int i=0, l1=indexesT.size(); i<l1; i++){
				//List<Integer> seila = data.indexes.get(i);
				int index=indexesT.get(i).indexOf(k);
				List<Integer> indexL = new ArrayList<Integer>();
				if(index>=0){
					int value=valuesT.get(i).get(index);
					for(int j=0, l2=valuesT.get(i).size(); j<l2; j++)
						if(valuesT.get(i).get(j)==value) indexL.add(indexesT.get(i).get(j));
				} 
				aligned.add(indexL);
			}
			//BicResult.println(aligned.toString());
			List<List<Integer>> dataset = ItemMapper.itemize(aligned); 
			pminer.setDataset(dataset);
			Biclusters newitemsets = pminer.findFrequentItemSets();
			newitemsets.transpose();
			itemsets.addAll(newitemsets);
		}*/
		return itemsets;
	}
	
	public ConstantBiclusterMiner() {}
	
	public ConstantBiclusterMiner(Dataset data, PM pminer, Biclusterizer bichandler, Orientation orientation) {
		super(data,pminer,bichandler,orientation);
	}
}
