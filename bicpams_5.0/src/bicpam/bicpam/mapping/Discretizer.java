package bicpam.mapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import utils.BicException;
import utils.BicPrinting;
import utils.BicResult;
import weka.core.Attribute;
import domain.Dataset;
import bicpam.mapping.Itemizer.DiscretizationCriteria;
import bicpam.mapping.Itemizer.NoiseRelaxation;


/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class Discretizer {

	private static double INF = Dataset.MISSING-1;	
	public static double[][] breakingPoints = new double[][]{{},{},
		{-INF, 0},//2
		{-INF, -0.43,  0.43}, 
		{-INF, -0.67,  0,     0.67}, 
		{-INF, -0.84, -0.25,  0.25,  0.84}, 
		{-INF, -0.97, -0.43,  0,     0.43,  0.97}, 
		{-INF, -1.07, -0.57, -0.18,  0.18,  0.57,  1.07}, 	
		{-INF, -1.15, -0.67, -0.32,  0,     0.32,  0.67,  1.15}, 
		{-INF, -1.22, -0.76, -0.43, -0.14,  0.14,  0.43,  0.76,  1.22}, 
		{-INF, -1.28, -0.84, -0.52, -0.25,  0,     0.25,  0.52,  0.84,  1.28}, 
		{-INF, -1.34, -0.91, -0.6,  -0.35, -0.11,  0.11,  0.35,  0.6,   0.91, 1.34}, 
		{-INF, -1.38, -0.97, -0.67, -0.43, -0.21,  0,     0.21,  0.43,  0.67, 0.97, 1.38}, 
		{-INF, -1.43, -1.02, -0.74, -0.5,  -0.29, -0.1,   0.1,   0.29,  0.5,  0.74, 1.02, 1.43}, 
		{-INF, -1.47, -1.07, -0.79, -0.57, -0.37, -0.18,  0,     0.18,  0.37, 0.57, 0.79, 1.07, 1.47}, 
		{-INF, -1.5,  -1.11, -0.84, -0.62, -0.43, -0.25, -0.08,  0.08,  0.25, 0.43, 0.62, 0.84, 1.11, 1.5},//15 
		{-INF, -1.53, -1.15, -0.89, -0.67, -0.49, -0.32, -0.16,  0,     0.16, 0.32, 0.49, 0.67, 0.89, 1.15, 1.53}, 
		{-INF, -1.56, -1.19, -0.93, -0.72, -0.54, -0.38, -0.22, -0.07,  0.07, 0.22, 0.38, 0.54, 0.72, 0.93, 1.19, 1.56}, 
		{-INF, -1.59, -1.22, -0.97, -0.76, -0.59, -0.43, -0.28, -0.14,  0,    0.14, 0.28, 0.43, 0.59, 0.76, 0.97, 1.22, 1.59}, 
		{-INF, -1.62, -1.25, -1,    -0.8,  -0.63, -0.48, -0.34, -0.2,  -0.07, 0.07, 0.2,  0.34, 0.48, 0.63, 0.8,  1,    1.25, 1.62}, 
		{-INF, -1.64, -1.28, -1.04, -.84, -0.67, -0.52, -0.39, -0.25, -0.13, 0,    0.13, 0.25, 0.39, 0.52, 0.67, 0.84, 1.04, 1.28, 1.64}};//20

	
	public static Dataset run(Dataset data, DiscretizationCriteria criteria, NoiseRelaxation noiseCriteria, int nrLabels){
		if(data.scores==null) return data;
		List<List<Double>> scores = data.scores;
		List<List<Integer>> result = new ArrayList<List<Integer>>();
		List<List<Integer>> noise = new ArrayList<List<Integer>>();
		double close = noiseCriteria.equals(NoiseRelaxation.OptionalItem) ? 0.2 : 0;
		double[] cutPoints = null;
		

		switch(criteria) {
			case None: 
				for(int i=0, l1=scores.size(); i<l1; i++){
					result.add(new ArrayList<Integer>());
					for(int j=0, l2=scores.get(i).size(); j<l2; j++)
						result.get(i).add((int)Math.round(scores.get(i).get(j)));
				}
				data.intscores = result;
				return data;

			case MultiDisc: 
				for(int i=0, l1=scores.size(); i<l1; i++){
					result.add(new ArrayList<Integer>());
					noise.add(new ArrayList<Integer>());
					for(int j=0, l2=scores.get(i).size(); j<l2; j++) {
						double v = scores.get(i).get(j);
						double newvalue = (v%1==0) ? Dataset.MISSING : Math.ceil(v);
						result.get(i).add((int)Math.floor(v));
						noise.get(i).add((int)newvalue);
					}
				}
				break;

			case ParamRange:
				
				//ranges ]0,0.18] => 1, ]0.12,1] => 2
				double[][] ranges = new double[][] {{0,0.1},{0.035,1}};
				//double[][] ranges = new double[][] {{0,0.03},{0.02,0.05},{0.04,0.08},{0.06,0.14},{0.12,0.5},{0.4,1}};
				
				List<List<Integer>> indexes = new ArrayList<List<Integer>>();
				List<List<Double>> newscores = new ArrayList<List<Double>>();
				for(int i=0, l1=scores.size(); i<l1; i++){
					result.add(new ArrayList<Integer>());
					newscores.add(new ArrayList<Double>());
					indexes.add(new ArrayList<Integer>());
					for(int j=0, l2=scores.get(i).size(); j<l2; j++) {
						double p = scores.get(i).get(j);
						int label = 1;
						for(double[] range : ranges) {
							if(p>range[0] && p<=range[1]) {
								result.get(i).add(0,label);
								indexes.get(i).add(0,data.indexes.get(i).get(j));
								newscores.get(i).add(0,data.scores.get(i).get(j));
							}
							label++;
						}
					}
				}
				data.indexes = indexes;
				data.intscores = result;
				data.scores = newscores;
				return data;
				
			case SimpleRange: 
				double max=0, min=0;
				for(int i=0, l1=scores.size(); i<l1; i++){
					for(int j=0, l2=scores.get(i).size(); j<l2; j++){
						int att = data.indexes.get(i).get(j);
						if(data.domains.get(att).isNumeric()) { 
							if(scores.get(i).get(j)>max) max = scores.get(i).get(j); 
							else if(scores.get(i).get(j)<min) min = scores.get(i).get(j);
						}
					}
				}
				double window = (max-min)/nrLabels;
				for(int i=0, l1=scores.size(); i<l1; i++){
					result.add(new ArrayList<Integer>());
					noise.add(new ArrayList<Integer>());
					for(int j=0, l2=scores.get(i).size(); j<l2; j++){
						double value = scores.get(i).get(j);
						int att = data.indexes.get(i).get(j);
						if(!data.domains.get(att).isNumeric()) { 
							result.get(i).add((int) value);
							continue;
						}
						for(int k=0; min+k*window<=max; k++){
							if(value<=min+(k+1)*window) {
								result.get(i).add(k);
								
								if(!noiseCriteria.equals(NoiseRelaxation.None)){
									int newvalue = (int) Dataset.MISSING;
									if(k>0 && value<min+k*window+window*close) newvalue = k-1;
									else if(k<(nrLabels-1) && value>min+(k+1)*window-window*close) newvalue = k+1;
									noise.get(i).add(newvalue);
								}
								break;
							}
						}
					}
				}
				break;
				
			case EqualDepth:
				//if(nrLabels > breakingPoints.length) throw new BicException("Error! alphabet_size is too big"); 
				cutPoints = breakingPoints[nrLabels];
				List<Double> vector = new ArrayList<Double>();
				int nrObservations = 0;
				for(int i=0, l1=data.indexes.size(); i<l1; i++) {
					nrObservations += data.indexes.get(i).size();
					for(int j=0, l2=data.indexes.get(i).size(); j<l2; j++)
						if(data.domains.get(j).isNumeric()) vector.add(data.scores.get(i).get(j));
				}
				Collections.sort(vector);
				int observationsPerLabel = nrObservations/nrLabels;
				for(int i=1; i<nrLabels; i++)
					cutPoints[i]=vector.get(observationsPerLabel*i);
				cutPoints[0]=-INF;
				System.out.println(">>"+BicPrinting.plot(cutPoints));
				for(int i=0, l1=scores.size(); i<l1; i++){
					result.add(new ArrayList<Integer>());
					noise.add(new ArrayList<Integer>());

					for(int j=0, l2=scores.get(i).size(); j<l2; j++){
						double value = scores.get(i).get(j);
						int att = data.indexes.get(i).get(j);
						if(!data.domains.get(att).isNumeric()) { 
							result.get(i).add((int) value);
							continue;
						}
						for(int k=cutPoints.length-1; k>=0; k--){
							if(value >= cutPoints[k]) { 
								result.get(i).add(k);

								if(!noiseCriteria.equals(NoiseRelaxation.None)){
									int newvalue = (int) Dataset.MISSING;
									if(k==0){
									  double distance=Math.abs(2*cutPoints[1])-Math.abs(cutPoints[1]);
									  //System.out.println("k=0 V>"+(cutPoints[1]-distance*close)+" p="+cutPoints[1]+" val="+value+" d="+distance+" c="+close);
									  if(value>=cutPoints[1]-distance*close) newvalue=1;
									  //System.out.println("v:"+value+"\tk>0 v<"+(min+k*window+window*close)+"k<nL-1 v>"+(min+(k+1)*window-window*close));
									} else if(k==(nrLabels-1)){
									  double distance=(cutPoints[k]-cutPoints[k-1]);
									  //System.out.println("k=<L V<"+(cutPoints[k]+distance*close)+" p="+cutPoints[k]+" val="+value+" d="+distance+" c="+close);
									  if(value<=cutPoints[k]+distance*close) newvalue=k-1;
									} else {
									  double distance=(cutPoints[k+1]-cutPoints[k]);
									  if(value<=cutPoints[k]+distance*close) newvalue=k-1;
									  else if(value>=cutPoints[k+1]-distance*close) newvalue=k+1;
									}
									noise.get(i).add(newvalue);
								}
								break;
							}
						}
					}
				}
				break;
				
			case NormalDist: 
			case OtherDist: 
			default:
				if(nrLabels > breakingPoints.length) throw new BicException("Error! alphabet_size is too big"); 
				cutPoints = breakingPoints[nrLabels];
				
				/*int p=0;
				for(int j=0; j<data.columns.size(); j++){
					if(!data.domains.get(j).isNumeric()) continue;
					double mean=data.mean.get(p), std=data.std.get(p);
					System.out.println("\n"+data.columns.get(j));
					for(double cut : cutPoints)
						System.out.print(cut*std+mean+"|");
					p++;
				}*/
				for(int i=0, l1=scores.size(); i<l1; i++){
					result.add(new ArrayList<Integer>());
					noise.add(new ArrayList<Integer>());
					
					for(int j=0, l2=scores.get(i).size(); j<l2; j++){
						double value = scores.get(i).get(j);
						int att = data.indexes.get(i).get(j);
						if(!data.domains.get(att).isNumeric()) { 
							result.get(i).add((int) value);
							noise.get(i).add((int) Dataset.MISSING);
							continue;
						}
						for(int k=cutPoints.length-1; k>=0; k--){
							if(value >= cutPoints[k]) { 
								result.get(i).add(k);

								if(!noiseCriteria.equals(NoiseRelaxation.None)){
									int newvalue = (int) Dataset.MISSING;
									if(k==0){
									  double distance=(cutPoints[2]-cutPoints[1]);
									  //System.out.println("k=0 V>"+(cutPoints[1]-distance*close)+" p="+cutPoints[1]+" val="+value+" d="+distance+" c="+close);
									  if(value>=cutPoints[1]-distance*close) newvalue=1;
									  //System.out.println("v:"+value+"\tk>0 v<"+(min+k*window+window*close)+"k<nL-1 v>"+(min+(k+1)*window-window*close));
									} else if(k==(nrLabels-1)){
									  double distance=(cutPoints[k]-cutPoints[k-1]);
									  //System.out.println("k=<L V<"+(cutPoints[k]+distance*close)+" p="+cutPoints[k]+" val="+value+" d="+distance+" c="+close);
									  if(value<=cutPoints[k]+distance*close) newvalue=k-1;
									} else {
									  double distance=(cutPoints[k+1]-cutPoints[k]);
									  if(value<=cutPoints[k]+distance*close) newvalue=k-1;
									  else if(value>=cutPoints[k+1]-distance*close) newvalue=k+1;
									}
									noise.get(i).add(newvalue);
								}
								break;
							}
						}
					}
					//System.out.println("Freqs:"+BicMath.count(result.get(i),0)+","+BicMath.count(result.get(i),1)+","+BicMath.count(result.get(i),2)+","+BicMath.count(result.get(i),3)+","+BicMath.count(result.get(i),4));
				}
		}
		
		//List<List<String>> test = Arrays.asList(Arrays.asList("a","b"),Arrays.asList("c","d"));
		//BicResult.println("A0\n"+test.toString());
		
		//BicResult.println(">>NOISE:"+noise);
		if(!noiseCriteria.equals(NoiseRelaxation.None) || criteria.equals(DiscretizationCriteria.MultiDisc)){
			List<List<Integer>> indexes = data.indexes;
			for(int i=0, l1=noise.size(); i<l1; i++){
				for(int j=0, l2=noise.get(i).size(); j<l2; j++){
					if(noise.get(i).get(j)!=(int)Dataset.MISSING){
						result.get(i).add(noise.get(i).get(j));
						indexes.get(i).add(indexes.get(i).get(j));
					}
				}
			}
			data.indexes = indexes;
		}

		//System.out.println("I0:"+result.get(0));
		//System.out.println("I1:"+result.get(1));
		if(data.symmetry){
			System.out.println("SYMMETRY");
			int shift = data.symmetry ? nrLabels/2 : 0;
			int adjustpos = (data.symmetry && (nrLabels%2==0)) ? 1 : 0;
			for(int i=0, l1=result.size(); i<l1; i++){
				for(int j=0, l2=result.get(i).size(); j<l2; j++){
					int value=result.get(i).get(j)-shift;
					result.get(i).set(j,value+(value>=0 ? adjustpos : 0));				
				}
			}
		}
		data.intscores = result;
		//System.out.println("I0:"+data.intscores.get(0));
		//System.out.println("I1:"+data.intscores.get(1));
		return data;
	}
}