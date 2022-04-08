package bicpam.pminer.fim;

import domain.Biclusters;
import bicpam.pminer.PM;
import bicpam.pminer.PM.ItemsetType;
import bicpam.pminer.fim.algorithms.BicSimpleAprioriTID;
import bicpam.pminer.fim.algorithms.BicSimpleEclat;
import bicpam.pminer.fim.algorithms.BicSimpleF2G;
import bicpam.pminer.fim.algorithms.BicSimpleF2GConstraint;
import bicpam.pminer.fim.algorithms.BicSimpleFPGrowth;
import bicpam.pminer.fim.algorithms.BicSimpleSFPGrowthTID;
import bicpam.pminer.fim.algorithms.BicSimpleWFPGrowthTID;

/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class SimpleFIM extends PM {

	public enum SimpleImplementation { F2GConstraint, F2G, Apriori, Vertical, SimpleFPGrowth };
	public SimpleImplementation algorithm = SimpleImplementation.Apriori;
	
	public Biclusters findFrequentItemSets() throws Exception {
		switch(algorithm){
			case F2G: PMiner = new BicSimpleF2G(dataset); break;
			case F2GConstraint : 
				PMiner = new BicSimpleF2GConstraint(dataset);
				((BicSimpleF2GConstraint)PMiner).addConstraints(constraints);
				break;
			case Apriori: PMiner = new BicSimpleAprioriTID(dataset); break;
			case Vertical: PMiner = new BicSimpleEclat(dataset); break;
			case SimpleFPGrowth: PMiner = new BicSimpleFPGrowth(dataset); break;
		}
		return super.run();
	}
		
	public ItemsetType getItemsetType(){
		return ItemsetType.Simple;
	}
		
	public void setImplementation(SimpleImplementation criteria){ algorithm = criteria; }
	public SimpleImplementation getImplementatioh(){ return algorithm; }

	public SimpleFIM(){ super(); }	

	public long getMemory() { return PMiner.getMemory(); }
}
