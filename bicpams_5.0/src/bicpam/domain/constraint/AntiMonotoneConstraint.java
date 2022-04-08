package domain.constraint;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import utils.BicMath;

/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class AntiMonotoneConstraint extends Constraint {
	
	public double value; 
	public Aggregator aggregator;
	public AntiMonotoneOperation operator;
	
	public AntiMonotoneConstraint(Aggregator _aggregator, AntiMonotoneOperation _operator, double _value){
		value=_value; 
		aggregator=_aggregator;
		operator=_operator;
	}
	public List<Integer> satisfy(List<Integer> row, List<Integer> prefix, int nrLabels, Map<Integer, Integer> map){
		List<Integer> removals = new ArrayList<Integer>();
		switch(aggregator){
			case Length : 
				if(prefix.size()<value) return row;
				else return new ArrayList<Integer>();
			case Count : 
				Set<Integer> newvalues = new HashSet<Integer>();
				for(Integer val : prefix) newvalues.add(val%nrLabels);
				if(newvalues.size()<value) return row;
				else return new ArrayList<Integer>();
			case Sum : 
				List<Integer> prefix1 = new ArrayList<Integer>();
				for(Integer val : prefix) prefix1.add(val%nrLabels);
				int minUpTo = (int)(value-(int)BicMath.sum(prefix1));
				if(operator.equals(AntiMonotoneOperation.Less)) minUpTo--;
				//if(minUpTo<0) return new ArrayList<Integer>();
				for(int i=row.size()-1; i>=0; i--){
					int val = row.get(i);
					if((val%nrLabels)>minUpTo){
						removals.add(i);
						map.put(val, map.get(val)-1);
					}
				}
				//System.out.println(prefix+"|"+row+" Minupto:"+minUpTo+" => "+removals);//+"\nMAP:"+map
				return removals;
			case Average : 
			case Range : 
				int min=BicMath.min(prefix), max=BicMath.max(prefix);
				int delta = (int)value-(max-min);
				if(operator.equals(AntiMonotoneOperation.Less)) delta--;
				if(delta<0) return new ArrayList<Integer>();
				for(Integer val : row) 
					if(val<min){ if(min-val<delta) removals.add(val); }
					else if(val>max) { if(val-max<delta) removals.add(val); }
					else removals.add(val);
				return removals;
		}
		return null;
	}
}
