package cpam.others;

import cpam.BClassifier;
import cpam.cmar.PlugableCMAR;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;

public class SPMClassifier extends BClassifier {

	PlugableCMAR cmar;
	public int minRows, minCols;
	public int nitems;

	public SPMClassifier(int _minRows, int _minCols, int numItems){
		nitems = numItems;
		minRows=_minRows;
		minCols=_minCols;
	}

	public void buildClassifier(Instances instances) throws Exception {
		/*System.out.println("BUILD");
		cmar = new PlugableCMAR(instances);

        System.out.println("Computing frequent temporal patterns");
        Dataset data = new Dataset(instances);
    	data.setNrLabels(nitems);

        List<Bicluster> allpatterns = new CoherentBMiner(data).run(minRows/instances.numClasses(),minCols);
    	//System.out.println("#all:"+allpatterns.getSupport().size());
    	System.out.println("ALL"+allpatterns.toString());

        for(int i=0, l=instances.numClasses(); i<l; i++){
        	Dataset dataI = new Dataset(instances,i);
        	dataI.setNrLabels(nitems);
        	List<Bicluster> ipatterns = new CoherentBMiner(dataI).run(minRows,minCols);
        	System.out.println("PAT["+i+"]"+ipatterns.toString());
        	//System.out.println("#pat:"+ipatterns.getSupport().size());
            cmar.addRules(ipatterns, allpatterns, i, data.getNrTrans());
        }
        System.out.println(cmar.toString());*/
    }
    
	public double classifyInstance(Instance instance) throws Exception {
		//return BMath.maxindex(cmar.run(instance));
		return 0;
	}
	public double[] distributionForInstance(Instance instance) throws Exception {
		//return cmar.run(instance);
		return null;
	}
	public String getModelName() {
		return "SPMClassifier";
	}

	public Capabilities getCapabilities() {
		return null;
	}
}
