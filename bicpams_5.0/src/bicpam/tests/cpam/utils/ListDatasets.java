package tests.cpam.utils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import domain.Dataset;
import utils.BicReader;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;


/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class ListDatasets {

	public enum AttributeSelection { NoSelection, Carolina, Ordinal, ALL, NoMV, PAC }; //Ordinal
	
	public static AttributeSelection attribute = AttributeSelection.NoSelection;
	public static boolean allclasses = true;
	public static boolean modelpatients = true;
	
	private static String namespace = "/data/"; 

	public static List<Instances> getRealDatasets() throws FileNotFoundException, IOException {
		String path = "data/classifier/"; 
		List<String> names = new ArrayList<String>();
		List<Instances> datasets = new ArrayList<Instances>();
		//names.add(path+"Heritage.arff");//atts with same name
		//names.add(path+"ColonDiff62x2000.txt");//atts with same name
		//names.add(path+"Lymphoma45x4026L2.arff");
		//names.add(path+"Lymphoma96x4026L9.arff");
		//names.add(path+"Lymphoma96x4026L11.arff");
		//names.add(path+"GlobalCancer190x16063.arff");//L14 3Min with J48; good res
		names.add(path+"Leukemia72x7129.arff");
		//names.add(path+"ECML90x27679.arff");//slow
		
		for(String name : names){
			Instances dataset = new Instances(new BufferedReader(new FileReader(name))); 
			dataset.setClassIndex(dataset.numAttributes()-1);
			datasets.add(dataset);
		}
		return datasets;
	}

	public static List<Instances> getSyntheticDatasets() throws Exception {
		//DefaultDatasets defaultdata = new DefaultDatasets();
		List<Dataset> datasets = new ArrayList<Dataset>();//defaultdata.getClassifierSyntheticData();
		List<Instances> datasetsArff = new ArrayList<Instances>();
		//datasetsArff.add(convertToArff(datasets.get(0).getData(),datasets.get(0).name,datasets.get(0).getNrLabels()));
		for(Dataset dataset : datasets){
			Instances datasetArff = null; //convertToArff(dataset.getData(),dataset.name,dataset.getNrLabels());
			datasetArff.setClassIndex(datasetArff.numAttributes()-1);
			datasetsArff.add(datasetArff);
		}
		return datasetsArff;
	}
	
	public static List<Instances> getDatasets() throws Exception {
		List<Instances> datasets = new ArrayList<Instances>();
		if(allclasses) namespace += "3classes/";
		namespace += modelpatients ? "modelo/" : "all/";
		String suffix = modelpatients ? "N36" : "N104";
		
		List<String> datanames = new ArrayList<String>();
		if(attribute.equals(AttributeSelection.NoSelection)) {
			AttributeSelection[] values = AttributeSelection.values(); 
			for(int i=1, l=values.length; i<l; i++)
				datanames.add(values[i].toString().toLowerCase() + "Att" + suffix);
		} else datanames.add(attribute.toString().toLowerCase() + "Att" + suffix);
		
		for(String name : datanames){
			Instances dataset = BicReader.getInstances(namespace + name + ".arff");
			dataset.setRelationName(name);
			dataset.setClassIndex(0);
			dataset.deleteAttributeAt(1);
			datasets.add(dataset);
		}
		return datasets;
	}

	public static List<Instances> getDatasets(AttributeSelection att, boolean model) throws Exception {
		modelpatients = model;
		attribute = att;
		return getDatasets();
	}
	public static List<Instances> getDatasets(AttributeSelection att) throws Exception {
		attribute = att;
		return getDatasets();
	}
	public static Instances convertToArff(Dataset dataset) {
		FastVector atts = new FastVector();
		for(int j=0, l=dataset.columns.size(); j<l; j++) atts.addElement(new Attribute("A"+j));					
		FastVector classes = new FastVector();
		int numClasses = dataset.countClasses.length;
		for(int i=0; i<numClasses; i++) classes.addElement(i+"");
		atts.addElement(new Attribute("class",classes));
			
		Instances data = new Instances(dataset.name,atts,0);
		for(int i=0, l1=dataset.indexes.size(); i<l1; i++){
			double[] row = new double[dataset.columns.size()+1];
			for(int k=0, l2=dataset.indexes.get(i).size(); k<l2; k++)
				row[dataset.indexes.get(i).get(k)]=dataset.scores.get(i).get(k);
			row[dataset.columns.size()] = dataset.classValues[i];
			data.add(new DenseInstance(1,row));
		}
		return data;
	}

}

