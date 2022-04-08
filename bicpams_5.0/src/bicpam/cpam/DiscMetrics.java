package cpam;

import domain.Bicluster;
import domain.Dataset;
import utils.BicMath;

public final class DiscMetrics {

    public static final double THRESHOLD_10    = 2.7055;
    public static final double THRESHOLD_5     = 3.8415;
    public static final double THRESHOLD_2HALF = 5.0239;
    public static final double THRESHOLD_1     = 6.6349;
    public static final double THRESHOLD_HALF  = 7.8794;

	public static double getFoil(Bicluster bic){
		return Math.log(((double)bic.rows.size())/BicMath.sum(bic.support));
	}
	private static double getConfidence(Bicluster bic) {
		return ((double)bic.rows.size())/BicMath.sum(bic.support);
	}
	private static Double getWeightedConfidence(Bicluster bic) {
		return bic.weightedSupport[bic.condition]/BicMath.sum(bic.weightedSupport);
	}
	private static double getFisher(){
		return 0;
	}
	public static double[] getLowInterClassOverlap(Bicluster bic, int[] classes){
		double[] result = new double[bic.support.length];
		for(int i=0, l=bic.support.length; i<l; i++) 
			result[i]=((double)bic.rows.size())/(double)classes[i];
		return result;
	}
	public static double getChi2(Bicluster bic, Dataset data){
		double[] obsValues = new double[4];
    	double[] expValues = new double[4];
		
	    /** Calculates observed values for Chi squared testing calculation. */
    	double numRows = data.rows.size();
    	double suppConsequent = data.classValues[bic.condition];
    	double supportForRule = bic.numRows();
    	double suppAntecedent = BicMath.sum(bic.support);
        double supNotAntecedent=numRows-suppAntecedent;
    	double supNotConsequent=numRows-suppConsequent;

    	obsValues[0]=supportForRule;
        obsValues[1]=suppAntecedent-supportForRule;
        obsValues[2]=suppConsequent-supportForRule;
        obsValues[3]=numRows-suppAntecedent-suppConsequent+supportForRule;
        
        expValues[0]=(suppConsequent*suppAntecedent)/numRows;
		expValues[1]=(supNotConsequent*suppAntecedent)/numRows;
		expValues[2]=(suppConsequent*supNotAntecedent)/numRows;
		expValues[3]=(supNotConsequent*supNotAntecedent)/numRows;

		/** Calculates the Chi squared values and returns their sum. */ 
        double sumChiSquaredValues = 0.0;
        for (int index=0;index<obsValues.length;index++)
        	sumChiSquaredValues += Math.pow((obsValues[index]-expValues[index]),2.0)/expValues[index];

        return sumChiSquaredValues;
	}
    public boolean testChi2(double threshold, Bicluster bic, Dataset data) {
    	return getChi2(bic,data) < threshold;
	}
	public static double getChi2UpperBound(Bicluster bic, Dataset data) {
    	double result, numRows = data.rows.size();
    	double suppAntecedent = BicMath.sum(bic.support);
    	double suppConsequent = data.classValues[bic.condition];
		if (suppAntecedent<suppConsequent) result = Math.pow(suppAntecedent-((suppAntecedent*suppConsequent)/numRows),2.0);
		else result = Math.pow(suppConsequent-((suppAntecedent*suppConsequent)/numRows),2.0);
        double term1 = 1/(suppAntecedent*suppConsequent);
        double term2 = 1/(suppAntecedent*(numRows-suppConsequent));
        double term3 = 1/(suppConsequent*(numRows-suppAntecedent));
        double term4 = 1/((numRows-suppAntecedent)*(numRows-suppConsequent));
        double wcs = term1+term2+term3+term4; 
        return result*wcs*numRows; 
	}
}
