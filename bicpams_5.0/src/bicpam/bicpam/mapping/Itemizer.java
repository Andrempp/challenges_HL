package bicpam.mapping;

import utils.BicResult;
import weka.core.Attribute;
import bicpam.bicminer.BiclusterMiner.Orientation;
import bicpam.mapping.Itemizer.DiscretizationCriteria;
import bicpam.mapping.Itemizer.FillingCriteria;
import bicpam.mapping.Itemizer.NoiseRelaxation;
import bicpam.mapping.Itemizer.NormalizationCriteria;
import bicpam.mapping.TFilter.FilterCriteria;
import bicpam.mapping.TFilter.RemoveCriteria;
import domain.Biclusters;
import domain.Dataset;


/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class Itemizer {

	public enum DiscretizationCriteria { None, SimpleRange, EqualDepth, NormalDist, OtherDist, ParamRange, MultiDisc } 
	public enum NormalizationCriteria { Overall, Column, Row, IshiiRow, None } 
	public enum NoiseRelaxation { None , OptionalItem, OneItem }
	public enum FillingCriteria { None, RemoveValue, Replace } 
	public enum OutlierizationCriteria { Overall, Column, Row } 

	
	/* =================================== */
	/* =========== CORE METHODS ========== */
	/* =================================== */
	
	public static Dataset run(Dataset data, int nrLabels, boolean symmetry, NormalizationCriteria norm, 
			DiscretizationCriteria disc, NoiseRelaxation noise, FillingCriteria fill) throws Exception{
		return run(data,nrLabels,symmetry,norm,disc,noise,fill,false,null);
	}


	public static Dataset run(Dataset data, int nrLabels, boolean symmetries,
			NormalizationCriteria norm, DiscretizationCriteria disc, NoiseRelaxation noise, 
			FillingCriteria fill, //OutlierHandler out, 
			boolean scalability, Orientation orientation) throws Exception{

		data = MissingsHandler.run(data, fill); 
		//data = OutlierHandler.run(dataset,outCriteria,outPercentage);

		if(scalability){
			if(orientation.equals(Orientation.PatternOnColumns)) data.invert();
			data = TFilter.run(data, FilterCriteria.HighDifferential);
			if(orientation.equals(Orientation.PatternOnColumns)) data.invert();
		}
		data = Normalizer.run(data, norm); 
		//BicResult.println(data.toString(false));

		data = Discretizer.run(data, disc, noise, nrLabels);
		//BicResult.print("\n\n\n\n\n\n"+data.toIntString());
		
		
		
		data.nrLabels = nrLabels;
		for(Attribute att : data.domains) 
			if(!att.isNumeric()) data.nrLabels = Math.max(data.nrLabels, att.numValues());
		return data;
	}
}