package bicpam.pminer.fim;

import domain.Biclusters;
import bicpam.pminer.PM;
import bicpam.pminer.PM.ItemsetType;
import bicpam.pminer.fim.algorithms.BicMaximalCharm;
import bicpam.pminer.fim.algorithms.BicMaximalFPGrowthTID;

/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class MaximalFIM extends PM {

	public enum MaximalImplementation { FPGrowth, CharmMFI };
	public MaximalImplementation algorithm = MaximalImplementation.CharmMFI;
	
	public Biclusters findFrequentItemSets() throws Exception {
		switch(algorithm){
			//case FPGrowth : PMiner = new BicMaximalFPGrowthTID(dataInstances); break; 
			case CharmMFI : PMiner = new BicMaximalCharm(dataset); break;
			default: PMiner = new BicMaximalCharm(dataset); break;
		}
		return super.run();
	}
		
	public ItemsetType getItemsetType(){
		return ItemsetType.Maximal;
	}
		
	public void setImplementation(MaximalImplementation criteria){ algorithm = criteria; }
	public MaximalImplementation getImplementatioh(){ return algorithm; }

	public MaximalFIM(){ super(); }	
}
