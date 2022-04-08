package bicpam.mapping;

import domain.Dataset;
import utils.WekaUtils;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;

import java.util.ArrayList;

import bicpam.mapping.Itemizer.FillingCriteria;


/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class MissingsHandler {

	public static Dataset run(Dataset data, FillingCriteria criteria) throws Exception{
		Instances instances = null;
		switch(criteria) {
			case Replace: 
				instances = WekaUtils.toInstances(data.indexes,data.scores,(ArrayList)data.domains);
				ReplaceMissingValues filler = new ReplaceMissingValues();
			    filler.setInputFormat(instances);
				instances = Filter.useFilter(instances, filler);
				data.decodeMatrix(WekaUtils.toMatrix(instances,0,-1));
				return data;
			case RemoveValue :
				//System.out.println("Removing missings");
				int c=0;
				if(data.scores==null){
					for(int i=0, l1=data.intscores.size(); i<l1; i++){
						for(int j=data.intscores.get(i).size()-1; j>=0; j--){
							if(data.intscores.get(i).get(j)==Dataset.MISSING){
								c++;
								data.intscores.get(i).remove(j);
								data.indexes.get(i).remove(j);
							}
						}
					}
					//System.out.println("Total:"+c);					
				} else {
					for(int i=0, l1=data.scores.size(); i<l1; i++){
						for(int j=data.scores.get(i).size()-1; j>=0; j--){
							if(data.scores.get(i).get(j)==Dataset.MISSING){
								c++;
								data.scores.get(i).remove(j);
								data.indexes.get(i).remove(j);
							}
						}
					}
					//System.out.println("Total:"+c);
				}
			default :;
		}
		return data;
	}
}
