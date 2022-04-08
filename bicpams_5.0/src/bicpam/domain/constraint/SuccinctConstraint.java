package domain.constraint;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class SuccinctConstraint extends Constraint {
	
	public List<Integer> values; 
	public SuccinctOperation operator;
	public ValueType valueType;
	
	public SuccinctConstraint(SuccinctOperation _operator, List<Integer> _values, ValueType _valueType){
		values = _values; 
		operator = _operator;
		valueType = _valueType;
	}
	public List<Integer> satisfy(List<Integer> row, int nrLabels){
		List<Integer> newvalues = new ArrayList<Integer>();
		switch(valueType){
			case Values :
				for(Integer val : row) newvalues.add(val%nrLabels);
				break;
			case Columns :
				for(Integer val : row) newvalues.add(val/nrLabels);
				break;
			default : newvalues = row;
		}
		switch(operator){
			case Includes : 
				for(Integer val : values) 
					if(!newvalues.contains(val)) return new ArrayList<Integer>();  
				return row;
			case Excludes :
				for(Integer val : values) row.remove(val);  
				return row;
		}
		return null;
	}
	public List<Integer> remaining(List<Integer> row, int nrLabels) {
		List<Integer> result = new ArrayList<Integer>();
		List<Integer> newvalues = new ArrayList<Integer>();
		switch(valueType){
			case Values :
				for(Integer val : row) newvalues.add(val%nrLabels);
				break;
			case Columns :
				for(Integer val : row) newvalues.add(val/nrLabels);
				break;
			default : newvalues = row;
		}
		for(Integer val : values) 
			if(!newvalues.contains(val)) result.add(val);  
		return result;
	}
	public boolean contains(List<Integer> row, List<Integer> remainingValues, int nrLabels) {
		List<Integer> newvalues = new ArrayList<Integer>();
		switch(valueType){
			case Values :
				for(Integer val : row) newvalues.add(val%nrLabels);
				break;
			case Columns :
				for(Integer val : row) newvalues.add(val/nrLabels);
				break;
			default : newvalues = row;
		}
		for(Integer val : values) 
			if(!newvalues.contains(val)) return false;  		
		return true;
	}
}
