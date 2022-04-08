package bicpam.pminer;

import java.util.List;
import domain.Biclusters;

public interface BicPM {

	public Biclusters run(double support, int nrLabels) throws Exception;
	public Biclusters run(int minColumns, int minRows, int maxColumns, int maxRows, int nrLabels) throws Exception;
	public void reset();
	public void setData(List<List<Integer>> dataI); 
	public long getMemory();

}
