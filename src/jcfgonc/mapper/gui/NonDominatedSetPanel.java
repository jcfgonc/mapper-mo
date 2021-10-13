package jcfgonc.mapper.gui;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.moeaframework.core.Problem;
import org.moeaframework.core.Solution;

import jcfgonc.moea.generic.ProblemDescription;

public class NonDominatedSetPanel extends JPanel {

	private static final long serialVersionUID = -3640671737661415056L;
	private int numberOfObjectives;
	private ArrayList<XYSeries> ndsSeries;
	private Collection<Solution> nonDominatedSet;
	private XYPlot plot;
	private Problem problem;
	private Color paint;
	private JFreeChart chart;

	public NonDominatedSetPanel(Problem problem, Color paint) {
		this.problem = problem;
		this.paint = paint;
		setBorder(null);
		setLayout(new GridLayout(1, 0, 0, 0));
	}

	public void initialize() {
		numberOfObjectives = problem.getNumberOfObjectives();

		// if too many objectives put the graphs side by side, otherwise stack them vertically
		if (numberOfObjectives > 2) {
			setLayout(new GridLayout(1, 0, 0, 0));
		} else {
			setLayout(new GridLayout(0, 1, 0, 0));
		}

		int numberNDSGraphs = (int) Math.ceil((double) numberOfObjectives / 2); // they will be plotted in pairs of objectives

		ndsSeries = new ArrayList<>();
		int objectiveIndex = 0; // for laying out axis' labels
		for (int i = 0; i < numberNDSGraphs; i++) {
			XYSeries xySeries = new XYSeries("untitled");
			XYSeriesCollection dataset = new XYSeriesCollection();
			dataset.addSeries(xySeries);
			ndsSeries.add(xySeries);

			String xAxisLabel;
			String yAxisLabel;
			if (problem instanceof ProblemDescription) {
				ProblemDescription pd = (ProblemDescription) problem;
				if (objectiveIndex < numberOfObjectives - 1) { // only one objective
					xAxisLabel = String.format("(%d) %s", objectiveIndex, pd.getObjectiveDescription(objectiveIndex));
					yAxisLabel = String.format("(%d) %s", objectiveIndex + 1, pd.getObjectiveDescription(objectiveIndex + 1));
				} else { // more than two objectives to follow
					xAxisLabel = String.format("(%d) %s", 0, pd.getObjectiveDescription(0));
					yAxisLabel = String.format("(%d) %s", objectiveIndex, pd.getObjectiveDescription(objectiveIndex));
				}
			} else {
				if (objectiveIndex < numberOfObjectives - 1) { // only one objective
					xAxisLabel = String.format("(%d) Objective %d", objectiveIndex, objectiveIndex);
					yAxisLabel = String.format("(%d) Objective %d", objectiveIndex + 1, objectiveIndex + 1);
				} else { // more than two objectives to follow
					xAxisLabel = String.format("(%d) Objective %d", 0, 0);
					yAxisLabel = String.format("(%d) Objective %d", objectiveIndex, objectiveIndex);
				}
			}
			objectiveIndex += 2;

			chart = ChartFactory.createScatterPlot(null, xAxisLabel, yAxisLabel, dataset, PlotOrientation.VERTICAL, false, false, false);
			chart.setRenderingHints(GUI_Utils.createDefaultRenderingHints());

//			// color
			plot = chart.getXYPlot();
			plot.setBackgroundAlpha(1);
			XYItemRenderer renderer = plot.getRenderer();
			renderer.setSeriesPaint(0, paint);
			Shape shape = new Ellipse2D.Double(-1.0, -1.0, 2, 2);
			renderer.setSeriesShape(0, shape);
			// fill shapes
//			XYStepRenderer rend = (XYStepRenderer) renderer;
//			rend.setShapesFilled(true);

			ChartPanel chartPanel = new ChartPanel(chart, true);
			chartPanel.setDomainZoomable(false);
			chartPanel.setRangeZoomable(false);
			add(chartPanel);

			// add click event
			chartPanel.addChartMouseListener(new ChartMouseListener() {
				@Override
				public void chartMouseClicked(ChartMouseEvent e) {
				//	System.out.println(e.getTrigger());
				}

				@Override
				public void chartMouseMoved(ChartMouseEvent arg0) {
				}
			});

		}
	}

	public void printNonDominatedSet() {
		if (nonDominatedSet == null || nonDominatedSet.isEmpty())
			return;
		Iterator<Solution> pi = nonDominatedSet.iterator();
		while (pi.hasNext()) {
			Solution solution = pi.next();
			for (int objectiveIndex = 0; objectiveIndex < numberOfObjectives; objectiveIndex++) {
				double x = solution.getObjective(objectiveIndex);
				System.out.print(x);
				if (objectiveIndex < numberOfObjectives - 1)
					System.out.print("\t");
			}
			if (pi.hasNext()) {
				System.out.println();
			}
		}
	}

	/**
	 * Called by the main GUI class, who knows everything.
	 * 
	 * @param nds
	 */
	public void updateGraphs(Collection<Solution> nds) {
		try {
			this.nonDominatedSet = nds;
			setNotifySeries(false);
			refillXYSeries(); // draw NDS/solutions charts
			setNotifySeries(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Used by the async thread to run JFreeChart's rendering code.
	 */
	private void refillXYSeries() {
		clearData();
		// update the non-dominated sets' graphs
		int objectiveIndex = 0;
		// iterate the scatter plots (each can hold two objectives)
		for (XYSeries graph : ndsSeries) {
			// iterate the solutions
			for (Solution solution : nonDominatedSet) {
				// pairs of objectives
				double x;
				double y;
				if (objectiveIndex < numberOfObjectives - 1) {
					x = solution.getObjective(objectiveIndex);
					y = solution.getObjective(objectiveIndex + 1);
				} else {
					x = solution.getObjective(0);
					y = solution.getObjective(objectiveIndex);
				}
				graph.add(x, y);
			}

			objectiveIndex += 2;
		}
	}

	public void clearData() {
		for (XYSeries graph : ndsSeries) {
			// empty data series
			graph.clear();
		}
	}

	/**
	 * enables/disables series changed event (to redraw the graph)
	 * 
	 * @param notify
	 */
	public void setNotifySeries(boolean notify) {
		for (XYSeries graph : ndsSeries) {
			graph.setNotify(notify);
		}
	}
}
