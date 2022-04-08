package tests.others.constraints;

import generator.Annotation;
import generator.BicMatrixGenerator;
import generator.BicMatrixGenerator.PatternType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import domain.Biclusters;
import domain.Dataset;
import utils.BicReader;
import utils.BicResult;
import utils.NetMatrixMapper;


/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class AnnotatedTestingData {

	private List<Dataset> datasets;
	private List<Biclusters> consistentBics;
	String path = new File("").getAbsolutePath();

	public AnnotatedTestingData(List<String> files) throws Exception{
		consistentBics = null;
		datasets = new ArrayList<Dataset>();
		for(String file : files){
			Dataset data;
			if(file.contains("network")) data = NetMatrixMapper.getTable(path+file,0,1,2," ",true);
			else data = new Dataset(BicReader.getInstances(path+file));
			data.nrLabels = 4;
			List<List<String>> annotations = Annotation.run(data.rows,true);
			data.annotations = Annotation.convertToNumericAnnotations(annotations);
			BicResult.println("Annotations:\n\n"+annotations);
			BicResult.println("Annotations:\n\n"+data.annotations);
			datasets.add(data);
		}
	}
	public AnnotatedTestingData(List<Integer> avgNrRowsPerAnnotation, List<Integer> avgNrAnnotationsPerRow) throws Exception{
		datasets = new ArrayList<Dataset>();
		consistentBics = new ArrayList<Biclusters>();
    	int numRows = 2000, numColumns = 200;
		int minrowsBics = 300, maxrowsBics = 300, mincolsBics = 5, maxcolsBics = 5;
		int nrLabels = 5;
		
		BicMatrixGenerator generator = new BicMatrixGenerator(numRows,numColumns,10,"random",nrLabels,false);
		Biclusters trueBics = generator.generateKBiclusters(PatternType.Constant, "uniform", minrowsBics, maxrowsBics, "uniform", mincolsBics, maxcolsBics, false, false, false);

		for(Integer rowsPerAnnot : avgNrRowsPerAnnotation){
			for(Integer annotPerRow : avgNrAnnotationsPerRow){
				Dataset data = new Dataset(generator.getSymbolicExpressionMatrix());
				data.nrLabels = nrLabels;
				data.annotations = Annotation.run(data.rows.size(),rowsPerAnnot,annotPerRow,trueBics);
				datasets.add(data);
				consistentBics.add(trueBics);
			}
		}
		//BicGeneratorTests.writeFile("/Data3.txt",BicGeneratorTests.matrixToString(dataset));
		//BicGeneratorTests.writeFile("/Annotations3.txt",data.annotations.toString());
	}
    
    public List<Dataset> getDatasets(){
    	return datasets;
    }
    public List<Biclusters> getConsistentBiclusters(){
    	return consistentBics;
    }
}