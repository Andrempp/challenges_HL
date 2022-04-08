package bicpam.gui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;
import generator.BicMatrixGenerator.PatternType;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.border.TitledBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.InputVerifier;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.ToolTipManager;
import java.awt.Color;
import javax.swing.JButton;
import utils.BicReader;
import utils.NetMatrixMapper;
import utils.others.CopyUtils;
import utils.others.RemovalUtils;
import domain.Biclusters;
import domain.Dataset;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import javax.swing.JMenuItem;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import bicpam.bicminer.BiclusterMiner;
import bicpam.bicminer.BiclusterMiner.Orientation;
import bicpam.bicminer.coherent.AdditiveBiclusterMiner;
import bicpam.bicminer.coherent.MultiplicativeBiclusterMiner;
import bicpam.bicminer.coherent.SymmetricBiclusterMiner;
import bicpam.bicminer.constant.ConstantBiclusterMiner;
import bicpam.bicminer.constant.ConstantOverallBiclusterMiner;
import bicpam.bicminer.order.OrderPreservingBiclusterMiner;
import bicpam.closing.BiclusterFilter;
import bicpam.closing.BiclusterFilter.FilteringCriteria;
import bicpam.closing.BiclusterMerger;
import bicpam.closing.Biclusterizer;
import bicpam.closing.BiclusterMerger.MergingStrategy;
import bicpam.mapping.ItemMapper;
import bicpam.mapping.Itemizer;
import bicpam.mapping.Itemizer.DiscretizationCriteria;
import bicpam.mapping.Itemizer.FillingCriteria;
import bicpam.mapping.Itemizer.NoiseRelaxation;
import bicpam.mapping.Itemizer.NormalizationCriteria;
import bicpam.pminer.PM;
import bicpam.pminer.fim.ClosedFIM;
import bicpam.pminer.fim.ClosedFIM.ClosedImplementation;
import bicpam.pminer.fim.MaximalFIM;
import bicpam.pminer.fim.MaximalFIM.MaximalImplementation;
import bicpam.pminer.fim.SimpleFIM;
import bicpam.pminer.fim.SimpleFIM.SimpleImplementation;
import bicpam.pminer.spm.SequentialPM;
import bicpam.pminer.spm.SequentialPM.SequentialImplementation;

public class GraphicalInterface extends Interface {

	private static final long serialVersionUID = 1L;

	/** Parameters **/	
	
	static Dataset data;
	static List<List<Integer>> fixedIndexes;
	static List<List<Double>> fixedScores;
	static Biclusters bics = new Biclusters();
	static JComboBox Cnormalizer, Cdiscretizer, CSymmetric, Cmissingshandler, Cnoisehandler, CRemoveElem, CSca;
	static JComboBox Ccoherency, CFIMSPMPattMin, CPatternRep, Corientation, Cfilter, Cmerger, CcomboStopCriteria; 
	static JTextField CNrItems, COverlapping, CstringFilter, CMinColBic, CstringStopCriteria, CnumberIter;
	static boolean sca = false, bidir = false;
	static Orientation orientation;
	static int n1, n2, n3;
	static String filePath;
	
	boolean flexible = true;
	static JPanel PanelGraph, PanelGraph2;
	static JList CcomboBic1, CcomboBic2;
	static JMenuItem MSaveBiclusters;
	static JTabbedPane mainPanel;
	static boolean bicCombo = false;
	static JButton BRun;

	
	public GraphicalInterface() {
		
		
		/** A: Main panel setting */
		
		setTitle("BicPAMS");		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainPanel = new JTabbedPane(JTabbedPane.TOP);
		mainPanel.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		add(mainPanel);
		ToolTipManager.sharedInstance().setInitialDelay(0);
		JPanel algoPanel = new JPanel(), outputPanel = new JPanel();
		mainPanel.addTab("BicPAMS", null, algoPanel, "Parameterize BicPAMS");
		mainPanel.addTab("Output", null, outputPanel, "Visualize the biclustering solution");
		mainPanel.setEnabledAt(1, false);
		if(!flexible){
			setResizable(false);
			setBounds(100, 100, 710, 411);
			mainPanel.setBounds(8, 5, 700, 364);
			algoPanel.setLayout(null);
			outputPanel.setLayout(null);
		} else {
			setMinimumSize(new Dimension(800,500));
			setMaximumSize(new Dimension(1600,500));
			setResizable(true);
			algoPanel.setLayout(new GridBagLayout());
			outputPanel.setLayout(new GridBagLayout());
		}
		
		createMenuBars();
		createAlgoMenu(algoPanel);
		createOutputMenu(outputPanel);
		BRun.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				runBicPAMS();
			}
		});
	}

	public void createAlgoMenu(JPanel algoPanel){
		
		/** C0: biclusters combo boxes */
		URL url = null;
		try {
			url = new File("img/greyQuestionMark.png").toURI().toURL();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		ImageIcon img = new ImageIcon(url);//getClass().getResource("/greyQuestionMark.png"));
		JPanel panel1 = new JPanel(), panel2 = new JPanel(), panel3 = new JPanel(), panel4 = new JPanel(), panel5 = new JPanel();
		panel1.setBorder(new TitledBorder(new LineBorder(new Color(130,180,246),2), "Target homogeneity", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(50,100,200)));
		panel2.setBorder(new TitledBorder(new LineBorder(new Color(179,203,245),2), "Mapping options (optional)", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(70,130,180)));
		panel3.setBorder(new TitledBorder(new LineBorder(new Color(179,203,245),2), "Convergence (optional)", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(70,130,180)));
		panel4.setBorder(new TitledBorder(new LineBorder(new Color(179,203,245),2), "Mining options (optional)", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(70,130,180)));
		panel5.setBorder(new TitledBorder(new LineBorder(new Color(179,203,245),2), "Closing options (optional)", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(70,130,180)));
		BRun = new JButton("Run BicPAMS");
		BRun.setEnabled(false);
		if(!flexible){
			panel1.setLayout(null);
			panel2.setLayout(null);
			panel3.setLayout(null);
			panel4.setLayout(null);
			panel5.setLayout(null);
			panel1.setBounds(8, 5, 320, 90);			
			panel2.setBounds(8, 118, 320, 155);
			panel3.setBounds(329, 5, 348, 111);
			panel4.setBounds(329, 118, 348, 112);
			panel5.setBounds(329, 231, 348, 86);
			BRun.setBounds(10, 283, 311, 30);
			algoPanel.add(panel1);
			algoPanel.add(panel2);
			algoPanel.add(panel3);
			algoPanel.add(panel4);
			algoPanel.add(panel5);
			algoPanel.add(BRun);
		} else {
			panel1.setLayout(new GridBagLayout());
			panel2.setLayout(new GridBagLayout());
			panel3.setLayout(new GridBagLayout());
			panel4.setLayout(new GridBagLayout());
			panel5.setLayout(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1; c.insets = new Insets(3,3,3,3);
            c.gridy = 1; c.gridx = 1; c.gridheight = 5; 
    		algoPanel.add(panel1,c);
            c.gridy = 6; c.gridx = 1; c.gridheight = 8; 
    		algoPanel.add(panel2,c);
            c.gridy = 1; c.gridx = 2; c.gridheight = 6; 
    		algoPanel.add(panel3,c);
            c.gridy = 7; c.gridx = 2; c.gridheight = 5; 
    		algoPanel.add(panel4,c);
            c.anchor = GridBagConstraints.NORTH;
            c.gridy = 12; c.gridx = 2; c.gridheight = 3; 
    		algoPanel.add(panel5,c);
            c.anchor = GridBagConstraints.CENTER;
            c.gridy = 14; c.gridx = 1; c.gridheight = 1;
            BRun.setMinimumSize(new Dimension(50,50));
    		algoPanel.add(BRun,c);
		}

		JLabel labeltype = new JLabel("Coherency assumption:");
		JLabel lblItemsFrom = new JLabel("Coherency strength (#items):");
		JLabel lblMinOverlapping = new JLabel("Quality (%noise-free values):");
		Ccoherency=new JComboBox(new DefaultComboBoxModel(new String[] {"Constant", "Order Preserving", "Constant Overall", "Additive", "Multiplicative", "Symmetric"}));
		CNrItems = new JTextField("6");
		COverlapping = new JTextField("80");
		JLabel questionCoheAssum = new JLabel(img), questionCoheStren = new JLabel(img), questionQuality = new JLabel(img);
		questionCoheAssum.setToolTipText("<html>"+"<html>"+AssumptionVar+"</html>");
		questionCoheStren.setToolTipText("<html>"+CohenrecyStrengthVar+"</html>");
		questionQuality.setToolTipText("<html>"+QualityVar+"</html>");
		if(!flexible){
			lblItemsFrom.setBounds(9, 38, 185, 20);
			CNrItems.setBounds(191, 38, 30, 20);
			labeltype.setBounds(9, 16, 185, 20);
			Ccoherency.setBounds(191, 16, 107, 20);
			lblMinOverlapping.setBounds(9, 60, 185, 20);
			COverlapping.setBounds(191, 60, 30, 20);
			questionCoheAssum.setBounds(300, 16, 13, 20);
			questionCoheStren.setBounds(226, 38, 13, 20);
			questionQuality.setBounds(226, 60, 13, 20);			
			panel1.add(lblItemsFrom);
			panel1.add(CNrItems);
			panel1.add(labeltype);
			panel1.add(Ccoherency);				
			panel1.add(lblMinOverlapping);
			panel1.add(COverlapping);
			panel1.add(questionCoheAssum);
			panel1.add(questionCoheStren);
			panel1.add(questionQuality);
		} else {
			GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.BOTH;
            c.weighty = 1;
            c.gridx = 1; c.weightx = 4; 
            c.gridy = 1; panel1.add(labeltype,c);
            c.gridy = 2; panel1.add(lblItemsFrom,c);
            c.gridy = 3; panel1.add(lblMinOverlapping,c);
            c.gridx = 2; c.weightx = 2; c.gridwidth = 2;
            c.gridy = 1; panel1.add(Ccoherency,c);
            c.gridwidth = 1;
            c.gridy = 2; panel1.add(CNrItems,c);				
            c.gridy = 3; panel1.add(COverlapping,c);
            c.gridx = 3;
            c.gridy = 2; panel1.add(new JLabel("        "),c);				
            c.gridy = 3; panel1.add(new JLabel("        "),c);
            c.gridx = 4; c.weightx = 1; 
            c.gridy = 1; panel1.add(questionCoheAssum,c);
            c.gridy = 2; panel1.add(questionCoheStren,c);
            c.gridy = 3; panel1.add(questionQuality,c);
		}
		
		
		/** C1: pre-processing combo boxes */

		JLabel lblNormalizer = new JLabel("Normalization:");
		JLabel lblDiscretizer = new JLabel("Discretization:");
		JLabel lblNoiseHandler = new JLabel("Noise handler:");
		JLabel lblIs = new JLabel("Symmetries:");
		JLabel lblMissingsHandler = new JLabel("Missings handler:");
		JLabel lblRemoveElements = new JLabel("Remove elements:");
		lblNormalizer.setForeground(new Color(70,70,70));
		lblDiscretizer.setForeground(new Color(70,70,70));
		lblNoiseHandler.setForeground(new Color(70,70,70));
		lblIs.setForeground(new Color(70,70,70));
		lblMissingsHandler.setForeground(new Color(70,70,70));
		lblRemoveElements.setForeground(new Color(70,70,70));

		Cnormalizer=new JComboBox(new DefaultComboBoxModel(new String[] {"Row", "Overall", "Column", "None"}));
		Cdiscretizer=new JComboBox(new DefaultComboBoxModel(new String[] {"Gaussian", "FixedRange", "None"}));
		Cnoisehandler=new JComboBox(new DefaultComboBoxModel(new String[] {"None", "MultiItem"}));
		CSymmetric=new JComboBox(new DefaultComboBoxModel(new String[] {"Yes", "No"}));
		Cmissingshandler=new JComboBox(new DefaultComboBoxModel(new String[] {"Remove", "Replace"}));
		CRemoveElem = new JComboBox(new DefaultComboBoxModel(new String[] {"Zero-Entries", "Non-Diff. Values", "None"}));

		JLabel questionNormalization = new JLabel(img), questionDiscretization = new JLabel(img), questionNoise = new JLabel(img);
		JLabel questionSymmetries = new JLabel(img), questionMissings = new JLabel(img), questionRemove = new JLabel(img);
		questionNormalization.setToolTipText("<html>"+NormalizationVar+"</html>");
		questionDiscretization.setToolTipText("<html>"+DiscretizationVar+"</html>");
		questionNoise.setToolTipText("<html>"+NoiseHandlingVar+"</html>");
		questionSymmetries.setToolTipText("<html>"+SymmetriesVar+"</html>");
		questionMissings.setToolTipText("<html>"+MissingsVar+"</html>");
		questionRemove.setToolTipText("<html>"+RemovalsVar+"</html>");
		
		if(flexible){
			GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.BOTH;
            c.weighty = 1;
            c.gridx = 1; c.weightx = 4; 
            c.gridy = 1; panel2.add(lblNormalizer,c);
            c.gridy = 2; panel2.add(lblDiscretizer,c);
            c.gridy = 3; panel2.add(lblNoiseHandler,c);
            c.gridy = 4; panel2.add(lblIs,c);
            c.gridy = 5; panel2.add(lblMissingsHandler,c);
            c.gridy = 6; panel2.add(lblRemoveElements,c);
            c.gridx = 2; c.weightx = 4; 
            c.gridy = 1; panel2.add(Cnormalizer,c);
            c.gridy = 2; panel2.add(Cdiscretizer,c);				
            c.gridy = 3; panel2.add(Cnoisehandler,c);
            c.gridy = 4; panel2.add(CSymmetric,c);
            c.gridy = 5; panel2.add(Cmissingshandler,c);				
            c.gridy = 6; panel2.add(CRemoveElem,c);
            c.gridx = 3; c.weightx = 1; 
            c.gridy = 1; panel2.add(questionNormalization,c);
            c.gridy = 2; panel2.add(questionDiscretization,c);
            c.gridy = 3; panel2.add(questionNoise,c);
            c.gridy = 4; panel2.add(questionSymmetries,c);
            c.gridy = 5; panel2.add(questionMissings,c);
            c.gridy = 6; panel2.add(questionRemove,c);
		} else {			
			lblNormalizer.setBounds(9, 16, 150, 20);
			Cnormalizer.setBounds(168, 16, 129, 20);
			lblDiscretizer.setBounds(9, 38, 150, 20);
			Cdiscretizer.setBounds(168, 38, 129, 20);
			lblNoiseHandler.setBounds(9, 60, 150, 20);
			Cnoisehandler.setBounds(168, 60, 129, 20);
			lblIs.setBounds(9, 82, 150, 20);
			CSymmetric.setBounds(168, 82, 129, 20);
			lblMissingsHandler.setBounds(9, 104, 150, 20);
			Cmissingshandler.setBounds(168, 104, 129, 20);
			lblRemoveElements.setBounds(9, 126, 150, 20);
			CRemoveElem.setBounds(168, 126, 129, 20);
			questionNormalization.setBounds(299, 16, 13, 20);
			questionDiscretization.setBounds(299, 38, 13, 20);
			questionNoise.setBounds(299, 60, 13, 20);		
			questionSymmetries.setBounds(299, 82, 13, 20);
			questionMissings.setBounds(299, 104, 13, 20);
			questionRemove.setBounds(299, 126, 13, 20);
			panel2.add(lblNormalizer);
			panel2.add(Cnormalizer);
			panel2.add(lblDiscretizer);
			panel2.add(Cdiscretizer);
			panel2.add(lblNoiseHandler);
			panel2.add(Cnoisehandler);
			panel2.add(lblIs);
			panel2.add(CSymmetric);
			panel2.add(lblMissingsHandler);
			panel2.add(Cmissingshandler);
			panel2.add(lblRemoveElements);
			panel2.add(CRemoveElem);
			panel2.add(questionNormalization);
			panel2.add(questionDiscretization);
			panel2.add(questionNoise);
			panel2.add(questionSymmetries);
			panel2.add(questionMissings);
			panel2.add(questionRemove);
		}	
		Cnoisehandler.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String aux = (String) Cnoisehandler.getSelectedItem();
				CRemoveElem.removeAllItems();
				if(aux.equals("MultiItem")){
					CRemoveElem.addItem("None");
				} else {
					CRemoveElem.addItem("Zero Entries");
					CRemoveElem.addItem("Non-Diff. Values");
					CRemoveElem.addItem("None");
				}
			}
		});


		/** C2: mining combo boxes */		
		
		JLabel lblStopCriteria = new JLabel("Stopping griteria:");
		JLabel lblOther = new JLabel("          with value:");
		JLabel lblMinColumns = new JLabel("Minimum #columns:");
		JLabel lblNumberOfIterations = new JLabel("Number of iterations:");
		lblStopCriteria.setForeground(new Color(70,70,70));
		lblOther.setForeground(new Color(70,70,70));
		lblMinColumns.setForeground(new Color(70,70,70));
		lblNumberOfIterations.setForeground(new Color(70,70,70));

		CcomboStopCriteria=new JComboBox(new DefaultComboBoxModel(new String[] {"Min. #Bics (before merging)", "Min. Area (%elements)", "Min. Support (%rows/bic)"}));
		CstringStopCriteria = new JTextField("50");
		CMinColBic = new JTextField("4");
		CnumberIter = new JTextField("2");
		JLabel questionCriteria = new JLabel(img), questionMinCol = new JLabel(img), questionNumIter = new JLabel(img);
		questionCriteria.setToolTipText("<html>"+StoppingCriteriaVar+"</html>");
		questionMinCol.setToolTipText("<html>"+MinColumnsVar+"</html>");
		questionNumIter.setToolTipText("<html>"+NrIterationsVar+"</html>");

		//lblStopCriteria.setForeground(new Color(70,70,70));
		if(flexible){
			GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.BOTH;
            c.weighty = 1;
            c.gridx = 1; c.weightx = 4; 
            c.gridy = 1; panel3.add(lblStopCriteria,c);
            c.gridy = 2; panel3.add(lblOther,c);
            c.gridy = 3; panel3.add(lblMinColumns,c);
            c.gridy = 4; panel3.add(lblNumberOfIterations,c);
            c.gridx = 2; c.weightx = 2; c.gridwidth = 2; 
            c.gridy = 1; panel3.add(CcomboStopCriteria,c);
            c.gridwidth = 1;
            c.gridy = 2; panel3.add(CstringStopCriteria,c);
            c.gridy = 3; panel3.add(CMinColBic,c);
            c.gridy = 4; panel3.add(CnumberIter,c);
            c.gridx = 3;
            c.gridy = 2; panel3.add(new JLabel("        "),c);				
            c.gridy = 3; panel3.add(new JLabel("        "),c);
            c.gridy = 4; panel3.add(new JLabel("        "),c);
            c.gridx = 4; c.weightx = 1; 
            c.gridy = 1; panel3.add(questionCriteria,c);
            c.gridy = 3; panel3.add(questionMinCol,c);
            c.gridy = 4; panel3.add(questionNumIter,c);
		} else {
			lblStopCriteria.setBounds(9, 18, 137, 20);
			CcomboStopCriteria.setBounds(136, 18, 188, 20);
			lblOther.setBounds(144, 39, 90, 20);
			CstringStopCriteria.setBounds(227, 39, 30, 20);
			lblMinColumns.setBounds(9, 61, 140, 20);
			CMinColBic.setBounds(144, 61, 30, 20);
			lblNumberOfIterations.setBounds(9, 83, 140, 20);
			CnumberIter.setBounds(144, 83, 30, 20);
			questionCriteria.setBounds(325, 18, 13, 20);
			questionMinCol.setBounds(175, 61, 13, 20);
			questionNumIter.setBounds(175, 83, 13, 20);
			panel3.add(lblStopCriteria);
			panel3.add(CcomboStopCriteria);
			panel3.add(lblOther);
			panel3.add(CstringStopCriteria);
			panel3.add(lblMinColumns);
			panel3.add(CMinColBic);
			panel3.add(lblNumberOfIterations);
			panel3.add(CnumberIter);
			panel3.add(questionCriteria);
			panel3.add(questionMinCol);
			panel3.add(questionNumIter);
		}
		CcomboStopCriteria.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String aux4 = (String) CcomboStopCriteria.getSelectedItem();
				if(aux4.contains("Min. #Bics")){
					CstringStopCriteria.setText("50");
				} else if(aux4.contains("Min. Area")){
					CstringStopCriteria.setText("4");
					CstringStopCriteria.setInputVerifier(new MyInputVerifier(1,100));
				} else if(aux4.contains("Min. Support")){
					CstringStopCriteria.setText("20");
					CstringStopCriteria.setInputVerifier(new MyInputVerifier(1,100));
				}
			}
		});

		
		/** C3: mining combo boxes */

		JLabel lblPatternrep = new JLabel("Pattern Representation:");
		JLabel labelorientation = new JLabel("Coherency orientation:");
		JLabel lblPatternMinerfimspm = new JLabel("Pattern miner (FIM/SPM):");
		JLabel lblSca = new JLabel("Scalability enhancer:");
		lblPatternrep.setForeground(new Color(70,70,70));
		labelorientation.setForeground(new Color(70,70,70));
		lblPatternMinerfimspm.setForeground(new Color(70,70,70));
		lblSca.setForeground(new Color(70,70,70));

		CPatternRep=new JComboBox(new DefaultComboBoxModel(new String[] {"Closed (maximal bics)", "Maximal", "Simple"}));
		Corientation=new JComboBox(new String[] {"Rows", "Columns"});
		CFIMSPMPattMin = new JComboBox(new DefaultComboBoxModel(new String[] {"CharmDiffsets", "CharmTID", "AprioriTID"}));
		CSca=new JComboBox(new String[] {"No","Yes"});
		JLabel questionPatternRe = new JLabel(img), questionCoheOrie = new JLabel(img), questionPatternMiner = new JLabel(img), questionSca = new JLabel(img);
		questionPatternRe.setToolTipText("<html>"+PatternRepresentationVar+"</html>");
		questionSca.setToolTipText("<html>"+ScalabilityVar+"</html>");
		questionCoheOrie.setToolTipText("<html>"+OrientationVar+"</html>");
		questionPatternMiner.setToolTipText("<html>"+PatternMinerVar+"</html>");

		if(flexible){
			GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.BOTH;
            c.weighty = 1;
            c.gridx = 1; c.weightx = 4; 
            c.gridy = 1; panel4.add(labelorientation,c);
            c.gridy = 2; panel4.add(lblPatternrep,c);
            c.gridy = 3; panel4.add(lblPatternMinerfimspm,c);
            c.gridy = 4; panel4.add(lblSca,c);
            c.gridx = 2; c.weightx = 4; 
            c.gridy = 1; panel4.add(Corientation,c);				
            c.gridy = 2; panel4.add(CPatternRep,c);
            c.gridy = 3; panel4.add(CFIMSPMPattMin,c);
            c.gridy = 4; panel4.add(CSca,c);
            c.gridx = 3; c.weightx = 1; 
            c.gridy = 1; panel4.add(questionCoheOrie,c);
            c.gridy = 2; panel4.add(questionPatternRe,c);
            c.gridy = 3; panel4.add(questionPatternMiner,c);
            c.gridy = 4; panel4.add(questionSca,c);
		} else {
			lblPatternrep.setBounds(9, 16, 151, 20);
			CPatternRep.setBounds(167, 16, 155, 20);
			labelorientation.setBounds(9, 38, 160, 20);
			Corientation.setBounds(167, 38, 155, 20);
			lblPatternMinerfimspm.setBounds(9, 60, 160, 20);
			CFIMSPMPattMin.setBounds(167, 60, 155, 20);
			lblSca.setBounds(9, 82, 151, 20);
			CSca.setBounds(167, 82, 155, 20);
			questionPatternRe.setBounds(323, 16, 13, 20);
			questionCoheOrie.setBounds(323, 38, 13, 20);
			questionPatternMiner.setBounds(323, 60, 13, 20);
			questionSca.setBounds(323, 82, 13, 20);
			panel4.add(lblPatternrep);
			panel4.add(CPatternRep);
			panel4.add(labelorientation);
			panel4.add(Corientation);
			panel4.add(lblPatternMinerfimspm);
			panel4.add(CFIMSPMPattMin);
			panel4.add(lblSca);
			panel4.add(CSca);
			panel4.add(questionPatternRe);
			panel4.add(questionCoheOrie);
			panel4.add(questionPatternMiner);
			panel4.add(questionSca);
		}
		Ccoherency.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				CFIMSPMPattMin.removeAllItems();
				String aux2 = (String) Ccoherency.getSelectedItem();
				if(aux2.equals("Order Preserving")) CPatternRep.setSelectedIndex(2);
				else CPatternRep.setSelectedIndex(0);
			}
		});
		Corientation.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String aux2 = (String) Corientation.getSelectedItem();
				if(!data.network){
					if(aux2.equals("Rows") && data.columns.size()>500) CSca.setSelectedItem("Yes");
					else if(aux2.equals("Columns") && data.rows.size()>500) CSca.setSelectedItem("Yes");
					else CSca.setSelectedItem("No");
				}
			}
		});
		CPatternRep.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				CFIMSPMPattMin.removeAllItems();
				String aux = (String) CPatternRep.getSelectedItem();
				String aux2 = (String) Ccoherency.getSelectedItem();
				if(aux.equals("Simple")){
					if(aux2.equals("Order Preserving")){ 
						CFIMSPMPattMin.addItem("PrefixSpan");
						CFIMSPMPattMin.addItem("IndexSpan");
					} else {
						CFIMSPMPattMin.addItem("AprioriTID");
						CFIMSPMPattMin.addItem("F2G");
						CFIMSPMPattMin.addItem("Eclat");
					}
				} else if(aux.contains("Closed")){
					if(aux2.equals("Order Preserving")) 
						CFIMSPMPattMin.addItem("BidePlus");
					else {
						CFIMSPMPattMin.addItem("CharmDiffsets");
						CFIMSPMPattMin.addItem("CharmTID");
						CFIMSPMPattMin.addItem("AprioriTID");
					}
				} else {
					if(aux2.equals("Order Preserving")) 
						CFIMSPMPattMin.addItem("BidePlus");
					else CFIMSPMPattMin.addItem("CharmMFI");
				}
			}
		});

		
		/** C4: closing combo boxes */

		JLabel lblMerger = new JLabel("Merging procedure:");
		JLabel lblFilter = new JLabel("Filtering procedure:");
		JLabel lblFilter2 = new JLabel("              with value:");
		lblMerger.setForeground(new Color(70,70,70));
		lblFilter.setForeground(new Color(70,70,70));
		lblFilter2.setForeground(new Color(70,70,70));

		Cmerger=new JComboBox(new DefaultComboBoxModel(new String[] {"Heuristic", "Combinatorial", "FIM"}));
		Cfilter=new JComboBox(new DefaultComboBoxModel(new String[] {"Dissimilar Elements", "Dissimilar Rows", "Dissimilar Columns"}));
		CstringFilter = new JTextField("50");
		JLabel questionMerging = new JLabel(img), questionFiltering = new JLabel(img);
		questionMerging.setToolTipText("<html>"+MergingVar+"</html>");
		questionFiltering.setToolTipText("<html>"+FilteringVar+"</html>");
		if(flexible){
			GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.BOTH;
            c.weighty = 1;
            c.gridx = 1; c.weightx = 4; 
            c.gridy = 1; panel5.add(lblMerger,c);
            c.gridy = 2; panel5.add(lblFilter,c);
            c.gridy = 3; panel5.add(lblFilter2,c);
            
            c.gridx = 2; c.weightx = 2; c.gridwidth = 2; 
            c.gridy = 1; panel5.add(Cmerger,c);
            c.gridy = 2; panel5.add(Cfilter,c);				
            c.gridwidth = 1;
            c.gridy = 3; panel5.add(CstringFilter,c);
            c.gridx = 3;
            c.gridy = 3; panel5.add(new JLabel("        "),c);				
            c.gridx = 4; c.weightx = 1; 
            c.gridy = 1; panel5.add(questionMerging,c);
            c.gridy = 2; panel5.add(questionFiltering,c);
		} else {
			lblMerger.setBounds(9, 16, 141, 20);
			Cmerger.setBounds(147, 16, 175, 20);
			lblFilter.setBounds(9, 38, 141, 20);
			Cfilter.setBounds(147, 38, 175, 20);
			lblFilter2.setBounds(155, 60, 90, 20);
			CstringFilter.setBounds(233, 60, 30, 20);
			questionMerging.setBounds(323, 16, 13, 20);
			questionFiltering.setBounds(323, 38, 13, 20);
			panel5.add(lblMerger);
			panel5.add(Cmerger);
			panel5.add(lblFilter);
			panel5.add(Cfilter);
			panel5.add(lblFilter2);
			panel5.add(CstringFilter);
			panel5.add(questionMerging);
			panel5.add(questionFiltering);
		}
		
		
		/** C5: verification */

		CNrItems.setInputVerifier(new MyInputVerifier(2,1000));
		COverlapping.setInputVerifier(new MyInputVerifier(1,100));
		CstringFilter.setInputVerifier(new MyInputVerifier(0,100));
		CMinColBic.setInputVerifier(new MyInputVerifier(1,10000));
		CstringStopCriteria.setInputVerifier(new MyInputVerifier(0,100000));
		CnumberIter.setInputVerifier(new MyInputVerifier(1,1000));
	}
	
	public void createOutputMenu(final JPanel outputPanel){

		/** D: biclusters tab */
		JLabel lblSelectTheBicluster0 = new JLabel("Display solution:");
		JLabel lblExport = new JLabel("Export solution:");
		final JComboBox CcomboBic0 = new JComboBox(new DefaultComboBoxModel(new String[] {"Summary", "Biclusters (row/column indexes)", "Biclusters (row/column names)"}));
		final JComboBox export = new JComboBox(new DefaultComboBoxModel(new String[] {"BicOverlapper", "Expander", "BicAT", "BicPAMS"}));
		CcomboBic0.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(bicCombo){
					if(CcomboBic0.getSelectedIndex()==0) 
						messageDisplay(ResultDisplay.toSummaryTable(bics),"Summary",new Dimension(400,200),true,"result.htm");
					else if(CcomboBic0.getSelectedIndex()==1) 
						messageDisplay(ResultDisplay.toIndexesTable(bics),"Indexes",new Dimension(800,400),true,"result.htm");
					else {
						List<String> cols = (data.originalColumns==null) ? data.columns : data.originalColumns; 
						messageDisplay(ResultDisplay.toNamesTable(data.rows,cols,bics),"Biclusters",new Dimension(800,400),true,"result.htm");	
					}
				}
			}
		});
		export.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(bicCombo){
					List<String> cols = (data.originalColumns==null) ? data.columns : data.originalColumns; 
					if(export.getSelectedIndex()==0) 
						messageDisplay(ExportDisplay.bicsToBicOverlapper(bics,data.rows,cols),"BicOverlapper format",new Dimension(800,400),false,"result.bic");
					else if(export.getSelectedIndex()==1) 
						messageDisplay(ExportDisplay.bicsToExpander(bics,data,cols),"Expander format",new Dimension(800,400),false,"result.samba");
					else if(export.getSelectedIndex()==2) 
						messageDisplay(ExportDisplay.bicsToBicat(bics,data.rows,cols),"Expander format",new Dimension(800,400),false,"result.samba");
					else messageDisplay(bics.toString(),"BicPAMS format",new Dimension(800,400),false,"solution.txt");
				}
			}
		});
		CcomboBic0.setSelectedItem(null);
		export.setSelectedItem(null);

		JLabel lblSelectTheBicluster1 = new JLabel("Heatmaps");
		JLabel lblSelectTheBicluster2 = new JLabel("Graphical display");
		JLabel lblAdvanced1 = new JLabel("Advanced displays: ");
		JLabel lblAdvanced2;
		try {
			lblAdvanced2 = linkify("BicOverlapper","http://vis.usal.es/bicoverlapper2/bicoverlapper/Intro.html","");
		} catch (Exception e2) {
			e2.printStackTrace();
			lblAdvanced2 = new JLabel("BicOverlapper");
		}
		lblAdvanced1.setForeground(Color.DARK_GRAY);
		
		PanelGraph = new JPanel(new BorderLayout());
		PanelGraph2 = new JPanel(new BorderLayout());
		CcomboBic1 = new JList(); //data has type Object[]
		CcomboBic1.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		CcomboBic1.setLayoutOrientation(JList.VERTICAL);
		CcomboBic1.setVisibleRowCount(-1);
		CcomboBic1.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				PanelGraph2.removeAll();
				PanelGraph2.add(ChartPlotter.getHeatMap(bics.getBiclusters().get(CcomboBic1.getSelectedIndex()), data, orientation));
				PanelGraph2.repaint();
				outputPanel.updateUI();
				PanelGraph.setVisible(false);
				PanelGraph2.setVisible(true);
			}
		});
		CcomboBic2 = new JList(); //data has type Object[]
		CcomboBic2.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		CcomboBic2.setLayoutOrientation(JList.VERTICAL);
		CcomboBic2.setVisibleRowCount(-1);
		CcomboBic2.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if(bicCombo){
					double coherenceStrength = 0.4;
					PanelGraph.removeAll();
					PanelGraph.add(ChartPlotter.getGraph(bics.getBiclusters().get(CcomboBic2.getSelectedIndex()), data, orientation));
					PanelGraph.repaint();
					//outputPanel.add(PanelGraph);
					outputPanel.updateUI();
				}
				PanelGraph2.setVisible(false);
				PanelGraph.setVisible(true);
			}
		});
		JScrollPane listScroller = new JScrollPane(CcomboBic1);
		JScrollPane listScroller2 = new JScrollPane(CcomboBic2);
		
		if(flexible){
			GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.BOTH;
            c.weighty = 1;
            c.gridx = 1; c.gridwidth = 2; 
            c.gridy = 1; outputPanel.add(lblSelectTheBicluster0,c);
            c.gridy = 2; outputPanel.add(CcomboBic0,c);
            c.gridy = 4; outputPanel.add(lblExport,c);
            c.gridy = 5; outputPanel.add(export,c);
             
            c.gridy = 7; c.gridwidth = 1;
            c.gridx = 1; outputPanel.add(lblSelectTheBicluster1,c);
            c.gridx = 2; outputPanel.add(lblSelectTheBicluster2,c);
            c.weighty = 15;
            c.gridy = 8; 
            c.gridx = 1; outputPanel.add(listScroller,c);
            c.gridx = 2; outputPanel.add(listScroller2,c);
            c.weighty = 1;
            c.gridy = 10;
			c.gridx = 1; outputPanel.add(lblAdvanced1,c);
            c.gridx = 2; outputPanel.add(lblAdvanced2,c);
            c.gridx = 3; c.gridy = 1;
			outputPanel.add(new JLabel("   "),c);
            c.gridx = 4; c.gridy = 1;
            c.gridheight = 10; c.weightx = 4;
			outputPanel.add(PanelGraph,c);
			outputPanel.add(PanelGraph2,c);
		} else {
			lblSelectTheBicluster0.setBounds(9, 3, 190, 20);
			CcomboBic0.setBounds(9, 23, 200, 20);
			lblExport.setBounds(9, 47, 190, 20);
			export.setBounds(9, 67, 200, 20);
			outputPanel.add(lblSelectTheBicluster0);
			outputPanel.add(CcomboBic0);
			outputPanel.add(lblExport);
			outputPanel.add(export);

			lblSelectTheBicluster1.setBounds(9, 90, 95, 20);
			lblSelectTheBicluster2.setBounds(104, 90, 100, 20);
			lblAdvanced1.setBounds(9, 293, 200, 20);
			lblAdvanced2.setBounds(123, 293, 200, 20);
			outputPanel.add(lblSelectTheBicluster1);
			outputPanel.add(lblSelectTheBicluster2);
			outputPanel.add(lblAdvanced1);
			outputPanel.add(lblAdvanced2);

			listScroller.setBounds(10, 111, 70, 175);
			listScroller.setPreferredSize(new Dimension(70, 175));
			listScroller2.setBounds(104, 111, 70, 175);
			listScroller2.setPreferredSize(new Dimension(70, 175));
			PanelGraph.setBounds(220, 20, 408, 285);
			PanelGraph2.setBounds(220, 20, 408, 285);			
			outputPanel.add(PanelGraph);
			outputPanel.add(PanelGraph2);
			outputPanel.add(listScroller);
			outputPanel.add(listScroller2);
		}
	}
	
	public void runBicPAMS(){
		
		bics=new Biclusters();		
		data.indexes = CopyUtils.copyIntList(fixedIndexes);
		data.scores = CopyUtils.copyDoubleList(fixedScores);
		BiclusterMiner bicminer = null;	
		int nrIterations=1;

		/** E1: Preprocessing **/

		try {
			//Normalizer
			NormalizationCriteria normCriteria=null;
			String aux = (String) Cnormalizer.getSelectedItem();
			if(aux.equals("Overall")) normCriteria = NormalizationCriteria.Overall; 
			else if(aux.equals("Column")) normCriteria = NormalizationCriteria.Column; 
			else if(aux.equals("Row")) normCriteria = NormalizationCriteria.Row; 
			else if(aux.equals("None")) normCriteria = NormalizationCriteria.None; 
			
			//Discretizer
			DiscretizationCriteria discCriteria=null;
			aux = (String) Cdiscretizer.getSelectedItem();
			if(aux.equals("Gaussian")) discCriteria = DiscretizationCriteria.NormalDist; 
			else if(aux.equals("FixedRange")) discCriteria = DiscretizationCriteria.SimpleRange; 
			else if(aux.equals("None")) discCriteria = DiscretizationCriteria.None; 
			
			//# Items
			String stringnritems = CNrItems.getText();
			int nritems = Integer.parseInt(stringnritems);

			//Is Symmetric?
			aux = (String) CSymmetric.getSelectedItem();
			boolean symmetric;
			if(aux.equals("Yes")) symmetric = true;
			else symmetric = false;

			//Missings Handler
			FillingCriteria missHandler=null;
			aux = (String) Cmissingshandler.getSelectedItem();
			if(aux.equals("Replace")) missHandler = FillingCriteria.Replace;
			else missHandler = FillingCriteria.RemoveValue; //"Remove"
			
			//Noise Relaxation
			NoiseRelaxation noiserelaxation=null;
			aux = (String) Cnoisehandler.getSelectedItem();
			if(aux.equals("MultiItem")) noiserelaxation = NoiseRelaxation.OptionalItem;
			else noiserelaxation = NoiseRelaxation.None; //"None"
			 
			//Removals
			String removals = (String) CRemoveElem.getSelectedItem();
			List<Integer> remItems = RemovalUtils.getItemsToRemove(nritems,removals,symmetric);

			//Scalability
			aux = (String) CSca.getSelectedItem();
			sca = aux.equals("Yes");

			//Orientation
			orientation=null;
			aux = (String) Corientation.getSelectedItem();
			if(aux.equals("Rows")) orientation = Orientation.PatternOnRows;
			else orientation = Orientation.PatternOnColumns; 

			System.out.println("Data statistics:\n"+data.getStatistics());
			System.out.println("ITEMIZER:\n#Items:"+nritems+"\nSymmetric:"+symmetric+"\nNorm:"+normCriteria+"\nDisc:"+discCriteria
					+"\nNoise:"+noiserelaxation+"\nMissing:"+missHandler+"\nRemovals:"+remItems+"\nScalability:"+sca);
			
			data.symmetry = symmetric;
			data = Itemizer.run(data, nritems, symmetric, normCriteria, discCriteria, noiserelaxation, missHandler, sca, orientation);
			System.out.println("Removals:"+remItems);
			data = ItemMapper.remove(data,remItems);
			
		} catch (Exception e1) {
			e1.printStackTrace();
			String errorMessage = "Unfortunately we were not able to finish your request.<br><em>Reason:</em> "
						+ "Unexpected problem during the preprocessing of the inputted data. Please verify if the values of the uploaded data file are preventing the selected preprocessing options (e.g. normalization applied over rows/columns/nodes where all values are missing).<br>"
						+ "If the problem persists, please contact us with the inputted parameters and data (if not private) to: <em>rmch AT tecnico.ulisboa.pt</em>. We will get back to you as soon as possible.";
			errorDisplay(errorMessage);
			return;
		}

		
		/** E2: Postprocessing **/
		
		try {
			//Merging
			double h = 100.000;
			MergingStrategy merger=null;
			String aux = (String) Cmerger.getSelectedItem();
			if(aux.equals("Heuristic")) merger = MergingStrategy.Heuristic;
			else if(aux.equals("Combinatorial")) merger = MergingStrategy.Combinatorial;
			else if(aux.equals("FIM")) merger = MergingStrategy.FIMOverall;
			double minoverlapping = (Double.parseDouble(COverlapping.getText()))/h;
			
			//Filtering
			FilteringCriteria filter=null;
			aux = (String) Cfilter.getSelectedItem();
			if(aux.equals("Dissimilar Elements")) filter = FilteringCriteria.Overall; 
			else if(aux.equals("Dissimilar Rows")) filter = FilteringCriteria.Rows; 
			else if(aux.equals("Dissimilar Columns")) filter = FilteringCriteria.Columns; 
			double minfiltering = (Double.parseDouble(CstringFilter.getText()))/h;

			Biclusterizer bichandler = new Biclusterizer(new BiclusterMerger(minoverlapping),
														 new BiclusterFilter(filter,1.0-minfiltering));

			
			/** E3: Mining step **/
			
			//Coherency
			PatternType coherency=null;
			aux = (String) Ccoherency.getSelectedItem();
			if(aux.equals("Constant")) coherency = PatternType.Constant; 
			else if(aux.equals("Order Preserving")) coherency = PatternType.OrderPreserving; 
			else if(aux.equals("Constant Overall")) coherency = PatternType.ConstantOverall; 
			else if(aux.equals("Additive")) coherency = PatternType.Additive; 
			else if(aux.equals("Multiplicative")) coherency = PatternType.Multiplicative; 
			else if(aux.equals("Symmetric")) coherency = PatternType.Symmetric;
			
			//Pattern miner
			PM pminer = null;
			if(coherency == PatternType.OrderPreserving){
				pminer = new SequentialPM();
				aux = (String) CFIMSPMPattMin.getSelectedItem();
				if(aux.equals("PrefixSpan")) ((SequentialPM)pminer).algorithm = SequentialImplementation.PrefixSpan; 
				else if(aux.equals("IndexSpan")) ((SequentialPM)pminer).algorithm = SequentialImplementation.IndexSpan; 
				else if(aux.equals("BidePlus")) ((SequentialPM)pminer).algorithm = SequentialImplementation.BIDEPlus; 
				System.out.println(">>"+((SequentialPM)pminer).algorithm.toString());
			} else{
				aux = (String) CPatternRep.getSelectedItem();
				String aux2 = (String) CFIMSPMPattMin.getSelectedItem();
				//System.out.println("AUX2:"+aux2);
				if(aux.equals("Simple")){
					pminer = new SimpleFIM();
					if(aux2.equals("Eclat")) ((SimpleFIM)pminer).algorithm = SimpleImplementation.Vertical;
					else if(aux2.equals("F2G")) ((SimpleFIM)pminer).algorithm = SimpleImplementation.F2G;
					else if(aux2.equals("AprioriTID")) ((SimpleFIM)pminer).algorithm = SimpleImplementation.Apriori; //"AprioriTID"
				}
				if(aux.contains("Closed")){
					pminer = new ClosedFIM();
					if(aux2.equals("CharmTID")) ((ClosedFIM)pminer).algorithm = ClosedImplementation.Charm;
					else if(aux2.equals("CharmDiffsets")) ((ClosedFIM)pminer).algorithm = ClosedImplementation.DCharm; 
					else if(aux2.equals("AprioriTID")) ((ClosedFIM)pminer).algorithm = ClosedImplementation.AprioriTID;
				}
				if(aux.equals("Maximal")){
					pminer = new MaximalFIM();
					//if(aux2.equals("CharmMFI")
						((MaximalFIM)pminer).algorithm = MaximalImplementation.CharmMFI; 
				}
			}

			//Numero minimo de colunas por bicluster
			aux = (String) CMinColBic.getText();
			int minColsBic = Integer.parseInt(aux);

			//Stopping Criteria
			String criteria = (String) CcomboStopCriteria.getSelectedItem();
			aux = (String) CcomboStopCriteria.getSelectedItem();
			int stopCriteriaInt = Integer.parseInt(CstringStopCriteria.getText());
			double stopCriteriaDouble = (Double.parseDouble(CstringStopCriteria.getText()))/h;

			pminer.inputMinColumns(minColsBic);
			if(criteria.contains("Min. #Bics")){
				pminer.inputMinNrBics(stopCriteriaInt);
			} else if(criteria.contains("Min. Area")){
				pminer.inputMinArea(stopCriteriaDouble);
			} else if(criteria.equals("Min. Support")){
				pminer.setSupport(stopCriteriaDouble);
			}

			//#Iterations
			aux = (String) CnumberIter.getText();
			nrIterations = Integer.parseInt(aux);
								
			System.out.println("MinCols:"+minColsBic+"\nStopCriteria:"+criteria+"\nValue:"+stopCriteriaDouble+"\nIterations:"+nrIterations);
			System.out.println("Coherency:"+coherency+"\nOrientation:"+orientation+"\nPMiner:"+pminer.toString());
			System.out.println("Merging:"+minoverlapping+"\nFiltering:"+filter.toString()+"\nValue:"+minfiltering);

			/** F: Run BicPAM **/		
			if(coherency == PatternType.OrderPreserving){
				bicminer = new OrderPreservingBiclusterMiner(data,(SequentialPM)pminer,bichandler,orientation);
			} else if(coherency == PatternType.Constant){
				bicminer = new ConstantBiclusterMiner(data,pminer,bichandler,orientation); 
			} else if(coherency == PatternType.ConstantOverall){
				bicminer = new ConstantOverallBiclusterMiner(data,pminer,bichandler,orientation); 
			} else if(coherency == PatternType.Additive){
				bicminer = new AdditiveBiclusterMiner(data,pminer,bichandler,orientation); 
			} else if(coherency == PatternType.Multiplicative){
				bicminer = new MultiplicativeBiclusterMiner(data,pminer,bichandler,orientation); 
			} else if(coherency == PatternType.Symmetric){
				bicminer = new SymmetricBiclusterMiner(data,pminer,bichandler,orientation); 
			} 
		} catch(Exception e){
	   		String errorMessage = "There was an unexpected problem with the processing of the inputted parameters.<br>"
	   				+"We deeply appreciate if you could report the problem to <em>rmch AT tecnico.ulisboa.pt</em> with a printscreen of your request.<br>We will get back to you as soon as possible.";
	   		errorDisplay(errorMessage);
			return;
		}
		
		try {
			long time = System.currentTimeMillis();
			List<List<Integer>> originalScores = CopyUtils.copyIntList(data.intscores);
			List<List<Integer>> originalIndexes = CopyUtils.copyIntList(data.indexes);

			double removePercentage = 0.3;
			for(int i=0; i<nrIterations; i++){
				Biclusters iBics = bicminer.mineBiclusters();
				if(iBics.size()==0){
					String errorMessage = "No biclusters were found in the data that satisfy the inputted parameters.<br>"
							+"<em>Solution #1:</em> relax your stopping criteria.<br>"
			   				+"<em>Solution #2:</em> change the selected coherency and quality.<br>"
			   				+"If the problem persists, you can also contact the authors to <em>rmch AT tecnico.ulisboa.pt</em> with the input data and a printscreen of your request.";
					errorDisplay(errorMessage);
					return;
				}
				data.remove(iBics.getElementCounts(),removePercentage);
				bicminer.setData(data);
				bics.addAll(iBics);
			}
			data.indexes = originalIndexes;
			data.intscores = originalScores;
			time = System.currentTimeMillis() - time;
			
		} catch (Exception e) {
			String errorMessage = "Unfortunately we were not able to finish your request.<br><em>Reason:</em><br>"
					+"Unexpected problem during the mining or postprocessing stages of BicPAMS.<br>"
	   				+"This might be a problem caused by the current version of BicPAMS (e.g. non-supported combination of parameters).<br>"
					+"We appreciate if you could contact us informing the problem (with the inputted parameters and data (if not private)) to: <em>rmch AT tecnico.ulisboa.pt</em>. We will get back to you as soon as possible.<br><br>"
					+"<b>Additional information:</b> Your request was successfully interpreted and the data was successfully upload and preprocessed.";
	   		errorDisplay(errorMessage);
			e.printStackTrace();
			return;
		}
		displayBicSolution();
	}
	
	private void createMenuBars() {
		
		/** B1: Add menu bars */
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		final Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
		
		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);
		JMenuItem MLoadDataMatrix = new JMenuItem("Load Data Matrix");
		mnFile.add(MLoadDataMatrix);
		JMenuItem MLoadNetwork = new JMenuItem("Load Network");
		mnFile.add(MLoadNetwork);
		MSaveBiclusters = new JMenuItem("Save Biclusters");
		MSaveBiclusters.setEnabled(false);
		mnFile.add(MSaveBiclusters);
		JMenuItem MLoadSolution = new JMenuItem("Load Solution");
		mnFile.add(MLoadSolution);
		
		JMenu mnHelp = new JMenu("Help");
		menuBar.add(mnHelp);
		JMenuItem mntmTutorial = new JMenuItem("Tutorial"); 
		mnHelp.add(mntmTutorial);
		JMenuItem mntmContact = new JMenuItem("Contact BicPAMS team");
		mnHelp.add(mntmContact);
		
		JMenu mnOthers = new JMenu("Others");
		menuBar.add(mnOthers);
		JMenuItem mntmAbout = new JMenuItem("About BicPAMS");
		mnOthers.add(mntmAbout);
		JMenuItem mntmCases = new JMenuItem("Testing cases");
		mnOthers.add(mntmCases);
		//JMenuItem mntmOnline = new JMenuItem("Online interface");
		JMenuItem mntmInterface = new JMenuItem("Programmatic interface");
		mnOthers.add(mntmInterface);
		

		/** B2: Add frames to input net indexes and directionality */
		
		final JFrame indexesFrame = new JFrame();
		indexesFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		indexesFrame.setTitle("Column Indexes");
		JPanel indexesPanel = new JPanel();
		indexesFrame.getContentPane().add(indexesPanel);
		JLabel LC1 = new JLabel("First node:"), LC2 = new JLabel("Second node:"), LC3 = new JLabel("Score:");
		final JTextField CCol1 = new JTextField("0"), CCol2 = new JTextField("1"), CCol3 = new JTextField("2");
		JLabel lblHomoCriteria = new JLabel("Bidirectional: ");
		final JComboBox CcomboHomoCriteria=new JComboBox(new DefaultComboBoxModel(new String[] {"Yes","No"}));
		JButton loadNetworkButton = new JButton("OK");
		if(flexible){
			indexesPanel.setLayout(new GridBagLayout());
			indexesFrame.setSize(new Dimension(200, 180));
			GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.BOTH;
            c.weighty = 1;
            c.gridx = 1; c.gridwidth = 1; 
            c.gridy = 1; indexesPanel.add(LC1,c);
			c.gridy = 2; indexesPanel.add(LC2,c);
			c.gridy = 3; indexesPanel.add(LC3,c);
			c.gridy = 4; indexesPanel.add(lblHomoCriteria,c);
            c.gridx = 2; c.gridwidth = 1; 
			c.gridy = 1; indexesPanel.add(CCol1,c);
			c.gridy = 2; indexesPanel.add(CCol2,c);
			c.gridy = 3; indexesPanel.add(CCol3,c);
			c.gridy = 4; indexesPanel.add(CcomboHomoCriteria,c);
            c.gridx = 1; c.gridwidth = 2; 
			c.gridy = 6; indexesPanel.add(loadNetworkButton,c);
		} else {
			indexesPanel.setBounds(1, 1, 418, 133);
			indexesFrame.setBounds(200, 200, 178, 172);
			LC1.setBounds(10, 10, 90, 20);
			CCol1.setBounds(105,12,52,20);
			LC2.setBounds(9, 32, 90, 20);
			CCol2.setBounds(105,34,52,20);
			LC3.setBounds(9, 54, 90, 20);
			CCol3.setBounds(105,56,52,20);
			lblHomoCriteria.setBounds(9, 76, 140, 20);
			CcomboHomoCriteria.setBounds(106, 79, 50, 20);
			loadNetworkButton.setBounds(96,102,60,25);
			indexesPanel.add(LC1);
			indexesPanel.add(CCol1);
			indexesPanel.add(LC2);
			indexesPanel.add(CCol2);
			indexesPanel.add(LC3);
			indexesPanel.add(CCol3);
			indexesPanel.add(lblHomoCriteria);
			indexesPanel.add(CcomboHomoCriteria);
			indexesPanel.add(loadNetworkButton);
		}
		loadNetworkButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				n1 = Integer.valueOf(CCol1.getText());
				n2 = Integer.valueOf(CCol2.getText());
				n3 = Integer.valueOf(CCol3.getText());
				String aux = (String) CcomboHomoCriteria.getSelectedItem();
				if(aux.equals("Yes")) bidir=true;
				indexesFrame.dispose();
				try {
					dataRead(true,filePath);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		/** B3: Add menu bar File>Load */
		
		final JFileChooser fc = new JFileChooser();		
		MLoadDataMatrix.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				int returnVal = fc.showOpenDialog(GraphicalInterface.this);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					File file = fc.getSelectedFile();
					try {
						dataRead(false,file.getPath());
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		});		
		MLoadNetwork.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				int returnVal = fc.showOpenDialog(GraphicalInterface.this);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					File file = fc.getSelectedFile();
					filePath = file.getPath();
					indexesFrame.setVisible(true);
				}
			}
		});		
		MLoadSolution.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(data == null){
					JOptionPane.showMessageDialog(null,"Before uploading the biclustering solution, please upload the corresponding data in order to guarantee a proper display of biclusters.","Warning", JOptionPane.WARNING_MESSAGE);
					return;
				}
				int returnVal = fc.showOpenDialog(GraphicalInterface.this);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					File file = fc.getSelectedFile();
					try {
						bics = BicReader.readBicPAMBics(file.toString(),true);
						displayBicSolution();
					} catch (Exception e) {
						e.printStackTrace();
						errorDisplay("Error while importing the biclustering solution. Please make sure that the biclustering solution properly follows the BicPAMS format or it was exported using the BicPAMS tool.");						
					}
				}
			}
		});		

		
		/** B4: Add menu help, about, contact */
		
		mntmTutorial.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				JTextPane textTutorial = new JTextPane();
				textTutorial.setContentType("text/html");
				textTutorial.setText(tutorialText.replace("width:365px", "width:550px"));//+"<div style=\"width:365px;\">","").replace("</div></html>","")+"</html>");
				textTutorial.setEditable(false);
				textTutorial.setCaretPosition(0);
				textTutorial.addHyperlinkListener(new HyperlinkListener(){
					public void hyperlinkUpdate(HyperlinkEvent hle) {
				        if (HyperlinkEvent.EventType.ACTIVATED.equals(hle.getEventType())) {
				            try { desktop.browse(new URI(hle.getURL().toString())); }
				            catch (Exception ex) { ex.printStackTrace(); }
				        }
				    }
				});
				JScrollPane scrollPaneTutorial = new JScrollPane(textTutorial);
				scrollPaneTutorial.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
				scrollPaneTutorial.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
				scrollPaneTutorial.setPreferredSize(new Dimension(950,600));
				JOptionPane.showMessageDialog(null, scrollPaneTutorial, "Tutorial", JOptionPane.PLAIN_MESSAGE);
			}
		});		
		mntmContact.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				JTextPane textTutorial = new JTextPane();
				textTutorial.setContentType("text/html");
				textTutorial.setText("<html>"+contactUs+"</html>");
				textTutorial.setEditable(false);
				textTutorial.addHyperlinkListener(new HyperlinkListener(){
					public void hyperlinkUpdate(HyperlinkEvent hle) {
				        if (HyperlinkEvent.EventType.ACTIVATED.equals(hle.getEventType())) {
				            try { desktop.browse(new URI(hle.getURL().toString())); }
				            catch (Exception ex) { ex.printStackTrace(); }
				        }
				    }
				});
				JScrollPane scrollPaneTutorial = new JScrollPane(textTutorial);
				scrollPaneTutorial.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
				scrollPaneTutorial.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
				scrollPaneTutorial.setPreferredSize(new Dimension(900,200));
				JOptionPane.showMessageDialog(null, scrollPaneTutorial, "Contact Us", JOptionPane.PLAIN_MESSAGE);
			}
		});		
		mntmAbout.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				JTextPane textTutorial3 = new JTextPane();
				textTutorial3.setContentType("text/html");
				textTutorial3.setText("<html>"+aboutBicPAMS+"</html>");
				textTutorial3.setEditable(false);
				textTutorial3.addHyperlinkListener(new HyperlinkListener(){
					public void hyperlinkUpdate(HyperlinkEvent hle) {
				        if (HyperlinkEvent.EventType.ACTIVATED.equals(hle.getEventType())) {
				            try { desktop.browse(new URI(hle.getURL().toString())); }
				            catch (Exception ex) { ex.printStackTrace(); }
				        }
				    }
				});
				JScrollPane scrollPaneTutorial = new JScrollPane(textTutorial3);
				scrollPaneTutorial.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
				scrollPaneTutorial.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
				scrollPaneTutorial.setPreferredSize(new Dimension(1000,500));
				JOptionPane.showMessageDialog(null, scrollPaneTutorial, "About BicPAMS", JOptionPane.PLAIN_MESSAGE);
			}
		});
		mntmCases.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				JTextPane textTutorial = new JTextPane();
				textTutorial.setContentType("text/html");
				textTutorial.setText("<html>"+testCases+"</html>");
				textTutorial.setEditable(false);
				textTutorial.addHyperlinkListener(new HyperlinkListener(){
					public void hyperlinkUpdate(HyperlinkEvent hle) {
				        if (HyperlinkEvent.EventType.ACTIVATED.equals(hle.getEventType())) {
				            try { desktop.browse(new URI(hle.getURL().toString())); }
				            catch (Exception ex) { ex.printStackTrace(); }
				        }
				    }
				});
				JScrollPane scrollPaneTutorial = new JScrollPane(textTutorial);
				scrollPaneTutorial.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
				scrollPaneTutorial.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
				scrollPaneTutorial.setPreferredSize(new Dimension(600,160));
				JOptionPane.showMessageDialog(null, scrollPaneTutorial, "Case Tests", JOptionPane.PLAIN_MESSAGE);
			}
		});
		/*mntmOnline.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
			    if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
			        try {
			            desktop.browse(new URI("http://web.ist.utl.pt/rmch/bicpams/"));
			        } catch (Exception e) {
			            e.printStackTrace();
			        }
			    }
			}
		});*/
		mntmInterface.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
			    if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
			        try {
			            desktop.browse(new URI("http://web.ist.utl.pt/rmch/bicpams/"));
			        } catch (Exception e) {
			            e.printStackTrace();
			        }
			    }
			}
		});
	}

	private void dataRead(boolean network, String path_global){
		String fileErrorMessage = "Unfortunately we were not able to finish your request.<br><em>Reason:</em><br><b>File error:</b> we were not able to process your request due to an error associated with the loading of the inputted data.<br><b>Description:</b> ";
		fileErrorMessage += "Exception while reading the input data file. Please guarantee that the format conforms to the accepted rules described in tutorial.<br>";
		try {
			if(network){
				String delimiter = BicReader.detectDelimeter(path_global);
				data = NetMatrixMapper.getTable(path_global, n1, n2, n3, delimiter, bidir);
			} else if(path_global.contains(".arff")){
				data = new Dataset(BicReader.getInstances(path_global));
			} else {
				String delimiter = BicReader.detectDelimeter(path_global);
				List<String> conds = BicReader.getConds(path_global,1,delimiter); 
				List<String> genes = BicReader.getGenes(path_global,delimiter); 
				data = new Dataset(conds,genes,BicReader.getTable(path_global,3,delimiter));
			}
			if(data.rows.size()<3 || data.indexes.size()<3 || data.scores.size()<3){
				errorDisplay(fileErrorMessage);
				return;
			}
			fixedIndexes = CopyUtils.copyIntList(data.indexes);
			fixedScores = CopyUtils.copyDoubleList(data.scores);
			if(data.min<0) CSymmetric.setSelectedItem("Yes");
			else CSymmetric.setSelectedItem("No");
			if(network && data.rows.size()>20000) CSca.setSelectedItem("Yes");
			else if(!network && data.columns.size()>500) CSca.setSelectedItem("Yes");
			else CSca.setSelectedItem("No");
			BRun.setEnabled(true);
		} catch (Exception e1) {
			errorDisplay(fileErrorMessage);
			e1.printStackTrace();
			return;
		}
	}

	private void displayBicSolution(){
		try {
			int n = bics.size();
			if(n>0) mainPanel.setEnabledAt(1, true);

			bicCombo = false;				
			String[] list = new String[n];
			for (int i=0; i<n;i++) list[i]="Bic "+Integer.toString(i+1);
			CcomboBic1.setListData(list);
			CcomboBic2.setListData(list);
			
			bicCombo = true;
			mainPanel.setSelectedIndex(1);
			JOptionPane.showMessageDialog(null,n+" biclusters found!");

			MSaveBiclusters.setEnabled(true);
			MSaveBiclusters.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					JFileChooser fc = new JFileChooser();
					fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
					fc.setSelectedFile(new File("result.htm"));
					int returnVal = fc.showSaveDialog(GraphicalInterface.this);
					if (returnVal == JFileChooser.APPROVE_OPTION) {
						File file = fc.getSelectedFile();
						StringBuffer resultTxt = new StringBuffer("<b>Input:</b> "+data.getStatistics());
						resultTxt.append("<br><br><b>Overview</b><br>Number of biclusters: <b>"+bics.size());
						resultTxt.append("</b> with sizes {"+ResultDisplay.toSummaryTable(bics)+"}");
						resultTxt.append("<br><br><b>Biclusters (row/column indexes):</b> "+ResultDisplay.toIndexesTable(bics));
						List<String> cols = data.columns, rows = data.rows;
			   		    if(sca && data.originalColumns!=null){
			   		    	if(orientation.equals(Orientation.PatternOnRows)) cols = data.originalColumns;
			   		    	else rows = data.originalColumns;
			   		    }
						resultTxt.append("<br><br><b>Biclusters (row/column names):</b> "+ResultDisplay.toNamesTable(rows,cols,bics));
						try {
							ResultDisplay.writeFile(file.getName(),"<html>"+resultTxt.toString()+"</html>",file.getPath());
						} catch (Exception e1) {
							e1.printStackTrace();
						}
					}	
				}
			});
		} catch (Exception e) {
			String errorMessage = "Unfortunately we were not able to finish your request.<br><em>Reason:</em><br><b>File error:</b> we were not able to process your request due to an error associated with the loading of the inputted data.<br><b>Description:</b> "
					+"<b>Display error:</b> your query was computed successfully (#"+bics.size()+" biclusters)! However, there was an unexpected problem during the display of results.<br>"
	   				+"Please try again. If the problem persists, since it is highly likely to be related with a problem with the version on our remote server, we deeply appreciate if you can contact us <em>rmch AT tecnico.ulisboa.pt</em> with a copy of the inputted data and parameters.<br>We will get back to you as soon as possible.";
			errorDisplay(errorMessage);
			e.printStackTrace();
			return;
		}
	}
	
	private void errorDisplay(String errorMessage) {
		JTextPane output = new JTextPane();
		output.setContentType("text/html");
		output.setText("<html>"+errorMessage+"</html>");
		output.setEditable(true);
		output.setCaretPosition(0);
		JScrollPane scrollPaneoutput = new JScrollPane(output);
		scrollPaneoutput.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPaneoutput.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPaneoutput.setPreferredSize(new Dimension(400,300));
		JOptionPane.showMessageDialog(null, scrollPaneoutput, "Error", JOptionPane.PLAIN_MESSAGE);
	}

	private void messageDisplay(String message, String title, Dimension dim, boolean html, String filename) {
		JTextPane output = new JTextPane();
		if(html){
			output.setContentType("text/html");
			output.setText("<html>"+message+"</html>");
		} else output.setText(message);
			
		output.setEditable(true);
		output.setCaretPosition(0);
		JScrollPane scrollPaneoutput = new JScrollPane(output);
		scrollPaneoutput.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPaneoutput.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPaneoutput.setPreferredSize(dim);
		int dialogResult = JOptionPane.showOptionDialog(null, scrollPaneoutput, title, JOptionPane.YES_OPTION, JOptionPane.PLAIN_MESSAGE, null, new String[]{"SAVE","CLOSE"}, "default");
		if(dialogResult == JOptionPane.YES_OPTION){
				JFileChooser fcnet = new JFileChooser();
				fcnet.setFileSelectionMode(JFileChooser.FILES_ONLY);
				fcnet.setSelectedFile(new File(filename));
				int returnVal = fcnet.showSaveDialog(this);
				if(returnVal == JFileChooser.APPROVE_OPTION){
					try {
						BufferedWriter file = new BufferedWriter(new FileWriter(fcnet.getSelectedFile()));
						if(html) file.write("<html>"+message+"</html>");
						else file.write(message);
				        file.close();
						JOptionPane.showMessageDialog(this, new JLabel("Output successfully saved"),"Success",JOptionPane.PLAIN_MESSAGE);
					} catch (IOException e) {
						JOptionPane.showMessageDialog(this, new JLabel("We were not able to save the output. Please contact the BicPAMS team!"),"Saving outputs...",JOptionPane.ERROR_MESSAGE);
						e.printStackTrace();
					}
				}
		}
	}
	
	public static JLabel linkify(final String text, String URL, String toolTip) throws Exception {
	    final URI uri = new URI(URL);
	    final JLabel link = new JLabel();
	    link.setText("<HTML><FONT color=\"#000099\">"+text+"</FONT></HTML>");
	    if(!toolTip.equals("")) link.setToolTipText(toolTip);
	    link.setCursor(new Cursor(Cursor.HAND_CURSOR));
	    link.addMouseListener(new MouseListener(){
	        public void mouseExited(MouseEvent arg0){
	            link.setText("<HTML><FONT color=\"#000099\">"+text+"</FONT></HTML>");
	        }
	        public void mouseEntered(MouseEvent arg0){
	            link.setText("<HTML><FONT color=\"#000099\"><U>"+text+"</U></FONT></HTML>");
	        }
	        public void mouseClicked(MouseEvent arg0){
	            if (Desktop.isDesktopSupported()){
	                try{
	                    Desktop.getDesktop().browse(uri);
	                }catch (Exception e){
	                    e.printStackTrace();
	                }
	            } else {
	                JOptionPane pane = new JOptionPane("Could not open link.");
	                JDialog dialog = pane.createDialog(new JFrame(), "");
	                dialog.setVisible(true);
	            }
	        }
	        public void mousePressed(MouseEvent e){}
	        public void mouseReleased(MouseEvent e){}
	    });
	    return link;
	}
	
	public class MyInputVerifier extends InputVerifier {
		
		int min=0, max=100;		
		public MyInputVerifier(int _min, int _max){
			super();
			min=_min;
			max=_max;
		}
		
	    @Override
	    public boolean verify(JComponent input) {
	        String text = ((JTextField)input).getText();
	        try {
	            int value = Integer.valueOf(text);
	            return value<=max && value>=min; 
	        } catch (NumberFormatException e) {
	            return false;
	        }
	    }
	}
	
	protected String open(){
		return "<div style=\"width:365px;\">"; 
	}
	protected String close(){
		return "</div>"; 
	}
}
