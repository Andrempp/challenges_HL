package domain;

import generator.BicMatrixGenerator.PatternType;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import utils.BicMath;
import utils.BicPrinting;
import bicpam.bicminer.BiclusterMiner.Orientation;
import bicpam.pminer.spm.algo.Sequence;


/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class Bicluster {
	
	
	/*********************
	 ***** A: STATE ****** 
	 *********************/
	
	public String key;
	public SortedSet<Integer> columns;
	public SortedSet<Integer> rows;
	public List<Integer> items;
	public List<Integer> orders;
	public List<Integer> rowFactors;
	public PatternType type;
	public double pvalue = -1;
	public int partition = -1;

	
	/****************************
	 ***** B: CONSTRUCTORS ****** 
	 ****************************/
	
	public Bicluster(SortedSet<Integer> _rows, SortedSet<Integer> _columns){
		columns = _columns;
		rows = _rows;
	}
	public Bicluster(SortedSet<Integer> _rows, SortedSet<Integer> _columns, List<Integer> _items, PatternType _type){
		columns = _columns;
		rows = _rows;
		items = _items;
		type = _type;
	}
	public Bicluster(int[] _rows, int[] _cols) {
		rows = new TreeSet<Integer>();
		for(int i=0; i<_rows.length; i++) rows.add(_rows[i]);
		columns = new TreeSet<Integer>();
		for(int i=0; i<_cols.length; i++) columns.add(_cols[i]);
	}
	
	
	/***********************
	 ***** C: METHODS ****** 
	 ***********************/
	
	public Bicluster invert(){
		SortedSet<Integer> aux = columns;
		columns = rows; 
		rows = aux;
		return this;
	}
	public double overlapArea(Bicluster bicluster2) {
		boolean greater = area()>bicluster2.area();
		double maxrows = greater ? rows.size() : bicluster2.rows.size();
		double maxcols = greater ? columns.size() : bicluster2.columns.size();
		Set<Integer> _columns = new HashSet<Integer>();
		Set<Integer> _rows = new HashSet<Integer>();
		_columns.addAll(columns);
		_columns.retainAll(bicluster2.columns);
		_rows.addAll(rows);
		_rows.retainAll(bicluster2.rows);
		return (_columns.size()*_rows.size())/(maxrows*maxcols);
	}
	public void shiftRows(int i) {
		SortedSet<Integer> shiftedRows = new TreeSet<Integer>();
		for(Integer row : rows) shiftedRows.add(row+i);
		rows = shiftedRows;
	}
	public void shiftColumns(int i) {
		SortedSet<Integer> shiftedColumns = new TreeSet<Integer>();
		for(Integer column : columns) shiftedColumns.add(column+i);
		columns = shiftedColumns;
	}
	public void computePattern(Dataset data, Orientation orientation) {
		double[][] matrix = data.getBicluster(columns,rows);
		items = new ArrayList<Integer>();
		if(orientation.equals(Orientation.PatternOnRows)){
			for(int j=0, l2=matrix[0].length; j<l2; j++){
				List<Double> vec = new ArrayList<Double>();
				for(int i=0, l=matrix.length; i<l; i++) vec.add(matrix[i][j]);
				items.add((int)Math.round(BicMath.mean(vec)));
			}
		} else {
			for(int i=0, l=matrix.length; i<l; i++) items.add((int)Math.round(BicMath.mean(matrix[i])));
		}
		//System.out.println("=>"+items);
	}
	public boolean filter(Bicluster bic, double overlapMax) {
		int c1=bic.columns.size();
		int minColumns = (int) (overlapMax*(double)Math.max(columns.size(),c1));
		SortedSet<Integer> cnews = news(columns.iterator(),bic.columns.iterator());
		if(c1-cnews.size()<minColumns) return false;
		int minArea = (int) (overlapMax*(double)Math.max(bic.area(),area()));
		SortedSet<Integer> rnews = news(rows.iterator(),bic.rows.iterator());
		if(cnews.size()==0 && columns.size()==c1 && rnews.size()==0) return true;
		if((bic.rows.size()-rnews.size())*(c1-cnews.size())<minArea) return false;
		else return true;
	}
	public boolean filterRows(Bicluster bic, double overlapMax) {
		int minRows = (int) (overlapMax*(double)Math.max(rows.size(),bic.rows.size()));
		SortedSet<Integer> rnews = news(rows.iterator(),bic.rows.iterator());
		if((bic.rows.size()-rnews.size())<minRows) return false;
		else return true;
	}
	public boolean filterColumns(Bicluster bic, double overlapMax) {
		int minCols = (int) (overlapMax*(double)Math.max(columns.size(),bic.columns.size()));
		SortedSet<Integer> cnews = news(columns.iterator(),bic.columns.iterator());
		if((bic.columns.size()-cnews.size())<minCols) return false;
		else return true;
	}
	
	/******************************
	 ***** D: DISCRIMINATIVE ****** 
	 ******************************/

	public int condition = -1;
	public double score = 0;
	public int[] support;
	public double[] weightedSupport;
	public double conf=-1, wconf=-1, lift=-1, wlift=-1;
	public double chi2=-1, chi2UB=-1;

	public List<Double> lifts = null;
	public double computeLifts(int[] classValues, int[] supports, int nrows, int targetClass, boolean liftSuperiority) {
		Map<Integer,Double> counts = new HashMap<Integer,Double>();
		for(int i=0; i<supports.length; i++) counts.put(i,0.0);
		for(Integer row : rows) { 
			int classValue = classValues[row];
			counts.put(classValue, counts.get(classValue)+1);
		}
		double maxLift = Double.MIN_VALUE;
		lifts = new ArrayList<Double>();
		for(int i=0; i<supports.length; i++) {
			//lift(a=>b)=sup(ab)/(sup(a)sup(b))
			double supAB= counts.get(i)/nrows;
			double supA = ((double)rows.size())/nrows;
			double supB = ((double)supports[i])/nrows;
			double lift = supAB/(supA*supB); 
			lifts.add(lift);
			if(lift>maxLift) maxLift = lift;
		}
		//System.out.println(lifts);
		if(targetClass>=0) {
			double classLift = lifts.get(targetClass);
			if(liftSuperiority && classLift<maxLift) return -1; 
			return classLift;
		}
		return maxLift;
	}

	
	/***********************
	 ***** D: MERGING ****** 
	 ***********************/

	public Bicluster merge(Bicluster bic, double overlapMax) {
		int c1=bic.columns.size(), r1=bic.rows.size(); 
		int maxCols=Math.max(columns.size(),c1), maxRows=Math.max(rows.size(),r1);
		int minColumns = (int) (overlapMax*(double)maxCols);
		if(minColumns==0) minColumns++;
		SortedSet<Integer> cnews = news(columns.iterator(),bic.columns.iterator());
		//System.out.println("C1:"+c1+",CNews:"+cnews.size()+"MinCols:"+minColumns);
		if(c1-cnews.size()<minColumns) return null;
		//auxiliar="c1:"+c1+"cnewsize"+cnews.size()+"MinCols:"+minColumns+"\n";
		//auxiliar+="CNews:"+cnews+"\n";

		//System.out.println("("+rows.size()+","+bic.rows.size()+")");
		SortedSet<Integer> rnews = news(rows.iterator(),bic.rows.iterator());
		if(cnews.size()==0 && columns.size()==c1 && rnews.size()==0){
			SortedSet<Integer> irows = new TreeSet<Integer>(rows);
			SortedSet<Integer> icols = new TreeSet<Integer>(columns);
			return new Bicluster(irows,icols);
		}
		//auxiliar+="MinArea:"+minArea+"\n";
		//auxiliar+="RNews:"+rnews+"\n";
		//System.out.println(auxiliar);

		/*int minArea = (int) (overlapMax*(double)Math.max(bic.area(),area()));
		if(minArea==0) minArea++;
		if((r1-rnews.size())*(c1-cnews.size())<minArea) return null;*/
		if(overlapArea(bic)<overlapMax) return null;
		else {
			SortedSet<Integer> irows = new TreeSet<Integer>(rows);
			SortedSet<Integer> icols = new TreeSet<Integer>(columns);
			irows.addAll(rnews);
			icols.addAll(cnews);
			return new Bicluster(irows,icols);
		}
	}
	private SortedSet<Integer> news(Iterator<Integer> it1, Iterator<Integer> it2) {
		SortedSet<Integer> over = new TreeSet<Integer>();
		int i1 = it1.next(), i2 = it2.next();
		while(true){
			if(i1<i2){
				if(it1.hasNext()) i1=it1.next();
				else {
					over.add(i2);
					while(it2.hasNext()){
						i2=it2.next();
						over.add(i2);
					}
					break;
				}
			} else if(i2<i1){
				over.add(i2);
				if(it2.hasNext()) i2=it2.next();
				else break;
			} else {
				if(!it1.hasNext()){
					while(it2.hasNext()){
						i2=it2.next();
						over.add(i2);
					}
					break;
				} else if(!it2.hasNext()) break;
				i1=it1.next();
				i2=it2.next();
			}
		}
		return over;
	}
	public Bicluster merge(Bicluster bicluster2) {
		columns.addAll(bicluster2.getColumns());
		rows.addAll(bicluster2.getRows());
		return this;
	}
	
	
	/*****************************
	 ***** E: GETTER-SETTER ****** 
	 *****************************/

	private Set<Integer> getRows() { return rows; }
	private Set<Integer> getColumns() { return columns; }
	
	public int area() { return columns.size()*rows.size(); }
	public int numRows() { return rows.size(); }
	public int numColumns() { return columns.size(); }	

	
	/*********************
	 ***** E: UTILS ****** 
	 *********************/

	public String toString() {
		StringBuffer res = new StringBuffer((key!=null) ? "ID:"+key : ""); 
		res.append(" ("+columns.size()+","+rows.size()+") Y=[");
		for(int i : columns) res.append(i+",");
		res.append("] X=[");
		for(int i : rows) res.append(i+",");
		res.append("]");
		if(items != null){
			res.append(" I=[");
			for(int i : items) res.append(i+",");
			res.append("]");
		}
		return res.toString().replace(",]","]");
	}
	public String toFullString() {
		String res = toString();
		res += "\n\tsup="+BicPrinting.plot(support)+" wsup=" +BicPrinting.plot(weightedSupport);
		return res + "\n\tconf="+conf+" wconf=" +wconf+ " lift="+lift+" wlift="+wlift;
	}
	
	public String toShortString() {
		return "("+rows.size()+","+columns.size()+")";
	}
	
	public String toString(List<String> nodesX, List<String> nodesY) {
		StringBuffer res = new StringBuffer((key!=null) ? "ID:"+key : ""); 
		if(items != null){
			res.append(" I=[");
			for(int i : items) res.append(i+",");
			res.append("]");
		}
		res.append(" ("+columns.size()+","+rows.size()+") Y=[");
		for(int i : columns) res.append(nodesY.get(i)+",");
		res.append("] X=[");
		if(nodesX == null) res.append("<too long>");
		else for(int i : rows) res.append(nodesX.get(i)+",");
		res.append("]");
		if(pvalue!=-1) 
			if(pvalue==0) res.append(" pvalue="+pvalue+" (too small)");
			else {
				if(pvalue>1) res.append(" pvalue=1.0");
				else res.append(" pvalue="+(pvalue>0.01 ? (((int)(pvalue*100))/100.0) : pvalue));
			}
		if(lifts!=null) res.append(" Lifts="+lifts);
		//System.out.println(">>>"+res.toString());
		return res.toString().replace(",]","]");
	}

	public String patternToString() {
		int i=0;
		String pattern = "";
		if(orders==null) {
			for(Integer col : columns) 
				pattern += "$k_"+col+"$="+items.get(i++)+",";
			pattern = pattern.substring(0, pattern.length()-1);
		} else pattern = orders.toString();
		return "\\{"+pattern+"\\} & $|I|$="+rows.size();
	}

	public String toString(Dataset data) {
		StringBuffer res = new StringBuffer(toString(null, data.columns));
		double[][] matrix = data.getBicluster(columns,rows);
		int[][] context = null;
		if(data.context!=null) context = data.getContext(columns,rows);
		int k=0;
		NumberFormat formatter = new DecimalFormat("#0.0");
		//for(int i : columns) res.append("\t"+data.columns.get(i));
		for(int i : rows){ 
			res.append("\n");
			res.append(data.rows.get(i));
			for(int j=0, l=matrix[k].length; j<l; j++) {
				if(matrix[k][j]%1==0) res.append("\t"+(int)matrix[k][j]);
				else res.append("\t"+formatter.format(matrix[k][j]));
				if(context!=null) res.append(":"+context[k][j]); 
			}
			k++;
		}
		return res.toString().replace(",]","]");
	}
	
	public boolean testZero(Dataset data) {
		double[][] matrix = data.getBicluster(columns,rows);
		for(int i=0, l=matrix.length; i<l; i++)
			if(BicMath.sum(matrix[i])==0) return true;
		for(int j=0, l=matrix[0].length; j<l; j++){
			int count = 0;
			for(int i=0; i<matrix.length; i++) count+=matrix[i][j];
			if(count==0) return true;
		}
		return false;
	}
}
