package bicpam.bicminer.constant;

import generator.BicMatrixGenerator.PlaidCoherency;
import java.util.ArrayList;
import java.util.List;
import domain.Bicluster;
import domain.Biclusters;


/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public final class PlaidMiner {

	public enum CoherencyConstraint { Exact , InBetween , Relaxed };
	
	public static CoherencyConstraint constraint;
	public static double relaxedError;
	public static int minCols = 1, minRows = 2;
	
	public static Biclusters run(Biclusters biclusters, int[][] matrix, PlaidCoherency criteria, CoherencyConstraint check, double error){
	  constraint = check;
	  relaxedError = error;
	  System.out.println("Plaid extensions/reductions");
	  if(!biclusters.hasBiclusters()) return biclusters;

	  int k=0, oldSize = 0;
	  /*
	  while(biclusters.size()!=oldSize && k++<3){
			
		List<Bicluster> plaids = new ArrayList<Bicluster>();
		List<Bicluster> bics = biclusters.getBiclusters();
		for(int i=0, l=bics.size(); i<l; i++)
			for(int j=i+1; j<l; j++)
				if(bics.get(i).overlapArea(bics.get(j))>0){
					Bicluster block = bics.get(j).missingBlock(bics.get(i));
					if(block==null) block = bics.get(i).missingBlock(bics.get(j));
					if(block!=null && block.columns.size()>=minCols && block.rows.size()>=minRows) plaids.add(block);
				} 

		int[][] plaidMatrix = new int[matrix.length][matrix[0].length];
		for(Bicluster bic : plaids)
			for(Integer row : bic.rows){
				int j=0;
				for(Integer col : bic.columns)
					plaidMatrix[row][col]+=bic.items.get(j++);
			}
		
		oldSize = biclusters.size();
		if(constraint.equals(CoherencyConstraint.Relaxed))
			biclusters = mergeSomeBiclusters(biclusters.copy(),matrix,criteria,null);
		else biclusters = mergeSomeBiclusters(biclusters.copy(),matrix,criteria,plaidMatrix);
		System.out.println("I"+k+" plaid: " + oldSize + "=>" + biclusters.size());
	  }*/
	  return biclusters;	  	
	}

	public static Biclusters mergeSomeBiclusters(Biclusters biclusters, int[][] matrix, PlaidCoherency criteria, int[][] plaidMatrix){
		Biclusters result = new Biclusters();
		List<Bicluster> bics = biclusters.getBiclusters();
		List<Integer> removals = new ArrayList<Integer>();
		List<Integer> adds = new ArrayList<Integer>();
		
		/*for(int i=0; i<bics.size(); i++){
			Bicluster bicluster1 = bics.get(i);
			Biclusters mergings = new Biclusters();
			
			for(int j=i+1; j<bics.size(); j++){
				Bicluster bicluster2 = bics.get(j);
				if(bicluster1.overlapArea(bicluster2)>0.2){
					Bicluster block = bicluster2.missingBlock(bicluster1);
					if(block==null) block = bicluster1.missingBlock(bicluster2);
					
					// test: total overlap, small or incoherent 
					if(block!=null && block.columns.size()>minCols && block.rows.size()>minRows){
						//System.out.println(verifyCoherency(block,matrix,criteria,plaidMatrix)+"=>"+bicluster1+"|"+bicluster1+">"+block);
						if(verifyCoherency(block,matrix,criteria,plaidMatrix)){
							//System.out.println("True:"+block.toString());
							Bicluster bic3 = bicluster2.merge(bicluster1);
							removals.add(j);
							//System.out.println(bic3);
							mergings.add(bic3);
						} //else System.out.println("False:"+block.toString()); 
					} 
				}
			}
			if(mergings.size()==0) adds.add(i);
			else result.getBiclusters().addAll(mergings.getBiclusters());
		}
		adds.removeAll(removals);
		for(Integer add : adds) result.add(bics.get(add));*/
		return result;
	}

	private static boolean verifyCoherency(Bicluster block, int[][] matrix, PlaidCoherency criteria, int[][] plaidMatrix) {
	  switch(constraint){
	    case Exact : 
	      if(plaidMatrix==null){
		      switch(criteria){
			    case Additive : // all rows have the same symbols
				  for(Integer col : block.columns)
				    for(Integer row : block.rows)
					  if(matrix[row][col]!=matrix[block.rows.first()][col]) return false;
				  return true; 
			    case Multiplicative : // all rows have the same symbols and these can be divided by the original pattern
			    	  int j=0;
					  for(Integer col : block.columns)
						    for(Integer row : block.rows)
						    	if(matrix[row][col]!=matrix[block.rows.first()][col]
						    		|| matrix[row][col]%block.items.get(j++)!=0) return false;
				  return true; 
			    default : return true; // interpolated
		   	  }
	      } else { // matrix values coincide with plaids
			  for(Integer col : block.columns)
			    for(Integer row : block.rows)
				  if(matrix[row][col]!=plaidMatrix[row][col]) return false;
						  //System.out.println("::"+block.rows.get(i)+","+block.columns.get(j)+":"+matrix[block.rows.get(i)][block.columns.get(j)]+"<>"+plaidMatrix[block.rows.get(i)][block.columns.get(j)]);
			  return true; 
	      }
	    case InBetween : 
	    	if(relaxedError<1){
	    		int j=0, count=0;
				for(Integer col : block.columns){
				   for(Integer row : block.rows)
					  if(block.items.get(j)<=matrix[row][col]) count++;
				   j++;
				}
				//System.out.println("Relaxed:"+(count/((double)block.area())));
		    	return (count/((double)block.area()) > relaxedError);
	    	} else {
		    	if(plaidMatrix==null){ // contribution of the gene less than the sum (only positive expression values)
		    	  int j=0;
				  for(Integer col : block.columns){
				    for(Integer row : block.rows)
					  if(block.items.get(j)>matrix[row][col]) return false;
				    j++;
				  }
				  return true; 
		    	} else { // plaid sum less than overall value (only positive expression values)
				  for(Integer col : block.columns)
				    for(Integer row : block.rows)
					  if(matrix[row][col]>plaidMatrix[row][col]) return false;
				  return true; 
		    	}
	    	}
	    case Relaxed : 
	    	double count = 0;
	    	if(plaidMatrix==null){ // contribution of the gene less than the sum (only positive expression values)
	    		int j=0;
				for(Integer col : block.columns){
				   for(Integer row : block.rows)
					  if(block.items.get(j)<=matrix[row][col]) count++;
				   j++;
				}
				//System.out.println("Relaxed:"+(count/((double)block.area())));
		    	return (count/((double)block.area()) > relaxedError);
	    	} else {
				for(Integer col : block.columns)
				  for(Integer row : block.rows)
					if(matrix[row][col]==plaidMatrix[row][col]) count++;
				//System.out.println("Relaxed:"+(count/((double)block.area())));
		    	return (count/((double)block.area()) > relaxedError);
	    	}
	    default : return true;
	  }
	}
}
