package jcfgonc.mapper.gui;

import java.awt.GridLayout;
import java.awt.Paint;

import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class StepChartPanel extends JPanel {

	private static final long serialVersionUID = 6897997626552672853L;
	private XYSeriesCollection dataset;
	private String yAxisLabel;
	private String xAxisLabel;
	private String title;
	private XYPlot plot;
	private XYSeries series;
	private Paint paint;

	public StepChartPanel(String title, String xAxisLabel, String yAxisLabel, Paint paint) {
		this.title = title;
		this.xAxisLabel = xAxisLabel;
		this.yAxisLabel = yAxisLabel;
		this.paint = paint;
		// this is important, otherwise the panel will overflow its bounds
		setBorder(null);
		setLayout(new GridLayout(1, 0, 0, 0));
	}

	public void initialize() {
		dataset = new XYSeriesCollection();
		series = new XYSeries(xAxisLabel);
		dataset.addSeries(series);

		JFreeChart chart = ChartFactory.createXYStepChart(title, xAxisLabel, yAxisLabel, dataset, PlotOrientation.VERTICAL, false, false, false);
		chart.setRenderingHints(GUI_Utils.createDefaultRenderingHints());

		ChartPanel chartPanel = new ChartPanel(chart);
		plot = chart.getXYPlot();
		XYItemRenderer r = plot.getRenderer();
		// this is to change the type of bar
//		StandardBarPainter painter = new StandardBarPainter();
//		renderer.setBarPainter(painter);
		// disable bar shadows
//		renderer.setShadowVisible(false);
		r.setSeriesPaint(0, paint);
		plot.setBackgroundAlpha(1);
		// hide domain axis/labels
		ValueAxis domainAxis = plot.getDomainAxis();
		domainAxis.setAxisLineVisible(false);
		domainAxis.setVisible(false);
//		CategoryAxis domainAxis = plot.getDomainAxis();
		// hide domain axis/labels
//		domainAxis.setAxisLineVisible(false);
//		domainAxis.setVisible(false);
		// ValueAxis rangeAxis = plot.getRangeAxis();
		add(chartPanel);
	}

	public void addSample(double x, double y) {
		series.add(x, y);
	}

	public void clearData() {
		series.clear();
	}

}
