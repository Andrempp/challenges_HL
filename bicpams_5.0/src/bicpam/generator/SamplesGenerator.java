package generator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SamplesGenerator {

	int[] itemfrequencies;
	int transactions;
	
	public SamplesGenerator(int[] itemFrequencies, int length) {
		itemfrequencies = itemFrequencies;
		transactions = length;
	}

	public int[][] generateDataFrequencies() {
		List<List<Integer>> data = new ArrayList<List<Integer>>();
		for(int i=0, l=itemfrequencies.length; i<l; i++){
			List<Integer> itemtrans = new ArrayList<Integer>();
			Random r = new Random(itemfrequencies[i]);
			for(int j=0, s=itemfrequencies[i]; j<s; j++){
				int trans = r.nextInt();
				if(itemtrans.contains(trans)) j--;
				else itemtrans.add(trans);
			}
			data.add(itemtrans);
		}
		//int[][] result = new int[transactions][];
		return null;
	}

	/*public int[][] generateDataProperties() throws Exception {
		DefaultDatasets datagen = new DefaultDatasets();
		datagen.defaultdataset = true;
		ItemMapper.itemize(null,null,0,false); 
		//datagen.getBicSyntheticData(PatternType.Constant).get(0));
		return null;
	}*/
}