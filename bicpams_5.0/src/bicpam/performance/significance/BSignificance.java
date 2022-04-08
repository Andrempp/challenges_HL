package performance.significance;

import java.util.List;

import domain.Bicluster;
import domain.Biclusters;
import domain.Dataset;
import performance.significance.binomial.Binomial;
import utils.BicMath;
import utils.BicPrinting;
import utils.BicResult;

public class BSignificance {

	public enum Test { Binomial , Density }
	public enum Data { UniformColumn , UniformOverall }
	
	public static void run(Dataset data, Biclusters bics){
		run(data,bics,Test.Binomial,Data.UniformColumn);
	}
	private static void run(Dataset data, Biclusters bics, Test binomial, Data column) {
		//System.out.println("#Bics=>"+bics.size());
		double[][] columns = new double[data.columns.size()][data.nrLabels];
		for(int i=0; i<data.rows.size(); i++){
			List<Integer> rowvalues = data.intscores.get(i);
			List<Integer> rowindexes = data.indexes.get(i);
			for(int j=0; j<rowindexes.size(); j++)
				columns[rowindexes.get(j)][rowvalues.get(j)]++;
		}
		for(int j=0, l1=columns.length; j<l1; j++) {
			double sum = BicMath.sum(columns[j]);
			for(int v=0; v<data.nrLabels; v++)
				columns[j][v] /= sum;
		}
		/*for(int j=0, l1=columns.length; j<l1; j++){
			columns[j] = count/(double)data.nrNodes();
			double count = 0;
			for(int i=0, l2=data.nrNodes(); i<l2; i++) 
				if(data.indexes.get(i).contains(j)) count++;
			columns[j] = count/(double)data.nrNodes();
		}*/
		//BicResult.println(BicPrinting.plot(columns));
		for(int i=bics.size()-1; i>=0; i--){
			double p=1;	
			Bicluster bic = bics.getBiclusters().get(i);
			int labelindex = 0;
			for(Integer col : bic.columns) 
				p*=columns[col][bic.items.get(labelindex++)];
			//System.out.println(bic.columns.size()+"=>"+p);
			bic.pvalue = Binomial.cumulative(bic.rows.size(),data.nrNodes(),p,false,false);
			//BinomialDistribution bin = new BinomialDistribution(data.nrNodes(),p);
			//bic.pvalue = bin.probability(bic.rows.size());//cumulativeProbability(bic.rows.size(),data.nrNodes());
			//if(bic.pvalue>=0.001) bics.remove(i);
		}
		System.out.println("#Bics=>"+bics.size());
	}
	public static double binomialTest(int n, int N, double p) {
		return Binomial.cumulative(n,N,p,false,false);
	}
	
	public static void main(String[] args){
		int n=5, N=50;
		double p=0.2*0.2;
		System.out.println(binomialTest(n,N,p));
	}
}
