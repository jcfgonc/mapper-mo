package jcfgonc.mapper.gui;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Paint;
import java.awt.RenderingHints;

import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

import visual.GUI_Utils;

public class BarChartPanel extends JPanel {

	private static final long serialVersionUID = 6897997626552672853L;
	private DefaultCategoryDataset dataset;
	private String valueAxisLabel;
	private String categoryAxisLabel;
	private String title;
	private Paint paint;

	public BarChartPanel(String title, String categoryAxisLabel, String valueAxisLabel, Paint paint) {
		this.title = title;
		this.categoryAxisLabel = categoryAxisLabel;
		this.valueAxisLabel = valueAxisLabel;
		this.paint = paint;
		// this is important, otherwise the panel will overflow its bounds
		setBorder(null);
		setLayout(new GridLayout(1, 0, 0, 0));
	}

	public void initialize() {
		dataset = new DefaultCategoryDataset();

		JFreeChart chart = ChartFactory.createBarChart(title, categoryAxisLabel, valueAxisLabel, dataset, PlotOrientation.VERTICAL, false, false,
				false);
		RenderingHints renderingHints = GUI_Utils.createDefaultRenderingHints();
		// disable anti-aliasing in the bar graph, if on it creates moire between the bars
	//	renderingHints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		chart.setRenderingHints(renderingHints);

		ChartPanel chartPanel = new ChartPanel(chart);
		CategoryPlot catPlot = chart.getCategoryPlot();
		BarRenderer renderer = (BarRenderer) catPlot.getRenderer();

		// this is to change the type of bar
		// set using the theme
//		StandardBarPainter painter = new StandardBarPainter();
//		renderer.setBarPainter(painter);

		// disable bar shadows
		renderer.setShadowVisible(false);
		// bar colors
		renderer.setSeriesPaint(0, paint);
		// renderer.setDrawBarOutline(false);
		// background and bars alpha
		catPlot.setBackgroundAlpha(1.0f);
		catPlot.setForegroundAlpha(1.0f);
		// horizontal grid color
		catPlot.setRangeGridlinePaint(new Color(0, 0, 0, 255));
		CategoryAxis domainAxis = catPlot.getDomainAxis();
		// hide domain axis/labels
		domainAxis.setAxisLineVisible(false);
		domainAxis.setVisible(false);
		// left and right graph margins (to the window's limit) - OK: fully fills the available space
		domainAxis.setLowerMargin(0.0);
		domainAxis.setUpperMargin(0.0);
		// distance between the bars
		domainAxis.setCategoryMargin(-0.00);
		add(chartPanel);
	}

	public void addSample(double x, double y, String category) {
		try {
			// second and third arguments must be constant
			dataset.addValue(y, category, Double.toString(x));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void addSample(int x, double y, String category) {
		try {
			// second and third arguments must be constant
			dataset.addValue(y, category, Integer.toString(x));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void addSample(int x, double y) {
		addSample(x, y, "default");
	}

	public void clearData() {
		dataset.clear();
	}

}
