package bicpam.significance;

import generator.BicMatrixGenerator.PatternType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import domain.Dataset;

import org.apache.commons.math3.distribution.NormalDistribution;

import utils.BicMath;
import utils.BicPrinting;

public class BLocalDiscrete {

	protected int[] itemFrequencies;
	protected int nrLabels, nrows, ncolumns;
	protected double mean, std;
	
	public enum Approximation { Gaussian, Uniform, ValFrequencies, ItemFrequencies };
	public enum Tail { OneTailed, TwoTailed };
	public enum IndependenceDegree { All, Pair , None };
	
	public Approximation approximation = Approximation.ValFrequencies; 
	public IndependenceDegree degree = IndependenceDegree.All;

	public BLocalDiscrete(Dataset data) {
		itemFrequencies = new int[data.nrLabels];
		for(int i=0, l1=data.nrNodes(); i<l1; i++)
			for(int j=0, l2=data.intscores.get(i).size(); j<l2; j++)
				itemFrequencies[data.intscores.get(i).get(j)]++;
		double k=0, count=0, sum=0; 
		for(Integer ifreq : itemFrequencies){
			sum+=ifreq*k++;
			count+=ifreq;
		}
		mean=sum/count;
		std=0;
		k=0;
		for(Integer ifreq : itemFrequencies){ 
			for(int i=0; i<ifreq; i++) std+=Math.pow(k-mean,2);
			k++;
		}
		std=std/count;
		nrows=data.nrNodes();
		ncolumns=data.columns.size();
		//System.out.println("Elements:"+(nrows*ncolumns));
		//System.out.println("Item freqs:"+BicPrinting.plot(itemFrequencies));
	}

	/*public double computeThreshold(Bicluster bic, double prob){
		double observedsig = MultiHypothesisCorrection.runCorrection(correction, prob, getSpaceSize(bic));
		return SignificanceThreshold.runCorrection(threshold, observedsig, alpha);
	}*/
	
	protected double getDiscreteProbability(List<Integer> items, PatternType type) {
		
		if(type.equals(PatternType.Constant)) return getDiscreteProbability(items);
		
		if(type.equals(PatternType.Additive)) {
			int max = -1000, min = 1000;
			for(Integer i : items){
				max = Math.max(max, i);
				min = Math.min(min, i);
			}
			double prob = getDiscreteProbability(items);
			for(int i=max+1; i<nrLabels; i++) {
				int c = nrLabels-i;
				List<Integer> itemsC = new ArrayList<Integer>();
				for(Integer item : items) itemsC.add(item+c);
				prob += getDiscreteProbability(itemsC);
			}			
			for(int i=0; i<min; i++) { //no need for generated patterns
				int c = i-min;
				List<Integer> itemsC = new ArrayList<Integer>();
				for(Integer item : items) itemsC.add(item+c);
				prob += getDiscreteProbability(itemsC);
			}
			return prob;
		}
		
		int shift = 1;
		if(type.equals(PatternType.Multiplicative)){
			List<Integer> itemsC = new ArrayList<Integer>();
			for(Integer it : items) itemsC.add(it-shift);
			double prob = getDiscreteProbability(itemsC);
			
			for(int i=2; i<=nrLabels; i++) {
				boolean possibleDiv = true, possibleMult = true;
				for(Integer item : items){ 
					if( item % i != 0) possibleDiv = false;
					if( item * i - shift > nrLabels) possibleMult = false;
				}
				if(possibleDiv) { //no need for generated patterns
					itemsC = new ArrayList<Integer>();
					for(Integer it : items) itemsC.add(it/i-shift);
					//System.out.println("MULT /c:"+i+">>"+itemsC);
					prob += getDiscreteProbability(itemsC);
				} else if(possibleMult) {
					itemsC = new ArrayList<Integer>();
					for(Integer it : items) itemsC.add(it*i-shift);
					//System.out.println("MULT *c:"+i+">>"+itemsC);
					prob += getDiscreteProbability(itemsC);
				}
			}
			return prob;
		}
		return -1;
	}

	protected double getDiscreteProbability(List<Integer> items) {
		double prob=1;
		switch(approximation){
			case Gaussian : 
				NormalDistribution gaussian = new NormalDistribution(mean, std);
				System.out.println("N("+mean+","+std+")"+items);
				//for(Integer item : items) System.out.print(item+","+gaussian.density(item)+",");
				for(Integer item : items) prob = prob*gaussian.density(item);
				break;
			case Uniform : 
				prob = 1.0/Math.pow(nrLabels,items.size());
				System.out.println("U:"+nrLabels);
				break;
			case ValFrequencies : 
				prob = getValFrequencies(items);
				return prob;
			case ItemFrequencies : 
				for(Integer item : items) prob = prob*itemFrequencies[item]/(nrows*ncolumns);
				break;
			default : return -1;
		}

		boolean samecorrection = false;
		if(samecorrection){
			Map<Integer,Integer> symbols = new HashMap<Integer,Integer>();
			for(Integer item : items) 
				if(symbols.containsKey(item)) symbols.put(item, symbols.get(item)+1);
				else symbols.put(item,1);
			int nr=items.size(), adjustfactor=1;
			for(Integer itemnr : symbols.values())
				adjustfactor *= BicMath.combination(nr--,itemnr);
			prob = prob*adjustfactor;
		}
		//System.out.println("\nprob:"+prob);
		return prob;
	}

	private double getValFrequencies(List<Integer> items) {
		boolean samecorrection=false;
		double prob=1;
		Map<Integer,Integer> symbols = new HashMap<Integer,Integer>();
		List<Integer> norepitems = new ArrayList<Integer>();
		int[] itemCount = null;

		for(Integer item : items) 
			if(symbols.containsKey(item)) symbols.put(item, symbols.get(item)+1);
			else symbols.put(item,1);
		
		switch(degree){
			case All : 
				for(Integer item : items) prob = prob*itemFrequencies[item]/(double)(nrows*ncolumns);
				//System.out.print("prob:"+prob);
				
				if(samecorrection){
					int nr=items.size();
					double adjustfactor=1;
					System.out.println("Items:"+items+" Symbols:"+symbols.values());
					for(Integer itemnr : symbols.values()){
						adjustfactor *= BicMath.combination(nr,itemnr);
						nr=nr-itemnr;
					}
					if(adjustfactor<0) prob=1;
					else prob = prob*adjustfactor;
					System.out.println("adjusted:"+prob + " factor:" + adjustfactor );
				}
				break;
			case Pair :
				/*int[][] pairs = new int[nrLabels][nrLabels];
				for(int i=0, s1=matrix.length; i<s1; i++){
					itemCount = new int[nrLabels];
					for(int j=0, s2=matrix[i].length; j<s2; j++){
						//System.out.println("("+i+","+j+")"+matrix[i][j]);
						itemCount[matrix[i][j]]++;
					}
					for(int j=0; j<nrLabels; j++){
						for(int k=0; k<=j; k++){
							if(j==k && itemCount[j]>1) pairs[j][j] += (int) BicMath.combination(itemCount[j],2);
							else if(itemCount[j]>0 && itemCount[k]>0) pairs[k][j] += itemCount[j]*itemCount[k];
						}
					}
				}
				System.out.println(BicPrinting.plot(pairs));
				prob = 0;
				BTreeNode<BPair> npairs = BPair.computePairs(items);
				int ncomb = 0;
				for(BTreeNode<BPair> node : npairs.children){
					ncomb++;
					prob += computePaths(pairs, node, BicMath.combination(matrix[0].length,2));
					System.out.println("p:"+prob);
				}
				prob = prob/ncomb;
				System.out.println("prob:"+prob);*/
				break;
			case None : 
				/*prob = 0;
				norepitems.addAll(symbols.keySet());
				for(int i=0, s1=matrix.length; i<s1; i++){
					 itemCount = new int[symbols.size()];
					 for(int j=0, s2=matrix[i].length; j<s2; j++){
						 int index = norepitems.indexOf(matrix[i][j]);
						 if(index>=0) itemCount[index]++;
					 }
					 double icount=1;
 					 for(int k=0, l=norepitems.size(); k<l; k++){
 						 if(itemCount[k]<symbols.get(norepitems.get(k))){
 							 icount=0;
 							 break;
 						 }
 						 icount = icount*BMath.combination(itemCount[k],symbols.get(norepitems.get(k)));
 					 }
 					 double val = icount/BMath.combination(matrix[0].length,items.size());
 					 prob += val;
				}
				prob = prob/matrix.length;
				System.out.println(prob);*/
				break;
		}
		return prob;
	}

	/*private double computePaths(int[][] pairs, BTreeNode<BPair> node, double paircomb) {
		int item1=node.data.p1, item2=node.data.p2;
		double val, counts=1;
		if(item2==-1){
			val = valFrequencies[item1-min]/count;
			System.out.println("["+(item1-min)+"]"+valFrequencies[item1-min]+"/"+count);
		}
		else {
			counts = pairs[Math.min(item1,item2)][Math.max(item1,item2)]; 
			val = counts/(paircomb*matrix.length);			
			System.out.println("("+item1+","+item2+")="+counts+"/"+(paircomb*matrix.length));
		}
		if(!node.hasChild()) return val;
		for(BTreeNode<BPair> child : node.children){
			val *= computePaths(pairs, child, paircomb);
		}
		return val;
	}*/
}
