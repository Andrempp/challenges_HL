package bicpam.pminer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import domain.Bicluster;
import domain.Biclusters;
import domain.constraint.Constraints;
import utils.BicMath;

/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public abstract class PM {

	public enum SearchCriteria { FPGrowth, Apriori };
	public enum ItemsetType { Simple, Maximal, Closed, Sequential };

	protected BicPM PMiner;
	protected List<List<Integer>> dataset;
	protected Constraints constraints;
	protected int nrLabels = -1;
	protected SearchCriteria search;
	
	protected double minArea = -1;
	protected int minNrBics = -1;
	protected double support = -1, minsup=1;
	protected int minColumns = -1;
	protected int minRows = -1;
	protected int maxColumns = -1;
	protected int maxRows = -1;
	protected boolean partitioning = false;

	protected int targetClass = -1;
	protected int[] classValues;
	protected int[] supports;
	protected double lift;
	protected boolean liftSuperiority = false;
	
	public abstract Biclusters findFrequentItemSets() throws Exception;

	public abstract ItemsetType getItemsetType();

	public Biclusters run() throws Exception {
		//BicResult.println(dataset+"");
		if(!partitioning) return mine(dataset);
		else{
			int limitColumns = 50;
			List<List<List<Integer>>> datasets = PartitioningUtils.partition(dataset,nrLabels,limitColumns);
			Biclusters bics = new Biclusters();
			int i=0;
			for(List<List<Integer>> dataI : datasets){
				PMiner.setData(dataI);
				Biclusters bicsI = mine(dataI);
				bicsI.setPartition(i++);
				bics.addAll(bicsI);
				PMiner.reset();
			}
			int minSup = (int) Math.max(minRows, ((support<0) ? minsup : support)*dataset.size());
			return PartitioningUtils.merge(bics,minSup);
		}
	}

	private Biclusters mine(List<List<Integer>> data) throws Exception {
		if(support > 0) {
			int mRows = (int)(support*data.size());
			//System.out.println(">>>"+mRows);
			return PMiner.run(minColumns,mRows,maxColumns,maxRows,nrLabels);
		}
		else if(minArea > 0 || minNrBics > 0) {
			Biclusters result; 
			double area = BicMath.count(data);
			int absolutesup = data.size();
			for(double support=1; support>0; support/=1.1) {
				minsup = Math.min(minsup, support);
				int newsup = (int) Math.max(support*data.size(),minRows);
				if(newsup == 0) return new Biclusters();
				else if(newsup == absolutesup) continue;
				else absolutesup = newsup;
				
				result = PMiner.run(minColumns,absolutesup,maxColumns,maxRows,nrLabels);
				/*for(Bicluster pattern : result.getBiclusters())
					System.out.print("("+pattern.numColumns()+","+pattern.numRows()+")");
				System.out.println();*/
				if(absolutesup==minRows) return result;
				if(minNrBics > 0 && result.size() >= minNrBics) {
					if(classValues == null) return result;
					result = result.aboveLift(classValues,supports,dataset.size(),lift,targetClass,liftSuperiority); 
					if(result.size() >= minNrBics) return result;
				}

				Set<Integer> rows = new HashSet<Integer>();
				Set<Integer> cols = new HashSet<Integer>();
				for(Bicluster pattern : result.getBiclusters()){
					cols.addAll(pattern.columns);
					rows.addAll(pattern.rows);
				}
				if(minArea>0){
					double myarea = ((double)(cols.size()*rows.size()))/area; 
					if(myarea>minArea) return result;
				}
			}
			return null;
		}
		else {
			System.out.println("MinRows:"+minRows+",MinCols:"+minColumns);
			return PMiner.run(minColumns,minRows,maxColumns,maxRows,nrLabels);
		}
	}

	
	/* =================================== */
	/* ========== GETS AND SETS ========== */
	/* =================================== */
	
	public void inputParams(int _minColumns, int _minRows){
        minColumns = _minColumns;
        minRows = _minRows;
	}
	public void inputParams(int _minColumns, int _minRows, int _maxColumns, int _maxRows){
        minColumns = _minColumns;
        minRows = _minRows;
        maxColumns = _maxColumns;
        maxRows = _maxRows;
	}
	public void activatePartitioning() { partitioning  = true; }
	public void inputMinArea(double _minArea){ minArea = _minArea; }
	public void inputMinNrBics(int _minNrBics){ minNrBics = _minNrBics; }
	public void inputMinColumns(int _minColumns) { minColumns = _minColumns; }	
	
	public BicPM getPminer(){ return PMiner; }
	public SearchCriteria getSearch(){ return search; }
	public List<List<Integer>> getDataset(){ return dataset; }
	public int getMinNrBics() { return minNrBics; }
	public double getSupport(){ return support; }
	public int getMinColumns(){ return minColumns; }
	public int getMinRows(){ return minRows; }
	public int getMaxColumns(){ return maxColumns; }
	public int getMaxRows(){ return maxRows; }
	public int getNrLabels(){ return nrLabels; }
	
	public void setConstraints(Constraints _constraints) { constraints = _constraints; }
	public void setNrLabels(int _nrLabels){ nrLabels = _nrLabels; }
	public void setPMiner(BicPM pm){ PMiner = pm; }
	public void setDataset(List<List<Integer>> list){ dataset = list; }
	public void setSearch(SearchCriteria criteria){ search = criteria; }
	public void setSupport(double _support){ support = _support; }
	public void setMinColumns(int val){ minColumns = val; } 
	public void setMinRows(int val){ minRows = val; }
	public void setMaxColumns(int val){ maxColumns = val; }
	public void setMaxRows(int val){ maxRows = val; }
	public void setTargetClass(int index, boolean superior) { 
		targetClass = index; 
		liftSuperiority = superior;
	}	
	public void setClass(int[] values, double lift){ 
		this.classValues = values; 
		this.lift = lift;
		int max = BicMath.max(values);
		this.supports = new int[max+1];
		for(int v : values) if(v>=0) supports[v]++;
	}

	public boolean isFrequent(Bicluster itemset) {
		if(itemset.rows.size()>=minRows) return true;
		else return false;
	}
		
	public PM(){}

}