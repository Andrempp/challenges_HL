package bicpam.significance;

import java.util.HashSet;
import java.util.Set;

import bicpam.significance.BSearchSpace.SpaceType;
import generator.BicMatrixGenerator.PatternType;
import jdistlib.Binomial;
import utils.BicMath;
import domain.Bicluster;
import domain.Biclusters;
import domain.Dataset;

public class BSignificance {

	public static void runOrderPreserving(Dataset data, Biclusters bics, int minColumns){
		int count = 0, m = data.columns.size(), eligibleRows = 0;
		for(int i=0, l2=data.nrNodes(); i<l2; i++) {
			Set<Integer> all = new HashSet<Integer>(data.indexes.get(i));
			count += all.size();
			if(all.size() >= minColumns) eligibleRows++;
		}
		double missings = ((double)count)/(double)(m*data.rows.size());
		for(Bicluster bic : bics.getBiclusters()){
			double p=1.0/BicMath.factorial(bic.numColumns());	
			System.out.println("pattern prob = "+p+" | "+(int)(eligibleRows*missings));
			bic.pvalue = Binomial.cumulative(bic.rows.size(),(int)(eligibleRows*missings),p,false,false);
		}
		System.out.println("#Bics=>"+bics.size());
	}

	public static void runConstantFreqColumn(Dataset data, Biclusters bics){
		runConstantFreqColumn(data,bics,true);
	}
	
	public static void runConstantFreqColumn(Dataset data, Biclusters bics, boolean missingCorrection){
		int m = (data.originalColumns==null) ? data.columns.size() : data.originalColumns.size();
		double[][] freqColumns = new double[m][data.nrLabels]; 
		for(int j=0; j<m; j++){
			int count = 0;
			for(int i=0, l2=data.nrNodes(); i<l2; i++) {
				if(data.indexes.get(i).contains(j)) {
					freqColumns[j][data.intscores.get(i).get(data.indexes.get(i).indexOf(j))]++;
					count++;
				}
			}
			if(missingCorrection) count = data.nrNodes();
			for(int k=0; k<data.nrLabels; k++) freqColumns[j][k] /= (double)count;
		}
		for(Bicluster bic : bics.getBiclusters()){
			double p=1;	
			int j=0;
			for(Integer col : bic.columns)
				p*=freqColumns[col][bic.items.get(j++)];
			System.out.println("pattern prob = "+p);
			bic.pvalue = Binomial.cumulative(bic.rows.size(),data.nrNodes(),p,false,false);
		}
		System.out.println("#Bics=>"+bics.size());
	}

	public static void runUniformColumn(Dataset data, Biclusters bics){
		double[] columns = new double[data.columns.size()];
		for(int j=0, l1=columns.length; j<l1; j++){
			double count = 0;
			for(int i=0, l2=data.nrNodes(); i<l2; i++) 
				if(data.indexes.get(i).contains(j)) count++;
			columns[j] = count/(double)data.nrNodes();
		}
		for(int i=bics.size()-1; i>=0; i--){
			double p=1;	
			Bicluster bic = bics.getBiclusters().get(i);
			for(Integer col : bic.columns) p*=columns[col];
			bic.pvalue = Binomial.cumulative(bic.rows.size(),data.nrNodes(),p,false,false);
		}
		System.out.println("#Bics=>"+bics.size());
	}
	
	public static void runUniformOverall(Dataset data, Biclusters bics, PatternType type) {
		
		int nrows=data.nrNodes(), ncolumns=data.columns.size();
		BLocalDiscrete localProbs = new BLocalDiscrete(data); 
		for(int i=bics.size()-1; i>=0; i--){
			Bicluster bic = bics.getBiclusters().get(i);
			double p=localProbs.getDiscreteProbability(bic.items, type);	
			bic.pvalue = Binomial.cumulative(bic.rows.size(),data.nrNodes(),p,false,false);
			//System.out.print(" pval="+bic.pvalue);
			SpaceType spacetype = SpaceType.ExactSize;
			
			int spacesize = BSearchSpace.compute(spacetype, bic.numRows(), bic.numColumns(), nrows, ncolumns, data.nrLabels, type);
			bic.pvalue = spacesize*bic.pvalue;
			if(bic.pvalue<0) bic.pvalue = -bic.pvalue;
			//System.out.println(" adjusted="+bic.pvalue+" #("+bic.rows.size()+","+bic.columns.size()+")");
			/*double alpha=0.05;			
			Correction correction = Correction.Bonferroni;
			double corrected = MultiHypothesisCorrection.runCorrection(correction,alpha,spacesize);*/
		}
	}
}
