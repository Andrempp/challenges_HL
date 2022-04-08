package domain.constraint;

import java.util.List;

/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class AnnotationConstraint extends Constraint {
	
	/** inclusion of at least on annotation */
	public int nrColumns; 
	
	public AnnotationConstraint(int _nrColumns){
		nrColumns = _nrColumns;
	}
	public boolean satisfy(List<Integer> row, int nrLabels){
		for(int i=row.size()-1; i>=0; i--) 
			if(row.get(i) >= nrLabels*nrColumns) return true;
		return false; 
	}
}
