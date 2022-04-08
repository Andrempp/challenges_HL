package bicpam.closing;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import utils.BicPrinting;
import utils.BicResult;
import domain.Bicluster;
import domain.Biclusters;
import domain.Biclusters.Order;


/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class BiclusterMerger {

	private boolean print = false;
	private double overlapMax;
	private Order order = Order.Default;
	public enum MergingStrategy { Simple, Combinatorial, Heuristic, FIMRows, FIMColumns, FIMOverall }; 
	
	public BiclusterMerger(double _overlap){
		overlapMax = _overlap;
	}
	public BiclusterMerger(double _overlap, Order _order){
		overlapMax = _overlap;
		order = _order;
	}
	
	public Biclusters run(Biclusters biclusters){
		//System.out.println(biclusters.toShortString());
		if(print){
			biclusters.orderPValue();
			int key=0;
			for(Bicluster bic : biclusters.getBiclusters()) bic.key="B"+key++;
			double[][] overlapMatrix = BiclusterMerger.overlappingMatrix(biclusters);
			StringBuffer result = new StringBuffer("\n\nOverlapping Matrix:\n--\t");
			for(Bicluster bic : biclusters.getBiclusters()) result.append(bic.key+"\t");
			int k=0;
			for(Bicluster bic : biclusters.getBiclusters()) result.append("\n"+bic.key+"\t"+BicPrinting.plot(overlapMatrix[k++]));
			//BicResult.println("Similarity Matrix:\n"+BicPrinting.plot(overlapMatrix));
			BicResult.println(result.toString()+"\n");
		}
		
		biclusters.order(order);
		for(int i=biclusters.size()-1; i>=0; i--) 
			if(biclusters.get(i).area()<=1) biclusters.remove(i);
		if(!biclusters.hasBiclusters()) return biclusters;			
		return mergeSimpleBiclusters(biclusters);
	}
	
	public Biclusters mergeSimpleBiclusters(Biclusters bics) {
		//System.out.println("Merging... ");//bics.toShortString());
		int j=0, key=77;		
		
		while(bics.size()>0 && j++<3){
			Biclusters newbics = new Biclusters();
			SortedSet<Integer> rembics = new TreeSet<Integer>();
			for(int i1=0, l=bics.size(); i1<l; i1++){
				if(rembics.contains(i1)) continue;
				Bicluster newbic = bics.getBiclusters().get(i1);
				//System.out.println(newbic.toString());
				boolean add = false;
				for(int i2=i1+1; i2<l; i2++){
					Bicluster bic = newbic.merge(bics.getBiclusters().get(i2),overlapMax);
					if(bic==null) continue;
					//System.out.println(">>"+bics.getBiclusters().get(i2).toString());
					//System.out.println("=>"+bic.toString());
					add = true;
					newbic = bic;
					if(print){
						newbic.key="B"+key++;
						BicResult.println("{"+bics.getBiclusters().get(i1).key+","+bics.getBiclusters().get(i2).key+"}=>"+newbic.key+" (overlap="+bics.getBiclusters().get(i1).overlapArea(bics.getBiclusters().get(i2))+")");
					}
					rembics.add(i1);
					rembics.add(i2);
				}
				if(add) newbics.add(newbic);
			}
			if(newbics.size()<=0) break;
			//System.out.println("I"+j+" bics merged: " + rembics.size() + "=>" + newbics.size());
			while(!rembics.isEmpty()){
				Integer i = rembics.last();
				bics.remove(i);
				rembics.remove(i);
			}
			bics.addAll(newbics);
			//System.out.println("Bics after merging: " + bics.toString());
		}
		//System.out.println("End merging");
		return bics;
	}
	
	public static double[][] overlappingMatrix(Biclusters biclusters){
		List<Bicluster> bics = biclusters.getBiclusters();
		System.out.println(">2>"+bics.get(1).overlapArea(bics.get(5)));
		double[][] result = new double[bics.size()][bics.size()];		
		for(int i=0, l=bics.size(); i<l; i++)
			for(int j=0; j<l; j++)
				result[i][j] = bics.get(i).overlapArea(bics.get(j));
		return result;
	}
	/*public static Biclusters nonOverlapping(double[][] overlappingMatrix, Biclusters biclusters){
		List<Bicluster> bics = biclusters.getBiclusters();
		Biclusters result = new Biclusters();
		for(int i=0, l=bics.size(); i<l; i++){
			boolean nonOverlapping = true;
			for(int j=0; j<l; j++){ 
				if(overlappingMatrix[i][j]!=0){
					nonOverlapping = false;
					break;
				}
			}
			if(nonOverlapping) result.add(bics.get(i));
		}
		return result;
	}*/
}
