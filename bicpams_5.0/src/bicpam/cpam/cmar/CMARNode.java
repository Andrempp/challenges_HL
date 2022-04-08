package cpam.cmar;

public class CMARNode {

	public short[] antecedent;
	public short[] consequent;
	public double confidenceForRule=0.0;
	public double supportForRule=0.0;
	public double suppAntecedent=0.0;
	public double suppConsequent=0.0;
	public int numRecords;

	public double chiSquaredValue=-1;
	public double chiSquaredUpperBound=-1;

	public CMARNode(short[] ante, short[]cons, double suppValue,
      	      double suppAnte, double suppCons, double confValue, int nRecs) {
    	antecedent        = ante;
    	consequent        = cons;
    	supportForRule    = suppValue;
    	suppAntecedent    = suppAnte;
    	suppConsequent    = suppCons;
    	confidenceForRule = confValue;
    	numRecords = nRecs;
   	}
	
    public boolean testRuleUsingChiSquaredTesting(double threshold) {
		if(chiSquaredValue == -1) chiSquaredValue = getChiSquaredValue(); // Calculate Chi squared value
		//System.out.println("X2:"+chiSquaredValue);
		if(chiSquaredValue>threshold) return(true); // Test Chi Squared value.
		else return(false);
	}

    /** Calculates and returns the Chi-Squared value for a rule. */
	public double getChiSquaredValue() {
		
		if(chiSquaredValue != -1) return chiSquaredValue; 
		double[] obsValues = new double[4];
    	double[] expValues = new double[4];
		
	    /** Calculates observed values for Chi squared testing calculation. */
        obsValues[0]=supportForRule;
        obsValues[1]=suppAntecedent-supportForRule;
        obsValues[2]=suppConsequent-supportForRule;
        obsValues[3]=numRecords-suppAntecedent-suppConsequent+supportForRule;
        double supNotAntecedent=numRecords-suppAntecedent;
    	double supNotConsequent=numRecords-suppConsequent;

	    /** Calculates expected values for Chi squared testing calculation. */
        expValues[0]=(suppConsequent*suppAntecedent)/numRecords;
		expValues[1]=(supNotConsequent*suppAntecedent)/numRecords;
		expValues[2]=(suppConsequent*supNotAntecedent)/numRecords;
		expValues[3]=(supNotConsequent*supNotAntecedent)/numRecords;

		/** Calculates the Chi squared values and returns their sum. */ 
        double sumChiSquaredValues = 0.0;
        for (int index=0;index<obsValues.length;index++)
        	sumChiSquaredValues += Math.pow((obsValues[index]-expValues[index]),2.0)/expValues[index];

        chiSquaredValue = sumChiSquaredValues;
        return(sumChiSquaredValues);
	}

    /** Calculates the upper bound for the Chi-Squared value of a rule. */
    public double calcChiSquaredUpperBound() {
    	if(chiSquaredUpperBound != -1) return chiSquaredUpperBound; 
		if(suppAntecedent<suppConsequent) chiSquaredUpperBound = Math.pow(suppAntecedent-((suppAntecedent*suppConsequent)/numRecords),2.0);
		else chiSquaredUpperBound = Math.pow(suppConsequent-((suppAntecedent*suppConsequent)/numRecords),2.0);
		chiSquaredUpperBound = chiSquaredUpperBound*calcWCSeValue()*numRecords; 
		return chiSquaredUpperBound;
	}
    
    /** Calculates and returns the e value for calculating Weighted Chi-Squared (WCS) values. */
    private double calcWCSeValue() {
        double term1 = 1/(suppAntecedent*suppConsequent);
        double term2 = 1/(suppAntecedent*(numRecords-suppConsequent));
        double term3 = 1/(suppConsequent*(numRecords-suppAntecedent));
        double term4 = 1/((numRecords-suppAntecedent)*(numRecords-suppConsequent));
        return (term1+term2+term3+term4); 
	}



	/*public int minPeriod;
	public int minOffset;
	public double weight;

	public RuleNodeCMAR(short[] ante, short[]cons, double suppValue,
      	      double suppAnte, double suppCons, double confValue, int minperiod, int minoffset, double w) {
		this(ante, cons, suppValue, suppAnte, suppCons, confValue);
    	minPeriod = minperiod;
    	minOffset = minoffset;
		weight = w;
	}*/
		
	public String toString(){
		String result = "";
		for(int i=0, l=antecedent.length; i<l; i++) result += antecedent[i]+",";
		result += "|" + consequent[0] + "(sA:" + suppAntecedent + ",sR:" + supportForRule + ",c:" + (int)confidenceForRule+")"; 
			// + minPeriod + "," + minOffset +"]";
		return result;
	}
}
