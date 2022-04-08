package utils;

import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;

/** Class for debugging the application (by logging)
 *  @author Rui Henriques
 *  @version 1.0
 */
public final class BicPrinting {

    public static String plot(double[] e) {
    	String result = new String("");
    	for(int i=0, l=e.length;i<l;i++) result += String.format("%.2f",e[i]) + "|"; //i + ":" + e[i] + ","; 
    	return result;
    }    
    
    public static String plot(int[] e, String str) {
    	String result = new String("\nPlot of " + str + ":\n");
    	for(int i=0, l=e.length;i<l;i++) result += e[i] + "|";//i + ":" + 
    	return result;
    }

    public static String plot(double[][] matrix) {
    	StringBuffer result = new StringBuffer();
		for(int i=0; i < matrix.length; i++){
			for(int j=0, l=matrix[0].length; j<l; j++)
				result.append(String.format("%.3f",matrix[i][j])+";");
			result.append("\n"); 
		}
    	return result.toString();
    }
    public static String roughplot(double[][] matrix) {
    	StringBuffer result = new StringBuffer();
    	//result.append("MATRIX:\n");
		for(int i=0; i < matrix.length; i++){
			for(int j=0, l=matrix[i].length; j<l; j++) result.append(matrix[i][j] + ",");
			result.append("\n"); 
		}
    	return result.toString();
    }
	public static String plot(String[][] matrix) {
    	StringBuffer result = new StringBuffer();
		for(int i=0, l2=matrix.length; i<l2; i++){
			for(int j=0, l3=matrix[i].length; j<l3; j++) 
				result.append(matrix[i][j] + ",");
			result.append("\n");
		}
    	return result.toString();
	}
	public static String plot(String[][][] matrix) {
    	StringBuffer result = new StringBuffer();
		for(int k=0, l1=matrix.length; k<l1; k++){
			result.append("\n========= k:"+k+" ==========\n");
			for(int i=0, l2=matrix[k].length; i<l2; i++){
				for(int j=0, l3=matrix[k][i].length; j<l3; j++) 
					result.append(matrix[k][i][j] + ",");
				result.append("\n");
			}
		}
    	return result.toString();
	}
	public static String plot(double[][][] matrix) {
    	StringBuffer result = new StringBuffer();
		for(int k=0, l1=matrix.length; k<l1; k++){
			result.append("\n========= k:"+k+" ==========\n");
			for(int i=0, l2=matrix[k].length; i<l2; i++){
				for(int j=0, l3=matrix[k][i].length; j<l3; j++) 
					result.append(matrix[k][i][j] + ",");
				result.append("\n");
			}
		}
    	return result.toString();
	}
    public static String plot(int[][] matrix) {
    	StringBuffer result = new StringBuffer();
    	//result.append("MATRIX:\n");
		for(int i=0; i < matrix.length; i++){
			for(int j=0, l=matrix[i].length; j<l; j++) result.append(matrix[i][j] + ",");
			result.append("\n"); 
		}
    	return result.toString();
    }

	public static String printInstances(Instances insts) {
      StringBuffer text = new StringBuffer();
	  for(int i=0; i<insts.numAttributes(); i++) 
		  text.append(insts.attribute(i).toString() + ";");
	  text.append("\n");
	  for(int m=0; m<insts.numInstances(); m++){
	    Instance inst = (Instance) insts.get(m);
	    for (int i = 0; i < inst.numAttributes(); i++) {
	        if (i > 0) text.append(",");
	        Attribute att = inst.attribute(i); 
	        switch (att.type()) {
	            case Attribute.NOMINAL:
	            case Attribute.STRING: text.append(att.value((int) inst.value(i)));	break;
	            case Attribute.RELATIONAL:
	            	Instances instsrel = inst.relationalValue(i);
	            	for(int j=0; j<instsrel.numInstances(); j++) {
	            		Instance inst2 = instsrel.instance(j);
		            	text.append("(");
		            	for(int k=0; k<inst2.numValues(); k++) {
		            		if(k!=0) text.append(" ");
		            		if(inst2.attribute(k).isNominal()) text.append(inst2.stringValue(k));
		            		else text.append(inst2.value(k));
		            	}
		            	text.append(")");
	            	}
	            	break;
	            case Attribute.NUMERIC: text.append(Utils.doubleToString(inst.value(i),6)); break;
	            default: throw new IllegalStateException("Unknown attribute type");
	        }
	    }
	    text.append("\n");
	  }
	  return text.toString();
	}

	public static String plot(boolean[][] matrix) {
    	StringBuffer result = new StringBuffer();
    	result.append("MATRIX:\n");
		for(int i=0; i < matrix.length; i++){
			for(int j=0, l=matrix[0].length; j<l; j++) result.append((matrix[i][j] ? "t" : "f") + ",");
			result.append("\n"); 
		}
    	return result.toString();
	}

	public static String plot(Integer[] index, double[] matrix) {
    	StringBuffer result = new StringBuffer();
		for(int i=0; i < matrix.length; i++) result.append(matrix[index[i]] + ",");
    	return result.toString() + "\n";
	}

	public static String plot(Integer[] e, String str) {
    	String result = new String("\nPlot of " + str + ":\n");
    	for(int i=0, l=e.length;i<l;i++) result += i + ":" + e[i] + ",";
    	return result;
	}

	public static String plot(float[][] matrix) {
    	StringBuffer result = new StringBuffer();
    	result.append("MATRIX:\n");
		for(int i=0; i < matrix.length; i++){
			for(int j=0, l=matrix[0].length; j<l; j++) result.append(matrix[i][j] + ",");
			result.append("\n");
		}
		return result.toString();
	}

	public static String plot(String[] conditions) {
    	String result = "";
    	for(int i=0, l=conditions.length;i<l;i++) result += conditions[i] + ",";
    	return result;
	}

	public static String plot(float[] vec) {
    	String result = "";
    	for(int i=0, l=vec.length;i<l;i++) result += vec[i] + ",";
    	return result;
	}

	public static String plot(int[] vec) {
    	String result = "";
    	for(int i=0, l=vec.length;i<l;i++) result += vec[i] + ",";
    	return result;
	}
}
