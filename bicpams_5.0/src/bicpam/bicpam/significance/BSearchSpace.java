package bicpam.significance;

import utils.BicMath;
import generator.BicMatrixGenerator.PatternType;

public class BSearchSpace {

	public enum SpaceType { ExactSize , ExactPattern , ExactColumns };

	public static int compute(SpaceType space, int bicRows, int bicColumns, int nrRows, int nrColumns, int nrLabels, PatternType type){
		int exactpattern = (int) BicMath.combination(nrColumns, bicColumns);
		int exactcolumns = -1;
		switch(type){
			case Additive :
				int sumadd=0;
				for(int i=2;i<=nrLabels;i++) sumadd+=Math.pow(i,bicColumns-2);
				exactcolumns = 1+bicColumns*(bicColumns-1)*sumadd;
				System.out.println(space.toString()+"A:"+exactcolumns);
				break;
			case Constant : 
				exactcolumns = (int) Math.pow(nrLabels,bicColumns);
				//System.out.println(space.toString()+"C:"+exactcolumns);
				break;
			case Multiplicative : 
				int summult=0;
				for(int i=2;i<=nrLabels;i++) summult+=Math.pow(i,bicColumns-2)*(nrLabels-(i-1));
				exactcolumns = 1+bicColumns*(bicColumns-1)*summult;
				System.out.println(space.toString()+"M:"+exactcolumns);
				break;
			default : break;
		}
		//System.out.println(space.toString());
		switch(space){
			case ExactSize : return exactpattern*exactcolumns;
			case ExactPattern :  return exactpattern;
			case ExactColumns :  return exactcolumns;
			default : return -1;
		}
	}

	public static int compute(SpaceType space, int bicRows, int bicColumns, int nrRows, int nrColumns, double range, PatternType type) {
		return compute(space, bicRows, bicColumns, nrRows, nrColumns, (int) range, type);	
	}
}
