package domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;

import utils.BicPrinting;
import utils.WekaUtils;
import utils.others.CopyUtils;
import weka.core.Attribute;
import weka.core.Instances;

/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class Dataset {
	
	public static double MISSING = 999999;
	public enum OrientationCriteria {Column, Row, Overall}
	public OrientationCriteria orientation;

	public String name;
	public List<String> rows;
	public List<String> columns;
	public List<Attribute> domains;
	public List<String> originalColumns = null; //originalRows = null;
	
	public int nrLabels;
	public boolean network = false;
	public boolean symmetry = false;
	public double min=1000, max=-1000;
	
	public List<List<Integer>> indexes;
	public List<List<Integer>> outliers;
	public List<List<Double>> scores;
	public List<List<Integer>> intscores;
	public List<List<Integer>> annotations = null;
	public List<Double> mean, std;
	
	public int[] classValues = null;
	public int[] countClasses = null;
	
	/*********************************
	 ******** A: CONSTRUCTORS ********
	 *********************************/

	public Dataset(List<String> _nodes, List<List<Integer>> _indexes, List<List<Double>> _scores) {
		rows = _nodes;
		columns = _nodes;
		indexes = _indexes;
		scores = _scores;
	}
	public Dataset(List<String> _rows, List<String> _columns, List<List<Integer>> _indexes, List<List<Integer>> _scores) {
		rows = _rows;
		columns = _columns;
		indexes = _indexes;
		intscores = _scores;
	}
	public Dataset(List<String> _columns, List<String> _rows, double[][] _matrix, int classIndex){
		if(classIndex<0) classIndex = _columns.size()-1;
		_columns.remove(classIndex);
		rows = _rows;
		columns = _columns;
		domains = new ArrayList<Attribute>();
		for(String col : columns) { 
			Attribute att = new Attribute(col);
			domains.add(att);
		}
		decodeMatrix(_matrix,classIndex);
		classValues = new int[rows.size()];
		Map<Integer,Integer> counts = new HashMap<Integer,Integer>();
		for(int i=0; i<rows.size(); i++) {
			int label = (int)_matrix[i][classIndex];
			classValues[i] = label; 
			if(counts.containsKey(label)) counts.put(label,counts.get(label)+1);
			else counts.put(label, 1);
		}		
		countClasses = new int[counts.size()];
		for(Entry<Integer,Integer> count : counts.entrySet())
			countClasses[count.getKey()]=count.getValue();
	}
	public Dataset(List<String> _columns, List<String> _rows, double[][] _matrix){
		rows = _rows;
		columns = _columns;
		domains = new ArrayList<Attribute>();
		for(String col : columns) { 
			Attribute att = new Attribute(col);
			domains.add(att);
		}
		decodeMatrix(_matrix);
	}
	public Dataset(int[][] _matrix){
		initVariables(_matrix.length,_matrix[0].length);
		decodeMatrix(_matrix);
	}
	public Dataset(double[][] _matrix){
		initVariables(_matrix.length,_matrix[0].length);
		decodeMatrix(_matrix);
	}
	public Dataset(Instances instances){
		this(instances,instances.classIndex());
	}
	public Dataset(Instances instances, int classIndex){
		name = instances.relationName();
		rows = new ArrayList<String>();
		columns = new ArrayList<String>();
		domains = new ArrayList<Attribute>();
		int skipAtt=1;
		if(instances.classIndex()<0) instances.setClassIndex(classIndex);
		if(instances.attribute(0).isString()){
          for(int i=0, l=instances.numInstances(); i<l; i++) 
        	  rows.add(instances.get(i).stringValue(0));
		} else if((instances.attribute(0).isNominal() || instances.attribute(0).isOrdinal())
				&& instances.attribute(0).numValues()==instances.numInstances()){
	          for(int i=0, l=instances.numInstances(); i<l; i++) 
	        	  rows.add(instances.get(i).stringValue(0));
		} else {
			skipAtt=0;
			for(int i=0, l=instances.numInstances(); i<l; i++) rows.add(""+i);
		}
		for(int i=skipAtt, l=instances.numAttributes(); i<l; i++){
			if(i==classIndex) continue;
			Attribute att = instances.getAttributes().get(i);
			columns.add(att.name());
			domains.add(att);
			/*if(att.isOrdinal()) domains.add(AttributeType.Ordinal);
			else if(att.isNominal()) domains.add(AttributeType.Nominal);
			else domains.add(AttributeType.Numeric);*/
		}
		if(classIndex>=0) {
			classValues = new int[instances.numInstances()];
			countClasses = new int[instances.numClasses()];
			for(int i=0; i<instances.numInstances(); i++) {
				if(instances.get(i).isMissing(classIndex)) classValues[i]=-1;
				else {
					int label = (int)instances.get(i).value(classIndex);
					classValues[i] = label; 
					countClasses[label]++;
				}
			}
		}
		decodeMatrix(WekaUtils.toMatrix(instances, skipAtt, classIndex));
		//System.out.println(indexes.get(0)+"\n"+indexes.get(1));
	}
	private void initVariables(int nrows, int ncols) {
		rows = new ArrayList<String>();
		columns = new ArrayList<String>();
		for(int i=0; i<nrows; i++) rows.add(""+i);
		for(int j=0; j<ncols; j++) columns.add(""+j);
	}
	public void decodeMatrix(double[][] matrix) {
		decodeMatrix(matrix,-1);
	}
	public void decodeMatrix(double[][] matrix, int classIndex) {
		indexes = new ArrayList<List<Integer>>();
		scores = new ArrayList<List<Double>>();
		for(int i=0, l1=matrix.length; i<l1; i++){
			indexes.add(new ArrayList<Integer>());
			scores.add(new ArrayList<Double>());
			for(int j=0, l2=matrix[i].length; j<l2; j++){
				if(j==classIndex) continue;
				if(matrix[i][j]==Dataset.MISSING) continue;
				indexes.get(i).add(j);
				scores.get(i).add(matrix[i][j]);
				max=Math.max(max,matrix[i][j]);
				min=Math.min(min,matrix[i][j]);
			}
		}
	}
	private void decodeMatrix(int[][] matrix) {
		indexes = new ArrayList<List<Integer>>();
		intscores = new ArrayList<List<Integer>>();
		for(int i=0, l1=matrix.length; i<l1; i++){
			indexes.add(new ArrayList<Integer>());
			intscores.add(new ArrayList<Integer>());
			for(int j=0, l2=matrix[i].length; j<l2; j++){
				if(matrix[i][j]==Dataset.MISSING) continue;
				indexes.get(i).add(j);
				intscores.get(i).add(matrix[i][j]);
				max=Math.max(max,matrix[i][j]);
				min=Math.min(min,matrix[i][j]);
			}
		}
	}
	
	/****************************
	 ******** B: ITEMIZE ********
	 ****************************/

	public void itemize(List<List<Integer>> _scores, int _nrLabels){ 
		intscores = _scores;
		nrLabels = _nrLabels;
	}
	public void roundValues() {
		intscores = new ArrayList<List<Integer>>();
		for(List<Double> scoresL : scores){
			List<Integer> intscoresL = new ArrayList<Integer>();
			for(Double score : scoresL) intscoresL.add((int)Math.round(score));
			intscores.add(intscoresL);
		}
		nrLabels = (int)Math.round(max)+1;
	}
	
	/****************************
	 ******** C: CONTEXT ********
	 ****************************/
	
	public int nrLabelsContext;
	public List<List<Integer>> context;
	
	public void addContext(double[][] _context){ 
		int maxInt = Integer.MIN_VALUE;
		context = new ArrayList<List<Integer>>();
		for(int i=0; i<indexes.size(); i++){
			List<Integer> entry = new ArrayList<Integer>();
			for(int j : indexes.get(i)) {
				double v = _context[i][j];
				if(v==Dataset.MISSING) entry.add(-1);
				else {
					entry.add((int)v);
					maxInt = (int) Math.max(maxInt, v);
				}
			}
			context.add(entry);
		}
		nrLabelsContext = (int)maxInt+1;
	}
	public int[][] getContext(SortedSet<Integer> columns, SortedSet<Integer> rows) {
		int[][] result = new int[rows.size()][columns.size()];
		int i=0;
		for(int row : rows){
			int j=0;
			for(int column : columns){
				int index = indexes.get(row).indexOf(column);
				result[i][j++]=(index>=0) ? context.get(row).get(index) : 0;
			}
			i++;
		}
		return result;
	}
	
	/**********************/
	/****** D: UTILS ******/	
	/**********************/
	
	public Dataset copy() { return new Dataset(rows,indexes,scores); }
	public int nrNodes() { return rows.size(); }
	public int numClasses() { return countClasses.length; }
	
	public int getElement(int i, int j){
		int index = indexes.get(i).indexOf(j);
		if(index>=0) return intscores.get(i).get(index);
		else return -1;
	}

	public Dataset getPartition(int k) {
		List<List<Integer>> newindexes = new ArrayList<List<Integer>>();
		List<List<Integer>> newscores = new ArrayList<List<Integer>>();
		List<String> newrows = new ArrayList<String>();
		for(int i=0, l=rows.size(); i<l; i++){
			if(classValues[i]!=k) continue;
			newrows.add(rows.get(i));
			newindexes.add(CopyUtils.copyList(indexes.get(i)));
			newscores.add(CopyUtils.copyList(intscores.get(i)));
		}
		return new Dataset(newrows,columns,newindexes,newscores);
	}

	public List<Integer> getPattern(SortedSet<Integer> columns, SortedSet<Integer> rows) {
		List<Integer> result = new ArrayList<Integer>();
		int row = rows.first();
		for(int column : columns){
			int index = indexes.get(row).indexOf(column);
			if(index>=0) result.add(intscores.get(row).get(index));
			else {
				row = rows.last();
				index = indexes.get(row).indexOf(column);
				if(index>=0) result.add(intscores.get(row).get(index));
				else result.add(-1);
			}
		}
		return result;
	}
	public double[][] getBicluster(SortedSet<Integer> columns, SortedSet<Integer> rows) {
		double[][] result = new double[rows.size()][columns.size()];
		int i=0;
		for(int row : rows){
			int j=0;
			for(int column : columns){
				int index = indexes.get(row).indexOf(column);
				int lastIndex = indexes.get(row).lastIndexOf(column);
				if(index>=0) {
					int v=intscores.get(row).get(index);
					if(index==lastIndex) result[i][j++]=intscores.get(row).get(index);
					else result[i][j++]=((double)(v+intscores.get(row).get(lastIndex)))/2.0;
				} else result[i][j++] = 0;				
			}
			i++;
		}
		return result;
	}
	public double[][] getRealBicluster(SortedSet<Integer> columns, SortedSet<Integer> rows) {
		double[][] result = new double[rows.size()][columns.size()];
		int i=0;
		for(int row : rows){
			int j=0;
			for(int column : columns){
				int index = indexes.get(row).indexOf(column);
				result[i][j++]=(index>=0) ? scores.get(row).get(index) : 0;
			}
			i++;
		}
		return result;
	}
	public void invert() {		
		List<List<Integer>> newIndexes = new ArrayList<List<Integer>>();
		List<List<Double>> newScores = new ArrayList<List<Double>>();
		List<List<Integer>> newIntscores = new ArrayList<List<Integer>>();
		int l=(originalColumns==null) ? columns.size() : originalColumns.size();
		for(int j=0; j<l; j++){
			newScores.add(new ArrayList<Double>());
			newIntscores.add(new ArrayList<Integer>());
			newIndexes.add(new ArrayList<Integer>());
		}
		/*System.out.println("rows:\n"+rows);
		System.out.println("cols:\n"+columns);
		System.out.println("Indexes:\n"+indexes);
		System.out.println("Scores:\n"+scores);*/
		
		List<String> newcolumns = rows;
		rows = columns;
		columns = newcolumns;

		if(scores!=null){
			for(int i=0, l1=scores.size(); i<l1; i++){
				for(int j=0, l2=scores.get(i).size(); j<l2; j++){
					int index=indexes.get(i).get(j);
					newIndexes.get(index).add(i);
					double val = scores.get(i).get(j);
					newScores.get(index).add(val);

				}
			}
			scores = newScores;
		}
		if(intscores!=null){
			for(int i=0, l1=indexes.size(); i<l1; i++)
				for(int j=0, l2=indexes.get(i).size(); j<l2; j++){
					if(scores==null) newIndexes.get(indexes.get(i).get(j)).add(i);
					newIntscores.get(indexes.get(i).get(j)).add(intscores.get(i).get(j));
				}
			intscores = newIntscores;
		}
		indexes = newIndexes;
	}
	public void remove(Map<String, Integer> posCount, double removePercentage) {
    	List<Integer> vec = new ArrayList<Integer>();
    	vec.addAll(posCount.values());
    	Collections.sort(vec);
    	if(vec.size()==0) return;
    	int cutoffvalue = vec.get((int)(vec.size()*(1-removePercentage)));
    	//System.out.println("Cutoff value:"+cutoffvalue);
    	for(String pos : posCount.keySet())
    		if(posCount.get(pos)>=cutoffvalue){
    			int row=Integer.valueOf(pos.split(",")[0]), col=Integer.valueOf(pos.split(",")[1]);
    			int index=indexes.get(row).indexOf(col);
    			if(index == -1) continue;
    			indexes.get(row).remove(index);
    			intscores.get(row).remove(index);
    		}
	}

	/**************************/
	/****** E: TO STRING ******/	
	/**************************/
	
	public String toString(boolean network){
		StringBuffer result = new StringBuffer((network?"Nodes: ":"Rows: ")+rows.toString()+"\n");
		if(network) result.append("\n\nNetwork:\n");
		else result.append("Courses: "+columns.toString());
		for(int i=0, l1=indexes.size(); i<l1; i++) {
			result.append("\n"+rows.get(i)+"=>");
			if(intscores != null) {
				boolean[] first = new boolean[columns.size()];
				double[] row = new double[columns.size()];
				for(int j=0, l2=indexes.get(i).size(); j<l2; j++) {
					int col = indexes.get(i).get(j);
				    row[col]+=intscores.get(i).get(j);
				    if(first[col]) row[col]/=2;
				    first[col] = true;
				}
			result.append(BicPrinting.plot(row));
			} else {
				for(int j=0, l2=scores.get(i).size(); j<l2; j++) 
					result.append(indexes.get(i).get(j)+":"+scores.get(i).get(j)+",");
			}
		}
		return result.toString()+"\n";
	}
	public String toIntString() {
		return toIntString(-1);
	}
	public String toIntString(int nmax) {
		StringBuffer result = new StringBuffer("Nodes: "+rows.toString());
		result.append("\n\nNetwork:\n");
		int l1 = nmax>0 ? nmax : indexes.size();
		for(int i=0; i<l1; i++, result.append("\n"))
			for(int j=0, l2=indexes.get(i).size(); j<l2; j++)
				result.append(indexes.get(i).get(j)+":"+intscores.get(i).get(j)+",");
		return result.toString()+"\n";
	}
	
	public String getStatistics() {
		int count = 0;
		for(List<Integer> index : indexes) count+=index.size();
		if(network){
			if(rows.size()==columns.size()) return "Network successfully uploaded ("+rows.size()+" nodes and "+count+" interactions)";
			else return "Network successfully uploaded (|X|="+rows.size()+" |Y|="+columns.size()+" and "+count+" interactions)";
		}
		return "Matrix successfully uploaded (|X|="+rows.size()+" |Y|="+columns.size()+" and "+count+" elements)";
	}
}
