package bicpam.mapping;

import java.util.ArrayList;
import java.util.List;

import utils.BicMath;
import utils.BicResult;
import domain.Dataset;
import bicpam.mapping.Itemizer.NormalizationCriteria;

/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class Normalizer {
	
	public static Dataset run(Dataset data, NormalizationCriteria criteria) throws Exception { 
		double mean, std;		
		List<List<Double>> scores = (data.scores==null) ? new ArrayList<List<Double>>() : data.scores;
		List<List<Double>> result = new ArrayList<List<Double>>();
		for(int i=0, l1=scores.size(); i<l1; i++) result.add(new ArrayList<Double>());
		
		switch(criteria) {
			case Overall :
				mean = BicMath.meanL(scores); 
				std = BicMath.stdL(scores);
				for(int i=0, l1=scores.size(); i<l1; i++){
					for(int j=0, l2=scores.get(i).size(); j<l2; j++)
						result.get(i).add((scores.get(i).get(j)-mean)/std);
				}
				//System.out.println("MEAN:"+BicMath.meanL(result));
				//System.out.println("STD:"+BicMath.stdL(result));
				break;
			case Row :
				for(int i=0, l1=scores.size(); i<l1; i++){
					mean = BicMath.mean(scores.get(i)); 
					std = BicMath.std(scores.get(i));
					for(int j=0, l2=scores.get(i).size(); j<l2; j++){
						if(std==0) result.get(i).add(0.0); 
						else result.get(i).add((scores.get(i).get(j)-mean)/std);
					}
				}
				break;
			case IshiiRow :
				int c=0, c2=0;
				for(int i=0, l1=scores.size(); i<l1; i++){ 
					if(data.indexes.get(i).size()>0 && data.indexes.get(i).get(0)==1) {
						c++;
						mean = scores.get(i).get(0);
					} else {
						//System.out.print(data.rows.get(i)+",");
						mean = BicMath.mean(scores.get(i)); 
						c2++;
					}
					std = BicMath.std(scores.get(i));
					for(int j=0, l2=scores.get(i).size(); j<l2; j++){
						if(std==0) result.get(i).add(0.0); 
						else result.get(i).add((scores.get(i).get(j)-mean)/std);
					}
				}
				//System.out.println("\nCOUNTS = "+c+" | "+c2);
				break;
			case Column :
				data.mean = new ArrayList<Double>();
				data.std = new ArrayList<Double>();
				for(int j=0, l2=data.columns.size(); j<l2; j++){
					if(!data.domains.get(j).isNumeric()) {
						for(int i=0, l1=scores.size(); i<l1; i++) {
							int v = data.indexes.get(i).indexOf(j);
							if(v>=0) result.get(i).add(scores.get(i).get(v));
						}
						data.mean.add(-1.);
						data.std.add(-1.);
						continue;
					}
					List<Double> colVec = new ArrayList<Double>();
					for(int i=0, l1=scores.size(); i<l1; i++){
						if(data.indexes.get(i).contains(j))
							colVec.add(scores.get(i).get(data.indexes.get(i).indexOf(j)));
					}
					mean = BicMath.mean(colVec); 
					std = BicMath.std(colVec);
					data.mean.add(mean);
					data.std.add(std);
					
					for(int i=0, l1=scores.size(); i<l1; i++){
						if(data.indexes.get(i).contains(j)){
							if(std==0) result.get(i).add(0.0); 
							else {
								double value = scores.get(i).get(data.indexes.get(i).indexOf(j));
								result.get(i).add((value-mean)/std);
							}
						}
					}
				}
				break;
			default : 
				return data;
		}
        double min=1000, max=-1000;
        for(List<Double> scoresL : result){
        	for(Double iscore : scoresL){
        		min=Math.min(min, iscore);
        		max=Math.max(max, iscore);
        	}
        }
        data.min=min;
        data.max=max;
		//BicResult.println(">"+data.scores);
		//BicResult.println(">"+result);
		data.scores = result;
		return data;
	}
}
