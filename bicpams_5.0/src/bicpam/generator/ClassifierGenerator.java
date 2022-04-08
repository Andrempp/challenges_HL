package generator;

import generator.BicMatrixGenerator.PatternType;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.PoissonDistribution;
import utils.BicException;
import utils.BicMath;
import utils.BicPrinting;
import utils.BicResult;
import domain.Bicluster;
import domain.Dataset;
import java.util.*;

public class ClassifierGenerator {

  public enum Distribution { Gaussian, Poisson, Local, LocalGaussian, LocalPoisson };
  protected Distribution distribution;
  protected int numRows, numColumns, numClasses;
  protected double[] params, means, stds;
  public double[][] dataset;
  public double noise;
  public double imbalance;
  public double skewedFeatures;

  // For local dependencies
  int nrLabels = 0;
  double avgNrBics, imbNrBics; 
  double avgAreaBics=-1, stdAreaBics, imbAreaBics; 
  double avgRowsBics=-1, stdRowsBics, imbRowsBics; 
  double avgColsBics=-1, stdColsBics, imbColsBics; 
  double avgDiscStrength, stdDiscStrength, imbDiscStrength; 
  PatternType bicType;
  
  protected List<Bicluster> generated;

  
  /***************************
   ******** CONSTRUCTOR ******
   ***************************/

  public ClassifierGenerator(int _numRows, int _numColumns, int _numClasses, double[] params) throws Exception {
	this(_numRows, _numColumns, _numClasses);
    this.distribution = Distribution.Poisson;
    this.params = params;
  }
  public ClassifierGenerator(int _numRows, int _numColumns, int _numClasses, double[] means, double[] stds) throws Exception {
	this(_numRows, _numColumns, _numClasses);
    this.distribution = Distribution.Gaussian;
    this.means = means;
    this.stds = stds;
  }
  public ClassifierGenerator(int _numRows, int _numColumns, int _numClasses, 
		  int _nrLabels, double _avgNrBics, double _imbNrBics, 
		  double _avgRowsBics, double _stdRowsBics, double _imbRowsBics, 
		  double _avgColsBics, double _stdColsBics, double _imbColsBics, 
		  double _avgDiscStrength, double _stdDiscStrength, double _imbDiscSteng, 
		  PatternType type) throws Exception {
	this(_numRows, _numColumns, _numClasses);
    distribution = Distribution.Local;
	initialize(_nrLabels, _avgNrBics, _imbNrBics, _avgDiscStrength, _stdDiscStrength, _imbDiscSteng, type);
    avgRowsBics = _avgRowsBics; 
    stdRowsBics = _stdRowsBics; 
    imbRowsBics = _imbRowsBics;  
    avgColsBics = _avgColsBics; 
    stdColsBics = _stdColsBics; 
    imbColsBics = _imbColsBics;  
  }
  public ClassifierGenerator(int _numRows, int _numColumns, int _numClasses, 
		  int _nrLabels, double _avgNrBics, double _imbNrBics, 
		  double _avgAreaBics, double _stdAreaBics, double _imbAreaBics, 
		  double _avgDiscStrength, double _stdDiscStrength, double _imbDiscSteng, 
		  PatternType type) throws Exception {
	this(_numRows, _numColumns, _numClasses);
    distribution = Distribution.Local;
	initialize(_nrLabels, _avgNrBics, _imbNrBics, _avgDiscStrength, _stdDiscStrength, _imbDiscSteng, type);
    avgAreaBics = _avgAreaBics; 
    stdAreaBics = _stdAreaBics; 
    imbAreaBics = _imbAreaBics;  
  }
  public ClassifierGenerator(int _numRows, int _numColumns, int _numClasses) throws Exception {
    numRows = _numRows;
    numColumns = _numColumns;
    numClasses = _numClasses;
    imbalance = 0;
    skewedFeatures = 0;
    noise = 0;
  }
  public void addLocalInfo(int _nrLabels, double _avgNrBics, double _imbNrBics, 
		  double _avgRowsBics, double _stdRowsBics, double _imbRowsBics, 
		  double _avgColsBics, double _stdColsBics, double _imbColsBics, 
		  double _avgDiscStrength, double _stdDiscStrength, double _imbDiscSteng, 
		  PatternType type) throws Exception {
	if(distribution.equals(Distribution.Gaussian)) distribution = Distribution.LocalGaussian;
	else distribution = Distribution.LocalPoisson;
	initialize(_nrLabels, _avgNrBics, _imbNrBics, _avgDiscStrength, _stdDiscStrength, _imbDiscSteng, type);
    avgRowsBics = _avgRowsBics; 
    stdRowsBics = _stdRowsBics; 
    imbRowsBics = _imbRowsBics;  
    avgColsBics = _avgColsBics; 
    stdColsBics = _stdColsBics; 
    imbColsBics = _imbColsBics;  
  }
  private void initialize(int _nrLabels, double _avgNrBics, double _imbNrBics,
		double _avgDiscStrength, double _stdDiscStrength, double _imbDiscSteng, PatternType type) {
    nrLabels = _nrLabels;
    avgNrBics = _avgNrBics; 
    imbNrBics = _imbNrBics; 
    avgDiscStrength = _avgDiscStrength; 
    stdDiscStrength = _stdDiscStrength; 
    imbDiscStrength = _imbDiscSteng;
    bicType = type;	
  }
  

  /****************************
   ******** CORE METHODS ******
   ****************************/

  public Dataset run() throws BicException {
	dataset = new double[numRows][numColumns];	
	int[] rowsPerClass = runImbalance();
	int[] sumRows = new int[rowsPerClass.length+1];
	for(int i=0, l=rowsPerClass.length; i<l; i++) 
		sumRows[i+1] = sumRows[i]+rowsPerClass[i];
	int[] classes = new int[numRows];
	for(int i=0, l=sumRows.length-1; i<l; i++)
		for(int j=sumRows[i]; j<sumRows[i+1]; j++) classes[j]=i;
	
	if(!distribution.equals(Distribution.Local)){
		for(int k=0; k<numClasses; k++){ 
			for(int i=0; i < numRows; i++){ 
				if(sumRows[k+1]==i) k++;
				if(distribution.toString().contains("Gaussian")){
					NormalDistribution gauss = new NormalDistribution(means[k],stds[k]);
					for(int j=0, l=numColumns; j<l; j++) dataset[i][j] = gauss.sample();
				} else {
					PoissonDistribution poisson = new PoissonDistribution(params[k]); 
					for(int j=0, l=numColumns; j<l; j++) dataset[i][j] = poisson.sample();		
				}
			}
		}
	}
	if(distribution.toString().contains("Local")){
		int[][] data = new int[numRows][numColumns];
		if(distribution.equals(Distribution.Local)){
			Random r = new Random();
			for(int i=0, l1=numRows; i<l1; i++)
				for(int j=0, l2=numColumns; j<l2; j++) 
					data[i][j] = r.nextInt(nrLabels);
			dataset = new double[numRows][numColumns];
		} //else data = Normalizer.run(dataset, NormalizationCriteria.Overall);
		data = generateLocalDependencies(sumRows,data);
		//BicResult.println("DATASET\n"+BicPrinting.plot(data));
		for(int i=0; i<numRows; i++)
			for(int j=0; j<numColumns; j++)
				dataset[i][j]=data[i][j];
	}
	if(skewedFeatures>0) skewFeatures();
	if(noise>0) plantNoise();
	Dataset data = new Dataset(dataset);
	data.nrLabels=nrLabels;
	data.classValues = classes;
	return data;
  }
  
  private int[][] generateLocalDependencies(int[] sumRows, int[][] data) throws BicException{
	double imbNrBicsDec = imbNrBics*2/(numClasses-1);
	double imbDiscDec = imbDiscStrength*2/(numClasses-1);
	
	int[] nrBics = new int[numClasses];
	double[][] discBics = new double[numClasses][];
	for(int k=0, l=numClasses; k<l; k++){
	    nrBics[k] = (int) (avgNrBics*(1.0+imbNrBics));
	    imbNrBics -= imbNrBicsDec;
		
	    double discK = avgDiscStrength*(1.0+imbDiscStrength); 
	    imbDiscStrength -= imbDiscDec;
		NormalDistribution discDist = new NormalDistribution(discK,stdDiscStrength);
		discBics[k] = new double[nrBics[k]];
		for(int i=0; i<nrBics[k]; i++) discBics[k][i] = Math.min(1, discDist.sample());
	}
	BicResult.println("NrBics: "+BicPrinting.plot(nrBics));
	BicResult.println("DiscBics: "+BicPrinting.plot(discBics));
	BicDiscGenerator gen = null;
	if(avgAreaBics == -1) {
		double imbRowsBicsDec = imbRowsBics*2/(numClasses-1);
		double imbColsBicsDec = imbColsBics*2/(numClasses-1);
		double[][] rowsBics = new double[numClasses][];
		double[][] colsBics = new double[numClasses][];
		for(int k=0, l=numClasses; k<l; k++){
		    double rowsBicsK = avgRowsBics*(1.0+imbRowsBics); 
		    imbRowsBics -= imbRowsBicsDec;
			NormalDistribution rowsDist = new NormalDistribution(rowsBicsK,stdRowsBics);
			rowsBics[k] = new double[nrBics[k]];
			for(int i=0; i<nrBics[k]; i++) rowsBics[k][i] = Math.max(0, rowsDist.sample());
			
		    double colsBicsK = avgColsBics*(1.0+imbColsBics); 
		    imbColsBics -= imbColsBicsDec;
			NormalDistribution colsDist = new NormalDistribution(colsBicsK,(stdColsBics==0 ? 0.0001 : stdColsBics));
			colsBics[k] = new double[nrBics[k]];
			for(int i=0; i<nrBics[k]; i++) colsBics[k][i] = Math.max(0, colsDist.sample());
		}
		BicResult.println("RowBics: "+BicPrinting.plot(rowsBics));
		BicResult.println("ColBics: "+BicPrinting.plot(colsBics));
		gen = new BicDiscGenerator(sumRows,numColumns,nrBics,rowsBics,colsBics,discBics,nrLabels,bicType);
	} else {
		double imbAreaBicsDec = imbAreaBics*2/(numClasses-1);
		double[][] areaBics = new double[numClasses][];
		for(int k=0, l=numClasses; k<l; k++){
		    double areaBicsK = avgAreaBics*(1.0+imbAreaBics); 
		    imbAreaBics -= imbAreaBicsDec;
			NormalDistribution areaDist = new NormalDistribution(areaBicsK,stdAreaBics);
			areaBics[k] = new double[nrBics[k]];
			for(int i=0; i<nrBics[k]; i++) areaBics[k][i] = Math.max(0, areaDist.sample());
		}
		BicResult.println("AreaBics: "+BicPrinting.plot(areaBics));
		gen = new BicDiscGenerator(sumRows,numColumns,nrBics,areaBics,discBics,nrLabels,bicType);
	}
	gen.symbolicMatrix = data;
	return gen.generateKDiscBiclusters();
  }
  
  private int[] runImbalance() {
	int[] rowsPerClass = new int[numClasses];
	int avgRows = (int) numRows/numClasses;
	double imbalanceDec = imbalance*2/(numClasses-1);
	for(int i=0; i<numClasses; i++){
		rowsPerClass[i]=(int)(avgRows*(1.0+imbalance));
		imbalance-=imbalanceDec;
	}
	rowsPerClass[0] += (numRows - BicMath.sum(rowsPerClass));
	System.out.println("NRows:"+numRows);
	System.out.println(BicPrinting.plot(rowsPerClass));
	return rowsPerClass;
  }

  private void plantNoise() {
	double min = BicMath.min(means);
	double range = BicMath.max(means)-BicMath.min(means);
	Random r = new Random();
	for(int j=0, l2=numColumns; j<l2; j++)
		for(int i=0, l1=numRows; i<l1; i++)
			dataset[i][j] = dataset[i][j]+(r.nextDouble()*range+min)*noise;
  }
  
  private void skewFeatures() {
	Random r = new Random();
    Set<Integer> featureSet = new HashSet<Integer>();
    int feature, nrSkewed = (int)(numColumns*skewedFeatures);

    for(int i=0; i<nrSkewed; i++) {
    	do{ feature = r.nextInt(this.numColumns); }
    	while(featureSet.contains(feature));
    	featureSet.add(feature);
    }  
	double min = BicMath.min(means);
	double range = BicMath.max(means)-min;
	for(int j : featureSet){
		for(int i=0, l1=numRows; i<l1; i++)
			dataset[i][j] = r.nextDouble()*range+min;
	}
  }

  
  /******************************
   ******* OTHER METHODS ********
   ******************************/

  public double getNoise(){ return noise; }
  public double getImbalance(){ return imbalance; }
  public double getSkewedFeatures(){ return skewedFeatures; }
  public void setNoise(double _noise){ this.noise = _noise; }
  public void setImbalance(double _imbalance){ this.imbalance = _imbalance; }
  public void setSkewedFeatures(double _skewedFeatures){ this.skewedFeatures = _skewedFeatures; }
}