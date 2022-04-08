package cpam;

import java.util.List;
import java.util.Map;
import cpam.BClassifier.DiscMetric;
import cpam.BClassifier.LearningFunction;
import cpam.structure.CMAR;
import cpam.structure.TreeLearner;
import cpam.structure.WeightedRules;
import domain.Biclusters;
import domain.Biclusters.Order;
import domain.Dataset;
import generator.BicMatrixGenerator.PatternType;

public abstract class Learner {
	
	public static Learner learn(Biclusters[] bics, Dataset data, LearningFunction function, int maxBics){
		switch(function){
			case WeightedRules : return new WeightedRules(bics,data,maxBics);
			//case TreeStructure : return new TreeLearner(bics,data,discMetric);
			//case CMAR : return new CMAR(bics,data,discMetric);
			default : return null;
		}
	}
	
	public abstract double[] test(Tester tester, double[] instance);
	public abstract String rulesToString();
	public abstract String rulesToString(List<String> rows, List<String> columns);

}
