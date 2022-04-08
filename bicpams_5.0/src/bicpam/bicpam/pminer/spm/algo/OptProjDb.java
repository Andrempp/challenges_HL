package bicpam.pminer.spm.algo;

import java.util.Set;

public class OptProjDb {
	
	public int[] indexes;
	public Set<Integer> seqsID;
	
	public OptProjDb(int[] _indexes, Set<Integer> _seqsID){
		indexes = _indexes.clone();
		seqsID = _seqsID;
	}
}
