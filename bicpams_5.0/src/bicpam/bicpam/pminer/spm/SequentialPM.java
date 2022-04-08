package bicpam.pminer.spm;

import bicpam.pminer.PM;
import domain.Biclusters;

/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class SequentialPM extends PM {

	public enum SequentialImplementation { PrefixSpan, IndexSpan, BIDEPlus, IndexSpanConstraint };
	public SequentialImplementation algorithm = SequentialImplementation.IndexSpan;//PrefixSpan;
	private int overlap=1;
	
	public Biclusters findFrequentItemSets() throws Exception {
		switch(algorithm){
			case IndexSpanConstraint :
				PMiner = new BicIndexSpanConstraint(dataset,overlap); 
				((BicIndexSpanConstraint)PMiner).addConstraints(constraints);
				break; 
			case PrefixSpan : PMiner = new BicPrefixSpan(dataset,overlap); break; 
			case IndexSpan : PMiner = new BicIndexSpan(dataset,overlap); break; 
			case BIDEPlus : PMiner = new BicClosedBIDEPlus(dataset,overlap); break; 
		}
		return super.run();
	}

	public ItemsetType getItemsetType(){
		return ItemsetType.Sequential;
	}
		
	public void setImplementation(SequentialImplementation criteria){ algorithm = criteria; }
	public SequentialImplementation getImplementatioh(){ return algorithm; }

	public void setOverlap(int _overlap){ overlap = _overlap; }
	public int getOverlap(){ return overlap; }
	public SequentialPM(){ super(); }

}
