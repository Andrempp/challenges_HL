package domain.constraint;

import java.util.ArrayList;
import java.util.List;

/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class Constraints {
	
	public int nrLabels;
	public List<Constraint> constraints;
	public boolean ordering;
	
	public Constraints(int _nrLabels, boolean _ordering) {
		constraints = new ArrayList<Constraint>();
		nrLabels = _nrLabels;
		ordering = _ordering;
	}
	public void add(Constraint constraint){
		constraints.add(constraint);
	}
}
