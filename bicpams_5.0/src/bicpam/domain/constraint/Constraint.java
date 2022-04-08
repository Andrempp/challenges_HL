package domain.constraint;

/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class Constraint {
	
	public enum SuccinctOperation { Includes, Excludes }
	public enum ValueType { Values, Rows, Columns, Concat }
	public enum AntiMonotoneOperation { LessEq, Less, Equal }
	public enum MonotoneOperation { GreaterEq, Greater, Equal }
	public enum Aggregator { Sum, Average, Count, Range, Length }

}
