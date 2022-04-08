package bicpam.bicminer;

import bicpam.bicminer.constant.PlaidMiner.CoherencyConstraint;
import bicpam.closing.Biclusterizer;
import bicpam.pminer.PM;
import domain.Bicluster;
import domain.Biclusters;
import domain.Dataset;
import generator.BicMatrixGenerator.PlaidCoherency;

/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public abstract class BiclusterMiner {

	public enum Orientation {PatternOnRows, PatternOnColumns}
	public enum BiclusterType {Constant, Coherent, Evolution}
	public enum CoherenceCriteria {Constant, Additive, Multiplicative, Plaid}
	public enum EvolutionCriteria {CoExpression, OrderPreserving, StateBased}

	protected Orientation orientation;

	protected Dataset data;
	protected PM pminer;
	protected Biclusterizer bichandler;
	protected Dataset globalData;
		
	public BiclusterMiner(){}
	
	public BiclusterMiner(Dataset _data, PM _pminer, Biclusterizer _bichandler){
		this(_data,_pminer,_bichandler,Orientation.PatternOnRows);
	}
	public BiclusterMiner(Dataset _data, PM _pminer, Biclusterizer _bichandler, Orientation _orientation){
		data = _data;
		pminer = _pminer;
		orientation = _orientation;
		bichandler = _bichandler;
	}
	
	public Biclusters mineBiclusters() throws Exception {
		if(orientation == Orientation.PatternOnRows) 
			return bichandler.run(mineItemsets());
		data.invert();
		Biclusters bics = exchangeXY(mineItemsets());
		data.invert();
		//System.out.println(">> rows:"+data.rows.size());
		//System.out.println(">> cols1:"+data.columns.size());
		//System.out.println(">> cols2:"+data.originalColumns.size());
		return bichandler.run(bics);
	}
	
	public abstract Biclusters mineItemsets() throws Exception;

	protected Biclusters exchangeXY(Biclusters biclusters) {
		Biclusters result = new Biclusters();
		for(Bicluster bic: biclusters.getBiclusters())
			result.add(new Bicluster(bic.columns,bic.rows));
		return result;
	}
	
	public Dataset getData(){ return data; }
	public PM getPMiner(){ return pminer; }
	public Biclusterizer getBiclusterizer(){ return bichandler; }
	public void setData(Dataset _data){ data = _data; }
	public void setGlobalData(Dataset _data){ globalData = _data; }
	public void pminer(PM _pminer){ pminer = _pminer; }
	public void setBiclusterizer(Biclusterizer _bichandler){ bichandler = _bichandler; }

	public Biclusters getPlaidFromCompactSet(Biclusters biclusters, PlaidCoherency criteria){
		return null;//PlaidMiner.run(biclusters, data.getItems(), criteria, CoherencyConstraint.InBetween, 0.9);
	}
	public Biclusters getPlaidFromCompactSet(Biclusters biclusters, PlaidCoherency criteria, CoherencyConstraint constraint, double error){
		return null;//PlaidMiner.run(biclusters, data.getItems(), criteria, constraint, error);
	}

}