package domain.constraint;

import java.util.ArrayList;
import java.util.List;

/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class SequentialConstraint extends Constraint {
	
	public List<List<Integer>> pattern;
	public boolean exclude;
	
	public SequentialConstraint(List<List<Integer>> _pattern, boolean _exclude){
		pattern = _pattern; 
		exclude = _exclude;
	}
}
