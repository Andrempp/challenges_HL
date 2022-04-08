package bicpam.pminer.spm;

import java.util.List;
import domain.Biclusters;
import bicpam.pminer.BicPM;
import bicpam.pminer.BicPMUtils;
import bicpam.pminer.spm.algo.AlgoIndexSpan;
import bicpam.pminer.spm.algo.Sequences;

/**
 * @author Rui Henriques
 * @version 1.0
 */
public class BicIndexSpan extends AlgoIndexSpan implements BicPM {

	public BicIndexSpan(List<List<Integer>> dataset, int overlap){
		super();
		super.setDataset(dataset,overlap);
	}
    public void setData(List<List<Integer>> dataI) {
    	super.setDataset(dataI,-1);
	}
	public void reset() {
		super.reset();
	}

	public Biclusters run(int minColumns, int minRows, int maxColumns, int maxRows, int nrLabels) throws Exception {
	  Sequences seqs = super.runAlgorithm(minRows,minColumns);
	  System.out.println("sup:"+minRows+" |#"+seqs.sequenceCount);
	  return BicPMUtils.toValidItemsets(seqs, minColumns, maxColumns, maxRows, nrLabels);
	}

	public Biclusters run(double support, int nrLabels) throws Exception {
	  Sequences seqs = super.runAlgorithm((int) (support*database.size()),0);
	  return BicPMUtils.toBicItemsets(seqs, nrLabels);
	}

	public long getMemory() { return -1; }
}
