package cpam;

import cpam.cmar.PlugableCMAR;
import domain.Biclusters;
import domain.Dataset;
import bicpam.bicminer.BiclusterMiner;
import bicpam.pminer.PM;
import bicpam.pminer.fim.ClosedFIM;
import utils.BicMath;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;

public class CPAM extends BClassifier {

	PlugableCMAR cmar;
	public double support;
	public int minCols;
	public int numLabels;

	public CPAM(double _support, int _minCols, int _numLabels){
		numLabels = _numLabels;
		support=_support;
		minCols=_minCols;
	}
	
	public void buildClassifier(Instances instances) throws Exception {
		cmar = new PlugableCMAR(instances);
        System.out.println("Computing frequent patterns");
        /*data.setNrLabels(numLabels);
    	data.transpose();*/
    	Dataset data = new Dataset(instances);
    	data.nrLabels=numLabels;
    	//data.computeTransactions(0);

    	PM pminer = new ClosedFIM();
    	pminer.inputParams((int)(support*data.rows.size())/2, minCols);
		//BiclusterMiner bicminer = new ColumnConstantBiclusterMiner(data,pminer,biclusterizer,itemizer);
    	//Biclusters allpatterns = bicminer.mineBiclusters();

        for(int i=0, l=instances.numClasses(); i<l; i++){
        	Dataset dataI = new Dataset(instances,i);
        	dataI.nrLabels=numLabels;
        	//dataI.computeTransactions(0);
        	
        	pminer.inputParams((int)(support*dataI.rows.size()), minCols);
        	BiclusterMiner bicminer = null; //ew ColumnConstantBiclusterMiner(dataI,pminer,new Biclusterizer(),new Itemizer());
        	Biclusters ipatterns = bicminer.mineBiclusters();
        	//ipatterns.computeItems(dataI.getItems(),bicminer.orientation);
        	System.out.println("#patterns["+i+"]="+ipatterns.size());
            cmar.addRules(ipatterns.getBiclusters(), null/*data.getItems()*/, i, dataI.rows.size(), numLabels);
        }
        //System.out.println("\n==== CMAR Tree ====\n"+cmar.toString());
    }
	
	public double classifyInstance(Instance instance) throws Exception {
		short[] values = new short[instance.numAttributes()];
		for(int i=0, l=instance.numAttributes(); i<l; i++)
			values[i] = (short) instance.value(i);
		for(int j=0, l=values.length; j<l; j++)
			values[j]=(short)(j*numLabels+values[j]);
		return BicMath.maxindex(cmar.run(values));
	}
	
	public double[] distributionForInstance(Instance instance) throws Exception {
		short[] values = new short[instance.numAttributes()];
		for(int i=0, l=instance.numAttributes(); i<l; i++)
			values[i] = (short) instance.value(i);
		for(int j=0, l=values.length; j<l; j++)
			values[j]=(short)(j*numLabels+values[j]);
		//System.out.println("Inst:"+BicPrinting.plot(values));
		return cmar.run(values);
	}

	public String getModelName() { return "SPMClassifier"; }
	public Capabilities getCapabilities() { return null; }

}
