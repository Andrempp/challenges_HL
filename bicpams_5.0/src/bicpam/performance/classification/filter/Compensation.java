package performance.classification.filter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Vector;

import utils.BicMath;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.supervised.instance.Resample;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;

/**
 * @author Rui Henriques
 * @version Revision: 1.0
 */
public final class Compensation  {

  static final long serialVersionUID = -1653880819059250364L;
  
  private static boolean printing = true;
  private static boolean reducesample = true;
  private static boolean resample = false;
  private static boolean replaceMissings = false;
  private static boolean mysmote = false;

  public static Instances reduceSample(Instances input, double percentage, double bias){
	System.out.println(printDataBalanceBasics(input));
	input = createSubsample(input,0.7,50); //1st: bias to Uniform class, 2nd: sample size %
	System.out.println(printDataBalanceBasics(input));
	return input;
  }

  public static Instances balanceDataset(Instances input) throws Exception {
	  if(printing) System.out.println(printDataBalanceBasics(input));	  
	  if(reducesample){
		System.out.println(printDataBalanceBasics(input));
		//System.out.println(EvocPrinting.printIDs(input));
		input = createSubsample(input,0.8,30); //1st: bias to Uniform class, 2nd: sample size %
		System.out.println(printDataBalanceBasics(input));
		//Result.println(EvocPrinting.printInstances(input));
	  }
	  if(resample){
		Resample resample = new Resample();
		resample.setInputFormat(input);
		resample.setBiasToUniformClass(0);
		resample.setSampleSizePercent(90);
		input = new Instances(Filter.useFilter(input, resample));
	  }
	  if(replaceMissings){
		  ReplaceMissingValues filler = new ReplaceMissingValues();
		  filler.setInputFormat(input);
		  input = new Instances (Filter.useFilter(input, filler));
	  }
	  if(mysmote){
		  System.out.println(printDataBalanceBasics(input));
		  input = doMySMOTE(input,5);
		  System.out.println(printDataBalanceBasics(input));
		  //Result.println(EvocPrinting.printInstances(input));
	  }
  	  if(printing) System.out.println(printDataBalanceBasics(input));
	  return new Instances(input);
  }

  @SuppressWarnings("unchecked")
  private static Instances createSubsample(Instances input, double m_BiasToUniformClass, int m_SampleSizePercent) {
	Instances m_output = new Instances(input);//.stringFreeStructure();
	m_output.delete();
    int origSize = input.numInstances();
    int sampleSize = (int) (origSize * m_SampleSizePercent / 100);

    // Subsample that takes class distribution into consideration
    input.sort(input.classIndex()); // Sort according to class attribute.
    int[] classIndices = new int [input.numClasses()+1];
    int currentClass = 0;
    classIndices[currentClass] = 0;
    for (int i=0; i<input.numInstances(); i++) {
      Instance current = input.instance(i);
      if (current.classIsMissing()) {
  	    for(int j=currentClass+1; j<classIndices.length; j++) classIndices[j]=i;
  	    break;
      } else if (current.classValue() != currentClass) {
  	    for(int j=currentClass+1; j<=current.classValue(); j++) classIndices[j]=i;
  	    currentClass = (int) current.classValue();
      }
    }
    if (currentClass <= input.numClasses()) {
      for(int j = currentClass + 1; j < classIndices.length; j++)
        classIndices[j] = input.numInstances();
    }
    int actualClasses = 0;
    for (int i = 0; i < classIndices.length - 1; i++)
      if (classIndices[i] != classIndices[i + 1]) actualClasses++;

    // Convert pending input instances
    //System.out.print(origSize+":"+sampleSize+","+actualClasses+"=>[");
    //for (int i = 0; i < classIndices.length - 1; i++) System.out.print(classIndices[i]+",");
    //System.out.println("]");
	Random random = new Random(1);
    if (sampleSize > origSize) sampleSize = origSize;
    Vector<Integer>[] indices = new Vector[classIndices.length - 1];
    Vector<Integer>[] indicesNew = new Vector[classIndices.length - 1];
    for (int i = 0; i < classIndices.length - 1; i++) { // generate list of all indices to draw from
      indices[i] = new Vector<Integer>(classIndices[i + 1] - classIndices[i]);
      indicesNew[i] = new Vector<Integer>(indices[i].capacity());
      for (int n = classIndices[i]; n < classIndices[i + 1]; n++) indices[i].add(n);
    }
    int currentSize = origSize;
    for (int i = 0; i < sampleSize; i++) { // draw X samples
      int index = 0;
      if (random.nextDouble() < m_BiasToUniformClass) {
		int cIndex = random.nextInt(actualClasses); // Pick a random class (of those classes that actually appear)
		for (int j = 0, k = 0; j < classIndices.length - 1; j++) {
		  if ((classIndices[j] != classIndices[j + 1]) && (k++ >= cIndex)) {
		    if (indices[j].size() == 0) i--; // no more indices for this class left, try again
		    else {
			    index = random.nextInt(indices[j].size());// Pick a random instance of the designated class
			    indicesNew[j].add(indices[j].get(index));
			    indices[j].remove(index);
		    }
		    break;
		  }
		}
      } else {
		index = random.nextInt(currentSize);
		for (int n = 0; n < actualClasses; n++) {
		  if (index < indices[n].size()) {
		    indicesNew[n].add(indices[n].get(index));
		    indices[n].remove(index);
		    break;
		  }
		  else index -= indices[n].size();
		}
		currentSize--;
      }
    }
    for(int i=0; i<indicesNew.length; i++) Collections.sort(indicesNew[i]);

    for(int i = 0; i < indicesNew.length; i++) { // add to ouput
      for(int n = 0; n < indicesNew[i].size(); n++)
    	  m_output.add((Instance) input.instance(indicesNew[i].get(n)).copy());
    }
    for (int i = 0; i < indices.length; i++) { // clean up
      indices[i].clear();
      indicesNew[i].clear();
    }
    indices = null;
    indicesNew = null;
    return m_output;
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private static Instances doMySMOTE(Instances m_data, int m_nearestNeighbors) {
	// A: CALCULATE PERCENTAGES PER CLASS
	Instances m_output = m_data.stringFreeStructure();
	int[] classes = m_data.attributeStats(m_data.classIndex()).nominalCounts;
	Instances[] sample = new Instances[m_data.numClasses()];
	double[] m_percentage = new double[m_data.numClasses()];
	int maxN = BicMath.max(classes);
	for(int i=0,l=classes.length;i<l;i++){
		//if(classes[i]==max) continue;
		double add = maxN*2-classes[i];
		m_percentage[i]=(add/(double)classes[i])*100;
		//System.out.println("%:"+m_percentage[i]);
		sample[i] = m_data.stringFreeStructure();
	}
	//System.out.println("Class:"+minIndex+"  NN:"+m_nearestNeighbors);
			
	// B: Compose minority class dataset, also push all dataset instances
	for(int i=0,l=m_data.numInstances();i<l;i++)
		sample[(int)m_data.instance(i).classValue()].add(m_data.instance(i));

	// C: Compute Value Distance Metric matrices for nominal features
	Map vdmMap = new HashMap();
	for(int i=0,l=m_data.numAttributes();i<l;i++){
		if(i==m_data.classIndex()) continue;
		Attribute attr = m_data.attribute(i);
		if(!attr.isNominal() && !attr.isString()) continue;
			
		int[] featureValueCounts = new int[attr.numValues()];
		int[][] featureValueCountsByClass = new int[m_data.classAttribute().numValues()][attr.numValues()];
		for(int j=0,s=m_data.numInstances();j<s;j++){
			int value = (int) m_data.instance(j).value(i);
			featureValueCounts[value]++;
			featureValueCountsByClass[(int) m_data.instance(j).classValue()][value]++;
		}
		double[][] vdm = new double[attr.numValues()][attr.numValues()];
		vdmMap.put(attr, vdm);
		for( int v1 = 0; v1 < attr.numValues(); v1++ ) {
			for( int v2 = 0; v2 < attr.numValues(); v2++ ) {
				double sum = 0;
				for( int cv = 0; cv < m_data.numClasses(); cv++ ) {
					double c1i = (double) featureValueCountsByClass[cv][v1];
					double c2i = (double) featureValueCountsByClass[cv][v2];
					double c1 = (double) featureValueCounts[v1];
					double c2 = (double) featureValueCounts[v2];
					double term1 = c1i / c1;
					double term2 = c2i / c2;
					sum += Math.abs( term1 - term2 );
				}
				vdm[v1][v2] = sum;
			}
		}
	}

	// D: Compute nearest neighbors and generating SMOTE examples from each instance
	for(int k=0,l=classes.length;k<l;k++){
	  for(int i=0,l1=sample[k].numInstances(); i<l1; i++) {
		Instance instanceI = sample[k].instance(i);
		//System.out.println("INSTANCE"+i);
			
		// D1: find k nearest neighbors for each instance
		Vector<Instance> topKInstances = new Vector<Instance>();
		double dists[] = new double[m_nearestNeighbors];
		for(int j=0; j<dists.length; j++) dists[j]=Double.POSITIVE_INFINITY;
		for(int j=0, nrNN=0; j<l1; j++) {
			if(i==j) continue;
			Instance instanceJ = sample[k].instance(j);
			double distance = 0;
			for(int p=0,l2=m_data.numAttributes();p<l2;p++){
				if(p==m_data.classIndex()) continue;
				Attribute attr = m_data.attribute(p);
				double iVal = instanceI.value(attr);
				double jVal = instanceJ.value(attr);
				if(attr.isNumeric()) distance += Math.pow(iVal-jVal, 2);
				else distance += ((double[][]) vdmMap.get(attr))[(int) iVal][(int) jVal];
			}
			if(nrNN<m_nearestNeighbors){
				int index = 0;
				for(int l2=dists.length; index<l2; index++) if(distance<dists[index]) break;
				dists[index] = distance;
				topKInstances.insertElementAt(instanceJ, index);
				nrNN++;
			} else if(distance<dists[m_nearestNeighbors-1]){
				int index = 0;
				for(int l2=dists.length; index<l2; index++) if(distance<dists[index]) break;
				dists[index] = distance;
				topKInstances.insertElementAt(instanceJ, index);
				topKInstances.remove(topKInstances.size()-1);
			}
		}
		Instance[] nnArray = new Instance[m_nearestNeighbors];
		for(int j=0; j<nnArray.length; j++) nnArray[j]=topKInstances.get(j);

		// D2: create synthetic examples
		Random rand = new Random(1);
		//System.out.println("N:"+(int)(m_percentage[k]/100)+" k:"+k);
		for(int n = (int) (m_percentage[k]/100); n>0; n--) {
			double[] values = new double[sample[k].numAttributes()];
			int nn = rand.nextInt( m_nearestNeighbors );
			for(int p=0,l2=m_data.numAttributes();p<l2;p++){
				if(p==m_data.classIndex()) continue;
				Attribute attr = m_data.attribute(p);
				if( attr.isNumeric() ) {
					double dif = nnArray[nn].value( attr ) - instanceI.value( attr );
					double gap = rand.nextDouble();
					values[attr.index()] = (double) ( instanceI.value( attr ) + gap * dif );
				//if(attr.isDate()) values[attr.index()]=(long) (instanceI.value(attr)+gap*dif);
				} else {
					int[] valueCounts = new int[attr.numValues()];
					valueCounts[(int) instanceI.value(attr)]++;
					for(int nnEx = 0; nnEx < m_nearestNeighbors; nnEx++) 
						valueCounts[(int) nnArray[nnEx].value(attr)]++;
					int maxIndex = 0, max = Integer.MIN_VALUE;
					for(int index = 0; index < attr.numValues(); index++) {
						if( valueCounts[index] > max ) {
							max = valueCounts[index];
							maxIndex = index;
						}
					}
					values[attr.index()] = maxIndex;
				}
			}
			values[sample[k].classIndex()] = k;
			m_output.add(new DenseInstance(1.0,values));
		}
	  }
	}
	//System.out.println("ADDS:"+m_output.numInstances());
	for (int i=0; i<m_output.numInstances(); i++) m_data.add(m_output.instance(i));
	return m_data;
  }

  private static String printDataBalanceBasics(Instances input) {
	StringBuffer text = new StringBuffer("#"+input.numInstances()+"=>");
	int[] sum=new int[input.numClasses()];
	for(int j=0,l=input.numInstances();j<l;j++) sum[(int) input.instance(j).classValue()]++;
	for(int i=0,l=input.numClasses();i<l;i++) text.append(sum[i]+",");
	return text.toString();
  }
}

