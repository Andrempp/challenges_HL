package cpam.cmar;

import java.util.List;

import domain.Bicluster;
import weka.core.Instances;

public class PlugableCMAR {
	
	CMAR cl;
	
	public PlugableCMAR(Instances instances){
		cl = new CMAR(instances.numInstances(),instances.numClasses()); // print parsing
	}
	public void addRules(List<Bicluster> ipatterns, int[][] dataset, int condition, int classup, int nitems){
		for(Bicluster bic : ipatterns){
			//System.out.println(bic.toString());
			int support=0;
			for(int i=0, l1=dataset.length; i<l1; i++){
				boolean inc = true;
				int j = 0;
				for(Integer col : bic.columns)
					if(dataset[i][col]!=bic.items.get(j++)){
						inc = false;
						break;
					}
				if(inc) support++;
			}
			double confidence = ((double)bic.rows.size())/((double)support);
			short[] ant = new short[bic.items.size()];
			int j=0;
			for(Integer col : bic.columns){
				ant[j] = (short) ((int) col*nitems+bic.items.get(j));
				j++;
			}
			//System.out.println("\n>> allSup:"+support+",classSup:"+classup+",ruleSupe:"+bic.rows.size()+",conf:"+confidence);
			
			cl.insertRinRlistCMARranking(ant, 
					new short[]{(short)condition},
					support,//sup antecedent
					classup,//sup conseq
					bic.rows.size(),//sup rule
					confidence*100);//confidence
		}
	}
	
	public void prune(){
		//cl.pruneUsingCover();
	}
        
	public double[] run(short[] values) {
		return cl.classifyRecordWCS(values);
	}

	public String toString(){
		return cl.toString();
	}
}
