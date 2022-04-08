package bicpam.pminer.fim.fpgrowth.weka;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import weka.associations.*;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;
import weka.core.Utils;

/**Class adapting the FP-growth algorithm to enable the retrieval of transactions. Based on:
 * J. Han, J.Pei, Y. Yin: Mining frequent patterns without candidate generation. In: Proceedings of the 2000 ACM-SIGMID International Conference on Management of Data, 1-12, 2000.
 * BibTeX:
 * &#64;inproceedings{Han2000,
 *    author = {J. Han and J.Pei and Y. Yin},
 *    booktitle = {Proceedings of the 2000 ACM-SIGMID International Conference on Management of Data},
 *    pages = {1-12},
 *    title = {Mining frequent patterns without candidate generation},
 *    year = {2000}
 * }
 * @author Rui Henriques
 * @version 1.0
 */
public class WFPGrowthTID extends AbstractAssociator {
  
  private static final long serialVersionUID = 3620717108603442911L;
  public void setData(List<List<Integer>> dataI) {}
  
  /* =========================================== */  
  /* ========== FrequentBinaryItemSet ========== */
  /* =========================================== */  
  public static class FrequentBinaryItemSet implements Serializable, Cloneable {
    
    private static final long serialVersionUID = -6543815873565829448L;
    protected ArrayList<BinaryItem> m_items = new ArrayList<BinaryItem>();
    protected int m_support;

    public FrequentBinaryItemSet(ArrayList<BinaryItem> items, int support) {
      m_items = items;
      m_support = support;
      Collections.sort(m_items);
    }
    
    public void addItem(BinaryItem i) {
      m_items.add(i);
      Collections.sort(m_items);
    }
    
    public void setSupport(int support) { m_support = support; }
    public int getSupport() { return m_support; }
    public Collection<BinaryItem> getItems() { return m_items; }
    public BinaryItem getItem(int index) { return m_items.get(index); }
    public int numberOfItems() { return m_items.size(); }
    
    public String toString() {
      StringBuffer buff = new StringBuffer();
      Iterator<BinaryItem> i = m_items.iterator();
      while (i.hasNext()) buff.append(i.next().toString() + " ");        
      buff.append(": " + m_support);
      return buff.toString();
    }
    
    public Object clone() {
      ArrayList<BinaryItem> items = new ArrayList<BinaryItem>(m_items);
      return new FrequentBinaryItemSet(items, m_support);
    }
  }  
  
  /* =========================================== */  
  /* ============ FrequentItemSets ============= */
  /* =========================================== */  
  public static class FrequentItemSets implements Serializable {
    
    private static final long serialVersionUID = 4173606872363973588L;

    protected ArrayList<FrequentBinaryItemSet> m_sets = new ArrayList<FrequentBinaryItemSet>();
    protected int m_numberOfTransactions;

    public FrequentItemSets(int numTransactions) { m_numberOfTransactions = numTransactions; }
    public FrequentBinaryItemSet getItemSet(int index) { return m_sets.get(index); }
    public Iterator<FrequentBinaryItemSet> iterator() { return m_sets.iterator(); }
    public int getNumberOfTransactions() { return m_numberOfTransactions; }
    public void addItemSet(FrequentBinaryItemSet setToAdd) { m_sets.add(setToAdd); }
    public void sort(Comparator<FrequentBinaryItemSet> comp) { Collections.sort(m_sets, comp); }
    public int size() { return m_sets.size(); }
    
    public void sort() {
      Comparator<FrequentBinaryItemSet> compF = new Comparator<FrequentBinaryItemSet>() {
        public int compare(FrequentBinaryItemSet one, FrequentBinaryItemSet two) {
          Collection<BinaryItem> compOne = one.getItems();
          Collection<BinaryItem> compTwo = two.getItems();
            if (compOne.size() < compTwo.size()) return -1;
            else if (compOne.size() > compTwo.size()) return 1;
            else { // compare items
              Iterator<BinaryItem> twoIterator = compTwo.iterator();
              for (BinaryItem oneI : compOne) {
                BinaryItem twoI = twoIterator.next();
                int result = oneI.compareTo(twoI);
                if (result != 0) return result;
              }
              return 0; // equal
            }
        }
      };
      sort(compF);
    }
    
    public String toString(int numSets) {
      if (m_sets.size() == 0) return "No frequent items sets found!";
      StringBuffer result = new StringBuffer();
      result.append("" + m_sets.size() + " frequent item sets found");
      if (numSets > 0) result.append(" , displaying " + numSets);
      result.append(":\n\n");
      
      int count = 0;
      for (FrequentBinaryItemSet i : m_sets) {
        if (numSets > 0 && count > numSets) break;
        result.append(i.toString() + "\n");
        count++;
      }
      return result.toString();
    }
  }
  
  
  /* =========================================== */  
  /* ============== ShadowCounts =============== */
  /* =========================================== */  
  protected static class ShadowCounts implements Serializable {
	//This class holds the counts for projected tree nodes and header lists

    private static final long serialVersionUID = 4435433714185969155L;
    private ArrayList<Integer> m_counts = new ArrayList<Integer>();
    
    /**Get the count at the specified recursion depth.
     * @param recursionLevel the depth of the recursion.
     * @return the count.
     */
    public int getCount(int recursionLevel) {
      if (recursionLevel >= m_counts.size()) return 0;
      else return m_counts.get(recursionLevel);
    }
    
    /**Increase the count at a given recursion level.
     * @param recursionLevel the level at which to increase the count.
     * @param incr the amount by which to increase the count.
     */
    public void increaseCount(int recursionLevel, int incr) {
      // basically treat the list like a stack where we can add a new element, or increment the element at the top
      if (recursionLevel == m_counts.size()) m_counts.add(incr); // new element
      else if (recursionLevel == m_counts.size() - 1) { // otherwise increment the top
        int n = m_counts.get(recursionLevel).intValue();
        m_counts.set(recursionLevel, (n + incr));
      }
    }
    
    /**Remove the count at the given recursion level.
     * @param recursionLevel the level at which to remove the count.
     */
    public void removeCount(int recursionLevel) {
      if (recursionLevel < m_counts.size()) m_counts.remove(recursionLevel);
    }
  }

  
  /* =========================================== */  
  /* =============== FPTreeNode ================ */
  /* =========================================== */  
  public static class FPTreeNode implements Serializable {

    private static final long serialVersionUID = 4396315323673737660L;
    protected BinaryItem m_item;
    protected int m_ID; //for graphing the tree
    protected FPTreeNode m_levelSibling;
    protected FPTreeNode m_parent;
    protected List<Integer> m_transactions = new ArrayList<Integer>();
    protected Map<BinaryItem, FPTreeNode> m_children = new HashMap<BinaryItem, FPTreeNode>();
    
    /** counts associated with projected versions of this node */
    protected ShadowCounts m_projectedCounts = new ShadowCounts();
    
    /**Construct a new node with the given parent link and item.
     * @param parent a pointer to the parent of this node.
     * @param item the item at this node.
     */
    public FPTreeNode(FPTreeNode parent, BinaryItem item) {
      m_parent = parent;
      m_item = item;
    }
    
    /**Insert an item set into the tree at this node. Removes the first item from the supplied item set and makes a recursive call to insert the remaining items.
     * @param itemSet the item set to insert.
     * @param headerTable the header table for the tree.
     * @param incr the amount by which to increase counts.
     */
    public void addItemSet(Collection<BinaryItem> itemSet, Map<BinaryItem, FPTreeRoot.Header> headerTable, int transaction, int incr) {
      Iterator<BinaryItem> i = itemSet.iterator();
      if (i.hasNext()) {
        BinaryItem first = i.next();
        FPTreeNode aChild;
        if (!m_children.containsKey(first)) { 
          aChild = new FPTreeNode(this, first); // not in the tree, so add it.
          m_children.put(first, aChild);
          if (!headerTable.containsKey(first)) headerTable.put(first, new FPTreeRoot.Header()); // update the header
          headerTable.get(first).addToList(aChild); // append new node to header list
        } else aChild = m_children.get(first); // get the appropriate child node

        headerTable.get(first).getProjectedCounts().increaseCount(0, incr); // update counts in header table
        aChild.increaseProjectedCount(0, incr); // increase the child's count
        
        itemSet.remove(first); // proceed recursively        
        aChild.addItemSet(itemSet, headerTable, transaction, incr);
      } else m_transactions.add(transaction);
    }
    
    public void increaseProjectedCount(int recursionLevel, int incr) {
      m_projectedCounts.increaseCount(recursionLevel, incr);
    }    
    public void removeProjectedCount(int recursionLevel) {
      m_projectedCounts.removeCount(recursionLevel);
    }
    public int getProjectedCount(int recursionLevel) {
      return m_projectedCounts.getCount(recursionLevel);
    }
    public List<Integer> getItemLeafs(int recursion, int cursor, List<BinaryItem> items){
    	//for(int i=0;i<recursion;i++) System.out.print(" | ");
    	//System.out.println(m_item.getAttribute().index() + " cursor:" + items.get(cursor).getAttribute().index()); 
		List<Integer> trans = new ArrayList<Integer>();
    	if(items.get(cursor).equals(m_item)) cursor++;
        if(cursor < items.size()){
        	//System.out.println("NO" + cursor + " " + items.size());
        	if(m_children.isEmpty()) return trans; 
        	for(FPTreeNode child : m_children.values()) trans.addAll(child.getItemLeafs(recursion+1,cursor,items));
  	        /*if(!trans.isEmpty()){
  	        	System.out.println("NOOOO");
  	        	trans.addAll(m_transactions);
  	        }*/
   	    	return trans; 
        }
        else return addAllTransactions();
    }    
    public List<Integer> addAllTransactions(){
		List<Integer> trans = new ArrayList<Integer>();
    	trans.addAll(m_transactions);
    	for(FPTreeNode child : m_children.values()) trans.addAll(child.addAllTransactions());
    	return trans;
    }
    public void addTrans(int trans){ m_transactions.add(trans); }
    public List<Integer> getTransactions(){ return m_transactions; }
    public FPTreeNode getParent() { return m_parent; }
    public BinaryItem getItem() { return m_item; }    
    public String toString(int recursionLevel) { return toString("", recursionLevel); }

    /**Return a textual description of this node for a given recursion level.
     * @param prefix a prefix string to prepend.
     * @param recursionLevel the recursion level to use.
     * @return a textual description of this node. 
     */
    public String toString(String prefix, int recursionLevel) {
      StringBuffer buffer = new StringBuffer();
      buffer.append(prefix);
      buffer.append("|  ");
      buffer.append(m_item.getAttribute().index());//toString());
      buffer.append(" (");
      buffer.append(m_projectedCounts.getCount(recursionLevel));
      buffer.append(")");
      buffer.append("[");
      buffer.append(this.printTrans());
      buffer.append("]\n");
      for (FPTreeNode node : m_children.values()) buffer.append(node.toString(prefix + "|  ", recursionLevel));
      return buffer.toString();
    }
    
    protected String printTrans(){
        StringBuffer buffer = new StringBuffer();
    	for(Integer tid : m_transactions) buffer.append(tid + ",");//m_projectedCounts.getCount(recursionLevel)
    	for(FPTreeNode node : m_children.values()) buffer.append(node.printTrans());
    	return buffer.toString();
    }
    
    protected int assignIDs(int lastID) {
      int currentLastID = lastID + 1;
      m_ID = currentLastID;
      if (m_children != null) {
        Collection<FPTreeNode> kids = m_children.values();
        for (FPTreeNode n : kids) currentLastID = n.assignIDs(currentLastID);
      }
      return currentLastID;
    }
    
    /**Generate a dot graph description string for the tree.
     * @param text a StringBuffer to store the graph description in.
     */
    public void graphFPTree(StringBuffer text) {
      if (m_children != null) {
        Collection<FPTreeNode> kids = m_children.values();
        for (FPTreeNode n : kids) {
          text.append("N" + n.m_ID);
          text.append(" [label=\"");
          text.append(n.getItem().toString() + " (" + n.getProjectedCount(0) + ")\\n");
          text.append("\"]\n");
          n.graphFPTree(text);
          text.append("N" + m_ID + "->" + "N" + n.m_ID + "\n");
        }
      }
    }
  }
  
  /* =========================================== */  
  /* =============== FPTreeRoot ================ */
  /* =========================================== */  
  public static class FPTreeRoot extends FPTreeNode {
    
    private static final long serialVersionUID = 632150939785333297L;

    public static class Header implements Serializable {
      
      private static final long serialVersionUID = -6583156284891368909L;
      protected List<FPTreeNode> m_headerList = new LinkedList<FPTreeNode>();
      protected ShadowCounts m_projectedHeaderCounts = new ShadowCounts();
      
      public void addToList(FPTreeNode toAdd)  { m_headerList.add(toAdd); }
      public List<FPTreeNode> getHeaderList()  { return m_headerList; }
      public ShadowCounts getProjectedCounts() { return m_projectedHeaderCounts; }
    }
    
    /** Stores the header table as mapped Header entries */
    protected Map<BinaryItem, Header> m_headerTable = new HashMap<BinaryItem, Header>();
    
    public FPTreeRoot() {
      super(null, null);
    }
    
    public void addItemSet(Collection<BinaryItem> itemSet, int transaction, int incr) {
      super.addItemSet(itemSet, m_headerTable, transaction, incr);
    }
    public Map<BinaryItem, Header> getHeaderTable() {
      return m_headerTable;
    }
    
    public boolean isEmpty(int recursionLevel) {
      for (FPTreeNode c : m_children.values()) 
    	  if(c.getProjectedCount(recursionLevel) > 0) return false;
      return true;
    }
    
    /**Get a textual description of the tree at a given recursion (projection) level.
     * @param pad the string to use as a prefix for indenting nodes.
     * @param recursionLevel the recursion level (projection) to use.
     * @return the textual description of the tree.
     */
    public String toString(String pad, int recursionLevel) {
      StringBuffer result = new StringBuffer();
      result.append(pad);
      result.append("+ ROOT\n");
      for (FPTreeNode node : m_children.values())
        result.append(node.toString(pad + "|  ", recursionLevel));
      return result.toString();
    }
  }

  protected Instances source;
  protected FrequentItemSets m_largeItemSets;
  protected List<List<Integer>> m_TransactionSets;
  protected int m_maxItems = -1;
  protected double m_upperBoundMinSupport = 1.0;
  protected double m_lowerBoundMinSupport = 0.1;
  protected double m_delta = 0.05;
  protected int m_numInstances;
  protected int m_transCounter = 0;
  
  protected int m_offDiskReportingFrequency = 10000; // When processing data off of disk report progress this frequently (number of instances)
  protected int m_positiveIndex = 2; // The index (1 based) of binary attributes to treat as the positive value
  
  protected DefaultAssociationRule.METRIC_TYPE m_metric = DefaultAssociationRule.METRIC_TYPE.CONFIDENCE;
  protected double m_metricThreshold = 0.9;
  
  protected String m_transactionsMustContain = "";
  protected boolean m_mustContainOR = false;  

  public void reset() {
	  m_TransactionSets = null;
  }

  private boolean passesMustContain(Instance inst, boolean[] transactionsMustContainIndexes, int numInTransactionsMustContainList) {
    boolean result = false;
    if (inst instanceof SparseInstance) {
      int containsCount = 0;
      for (int i = 0; i < inst.numValues(); i++) {
        int attIndex = inst.index(i);
        
        if (m_mustContainOR) {
          if (transactionsMustContainIndexes[attIndex]) return true; // break here since the operator is OR and this instance contains at least one of the items
        } else if (transactionsMustContainIndexes[attIndex]) containsCount++;
      }
      if (!m_mustContainOR) if (containsCount == numInTransactionsMustContainList) return true;
    } else {
      int containsCount = 0;
      for (int i = 0; i < transactionsMustContainIndexes.length; i++) {
        if (transactionsMustContainIndexes[i]) {
          if ((int)inst.value(i) == m_positiveIndex - 1) {
            if (m_mustContainOR) return true; // break here since the operator is OR and this instance contains at least one of the requested items
            else containsCount++;
          }
        }
      }
      if (!m_mustContainOR) if (containsCount == numInTransactionsMustContainList) return true;
    }
    return result;
  }
  
  private void processSingleton(Instance current, ArrayList<BinaryItem> singletons) throws Exception {
    if (current instanceof SparseInstance) {
      for (int j = 0; j < current.numValues(); j++) {
        int attIndex = current.index(j);
        singletons.get(attIndex).increaseFrequency();
      }
    } else {
      for (int j = 0; j < current.numAttributes(); j++) {
        if (!current.isMissing(j)) 
          if (current.attribute(j).numValues() == 1 || current.value(j) == m_positiveIndex - 1) singletons.get(j).increaseFrequency();
      }
    }
  }
  
  /**Get the singleton items in the data
   * @param source the source of the data (either Instances or an ArffLoader).
   * @return a list of singleton item sets
   * @throws Exception if the singletons can't be found for some reason
   */
  protected ArrayList<BinaryItem> getSingletons(Object source) throws Exception {
    ArrayList<BinaryItem> singletons = new ArrayList<BinaryItem>();
    Instances data = null;
    
    if (source instanceof Instances) data = (Instances)source;
    else if (source instanceof weka.core.converters.ArffLoader)
      data = ((weka.core.converters.ArffLoader)source).getStructure();
    
    for (int i = 0; i < data.numAttributes(); i++) 
      singletons.add(new BinaryItem(data.attribute(i), m_positiveIndex - 1));
    
    if (source instanceof Instances) { 
      m_numInstances = data.numInstances(); // set the number of instances
      for (int i = 0; i < data.numInstances(); i++) {
        Instance current = data.instance(i);
        processSingleton(current, singletons);
      }
    } else if (source instanceof weka.core.converters.ArffLoader) {
      weka.core.converters.ArffLoader loader = (weka.core.converters.ArffLoader)source;
      Instance current = null;
      int count = 0;
      while ((current = loader.getNextInstance(data)) != null) {
        processSingleton(current, singletons);
        count++;
        if (count % m_offDiskReportingFrequency == 0) System.err.println("Singletons: done " + count);
      }
      m_numInstances = count; // set the number of instances
      loader.reset();
    }
    return singletons;
  }
  
  /**Get the singleton items in the data
   * @param data the Instances to process
   * @return a list of singleton item sets
   * @throws Exception if the singletons can't be found for some reason
   */
  protected ArrayList<BinaryItem> getSingletons(Instances data) throws Exception {
    return getSingletons((Object)data);
  }
  
  /*protected ArrayList<BinaryItem> getFrequent(ArrayList<BinaryItem> items, int minSupport) {
    ArrayList<BinaryItem> frequent = new ArrayList<BinaryItem>();
    for (BinaryItem b : items) if (b.getFrequency() > minSupport) frequent.add(b);
    Collections.sort(frequent);// sort in descending order of support
    return frequent;
  } */

  /**Inserts a single instance into the FPTree.
   * @param current the instance to insert
   * @param singletons the singleton item sets
   * @param tree the tree to insert into
   * @param minSupport the minimum support threshold
   */
  private void insertInstance(Instance current, ArrayList<BinaryItem> singletons, FPTreeRoot tree, int minSupport) {
    ArrayList<BinaryItem> transaction = new ArrayList<BinaryItem>();
    if (current instanceof SparseInstance) {
      for (int j = 0; j < current.numValues(); j++) {
        int attIndex = current.index(j);
        if (singletons.get(attIndex).getFrequency() >= minSupport) transaction.add(singletons.get(attIndex));
      }
      Collections.sort(transaction);
      tree.addItemSet(transaction, m_transCounter, 1);
    } else {
      for (int j = 0; j < current.numAttributes(); j++) {
        if (!current.isMissing(j)) {
          if (current.attribute(j).numValues() == 1 || current.value(j) == m_positiveIndex - 1) {
            if (singletons.get(j).getFrequency() >= minSupport)
              transaction.add(singletons.get(j));
          }
        }
      }
      Collections.sort(transaction);
      tree.addItemSet(transaction, m_transCounter, 1);
    }
    m_transCounter++;
  }
  
  /**Construct the frequent pattern tree by inserting each transaction in the data into
   * the tree. Only those items from each transaction that meet the minimum support threshold are inserted.
   * @param singletons the singleton item sets
   * @param data the Instances containing the transactions
   * @param minSupport the minimum support
   * @return the root of the tree
   */
  protected FPTreeRoot buildFPTree(ArrayList<BinaryItem> singletons, Object dataSource, int minSupport) throws Exception {
    FPTreeRoot tree = new FPTreeRoot();    
    Instances data = null;
    if (dataSource instanceof Instances) data = (Instances)dataSource;
    else if (dataSource instanceof weka.core.converters.ArffLoader)
      data = ((weka.core.converters.ArffLoader)dataSource).getStructure();
    
    if (dataSource instanceof Instances) {
      for (int i = 0; i < data.numInstances(); i++)
        insertInstance(data.instance(i), singletons, tree, minSupport);
    } else if (dataSource instanceof weka.core.converters.ArffLoader) {
      weka.core.converters.ArffLoader loader = 
        (weka.core.converters.ArffLoader)dataSource;
      Instance current = null;
      int count = 0;
      while ((current = loader.getNextInstance(data)) != null) {
        insertInstance(current, singletons, tree, minSupport);
        count++;
        if (count % m_offDiskReportingFrequency == 0) System.err.println("build tree done: " + count);
      }
    }
    return tree;
  }
  

  /**Find large item sets in the FP-tree.
   * @param tree the root of the tree to mine
   * @param largeItemSets holds the large item sets found
   * @param recursionLevel the recursion level for the current projected counts
   * @param conditionalItems the current set of items that the current (projected) tree is conditional on
   * @param minSupport the minimum acceptable support
   */
  protected void mineTree(FPTreeRoot tree, FrequentItemSets largeItemSets, 
      int recursionLevel, FrequentBinaryItemSet conditionalItems, int minSupport) {
    
    if (!tree.isEmpty(recursionLevel)) {
      if (m_maxItems > 0 && recursionLevel >= m_maxItems) return;
     
      Map<BinaryItem, FPTreeRoot.Header> headerTable = tree.getHeaderTable();
      Set<BinaryItem> keys = headerTable.keySet();
      Iterator<BinaryItem> i = keys.iterator();
      
      while (i.hasNext()) {
        BinaryItem item = i.next();
        FPTreeRoot.Header itemHeader = headerTable.get(item);
        
        int support = itemHeader.getProjectedCounts().getCount(recursionLevel);// check for minimum support at this level
        if (support >= minSupport) {
        	
          for (FPTreeNode n : itemHeader.getHeaderList()) {// process header list at this recursion level
            int currentCount = n.getProjectedCount(recursionLevel);// push count up path to root
            if (currentCount > 0) {                            
              FPTreeNode temp = n.getParent();
              while (temp != tree) {
                temp.increaseProjectedCount(recursionLevel + 1, currentCount); // set/increase for the node
                headerTable.get(temp.getItem()).getProjectedCounts().increaseCount(recursionLevel + 1, currentCount);
                temp = temp.getParent();
              }
            }
          }
          FrequentBinaryItemSet newConditional = (FrequentBinaryItemSet) conditionalItems.clone();
          newConditional.addItem(item); // this item gets added to the conditional items
          newConditional.setSupport(support);
          
          // now add this conditional item set to the list of large item sets
          largeItemSets.addItemSet(newConditional);
          
          // now recursively process the new tree
          mineTree(tree, largeItemSets, recursionLevel + 1, newConditional, minSupport);
          
          // reverse the propagated counts
          for (FPTreeNode n : itemHeader.getHeaderList()) {
            FPTreeNode temp = n.getParent();
            while (temp != tree) {
              temp.removeProjectedCount(recursionLevel + 1);
              temp = temp.getParent();
            }
          }
          for (FPTreeRoot.Header h : headerTable.values())
            h.getProjectedCounts().removeCount(recursionLevel + 1);
        }
      }
    }
  }
  
  public WFPGrowthTID() {
    resetOptions();
  }

  public WFPGrowthTID(Instances instances) {
	  source = instances;
	  resetOptions();
  }

  public void resetOptions() {
    m_delta = 0.05;
    m_metricThreshold = 0.9;
    m_lowerBoundMinSupport = 0.1;
    m_upperBoundMinSupport = 1.0;
    m_positiveIndex = 2;
    m_transactionsMustContain = "";
    m_mustContainOR = false;
  }
  
  public void setPositiveIndex(int index) {
    m_positiveIndex = index;
  }
  public int getPositiveIndex() {
    return m_positiveIndex;
  }
  public void setMaxNumberOfItems(int max) {
    m_maxItems = max;
  }
  public int getMaxNumberOfItems() {
    return m_maxItems;
  }
  public double getMinMetric() {
    return m_metricThreshold;
  }
  public void setMinMetric(double v) {
    m_metricThreshold = v;
  }
  public void setTransactionsMustContain(String list) {
    m_transactionsMustContain = list;
  }
  public String getTransactionsMustContain() {
    return m_transactionsMustContain;
  }
  public void setUseORForMustContainList(boolean b) {
    m_mustContainOR = b;
  }
  public boolean getUseORForMustContainList() {
    return m_mustContainOR;
  }
  public double getDelta() {
    return m_delta;
  }
  public void setDelta(double v) {
    m_delta = v;
  }
  public double getLowerBoundMinSupport() {
    return m_lowerBoundMinSupport;
  }
  public void setLowerBoundMinSupport(double v) {
    m_lowerBoundMinSupport = v;
  }
  public double getUpperBoundMinSupport() {
    return m_upperBoundMinSupport;
  }
  public void setUpperBoundMinSupport(double v) {
    m_upperBoundMinSupport = v;
  }
  public void setOffDiskReportingFrequency(int freq) {
    m_offDiskReportingFrequency = freq;
  }
  public AssociationRules getAssociationRules() {
	return null;
  }
  public boolean canProduceRules() {
	return false;
  }

  public String[] getRuleMetricNames() {
    String[] metricNames = new String[DefaultAssociationRule.TAGS_SELECTION.length];
    for (int i = 0; i < DefaultAssociationRule.TAGS_SELECTION.length; i++) 
      metricNames[i] = DefaultAssociationRule.TAGS_SELECTION[i].getReadable();
    return metricNames;
  }
  

   /**Parses a given list of options. <p/>
	* <pre> -P &lt;attribute index of positive value&gt;
	*  Set the index of the attribute value to consider as 'positive' for binary attributes in normal dense instances. Index 2 is always used for sparse instances. (default = 2)</pre>
	* <pre> -I &lt;max items&gt;
	*  The maximum number of items to include in large items sets (and rules). (default = -1, i.e. no limit.)</pre>
	* <pre> -N &lt;require number of rules&gt;
	*  The required number of rules. (default = 10)</pre>
	* <pre> -T &lt;0=confidence | 1=lift | 2=leverage | 3=Conviction&gt;
	*  The metric by which to rank rules. (default = confidence)</pre>
	* <pre> -C &lt;minimum metric score of a rule&gt;
	*  The minimum metric score of a rule. (default = 0.9)</pre>
	* <pre> -U &lt;upper bound for minimum support&gt;
	*  Upper bound for minimum support. (default = 1.0)</pre>
	* <pre> -M &lt;lower bound for minimum support&gt;
	*  The lower bound for the minimum support. (default = 0.1)</pre>
	* <pre> -D &lt;delta for minimum support&gt;
	*  The delta by which the minimum support is decreased in each iteration. (default = 0.05)</pre>
	* <pre> -S
	*  Find all rules that meet the lower bound on minimum support and the minimum metric constraint.
	*  Turning this mode on will disable the iterative support reduction procedure to find the specified number of rules.</pre>
	* <pre> -transactions &lt;comma separated list of attribute names&gt;
	*  Only consider transactions that contain these items (default = no restriction)</pre>
	* <pre> -rules &lt;comma separated list of attribute names&gt;
	*  Only print rules that contain these items. (default = no restriction)</pre>
	* <pre> -use-or
	*  Use OR instead of AND for must contain list(s). Use in conjunction with -transactions and/or -rules</pre>
    * @param options the list of options as an array of strings
    * @throws Exception if an option is not supported
    */
  public void setOptions(String[] options) throws Exception {
    resetOptions();
    String positiveIndexString = Utils.getOption('P', options);
    String maxItemsString = Utils.getOption('I', options);
    String minMetricString = Utils.getOption('C', options);
    String lowerBoundSupportString = Utils.getOption("M", options);
    String upperBoundSupportString = Utils.getOption("U", options);
    String deltaString = Utils.getOption("D", options);
    String transactionsString = Utils.getOption("transactions", options);

    if (positiveIndexString.length() != 0) setPositiveIndex(Integer.parseInt(positiveIndexString));
    if (maxItemsString.length() != 0) setMaxNumberOfItems(Integer.parseInt(maxItemsString));
    if (minMetricString.length() != 0) setMinMetric(Double.parseDouble(minMetricString));
    if (deltaString.length() != 0) setDelta(Double.parseDouble(deltaString));
    if (lowerBoundSupportString.length() != 0) setLowerBoundMinSupport(Double.parseDouble(lowerBoundSupportString));
    if (upperBoundSupportString.length() != 0) setUpperBoundMinSupport(Double.parseDouble(upperBoundSupportString));
    if (transactionsString.length() != 0) setTransactionsMustContain(transactionsString);
    setUseORForMustContainList(Utils.getFlag("use-or", options));
  }
  
  /**Gets the current settings of the classifier.
   * @return an array of strings suitable for passing to setOptions
   */
  public String[] getOptions() {
    ArrayList<String> options = new ArrayList<String>();
    options.add("-P"); options.add("" + getPositiveIndex());
    options.add("-I"); options.add("" + getMaxNumberOfItems());
    options.add("-C"); options.add("" + getMinMetric());
    options.add("-D"); options.add("" + getDelta());
    options.add("-U"); options.add("" + getUpperBoundMinSupport());
    options.add("-M"); options.add("" + getLowerBoundMinSupport());
    if (getTransactionsMustContain().length() > 0) options.add("-transactions"); options.add(getTransactionsMustContain());
    if (getUseORForMustContainList()) options.add("-use-or");
    return options.toArray(new String[1]);
  }
  
  protected Instances parseTransactionsMustContain(Instances data) {
    String[] split = m_transactionsMustContain.trim().split(",");
    boolean[] transactionsMustContainIndexes = new boolean[data.numAttributes()];
    int numInTransactionsMustContainList = split.length;
    
    for (int i = 0; i < split.length; i++) {
      String attName = split[i].trim();
      Attribute att = data.attribute(attName);
      if (att == null) {
        System.err.println("[FPGrowth] : WARNING - can't find attribute " + attName + " in the data.");
        numInTransactionsMustContainList--;
      } else transactionsMustContainIndexes[att.index()] = true;
    }
    
    if (numInTransactionsMustContainList == 0) return data;
    else {
      Instances newInsts = new Instances(data, 0);
      for (int i = 0; i < data.numInstances(); i++) {
        if (passesMustContain(data.instance(i), transactionsMustContainIndexes, numInTransactionsMustContainList))
          newInsts.add(data.instance(i));
      }
      newInsts.compactify();
      return newInsts;
    }
  }
    
  /**Method that generates all large item sets with a minimum support, and from
   * these all association rules with a minimum metric (i.e. confidence, lift etc.).
   * @param source the source of the data. May be an Instances object or an ArffLoader. 
   * @throws Exception if rules can't be built successfully
   */
  private void buildAssociations() throws Exception {
    Instances data = (Instances)source;
    
    // prune any instances that don't contain the requested items (if any). Can only do this if we are not reading the data incrementally
    if (m_transactionsMustContain.length() > 0 && (source instanceof Instances)) {
      data = parseTransactionsMustContain(data);
      getCapabilities().testWithFail(data);
    }	    
    ArrayList<BinaryItem> singletons = getSingletons(source);
    
    double lowerBoundMinSuppAsFraction = (m_lowerBoundMinSupport > 1) ? m_lowerBoundMinSupport / m_numInstances : m_lowerBoundMinSupport;	      
    double currentSupport = lowerBoundMinSuppAsFraction;
    int currentSupportAsInstances = (currentSupport > 1) ? (int)currentSupport : (int)Math.ceil(currentSupport * m_numInstances);

    FrequentItemSets largeItemSets = new FrequentItemSets(m_numInstances);
    m_TransactionSets = new ArrayList<List<Integer>>(m_numInstances);
    FrequentBinaryItemSet conditionalItems = new FrequentBinaryItemSet(new ArrayList<BinaryItem>(), 0);
    FPTreeRoot tree = buildFPTree(singletons, source, currentSupportAsInstances);
    //System.out.println(tree.toString(0));
    
    mineTree(tree, largeItemSets, 0, conditionalItems, currentSupportAsInstances);
    m_largeItemSets = largeItemSets;

    Map<BinaryItem, FPTreeRoot.Header> headerTable = tree.getHeaderTable();
    for(int i=0; i<m_largeItemSets.size(); i++){
    	List<Integer> trans = new ArrayList<Integer>();
    	List<BinaryItem> items = (List<BinaryItem>) m_largeItemSets.getItemSet(i).getItems();
    	//System.out.println("Itemset:"+i+"-->"+m_largeItemSets.getItemSet(i).toString());
    	for(FPTreeNode node : headerTable.get(items.get(0)).getHeaderList()){
    		//System.out.println("NO:" + node.m_item.toString());
    		//List<Integer> ts = node.getItemLeafs(0,0,items);
    		//for(Integer t : ts) System.out.print(t +",");
    		//System.out.println();
        	trans.addAll(node.getItemLeafs(0,0,items));
        }
    	m_TransactionSets.add(trans);
    }
  }

  public void buildAssociations(double support) throws Exception {
    m_lowerBoundMinSupport = support;
    buildAssociations();
  }
  public void buildAssociations(Instances data) throws Exception {
	    buildAssociations();
  }

  /**Assemble a dot graph representation of the FP-tree.
   * @param tree the root of the FP-tree
   * @return a graph representation as a String in dot format.
   */
  public String graph(FPTreeRoot tree) {
    StringBuffer text = new StringBuffer();
    text.append("digraph FPTree {\n");
    text.append("N0 [label=\"ROOT\"]\n");
    tree.graphFPTree(text);
    text.append("}\n");
    return text.toString();
  }
  
  /**Main method.
   * @param args the commandline options
   */
  public static void main(String[] args) {
    try {
      String[] argsCopy = args.clone();
      if (Utils.getFlag('h', argsCopy) || Utils.getFlag("help", argsCopy)) {
        runAssociator(new WFPGrowthTID(), args);
        System.out.println("-disk\n\tProcess data off of disk instead of loading\n\tinto main memory. This is a command line only option.");
        return;
      }
      if (!Utils.getFlag("disk", args)) runAssociator(new WFPGrowthTID(), args);
      else {
        String filename;
        filename = Utils.getOption('t', args);
        weka.core.converters.ArffLoader loader = null;
        if (filename.length() != 0) {
          loader = new weka.core.converters.ArffLoader();
          loader.setFile(new java.io.File(filename));
        } else throw new Exception("No training file specified!");
        WFPGrowthTID fpGrowth = new WFPGrowthTID(loader.getDataSet());
        fpGrowth.setOptions(args);
        Utils.checkForRemainingOptions(args);
        fpGrowth.buildAssociations();
        System.out.print(fpGrowth.toString());
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

}
