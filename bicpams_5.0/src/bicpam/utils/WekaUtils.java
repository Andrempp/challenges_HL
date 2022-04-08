package utils;

import java.util.ArrayList;
import java.util.List;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import domain.Dataset;


/** @author Rui Henriques
 *  @version 1.0
 */
public class WekaUtils {
	
	public static String plot(Instances insts) {
      StringBuffer text = new StringBuffer();
	  for(int i=0; i<insts.numAttributes(); i++) 
		  text.append(insts.attribute(i).toString() + ";");
	  text.append("\n");
	  for(int m=0; m<insts.numInstances(); m++){
	    Instance inst = (Instance) insts.instance(m);
	    for (int i = 0; i < inst.numAttributes(); i++) {
	        if (i > 0) text.append(",");
	        Attribute att = inst.attribute(i); 
	        switch (att.type()) {
	            case Attribute.NOMINAL:
	            case Attribute.STRING: text.append(att.value((int) inst.value(i)));	break;
	            case Attribute.NUMERIC: text.append(Utils.doubleToString(inst.value(i),6)); break;
	            default: throw new IllegalStateException("Unknown attribute type");
	        }
	    }
	    text.append("\n");
	  }
	  return text.toString();
	}

	public static Instances toInstances(List<List<Integer>> indexes, List<List<Double>> scores, ArrayList<Attribute> atts) {
		Instances instances;
		//ArrayList<Attribute> atts = new ArrayList<Attribute>();
		//for(int j=0; j<ncols; j++) atts.add(new Attribute("C"+j,j));
		instances = new Instances("BicData",atts,indexes.size());
		for(int i=0,l=indexes.size(); i<l; i++) {
			double[] array = new double[atts.size()];
			Instance inst = new DenseInstance(1,array);
			for(int j=0; j<atts.size(); j++) 
				if(!indexes.get(i).contains(j)) inst.setMissing(j);//array[j]=-1; 
				else inst.setValue(j,scores.get(i).get(indexes.get(i).indexOf(j)));
			instances.add(inst);
		}
		return instances;
	}
	public static Instances toInstances(double[][] matrix){
		Instances instances;
		ArrayList<Attribute> atts = new ArrayList<Attribute>();
		for(int j=0,l=matrix[0].length; j<l; j++) atts.add(new Attribute("C"+j,j));
		instances = new Instances("BicData",atts,matrix.length);
		for(int i=0,l=matrix.length; i<l; i++) {
			int size = matrix[i].length;
			double[] array = new double[size];
			Instance inst = new DenseInstance(1,array);
			for(int j=0; j<size; j++) 
				if(matrix[i][j] == Dataset.MISSING) inst.setMissing(j);//array[j]=-1; 
				else inst.setValue(j,matrix[i][j]);
			instances.add(inst);
		}
		return instances;
	}
	public static Instances toDiscreteInstances(Dataset data) {
		Instances instances;
		int ncols = data.columns.size();
		ArrayList<Attribute> atts = new ArrayList<Attribute>();
		List<String> attVals = new ArrayList<String>();
	    for(int i=0, l=data.nrLabels; i<l; i++) attVals.add("v"+i);
		for(int j=0; j<ncols; j++) atts.add(new Attribute("C"+j,attVals));
		if(data.classValues != null){
			int numClasses = data.countClasses.length;
			List<String> classes = new ArrayList<String>();
		    for(int i=0; i<numClasses; i++) classes.add("c"+i);
			atts.add(new Attribute("class",classes));
		}
		int c = data.classValues != null ? 1 : 0;
		instances = new Instances(data.name,atts,data.indexes.size());
		for(int i=0,l=data.indexes.size(); i<l; i++) {
			double[] array = new double[data.columns.size()+c];
			Instance inst = new DenseInstance(1,array);
		    for(int j=0; j<ncols; j++) 
				if(!data.indexes.get(i).contains(j)) inst.setMissing(j); 
				else inst.setValue(j,data.intscores.get(i).get(data.indexes.get(i).indexOf(j)));
			if(c==1) inst.setValue(ncols,data.classValues[i]);
			//System.out.println(inst.toString());
			instances.add(inst);
		}
	    //BicResult.println(instances.toString());
		if(c==1) instances.setClassIndex(instances.numAttributes()-1);
		return instances;
	}

	public static double[][] toMatrix(Instances instances, int skip, int classindex) {
		boolean skipclass = classindex>=0;
		double[][] matrix = new double[instances.numInstances()][instances.numAttributes()-skip-(skipclass?1:0)];
		for(int j=skip; j<instances.numAttributes(); j++) {
			if(j==classindex) continue;
			int add = (skipclass && j>classindex) ? 1 : 0;
			for(int i=0; i<instances.numInstances(); i++) {
				if(instances.get(i).isMissing(j)) matrix[i][j-skip-add]=Dataset.MISSING;
				else matrix[i][j-skip-add]=instances.get(i).value(j);
			}
		}
		return matrix;
	}

}
