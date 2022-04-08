package generator;

import generator.BicMatrixGenerator.PatternType;
import generator.ClassifierGenerator.Distribution;

import java.util.ArrayList;
import java.util.List;

import utils.BicPrinting;
import utils.BicResult;
import domain.Bicluster;
import domain.Biclusters;
import domain.Dataset;

/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class DefaultDatasets {

	public boolean defaultdataset = true;

	public int[] clNumRows, clNumColumns, classes;
	public double[] noise, imbalance, skewedFeatures;
	public double[][] means, stds, params;
    public double[] avgNrBics, imbNrBics, avgDiscStrength, stdDiscStrength, imbDiscSteng;
    public double[] avgAreaBics, stdAreaBics, imbAreaBics, avgRowsBics, stdRowsBics, imbRowsBics, avgColsBics, stdColsBics, imbColsBics;
    public int[] nrLabels;
    public PatternType[] bicType;

	public String background = "random";
	public int[] numRows, numColumns, numBics, alphabets;
	public String distRowsBics = "Uniform", distColsBics = "Uniform";
	public int[] minRowsBics, minColsBics, maxRowsBics, maxColsBics;

    public DefaultDatasets(){
    	
    	//CLASSIFIERS
    	clNumRows = new int[]{2000};
    	clNumColumns = new int[]{5000};//,200,1000,1000};
    	classes = new int[]{3};
    	
    	imbalance = new double[]{0.1,0.5,0.9};
    	noise = new double[]{}; //0,1.5,3
    	skewedFeatures = new double[]{}; //0,0.4,0.8
    	
    	//BACKGROUND ORIENTED TO CLASSIFICATION
    	params = new double[][]{{4,5,6}}; //Poisson
    	means = new double[][]{{1,0,-1}}; //N(u,-)
    	stds = new double[][]{{3,3,3}};   //N(-,s)

    	//DISC BICLUSTERS
    	nrLabels = new int[]{10,10};
    	avgNrBics = new double[]{4,4};
    	imbNrBics = new double[]{0,0.5};
    	
    	avgAreaBics = new double[]{0.1,0.1}; 
    	stdAreaBics = new double[]{0.01,0.01};
    	imbAreaBics = new double[]{0,0}; 
    	
    	avgRowsBics = new double[]{0.4,0.4};
    	stdRowsBics = new double[]{0.01,0.01};
    	imbRowsBics = new double[]{0,0}; 
    	avgColsBics = new double[]{0.005,0.005};
    	stdColsBics = new double[]{0.0,0.0};
    	imbColsBics = new double[]{0,0}; 
    	
    	avgDiscStrength = new double[]{0.9,0.9}; 
    	stdDiscStrength = new double[]{0.01,0.01}; 
    	imbDiscSteng = new double[]{0,0}; 
    	bicType = new PatternType[]{PatternType.Constant,PatternType.Constant};

    	// BICLUSTERS
		numRows = new int[]{20,100,500,1000,2000};
		numColumns = new int[]{10,30,60,100,200};
		
		numBics = new int[]{2,3,5,10,15};
		alphabets = new int[]{5};
		
	    minRowsBics = new int[]{4,12,17,22,43}; 
	    maxRowsBics = new int[]{5,20,30,40,60};
	    minColsBics = new int[]{4,6,6,6,6};
	    maxColsBics = new int[]{5,6,8,10,10};
	}

	public List<Dataset> getClassifierSyntheticData() throws Exception {

		Distribution type = Distribution.Local;//LocalGaussian;

		List<Dataset> results = new ArrayList<Dataset>();
		for(int i=0, l=clNumRows.length; i<l; i++){
			switch(type){ 
			case Poisson : 
				for(int k=0; k<params.length; k++)
					results.add(new ClassifierGenerator(clNumRows[i],clNumColumns[i],classes[0],params[k]).run());
				break;
			case Gaussian : 
				for(int k=0; k<means.length; k++)
					results.add(new ClassifierGenerator(clNumRows[i],clNumColumns[i],classes[0],means[k],stds[k]).run());
				break;
			case Local : 
				for(int k=0; k<1; k++){ //avgNrBics.length
					results.add(new ClassifierGenerator(clNumRows[i],clNumColumns[i],classes[0], nrLabels[0], avgNrBics[0], imbNrBics[0], 
						avgRowsBics[0], stdRowsBics[0], imbRowsBics[0], avgColsBics[0], stdColsBics[0], imbColsBics[0],
						//avgAreaBics[0], stdAreaBics[0], imbAreaBics[0], 
					  	avgDiscStrength[0], stdDiscStrength[0], imbDiscSteng[0], bicType[0]).run());
				}
				break;
			case LocalGaussian :
				for(int k=0; k<1; k++){ 
					ClassifierGenerator generator = new ClassifierGenerator(clNumRows[i],clNumColumns[i],classes[0],means[k],stds[k]);
					generator.addLocalInfo(nrLabels[0], avgNrBics[0], imbNrBics[0],
						avgRowsBics[0], stdRowsBics[0], imbRowsBics[0], avgColsBics[0], stdColsBics[0], imbColsBics[0],
					  	avgDiscStrength[0], stdDiscStrength[0], imbDiscSteng[0], bicType[0]);
					results.add(generator.run()); 
				}
				break;
			case LocalPoisson :
				for(int k=0; k<1; k++){ 
					ClassifierGenerator generator = new ClassifierGenerator(clNumRows[i],clNumColumns[i],classes[0],params[k]);
					generator.addLocalInfo(nrLabels[0], avgNrBics[0], imbNrBics[0],
						avgRowsBics[0], stdRowsBics[0], imbRowsBics[0], avgColsBics[0], stdColsBics[0], imbColsBics[0],
					  	avgDiscStrength[0], stdDiscStrength[0], imbDiscSteng[0], bicType[0]);
					results.add(generator.run()); 
				}
				break;
			}
	    }
      	return results;
	}

	public List<Dataset> getBicSyntheticData(PatternType type) throws Exception {
		List<Dataset> results = new ArrayList<Dataset>();

	    for(int alphabet : alphabets){	  
	    	BicResult.println("ALPHABET::"+alphabet);
	    	int i = defaultdataset ? 0 : 1;
	    	int l = defaultdataset ? 1 : numRows.length;
	    		    	
			for(; i<l; i++){
				BicResult.println("["+numRows[i]+","+numColumns[i]+"]("+numBics[i]+")");
				BicMatrixGenerator generator = new BicMatrixGenerator(numRows[i],numColumns[i],numBics[i],background,alphabet,false);
				Biclusters trueBics = generator.generateKBiclusters(type,
						distRowsBics, (double)minRowsBics[i], (double)maxRowsBics[i], 
						distColsBics, (double)minColsBics[i], (double)maxColsBics[i], false, false, false);
				for(Bicluster bic : trueBics.getBiclusters()) System.out.println(bic.toString());
					
				int[][] dataset = generator.getSymbolicExpressionMatrix();
				Dataset data = new Dataset(dataset);
				data.nrLabels=alphabet;
				data.name="aa"+type+"A"+numRows[i]+"N0M0";
				results.add(data);
			}
	    }
      	return results;
	}
}