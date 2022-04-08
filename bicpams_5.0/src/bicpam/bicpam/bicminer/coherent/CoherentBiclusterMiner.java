package bicpam.bicminer.coherent;

import java.util.ArrayList;
import java.util.List;

import utils.BicMath;
import utils.BicPrinting;
import domain.Dataset;
import bicpam.bicminer.BiclusterMiner;
import bicpam.closing.BiclusterMerger;
import bicpam.closing.Biclusterizer;
import bicpam.pminer.PM;

/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public abstract class CoherentBiclusterMiner extends BiclusterMiner {
	
	protected List<List<Integer>> alignBy(int j, Dataset data, CoherenceCriteria criteria) {
		List<List<Integer>> intscores = data.intscores;
		List<List<Integer>> indexes = data.indexes;
		List<List<Integer>> result = new ArrayList<List<Integer>>();
		for(int i=0; i<intscores.size(); i++) result.add(new ArrayList<Integer>());
		
		switch(criteria){
			case Constant: //symmetric
				for(int i=0, l1=intscores.size(); i<l1; i++){
					if(!indexes.get(i).contains(j)){
						for(int k=0,s=intscores.get(i).size(); k<s; k++) result.get(i).add(intscores.get(i).get(k));
					} else if(intscores.get(i).get(indexes.get(i).indexOf(j))<0){ 
						for(int k=0,s=intscores.get(i).size(); k<s; k++) result.get(i).add(-intscores.get(i).get(k));
					} else for(int k=0,s=intscores.get(i).size(); k<s; k++) result.get(i).add(intscores.get(i).get(k));
				}
				break;
			case Additive: 
				int max = -1000, min = 1000;
				for(int i=0, l1=intscores.size(); i<l1; i++)
					if(indexes.get(i).contains(j)){
						int index = indexes.get(i).indexOf(j);
						max=Math.max(max, intscores.get(i).get(index));
						min=Math.min(min, intscores.get(i).get(index));
					}
				//BicResult.println("MAX"+max);
				for(int i=0,l=intscores.size(); i<l; i++){
					int difference = 0;
					if(indexes.get(i).contains(j)){
						int index = indexes.get(i).indexOf(j);
						difference = max-intscores.get(i).get(index);
					}
					for(int k=0,s=intscores.get(i).size(); k<s; k++) result.get(i).add(intscores.get(i).get(k)+difference);	
				}
				break;
			case Multiplicative:
				List<Integer> vector = new ArrayList<Integer>();
				for(int i=0,l=intscores.size(); i<l; i++){
					if(indexes.get(i).contains(j)){
						int index = indexes.get(i).indexOf(j);
						vector.add(Math.abs(intscores.get(i).get(index)));
					} else {
						System.out.println("!!");
					}
				}

				int leastCommonMultiple = (vector.size()>0 ? BicMath.lcm(vector) : -1);
				//System.out.println("vec#"+vector.size()+"\t\t"+vector);
				
				for(int i=0,l=intscores.size(); i<l; i++){
					int factor = 1;
					if(indexes.get(i).contains(j)){
						int index = indexes.get(i).indexOf(j);
						int value = intscores.get(i).get(index);
						if(value!=0 && leastCommonMultiple>=0) 
							factor = (value<0 ?-1:1)*leastCommonMultiple/Math.abs(value);
					}
					for(int k=0, s=intscores.get(i).size(); k<s; k++) 
						result.get(i).add(intscores.get(i).get(k)*factor);
				}
				break;
			default: break;
		}
		return result;
	}

	public CoherentBiclusterMiner() {}
	
	public CoherentBiclusterMiner(Dataset data, PM pminer, Biclusterizer bichandler, Orientation criteria) {
		super(data,pminer,bichandler,criteria);
	}
	
}
