package domain.constraint;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import utils.BicMath;

/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class MonotoneConstraint extends Constraint {
	
	public double value; 
	public Aggregator aggregator;
	public MonotoneOperation operator;
	
	public MonotoneConstraint(Aggregator _aggregator, MonotoneOperation _operator, double _value){
		value=_value; 
		aggregator=_aggregator;
		operator=_operator;
	}
	public boolean satisfy(List<Integer> row, List<Integer> prefix, int nrLabels){
		//System.out.println("Satisfy:"+aggregator+row+"|"+prefix+operator+value);
		List<Integer> result = new ArrayList<Integer>();
		switch(aggregator){
			case Length : 
				if(operator.equals(MonotoneOperation.Greater)){ 
					if(prefix.size()+row.size()>value) return true;
				} else if(prefix.size()+row.size()>=value) return true; 
				return false;
			case Count : 
				Set<Integer> newvalues = new HashSet<Integer>();
				for(Integer val : prefix) newvalues.add(val%nrLabels);
				for(Integer val : row) newvalues.add(val%nrLabels);
				if(operator.equals(MonotoneOperation.Greater)){ 
					if(newvalues.size()>value) return true;
				} else if(newvalues.size()>=value) return true; 
				return false;
			case Sum : 
				for(Integer val : prefix) result.add(val%nrLabels);
				for(Integer val : row) result.add(val%nrLabels);
				int total = (int)BicMath.sum(result);
				if(operator.equals(MonotoneOperation.Greater)){ 
					if(total>value) return true;
				} else if(total>=value) return true; 
				return false;
			case Average : 
			case Range : 
				int min=Math.min(BicMath.min(row),BicMath.min(prefix));
				int max=Math.max(BicMath.max(row),BicMath.max(prefix));
				if(operator.equals(MonotoneOperation.Greater)){ 
					if((max-min)>value) return true;
				} else if((max-min)>=value) return true; 
				return false;
		}
		return true;
	}
}
