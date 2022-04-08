package bicpam.pminer.spm;

import java.util.List;

import bicpam.pminer.BicPM;
import bicpam.pminer.BicPMUtils;
import bicpam.pminer.spm.algo.AlgoPrefixSpan;
import bicpam.pminer.spm.algo.Sequences;
import domain.Biclusters;

/**
 * @author Rui Henriques
 * @version 1.0
 */
public class BicPrefixSpan extends AlgoPrefixSpan implements BicPM {

	public BicPrefixSpan(List<List<Integer>> dataset, int overlap){
		super();
		super.setDataset(dataset,overlap);
	}
    public void setData(List<List<Integer>> dataI) {
    	super.setData(dataI);
	}
	public void reset() {
		super.reset();
	}

	public Biclusters run(int minColumns, int minRows, int maxColumns, int maxRows, int nrLabels) throws Exception {
	  System.out.println("minCols:"+minColumns);
	  Sequences seqs = super.runAlgorithm((double)minRows/(double)database.size());
	  System.out.println("sup:"+minRows+" |#"+seqs.sequenceCount);//+" >"+seqs.getLevelCount());
	  //System.out.println(seqs.getLevels());
	  return BicPMUtils.toValidItemsets(seqs, minColumns, maxColumns, maxRows, nrLabels);
	}

	public Biclusters run(double support, int nrLabels) throws Exception {
	  Sequences seqs = super.runAlgorithm(support);
	  return BicPMUtils.toBicItemsets(seqs, nrLabels);
	}

	public long getMemory() { return -1; }
}
