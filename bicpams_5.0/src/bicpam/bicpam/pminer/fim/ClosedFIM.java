package bicpam.pminer.fim;

import domain.Biclusters;
import bicpam.pminer.PM;
import bicpam.pminer.PM.ItemsetType;
import bicpam.pminer.fim.algorithms.BicClosedAprioriTID;
import bicpam.pminer.fim.algorithms.BicClosedCharm;
import bicpam.pminer.fim.algorithms.BicClosedDCharm;

/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class ClosedFIM extends PM {

	public enum ClosedImplementation { AprioriTID, Charm, DCharm };
	public ClosedImplementation algorithm = ClosedImplementation.Charm;
	
	public Biclusters findFrequentItemSets() throws Exception {
		switch(algorithm){
			case AprioriTID : PMiner = new BicClosedAprioriTID(dataset); break; //not working properly
			case Charm : PMiner = new BicClosedCharm(dataset); break;
			case DCharm : PMiner = new BicClosedDCharm(dataset); break; //not working properly
		}
		return super.run();
	}
	
	public ItemsetType getItemsetType(){
		return ItemsetType.Closed;
	}
		
	public void setImplementation(ClosedImplementation criteria){ algorithm = criteria; }
	public ClosedImplementation getImplementatioh(){ return algorithm; }

	public ClosedFIM(){ super(); }
}
