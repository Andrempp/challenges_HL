package tests.cpam;

import java.util.List;
import java.util.Map;

import performance.classification.domain.Results;
import performance.classification.filter.SignificancePairs;


/** @author Rui Henriques
 *  @contact rmch@ist.utl.pt
 *  @version 1.0
 */
public class CpamComparisonTest {

  public static void main(String[] args) throws Exception {
	List<Map<String,Results>> results = CpamSyntheticTest.getClassifiersPerformance();
	calculateSignificances(results);
  }
  
  public static void calculateSignificances(List<Map<String,Results>> results) {
	SignificancePairs significance = new SignificancePairs();
	for(Map<String,Results> iresult : results){
		for(Results result : iresult.values()) {
			significance.classifiers.add(result.id);
			significance.metricsNames.put(result.id, result.names);
			significance.metricsValues.put(result.id, result.values);
		}
	}
	significance.calculateAllSignificances();
	System.out.println(significance.toString());
  }
}

