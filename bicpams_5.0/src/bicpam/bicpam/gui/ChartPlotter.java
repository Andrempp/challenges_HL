package bicpam.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Random;

import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.GrayPaintScale;
import org.jfree.chart.renderer.PaintScale;
import org.jfree.chart.renderer.xy.XYBlockRenderer;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.chart.title.Title;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.MatrixSeries;
import org.jfree.data.xy.MatrixSeriesCollection;
import org.jfree.data.xy.XYZDataset;
import org.jfree.ui.RectangleEdge;

import bicpam.bicminer.BiclusterMiner.Orientation;
import domain.Bicluster;
import domain.Dataset;


/** Expression View tab */
public class ChartPlotter {

	private static final long serialVersionUID = 1L;

	public static ChartPanel getGraph(Bicluster bc, Dataset data, Orientation orientation) {
		Random r = new Random();
		double[][] bicData = data.getRealBicluster(bc.columns, bc.rows);
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		List<String> cols = data.columns;
		if(data.originalColumns!=null && orientation.equals(Orientation.PatternOnRows)) cols = data.originalColumns;
		for(int i=0, j=0, l1=bicData.length; i<l1; i++, j=0){
			for(Integer col : bc.columns){
				dataset.addValue(bicData[i][j], "G"+i, cols.get(col));//+r.nextFloat()*0.3-0.15, "G"+i, cols.get(col));
				j++;
			}
		}
		double min=data.min, max=data.max;
		JFreeChart chart = ChartFactory.createLineChart(null,null,null,dataset,PlotOrientation.VERTICAL,false,true,false);
		chart.setBackgroundPaint(Color.white);
        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.white);
        plot.setRangeGridlinePaint(Color.white);
        final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setLowerBound(min);
        rangeAxis.setUpperBound(max);
        chart.setBorderStroke(new BasicStroke(0.5f));
        for(int i=0, l=bicData.length; i<l; i++)
        	plot.getRenderer().setSeriesStroke(i,new BasicStroke(1.5f));
        return new ChartPanel(chart);
	}

	public static ChartPanel getHeatMap(Bicluster bic, Dataset data, Orientation orientation) {
		
		class MyMatrixSeries extends MatrixSeries {
			public MyMatrixSeries(double[][] _data){
				super("",_data.length,_data[0].length);
				this.data = _data;
			}
		};
		
		double[][] matrix = data.getRealBicluster(bic.columns, bic.rows);		
		NumberAxis xAxis = new NumberAxis(null), yAxis = new NumberAxis(null), zAxis = new NumberAxis(null); 
        xAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits()); 
        yAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits()); 
        zAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        zAxis.setVisible(false);
		XYZDataset dataset = new MatrixSeriesCollection(new MyMatrixSeries(matrix));
        XYBlockRenderer renderer = new XYBlockRenderer();
        PaintScale scale = new GrayPaintScale(data.min, data.max);
        renderer.setPaintScale(scale);
        renderer.setSeriesVisible(true, true);
        XYPlot plot = new XYPlot(dataset, xAxis, yAxis, renderer); 
        //plot.setBackgroundPaint(Color.lightGray);
        plot.setDomainGridlinesVisible(false); 
        plot.setRangeGridlinePaint(Color.white);
        JFreeChart chart = new JFreeChart("", plot);
        Title title = new PaintScaleLegend(scale,zAxis);
        title.setPosition(RectangleEdge.BOTTOM);
        chart.addSubtitle(title);
        chart.removeLegend(); 
        //chart.setBackgroundPaint(Color.white); 
		return new ChartPanel(chart);
	}	
}
