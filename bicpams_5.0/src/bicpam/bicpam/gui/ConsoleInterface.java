package bicpam.gui;

import generator.BicMatrixGenerator.PatternType;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import utils.BicException;
import utils.BicReader;
import utils.NetMatrixMapper;
import utils.others.CopyUtils;
import utils.others.RemovalUtils;
import bicpam.bicminer.BiclusterMiner;
import bicpam.bicminer.BiclusterMiner.Orientation;
import bicpam.bicminer.coherent.AdditiveBiclusterMiner;
import bicpam.bicminer.coherent.MultiplicativeBiclusterMiner;
import bicpam.bicminer.coherent.SymmetricBiclusterMiner;
import bicpam.bicminer.constant.ConstantBiclusterMiner;
import bicpam.bicminer.constant.ConstantOverallBiclusterMiner;
import bicpam.bicminer.order.OrderPreservingBiclusterMiner;
import bicpam.closing.BiclusterFilter;
import bicpam.closing.BiclusterMerger;
import bicpam.closing.Biclusterizer;
import bicpam.closing.BiclusterFilter.FilteringCriteria;
import bicpam.closing.BiclusterMerger.MergingStrategy;
import bicpam.mapping.ItemMapper;
import bicpam.mapping.Itemizer;
import bicpam.mapping.Itemizer.DiscretizationCriteria;
import bicpam.mapping.Itemizer.FillingCriteria;
import bicpam.mapping.Itemizer.NoiseRelaxation;
import bicpam.mapping.Itemizer.NormalizationCriteria;
import bicpam.pminer.PM;
import bicpam.pminer.fim.ClosedFIM;
import bicpam.pminer.fim.MaximalFIM;
import bicpam.pminer.fim.SimpleFIM;
import bicpam.pminer.fim.ClosedFIM.ClosedImplementation;
import bicpam.pminer.fim.MaximalFIM.MaximalImplementation;
import bicpam.pminer.fim.SimpleFIM.SimpleImplementation;
import bicpam.pminer.spm.SequentialPM;
import bicpam.pminer.spm.SequentialPM.SequentialImplementation;
import domain.Biclusters;
import domain.Dataset;

public class ConsoleInterface extends Interface {

	private static final long serialVersionUID = 1L;
	
	public Biclusters run(String[] args) throws Exception {
		
		/** A: READING INPUT **/
		
		Dataset data = null;
		try {
			String dataset = getStringValue("input",args);
			if(dataset==null){
				System.out.println("Input data file either not provided or contains an error!");
				return null;
			}
			dataset=new File("").getAbsolutePath()+dataset;
			boolean network=getBoolValue("network",args);
			if(network){
				int n1=getIntValue("node1",args), n2=getIntValue("node2",args), score=getIntValue("score",args);
				if(n1==-1) System.out.println("Warning: index of 1st nodes not provided => 0 considered by default");
				if(n2==-1) System.out.println("Warning: index of 2nd nodes not provided => 1 considered by default");
				if(score==-1) System.out.println("Warning: index of scores not provided => 2 considered by default");
				boolean bidir=!getBoolValue("directional",args);
				String delimiter = BicReader.detectDelimeter(dataset);
				data = NetMatrixMapper.getTable(dataset, n1, n2, score, delimiter, bidir);
			} else if(dataset.contains(".arff")){
				data = new Dataset(BicReader.getInstances(dataset));
			} else {
				String delimiter = BicReader.detectDelimeter(dataset);
				List<String> conds = BicReader.getConds(dataset,1,delimiter); 
				List<String> genes = BicReader.getGenes(dataset,delimiter); 
				data = new Dataset(conds,genes,BicReader.getTable(dataset,3,delimiter));
			}
			if(data.rows.size()<3 || data.indexes.size()<3 || data.scores.size()<3){
				System.out.println(fileErrorMessage);
				return null;
			}
		} catch (Exception e1) {
			e1.printStackTrace();
			System.out.println(fileErrorMessage);
			return null;
		}

		/** B: Preprocessing **/

		//B1:Initializations
		BiclusterMiner bicminer = null;	
		boolean scalability=getBoolValue("scalability",args);
		int nrIterations=getIntValue("nrIterations",args);
		if(nrIterations<=0) nrIterations=1;
		int nritems=getIntValue("nrItems",args);
		if(nritems<=1){
			System.out.println("Warning: number of items (coherency strenght) not provided => 5 items considered by default");
			nritems=5;
		}

		data.symmetry=getBoolValue("symmetries",args);		
		Orientation orientation=getBoolValue("patternOnColumns",args) ?
				Orientation.PatternOnColumns : Orientation.PatternOnRows;
		
		try {
			//B2:Normalizer
			NormalizationCriteria normCriteria=null;
			String aux = getStringValue("normalization",args);
			if(aux==null){
				System.out.println("Warning: normalization not inputted => normalization on rows considered (default)");
				normCriteria = NormalizationCriteria.Row;
			} else if(aux.equals("Overall")) normCriteria = NormalizationCriteria.Overall; 
			else if(aux.equals("Column")) normCriteria = NormalizationCriteria.Column; 
			else if(aux.equals("Row")) normCriteria = NormalizationCriteria.Row; 
			else if(aux.equals("None")) normCriteria = NormalizationCriteria.None; 
			
			//B3:Discretizer
			DiscretizationCriteria discCriteria=null;
			aux = (String) getStringValue("discretization",args);
			if(aux==null){
				System.out.println("Warning: discretization not inputted => Gaussian breakpoints considered by default");
				aux = "Gaussian";
			}
			if(aux.equals("Gaussian")) discCriteria = DiscretizationCriteria.NormalDist; 
			else if(aux.equals("FixedRange")) discCriteria = DiscretizationCriteria.SimpleRange; 
			else if(aux.equals("None")) discCriteria = DiscretizationCriteria.None; 
			
			//B4:Missings Handler
			FillingCriteria missHandler=null;
			aux = getStringValue("missingsHandler",args);
			if(aux==null){ 
				System.out.println("Warning: missingsHandler not inputted => missing values are removed by default");
				missHandler = FillingCriteria.RemoveValue; //"Remove"
			}
			else if(aux.equals("Replace")) missHandler = FillingCriteria.Replace;
			else missHandler = FillingCriteria.RemoveValue; //"Remove"
			
			//B5:Noise Relaxation
			NoiseRelaxation noiserelaxation=null;
			aux = getStringValue("noiseRelaxation",args);
			if(aux==null){
				System.out.println("Warning: noiseRelaxation not inputted => no relaxation was considered (default)");
				noiserelaxation = NoiseRelaxation.None; 
			}
			else if(aux.equals("MultiItem")) noiserelaxation = NoiseRelaxation.OptionalItem;
			else noiserelaxation = NoiseRelaxation.None; 

			//B6:Removals
			String removals = getStringValue("removals",args);
			List<Integer> remItems = new ArrayList<Integer>();
			if(removals==null) System.out.println("Warning: removals not inputted => no items were removed (default)");
			else remItems = RemovalUtils.getItemsToRemove(nritems,removals,data.symmetry);

			//B7:Preprocessing
			System.out.println("Data statistics:\n"+data.getStatistics()+"\nRemovals:"+remItems);
			System.out.println("ITEMIZER:\n#Items:"+nritems+"\nSymmetric:"+data.symmetry+"\nNorm:"+normCriteria+"\nDisc:"+discCriteria
					+"\nNoise:"+noiserelaxation+"\nMissing:"+missHandler+"\nRemovals:"+remItems+"\nScalability:"+scalability);
			
			data = Itemizer.run(data,nritems,data.symmetry,normCriteria,discCriteria,noiserelaxation,missHandler,scalability,orientation);
			data = ItemMapper.remove(data,remItems);
			
		} catch (Exception e1) {
			e1.printStackTrace();
			System.out.println(preProcessingErrorMessage);
			return null;
		}
		
		/** C: Postprocessing **/
		
		try {
			//C1:Merging
			double minoverlapping = getDoubleValue("quality",args)/100.0;
			if(minoverlapping<0){
				System.out.println("Warning: quality not specified => 100% quality was assumed");
				minoverlapping = 1.0;
			}
			MergingStrategy merger=null;
			String aux = getStringValue("merging",args);
			if(aux==null || aux.equals("Heuristic")) merger = MergingStrategy.Heuristic;
			else if(aux.equals("Combinatorial")) merger = MergingStrategy.Combinatorial;
			else merger = MergingStrategy.FIMOverall;
			
			//C2:Filtering
			double minfiltering = getDoubleValue("dissimilarity",args)/100.0;
			if(minfiltering<0){
				System.out.println("Warning: dissimilarity not specified => 0% dissimilarity was assumed");
				minfiltering = 0;
			}
			FilteringCriteria filter=null;
			aux = getStringValue("filtering",args);
			if(aux==null) filter = FilteringCriteria.Overall;
			else if(aux.equals("DissimilarElements")) filter = FilteringCriteria.Overall; 
			else if(aux.equals("DissimilarRows")) filter = FilteringCriteria.Rows; 
			else if(aux.equals("DissimilarColumns")) filter = FilteringCriteria.Columns; 

			//C2:Postprocessing
			Biclusterizer bichandler = new Biclusterizer(
					new BiclusterMerger(minoverlapping),
					new BiclusterFilter(filter,1.0-minfiltering));
			
			/** D: Mining step **/

			//D1:Coherency
			PatternType coherency=null;
			aux = getStringValue("coherency",args);
			if(aux==null){
				System.out.println("Warning: coherency assumption not specified => Constant coherency (default) was assumed");
				coherency = PatternType.Constant;
			}
			else if(aux.equals("Constant")) coherency = PatternType.Constant; 
			else if(aux.equals("OrderPreserving")) coherency = PatternType.OrderPreserving; 
			else if(aux.equals("ConstantOverall")) coherency = PatternType.ConstantOverall; 
			else if(aux.equals("Additive")) coherency = PatternType.Additive; 
			else if(aux.equals("Multiplicative")) coherency = PatternType.Multiplicative; 
			else if(aux.equals("Symmetric")) coherency = PatternType.Symmetric;
			
			//D2:Pattern miner
			PM pminer = null;
			aux = getStringValue("patternMiner",args);
			if(coherency == PatternType.OrderPreserving){
				pminer = new SequentialPM();
				if(aux==null) ((SequentialPM)pminer).algorithm = SequentialImplementation.IndexSpan;
				else if(aux.equals("PrefixSpan")) ((SequentialPM)pminer).algorithm = SequentialImplementation.PrefixSpan; 
				else if(aux.equals("BidePlus")) ((SequentialPM)pminer).algorithm = SequentialImplementation.BIDEPlus; 
				else if(aux.equals("IndexSpan")) ((SequentialPM)pminer).algorithm = SequentialImplementation.IndexSpan;
				else System.out.println("Invalid sequential pattern miner selected for Order Preserving Biclustering ("+aux+" is unrecognized)");
			} else{
				String aux2 = getStringValue("patternRepresentation",args);
				if(aux==null){
					pminer = new ClosedFIM();
					((ClosedFIM)pminer).algorithm = ClosedImplementation.DCharm;
				} else {
					if(aux2==null||aux2.equals("Closed")){
						pminer = new ClosedFIM();
						if(aux==null) ((ClosedFIM)pminer).algorithm = ClosedImplementation.DCharm;
						else if(aux.equals("CharmTID")) ((ClosedFIM)pminer).algorithm = ClosedImplementation.Charm;
						else if(aux.equals("CharmDiffsets")) ((ClosedFIM)pminer).algorithm = ClosedImplementation.DCharm; 
						else if(aux.equals("AprioriTID")) ((ClosedFIM)pminer).algorithm = ClosedImplementation.AprioriTID;
					}
					if(aux2==null||aux2.equals("Simple")){
						if(aux==null||aux.equals("F2G")){
							pminer = new SimpleFIM();
							((SimpleFIM)pminer).algorithm = SimpleImplementation.F2G;
						} else if(aux.equals("Eclat")){
							pminer = new SimpleFIM();
							((SimpleFIM)pminer).algorithm = SimpleImplementation.Vertical;
						} else if(aux.equals("AprioriTID")){
							pminer = new SimpleFIM();
							((SimpleFIM)pminer).algorithm = SimpleImplementation.Apriori; //"AprioriTID"
						}
					}
					if(aux2!=null && aux2.equals("Maximal")){
						pminer = new MaximalFIM();
						((MaximalFIM)pminer).algorithm = MaximalImplementation.CharmMFI; 
					}
					if(pminer==null) System.out.println("Invalid pattern miner selected ("+aux+" cannot be selected with the remaining options)");
				}
			}

			//D3:Stopping criteria
			int minColsBic=getIntValue("minColumns",args);
			if(minColsBic<0){
				System.out.println("Warning: minimum number of columns per bicluster not specified => 3 columns assumed by default");
				minColsBic=3;
			}
			pminer.inputMinColumns(minColsBic);
			
			String criteria = getStringValue("stopCriteria",args);
			if(criteria==null){
				System.out.println("Warning: stopping criteria not specified => a minimum number of 100 biclusters (before postprocessing) was considered");
				pminer.inputMinNrBics(100);
			} else if(criteria.equals("MinBics")){
				int minBics = getIntValue("stopValue",args);
				if(minBics<=0) System.out.println("Warning: incorrect stopping criteria value => minimum number of biclusters (before postprocessing) reset to 100");
				pminer.inputMinNrBics(minBics<=0 ? 100 : minBics);
			} else if(criteria.equals("MinArea")){
				double area = getDoubleValue("stopValue",args);
				if(area<=0) System.out.println("Warning: incorrect stopping criteria value => minimum area covered by biclusters (before postprocessing) reset to 5%");
				pminer.inputMinArea(area<=0 ? 5 : area);
			} else if(criteria.equals("MinSupport")){
				double support = getDoubleValue("stopValue",args);
				if(support<=0) System.out.println("Warning: incorrect stopping criteria value => minimum support per bicluster reset to 10%");
				pminer.setSupport(support<=0 ? 10 : support);
			} else System.out.println("Attention: you introduced a non-valid stopping criteria ("+criteria+"). StopCriteria in {MinBics,MinArea,MinSupport}.");
			
			System.out.println("MinCols:"+minColsBic+"\nStopCriteria:"+criteria+"\nValue:"+getDoubleValue("stopValue",args)+"\nIterations:"+nrIterations);
			System.out.println("Coherency:"+coherency+"\nOrientation:"+orientation+"\nPMiner:"+pminer.toString());
			System.out.println("Merging:"+minoverlapping+"\nFiltering:"+filter.toString()+"\nValue:"+minfiltering);

			
			/** E: Instantiate BicPAM **/		
			
			if(coherency == PatternType.OrderPreserving)
				bicminer = new OrderPreservingBiclusterMiner(data,(SequentialPM)pminer,bichandler,orientation);
			else if(coherency == PatternType.Constant)
				bicminer = new ConstantBiclusterMiner(data,pminer,bichandler,orientation); 
			else if(coherency == PatternType.ConstantOverall)
				bicminer = new ConstantOverallBiclusterMiner(data,pminer,bichandler,orientation); 
			else if(coherency == PatternType.Additive)
				bicminer = new AdditiveBiclusterMiner(data,pminer,bichandler,orientation); 
			else if(coherency == PatternType.Multiplicative)
				bicminer = new MultiplicativeBiclusterMiner(data,pminer,bichandler,orientation); 
			else if(coherency == PatternType.Symmetric)
				bicminer = new SymmetricBiclusterMiner(data,pminer,bichandler,orientation); 
		} catch(Exception e){
			e.printStackTrace();
			System.out.println(paramErrorMessage);
			return null;
		}
		
		/** F: Run BicPAM **/		
		
		Biclusters bics = new Biclusters();
		try {
			long time = System.currentTimeMillis();
			List<List<Integer>> originalIndexes = CopyUtils.copyIntList(data.indexes);
			List<List<Integer>> originalScores = CopyUtils.copyIntList(data.intscores);
			
			double removePercentage = 0.3;
			for(int i=0; i<nrIterations; i++){
				Biclusters iBics = bicminer.mineBiclusters();
				if(iBics.size()==0){
					System.out.println(zeroErrorMessage);
					return null;
				}
				data.remove(iBics.getElementCounts(),removePercentage);
				bicminer.setData(data);
				bics.addAll(iBics);
			}
			data.indexes = originalIndexes;
			data.intscores = originalScores;
			time = System.currentTimeMillis() - time;
		} catch (Exception e) {
			e.printStackTrace();
	   		System.out.println(runRrrorMessage);
			return null;
		}
		
		/** F: Display Results **/		
		
		try {
			//F1:Output params
			boolean verbose=getBoolValue("verbose",args);
			String output=getStringValue("output",args);
			
			//F2:Display text
			StringBuffer resultTxt = new StringBuffer("<b>Input:</b> "+data.getStatistics());
			resultTxt.append("<br><br><b>Output</b><br>Number of biclusters: "+bics.size());
			resultTxt.append("<br><br><b>Biclusters size:</b> "+ResultDisplay.toSummaryTable(bics));
			resultTxt.append("<br><br><b>Biclusters (row/column indexes):</b> "+ResultDisplay.toIndexesTable(bics));
			List<String> cols = data.columns, rows = data.rows;
   		    if(scalability && data.originalColumns!=null){
   		    	if(orientation.equals(Orientation.PatternOnRows)) cols = data.originalColumns;
   		    	else rows = data.originalColumns;
   		    }
			resultTxt.append("<br><br><b>Biclusters (row/column names):</b> "+ResultDisplay.toNamesTable(rows,cols,bics));
			if(verbose) resultTxt.append("<br><br><b>Biclusters raw values:</b> "+ResultDisplay.toNamesTable(rows,cols,bics));
			
			//F3:Save outputs
			try {
				if(output!=null){
					output=new File("").getAbsolutePath()+output;
					BufferedWriter out = new BufferedWriter(new FileWriter(output));
					out.write("<html>"+resultTxt.toString()+"</html>");
					out.close();
				} else System.out.println("<html>"+resultTxt.toString()+"</html>");
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(outputErrorMessage);
			return null;
		}
		return bics;
	}
	
	private String getStringValue(String name, String[] args) {
		for(String arg : args)
			if(arg.startsWith("--"+name)) return arg.substring(arg.indexOf("=")+1);
		return null;
	}
	private int getIntValue(String name, String[] args) {
		for(String arg : args)
			if(arg.startsWith("--"+name)) return Integer.valueOf(arg.substring(arg.indexOf("=")+1));
		return -1;
	}
	private double getDoubleValue(String name, String[] args) {
		for(String arg : args)
			if(arg.startsWith("--"+name)) return Double.valueOf(arg.substring(arg.indexOf("=")+1));
		return -1;
	}
	private boolean getBoolValue(String name, String[] args) {
		for(String arg : args)
			if(arg.startsWith("--"+name)) return true;
		return false;
	}

	public static void main(String[] args) throws Exception{
		System.out.println("Arguments:");
		for(String arg : args) System.out.println(arg);
		/*if(args.length==0){
			System.out.println("\n"+new ConsoleInterface().getHelp());
			new Console();
		}*/
		ConsoleInterface console = new ConsoleInterface();
		if(console.getBoolValue("help",args)) System.out.println("\n"+console.getHelp());
		if(console.getBoolValue("input",args)) console.run(args);
		else if(!console.getBoolValue("help",args)) System.out.println("Please provide a valid command:\n\n"+console.getHelp());
		/*String arg = "--input=/data/arff/gyeast.arff --coherency=Constant --nrItems=5 --quality=80"
			+" --normalization=Row --discretization=Gaussian"
			+" --noiseRelaxation=MultiItem --symmetries --missingsHandler=Replace --removals=None" 
			+" --stopCriteria=MinBics --stopValue=30 --minColumns=4 --nrIterations=2"
			+" --patternRepresentation=Closed --patternMiner=CharmDiffsets"
			+" --merging=Heuristic --filtering=DissimilarElements --dissimilarity=50"
			+" --output=/output/result.htm";
		console.run(arg.split(" "));*/
	}	
	
	public String getHelp(){
	    return "1: Input\n"
			+"\t--input=path\tPath to the data file. "+MatrixVar+"\n" 
			+"\t--network\tSignals that the inputted file is a network. BicPAMS accepts any input file format (such as .TXT or .SIG) assuming that: the first line specifies the column identifiers, and each other line specifies an interaction/entry within the network. An entry specifies the nodes and the association strength. Entries can be either delimited by tabs, spaces or commas. Illustrating, for a network with header \"idProteinA,nameProteinA,idProteinB,nameProteinB,weight\", the user should fix (node1,node2,score) indexes as (0,2,4) or (1,3,4).\n"
			+"\t\t--n1=int\tColumn index with the first node\n"
			+"\t\t--n2=int\tColumn index with the second node\n"
			+"\t\t--score=int\tColumn index with the association strength\n"
			+"\t\t--directional\tSignals that the associations of the inputted network are directional (default: bidirectional)\n"
			
			+"\n2: Target homogeneity\n"
			+"\t--coherency={Constant,OrderPreserving,ConstantOverall,Additive,Multiplicative,Symmetric}\t"+AssumptionVar+"\n"
			+"\t--nrItems=int\t"+CohenrecyStrengthVar+"\n"
			+"\t--quality=percentage [0,100]\t"+QualityVar+"\n"
			
			+"\n3: Mapping options\n"
			+"\t--normalization={Row,Overall,Column,None}\t"+NormalizationVar+"\n"
			+"\t--discretization={Gaussian,FixedRange,None}\t"+DiscretizationVar+"\n"
			+"\t--noiseRelaxation={None,MultiItem}\t"+NoiseHandlingVar+"\n"
			+"\t--symmetries\t"+SymmetriesVar+"\n"
			+"\t--missingsHandler={Remove,Replace}\t"+MissingsVar+"\n"
			+"\t--removals={Zero-Entries,NonDiffValues,None}\t"+RemovalsVar+"\n"
			
			+"\n4: Convergence options\n"
			+"\t--stopCriteria={MinBics,MinSupport,MinArea}\t"+StoppingCriteriaVar+"\n"
			+"\t--stopValue=double\tThe value associated with the selected stopping criteria (see the description of the previous parameter).\n"
			+"\t--minColumns=int\t"+MinColumnsVar+"\n"
			+"\t--nrIterations=int\t"+NrIterationsVar+"\n"
			
			+"\n5: Mining options\n"
			+"\t--patternRepresentation={Closed,Simple,Maximal}\t"+PatternRepresentationVar+"\n"
			+"\t--patternOnColumns\tChanges the pattern orientation from rows (default) to columns. By selecting this option, searches are applied on the transposed matrix. When the number of columns highly exceeds the number of rows (or vice-versa when searches are applied on the transposed matrix), pattern miners with vertical data formats such as Eclat should be preferred.\n"
			+"\t--patternMiner={CharmDiffsets,CharmTID,AprioriTID,F2G,Eclat,CharmMFI,PrefixSpan,IndexSpan,BidePlus}\t"+PatternMinerVar+"\n"
			
			+"\n6: Closing options\n"
			+"\t--merging={Heuristic,Combinatorial,FIM}\t"+MergingVar+"\n"
			+"\t--filtering={DissimilarElements,DissimilarRows,DissimilarColumns}\t"+FilteringVar+"\n"
			+"\t--dissimilarity=double\tPercentage of dissimilar elements, rows or columns (see the description of the previous parameter).\n"
			
			+"\n7: Output\n"
			+"\t--verbose\tWhen selected, it prints not only the row and column indexes associated with each found bicluster, but it also prints all their elements (after discretization). We discourage its use for solutions with a high number of biclusters due to the escalating size of the output.\n"
			+"\t--output=path\tWhen provided, guarantees that the output (biclustering solution) is saved in the specified file path.";
	}
	
	protected String open(){ return ""; }
	protected String close(){ return ""; }

}


class Console extends WindowAdapter implements WindowListener, ActionListener, Runnable {
	
	private JFrame frame;
	private JTextArea textArea;
	private Thread reader, reader2;
	private boolean quit;
					
	private final PipedInputStream pin=new PipedInputStream(), pin2=new PipedInputStream(); 
	Thread errorThrower; // just for testing
	
	public Console(){
		frame=new JFrame("Java Console");
		Dimension screenSize=Toolkit.getDefaultToolkit().getScreenSize();
		Dimension frameSize=new Dimension((int)(screenSize.width/2),(int)(screenSize.height/2));
		int x=(int)(frameSize.width/2);
		int y=(int)(frameSize.height/2);
		frame.setBounds(x,y,frameSize.width,frameSize.height);
		textArea=new JTextArea();
		textArea.setEditable(false);
		JButton button=new JButton("clear");
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().add(new JScrollPane(textArea),BorderLayout.CENTER);
		frame.getContentPane().add(button,BorderLayout.SOUTH);
		frame.setVisible(true);		
		frame.addWindowListener(this);		
		button.addActionListener(this);
		try{
			PipedOutputStream pout=new PipedOutputStream(this.pin);
			System.setOut(new PrintStream(pout,true)); 
		} catch (java.io.IOException io) {
			textArea.append("Couldn't redirect STDOUT to this console\n"+io.getMessage());
		} catch (SecurityException se) {
			textArea.append("Couldn't redirect STDOUT to this console\n"+se.getMessage());
	    } 
		try {
			PipedOutputStream pout2=new PipedOutputStream(this.pin2);
			System.setErr(new PrintStream(pout2,true));
		} catch (java.io.IOException io) {
			textArea.append("Couldn't redirect STDERR to this console\n"+io.getMessage());
		} catch (SecurityException se) {
			textArea.append("Couldn't redirect STDERR to this console\n"+se.getMessage());
	    } 		
		quit=false; // signals the Threads that they should exit
				
		// Starting two seperate threads to read from the PipedInputStreams				
		reader=new Thread(this);
		reader.setDaemon(true);	
		reader.start();	
		reader2=new Thread(this);	
		reader2.setDaemon(true);	
		reader2.start();
				
		System.out.println("All fonts available to Graphic2D:\n");
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		String[] fontNames=ge.getAvailableFontFamilyNames();
		for(int n=0;n<fontNames.length;n++)  System.out.println(fontNames[n]);		
		System.out.println("\nLets throw an error on this console");	
		errorThrower=new Thread(this);
		errorThrower.setDaemon(true);
		errorThrower.start();					
	}
	
	public synchronized void windowClosed(WindowEvent evt) {
		quit=true;
		this.notifyAll(); // stop all threads
		try { reader.join(1000);pin.close();   } catch (Exception e){}		
		try { reader2.join(1000);pin2.close(); } catch (Exception e){}
		System.exit(0);
	}		
	public synchronized void windowClosing(WindowEvent evt) {
		frame.setVisible(false); // default behaviour of JFrame	
		frame.dispose();
	}
	public synchronized void actionPerformed(ActionEvent evt) {
		textArea.setText("");
	}
	public synchronized void run()	{
		try {			
			while (Thread.currentThread()==reader){
				try { this.wait(100);}catch(InterruptedException ie) {}
				if (pin.available()!=0){
					String input=this.readLine(pin);
					textArea.append(input);
				}
				if (quit) return;
			}
			while (Thread.currentThread()==reader2){
				try { this.wait(100);}catch(InterruptedException ie) {}
				if (pin2.available()!=0){
					String input=this.readLine(pin2);
					textArea.append(input);
				}
				if (quit) return;
			}			
		} catch (Exception e){
			textArea.append("\nConsole reports an Internal error.");
			textArea.append("The error is: "+e);			
		}
		
		// just for testing (Throw a Nullpointer after 1 second)
		if (Thread.currentThread()==errorThrower){
			try { this.wait(1000); }catch(InterruptedException ie){}
			throw new NullPointerException("Application test: throwing an NullPointerException It should arrive at the console");
		}
	}
	
	public synchronized String readLine(PipedInputStream in) throws IOException {
		String input="";
		do {
			int available=in.available();
			if (available==0) break;
			byte b[]=new byte[available];
			in.read(b);
			input=input+new String(b,0,b.length);														
		}while( !input.endsWith("\n") &&  !input.endsWith("\r\n") && !quit);
		return input;
	}	
}

