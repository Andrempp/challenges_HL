package bicpam.evaluation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import domain.Bicluster;
import domain.Biclusters;
import utils.BicPrinting;
import utils.others.HungarianAlgorithm;


/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class MatchMetrics {

	public static double[] fabiaConsensus(Biclusters _bicsA, Biclusters _bicsB){
		double result[] = new double[2];
		double sum = 0;
		
		List<Bicluster> bicsA, bicsB;
		if(_bicsA.size()<_bicsB.size()){
			bicsA = _bicsA.getBiclusters();
			bicsB = _bicsB.getBiclusters();
		} else {
			bicsA = _bicsB.getBiclusters();
			bicsB = _bicsA.getBiclusters();			
		}

		double[][] similarities = computeSimilarities(bicsA,bicsB);
		int[][] pairs = HungarianAlgorithm.hgAlgorithm(similarities, "max");
		//System.out.println("\nPAIRS\n"+BicPrinting.plot(pairs));
		for(int i=0; i<pairs.length; i++){
			//System.out.println("PAIR:"+similarities[pairs[i][0]][pairs[i][1]]+"("+pairs[i][0]+","+pairs[i][1]+")");
			sum += similarities[pairs[i][0]][pairs[i][1]];
		}
		result[0] = sum/bicsA.size();
		result[1] = sum/bicsB.size();
		return result;
	}
	
	private static double[][] computeSimilarities(List<Bicluster> bicsA, List<Bicluster> bicsB) {
		double[][] result = new double[bicsA.size()][bicsB.size()];
		for(int i=0, s1=bicsA.size(); i<s1; i++)
			for(int j=0, s2=bicsB.size(); j<s2; j++)
				result[i][j] = jaccard(bicsA.get(i),bicsB.get(j));
		//System.out.println(BicPrinting.plot(result));
		return result;
	}

	public static double jaccard(Bicluster bicA, Bicluster bicB){
		Set<Integer> intersectedRows = new HashSet<Integer>();
		Set<Integer> intersectedCols = new HashSet<Integer>();

		intersectedRows.addAll(bicA.rows);
		intersectedCols.addAll(bicA.columns);
		intersectedRows.retainAll(bicB.rows);
		intersectedCols.retainAll(bicB.columns);
		
		double intersection = intersectedRows.size()*intersectedCols.size();
		double union = (bicA.rows.size()*bicA.columns.size()) + (bicB.rows.size()*bicB.columns.size()) - intersection;
		return intersection / union;
	}

	public static double fabiaConsensusOld(Biclusters _bicsA, Biclusters _bicsB){
		double result = 0;
		List<Bicluster> bicsA, bicsB;
		if(_bicsA.size()>_bicsB.size()){
			bicsA = _bicsA.getBiclusters();
			bicsB = _bicsB.getBiclusters();
		} else {
			bicsA = _bicsB.getBiclusters();
			bicsB = _bicsA.getBiclusters();			
		}

		for(Bicluster bicA : bicsA){
			double max=0;
			for(Bicluster bicB : bicsB)
				max = Math.max(max, jaccard(bicB,bicA));
			result += max;
		}
		return result / Math.max(bicsB.size(),bicsA.size());
	}

	public static double RNIA(Biclusters _foundBics, Biclusters _hiddenBics){
		double result = 0;
		List<Bicluster> foundBics = _foundBics.getBiclusters();
		List<Bicluster> hiddenBics = _hiddenBics.getBiclusters();

		for(Bicluster foundBic : foundBics){
			double max=0;
			for(Bicluster hiddenBic : hiddenBics){
				//copy sets
				Set<Integer> hiddenBicRowsCopy = new HashSet<Integer>();
				Set<Integer> hiddenBicColsCopy = new HashSet<Integer>();

				hiddenBicRowsCopy.addAll(hiddenBic.rows);
				hiddenBicColsCopy.addAll(hiddenBic.columns);
				hiddenBicRowsCopy.retainAll(foundBic.rows);
				hiddenBicColsCopy.retainAll(foundBic.columns);
				
				double intersection = hiddenBicRowsCopy.size()*hiddenBicColsCopy.size();
				double union = (hiddenBic.rows.size()*hiddenBic.columns.size()) +
						(foundBic.rows.size()*foundBic.columns.size()) - intersection;
						
				max = Math.max(max, intersection / union);
			}
			result += max;
		}
		return result * (1.0 / foundBics.size());
	}

	public static double matchScore(Biclusters _foundBics, Biclusters _hiddenBics){
		double result = 0;
		List<Bicluster> foundBics = _foundBics.getBiclusters();
		List<Bicluster> hiddenBics = _hiddenBics.getBiclusters();

		for(Bicluster foundBic : foundBics){
			double max=0;
			for(Bicluster hiddenBic : hiddenBics){
				//copy sets
				Set<Integer> hiddenBicRowsCopy1 = new HashSet<Integer>();
				Set<Integer> hiddenBicRowsCopy2 = new HashSet<Integer>();
				hiddenBicRowsCopy1.addAll(hiddenBic.rows);
				hiddenBicRowsCopy2.addAll(hiddenBic.rows);
				
				hiddenBicRowsCopy1.addAll(foundBic.rows);
				hiddenBicRowsCopy2.retainAll(foundBic.rows);
				double intersection = hiddenBicRowsCopy2.size();
				double union = hiddenBicRowsCopy1.size();
				max = Math.max(max, intersection / union);
			}
			//System.out.println("MAX:"+max);
			result += max;
		}
		return result * (1.0 / foundBics.size());
	}

	public static double foundMatch(Biclusters _foundBics, Biclusters _hiddenBics){
		double result = 0;
		List<Bicluster> foundBics = _foundBics.getBiclusters();
		List<Bicluster> hiddenBics = _hiddenBics.getBiclusters();

		for(Bicluster foundBic : foundBics){
			double max=0;
			for(Bicluster hiddenBic : hiddenBics){
				Set<Integer> hiddenBicRowsCopy1 = new HashSet<Integer>();
				hiddenBicRowsCopy1.addAll(hiddenBic.rows);
				hiddenBicRowsCopy1.retainAll(foundBic.rows);
				double intersection = hiddenBicRowsCopy1.size();
				double union = foundBic.rows.size();
				max = Math.max(max, intersection / union);
			}
			result += max;
		}
		return result * (1.0 / foundBics.size());
	}

	public static double hiddenMatch(Biclusters _foundBics, Biclusters _hiddenBics){
		double result = 0;
		List<Bicluster> foundBics = _foundBics.getBiclusters();
		List<Bicluster> hiddenBics = _hiddenBics.getBiclusters();

		for(Bicluster hiddenBic : hiddenBics){
			double max=0;
			for(Bicluster foundBic : foundBics){
				Set<Integer> foundBicRowsCopy1 = new HashSet<Integer>();
				foundBicRowsCopy1.addAll(foundBic.rows);
				foundBicRowsCopy1.retainAll(hiddenBic.rows);
				double intersection = foundBicRowsCopy1.size();
				double union = hiddenBic.rows.size();
				max = Math.max(max, intersection / union);
			}
			result += max;
		}
		return result * (1.0 / hiddenBics.size());
	}
	
	public static String run(Biclusters foundBics, Biclusters hiddenBics){
		String result = "MATCH SCORE(B,H):" + MatchMetrics.matchScore(foundBics, hiddenBics);
		result += "\nMATCH SCORE(H,B):" + MatchMetrics.matchScore(hiddenBics, foundBics);
		result += "\nFOUND SCORE:" + MatchMetrics.foundMatch(foundBics, hiddenBics);
		//result += "\nFABIA CONSENSUS:" + fc[0] + "::" + fc[1];
		return result;
	}
	public static String runFabiaConsensus(Biclusters foundBics, Biclusters hiddenBics){
		double[] fc = MatchMetrics.fabiaConsensus(foundBics, hiddenBics);
		return "FABIA CONSENSUS = {" + fc[0] + "," + fc[1] + "}";
	}
}
