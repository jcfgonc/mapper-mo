package jcfgonc.mapper.gui;

import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.moeaframework.core.Problem;
import org.moeaframework.core.Solution;

import jcfgonc.moea.generic.ProblemDescription;

/**
 * This class handles the drawing of the objective's statistics.
 * 
 * @author jcfgonc@gmail.com
 *
 */
public class ObjectivesChartPanel extends JPanel {

	private static final long serialVersionUID = 6897997626552672853L;
	final private String valueAxisLabel;
	final private String categoryAxisLabel;
//	final private Problem problem; //commented because it fills the available space
	final private int numberOfObjectives;
	final private ArrayList<String> nameObjectives;
	final private ArrayList<DefaultBoxAndWhiskerCategoryDataset> objectivesDataset;

	public ObjectivesChartPanel(String categoryAxisLabel, String valueAxisLabel, Problem problem) {
		this.categoryAxisLabel = categoryAxisLabel;
		this.valueAxisLabel = valueAxisLabel;
//		this.problem = problem;
		this.numberOfObjectives = problem.getNumberOfObjectives();
		this.nameObjectives = new ArrayList<String>(numberOfObjectives);

		this.objectivesDataset = new ArrayList<DefaultBoxAndWhiskerCategoryDataset>();

		for (int i = 0; i < numberOfObjectives; i++) {
			this.objectivesDataset.add(new DefaultBoxAndWhiskerCategoryDataset());
		}

		// list of objectives description
		for (int objective_i = 0; objective_i < numberOfObjectives; objective_i++) {
			String category;
			if (problem instanceof ProblemDescription) {
				ProblemDescription pd = (ProblemDescription) problem;
				category = pd.getObjectiveDescription(objective_i);
				category = category.substring(category.indexOf(':') + 1);
			} else {
				category = String.format("Objective %d", objective_i);
			}
			nameObjectives.add(category);
		}

		// this is important, otherwise the panel will overflow its bounds
		setLayout(new GridLayout(1, 0, 0, 0));
	}

	public void initialize() {
		// create N boxplot graphs
		for (int i = 0; i < numberOfObjectives; i++) {
			DefaultBoxAndWhiskerCategoryDataset dataset = objectivesDataset.get(i);
			JFreeChart chart = ChartFactory.createBoxAndWhiskerChart(null, categoryAxisLabel, valueAxisLabel, dataset, false);
			chart.setRenderingHints(GUI_Utils.createDefaultRenderingHints());
			ChartPanel chartPanel = new ChartPanel(chart);
			add(chartPanel);
		}
	}

	public void clearData() {
		for (int i = 0; i < numberOfObjectives; i++) {
			DefaultBoxAndWhiskerCategoryDataset dataset = objectivesDataset.get(i);
			dataset.clear();
		}
	}

	public void setNotify(boolean flag) {
		for (int i = 0; i < numberOfObjectives; i++) {
			DefaultBoxAndWhiskerCategoryDataset dataset = objectivesDataset.get(i);
			dataset.setNotify(flag);
		}
	}

	/**
	 * jfreechart's boxplot requires an arraylist of Double with the data. This function creates it for the given objective.
	 * 
	 * @param nds
	 * @param objective
	 * @return
	 */
	private ArrayList<Double> createObjectiveData(Collection<Solution> nds, int objective) {
		ArrayList<Double> series = new ArrayList<Double>(nds.size());
		for (Solution solution : nds) {
			series.add(solution.getObjective(objective));
		}
		return series;
	}

	public void addValues(Collection<Solution> nds) {
		try {
			setNotify(false);
			clearData();

			for (int objective_i = 0; objective_i < numberOfObjectives; objective_i++) {
//			String category;
//			if (problem instanceof ProblemDescription) {
//				ProblemDescription pd = (ProblemDescription) problem;
//				category = pd.getObjectiveDescription(objective_i);
//				category = category.substring(category.indexOf(':') + 1);
//			} else {
//				category = String.format("Objective %d", objective_i);
//			}

				DefaultBoxAndWhiskerCategoryDataset dataset = objectivesDataset.get(objective_i);
				ArrayList<Double> objectiveData = createObjectiveData(nds, objective_i);
				dataset.add(objectiveData, "0", String.format("(%d)", objective_i));
			}
			setNotify(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
