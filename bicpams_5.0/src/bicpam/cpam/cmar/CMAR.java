package cpam.cmar;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CMAR {
	
    // Chi-Squared thresholds assuming 1-degree of freedom
    protected static final double THRESHOLD_10    = 2.7055;
    protected static final double THRESHOLD_5     = 3.8415;
    protected static final double THRESHOLD_2HALF = 5.0239;
    protected static final double THRESHOLD_1     = 6.6349;
    protected static final double THRESHOLD_HALF  = 7.8794;
    
    protected static int MIN_COVER=3; // At least 3 rules

	int numRecords;
	int numClasses;
	List<CMARNode> rules;
	short[][] trainset;
	
	public CMAR(int nRecs, int nClasses){
		rules = new ArrayList<CMARNode>();
		numRecords = nRecs;
		numClasses = nClasses;
	}

    /** Inserts rules in a list of rules ordered according to CMAR ranking 
	    @param antecedent the antecedent (LHS) of the rule.
	    @param consequent the consequent (RHS) of the rule.
	    @param supportForAntecedent the associated support for the antecedent.
	    @param supportForConsequent the associated support for the consequent.
	    @param supportForRule the associated support value. 
	    @param confidenceForRule the associated confidence value. */    
	public void insertRinRlistCMARranking(short[] ant, short[] cons, double supAnt, double supConseq, double supRule, double conf) {	

		CMARNode newRule = new CMARNode(ant,cons,supRule,supAnt,supConseq,conf,numRecords);
		//System.out.println(newRule.toString());
        if(!newRule.testRuleUsingChiSquaredTesting(THRESHOLD_10)) return;
		
		if(!rules.isEmpty()) {
			if(moreGeneralRuleExists(newRule)) return; // rule with higher ranking
	        	//System.out.println("THERE IS MORE GENERAL");
			boolean cmarGreater = false;
			for(int i=0, s=rules.size(); i<s; i++){
				if(ruleIsCMARgreater(newRule, rules.get(i))){
					rules.add(i, newRule);
					cmarGreater = true;
					break;
				}
			}
			if(!cmarGreater) rules.add(newRule); //System.out.println("NOT GREATER!!");
		}
		else rules.add(newRule);
	}

    protected double[] classifyRecordWCS(short[] instance) {
    	double[] classesprob = new double[numClasses];
    	for(int i=0; i<numClasses; i++) classesprob[i]=0;
    	
	    List<CMARNode> recordRules = obtainAllRulesForRecord(instance);
		if (recordRules.isEmpty()) classesprob[new Random().nextInt(numClasses)]=1;
		else classesprob = calcWCSvalues(recordRules);
		
		//if(recordRules.size()==1) classesprob[recordRules.get(0).consequent[0]]=1;
		//System.out.println("ONE:"+recordRules.get(0).consequent[0]);
		//else if (onlyOneClass()) return recordRules.get(0).consequent[0];
			/*int bestIndex = 0;
	        for (int i=0; i<numClasses; i++)
			    if (wcsValues[i]>wcsValues[bestIndex]) bestIndex=i;
	        classesprob[bestIndex]=1;*/
		//for(int j=0, s=classesprob.length; j<s; j++) System.out.print(classesprob[j]+";");
		//System.out.println();
		return classesprob;
	} 

	
	/*************************
	 ***** CORE METHODS ******
	 *************************/
	
    /** Tests whether a more general rule, with higher ranking, already exists in the rule list.
    @param rule the rule under consideration.
    @return true if more general rule with higher ranking exists, and false  otherwise. */
    protected boolean moreGeneralRuleExists(CMARNode rule) {
		for(CMARNode irule : rules){
			if(irule.consequent[0]==rule.consequent[0] 
					&& ruleIsMoreGeneral(rule,irule) 
					&& ruleIsCMARgreater2(rule,irule)) return true;
			//System.out.println("G:"+ruleIsMoreGeneral(rule,irule)+" C:"+ruleIsCMARgreater2(rule,irule)+"\n"+irule.toString());
		}
		return false;
	}

    /** Returns if r1 is more general than r2: fewer antecedent attributes */
    private boolean ruleIsMoreGeneral(CMARNode rule1, CMARNode rule2) {
        if (rule1.antecedent.length < rule2.antecedent.length) return(true);
        return(false);
	} 

    /** Compares two rules and returns true if the first is "CMAR greater" (has a higher ranking) than the second */
    protected boolean ruleIsCMARgreater(CMARNode rule1, CMARNode rule2) {
    	if (rule1.confidenceForRule > rule2.confidenceForRule) return(true);
        if ((int)(rule1.confidenceForRule*100.0)==(int)(rule2.confidenceForRule*100.0)) { //same confidence
        	if (rule1.supportForRule > rule2.supportForRule) return(true);
        	if ((int)(rule1.supportForRule*100.0)==(int)(rule2.supportForRule*100.0)) { //same support
        		if (rule1.antecedent.length > rule2.antecedent.length) return(true);
	       }
        }
        return(false);
	} 
    private boolean ruleIsCMARgreater2(CMARNode rule1, CMARNode rule2) {
    	if (rule1.confidenceForRule > rule2.confidenceForRule) return(true);
        if ((int)(rule1.confidenceForRule*100.0)==(int)(rule2.confidenceForRule*100.0)) 
        	if (rule1.supportForRule > rule2.supportForRule) return(true);
        return(false);
	}

	protected boolean onlyOneClass(List<CMARNode> recordRules) {
    	int classval = recordRules.remove(0).consequent[0];
    	for(CMARNode rule : recordRules) 
            if (rule.consequent[0]!=classval) return(false);
        return(true);
	}
    protected double[] calcWCSvalues(List<CMARNode> recordRules) {
    	double[] wcsArray = new double[numClasses];
    	for(int i=0; i<numClasses; i++) wcsArray[i]=0;
		for(CMARNode rule : recordRules){
			short index = rule.consequent[0];
			double chiSquaredValue = rule.getChiSquaredValue();//rule.suppAntecedent, rule.suppConsequent,rule.supportForRule);
			double chiSquaredUB = rule.calcChiSquaredUpperBound();
			wcsArray[index] = wcsArray[index] + (chiSquaredValue*chiSquaredValue)/chiSquaredUB;
	    }
		return(wcsArray);
	}

    /** Places all rules that satisfy the given record in a CMAR rule list (preserve order)
    	@param itemset the record to be classified.	*/
    protected List<CMARNode> obtainAllRulesForRecord(short[] instance) {
		List<CMARNode> newRules = new ArrayList<CMARNode>();
		for(CMARNode rule : rules) 
		    if(isSubset(rule.antecedent,instance)) newRules.add(rule);
		return newRules;
	}
    
    /** Checks whether one item set is subset of a second item set */
    protected boolean isSubset(short[] tpattern, short[] instance) {
		if (tpattern==null) return(true);
		if (instance==null) return(false);

		for(int i=0, j=0, l1=tpattern.length, l2=instance.length; i<l1; i++){
			int val = tpattern[i];
			for(; j<l2; j++) if(instance[j]==val) break;
	    	if(j>=l2) return(false);
		}
		return(true);
	}
    
    /** Prunes the current CMAR list of rules according to the "cover" principle */    
    public void pruneUsingCover() {
    	if(trainset == null) return;
		int[] cover = new int[trainset.length]; // Initialise cover array
		for(CMARNode rule : rules) {
		    boolean coverFlag=false;
		    for(int i=0, l=trainset.length; i<l; i++) {
				if (isSubset(rule.antecedent,trainset[i])) { // record satisfies: increment cover element for record
				   cover[i]++;
				   coverFlag=true; 
				}
			}
		    //rule is required by at least one record add to new rule list
		    if (!coverFlag) rules.remove(rule);
		    for (int i=0, l=cover.length; i<l; i++) // Remove records from training set if adequately covered
		        if (cover[i]>MIN_COVER) trainset[i]=null;
		}
	}
    
    public void addTrainSet(short[][] train){
    	trainset = train;
    }
    
    public String toString(){
    	StringBuffer res = new StringBuffer("#Recs:" + numRecords + " #Classes:" + numClasses + " #Rules:"+rules.size() + "\nRULES:\n");
    	for(CMARNode rule : rules) res.append(rule.toString()+"\n");
    	return res.toString();
    }
	
}
