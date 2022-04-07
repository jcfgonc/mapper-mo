package jcfgonc.moea.generic;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.apache.commons.math3.random.RandomAdaptor;
import org.moeaframework.core.Algorithm;
import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.Problem;
import org.moeaframework.core.Solution;
import org.moeaframework.core.spi.AlgorithmFactory;

import graph.DirectedMultiGraph;
import graph.GraphReadWrite;
import graph.StringGraph;
import jcfgonc.mapper.MOEA_Config;
import jcfgonc.mapper.MappingAlgorithms;
import jcfgonc.mapper.StaticSharedVariables;
import jcfgonc.mapper.gui.InteractiveExecutorGUI;
import jcfgonc.mapper.structures.MappingStructure;
import jcfgonc.moea.specific.CustomChromosome;
import jcfgonc.moea.specific.ResultsWriter;
import structures.OrderedPair;
import structures.Ticker;
import utils.VariousUtils;
import visual.SimpleGraphVisualizer;

public class InteractiveExecutor {
	private Properties algorithmProperties;
	private NondominatedPopulation results;
	/**
	 * cancels the MOEA in general
	 */
	private boolean canceled;
	/**
	 * cancels/skips the current run and jumps to the next
	 */
	private boolean skipCurrentRun;
	private Problem problem;
	private InteractiveExecutorGUI gui;
	private ResultsWriter resultsWriter;
//	private BlenderVisualizer blenderVisualizer;
	private volatile boolean guiUpdatingThreadRunning;
	private String windowTitle;
	private String resultsFilename;
	private String dateTimeStamp;
	/**
	 * current epoch
	 */
	private int epoch;
	private ReentrantLock pauseLock;
	private SimpleGraphVisualizer graphVisualizer;
	private RandomAdaptor random;
	/**
	 * used to store file IDs for saving graphs
	 */
	private int nextFileID;

	public InteractiveExecutor(Problem problem, Properties algorithmProperties, ResultsWriter resultsWriter, String windowTitle, RandomAdaptor random) {
		this.problem = problem;
		this.algorithmProperties = algorithmProperties;
		this.resultsWriter = resultsWriter;
		this.windowTitle = windowTitle;
		this.random = random;
//		this.blenderVisualizer = new BlenderVisualizer(populationSize);
		this.gui = new InteractiveExecutorGUI(this);
		this.gui.initializeTheRest();
		this.gui.setVisible(true);

		this.dateTimeStamp = VariousUtils.generateCurrentDateAndTimeStamp();
		this.resultsFilename = String.format("moea_results_%s.tsv", dateTimeStamp);
		this.resultsWriter.writeFileHeader(problem, resultsFilename);
		this.pauseLock = new ReentrantLock();
		this.graphVisualizer = null; // not shown initially

		this.nextFileID = VariousUtils.getNextAvailableFileId(MOEA_Config.saveFolder, "graph_", "csv");
	}

	public ArrayList<Solution> execute(int moea_run) throws InterruptedException {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			// couldn't set system look and feel, continue with default
			e.printStackTrace();
		}

		// code for external evolution visualizer
//		new Thread() {
//			public void run() {
//				try {
//					blenderVisualizer.execute();
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			};
//		}.start();

		epoch = 0;
		Algorithm algorithm = null;
		guiUpdatingThreadRunning = false;

		algorithm = AlgorithmFactory.getInstance().getAlgorithm(MOEA_Config.ALGORITHM, algorithmProperties, problem);
		canceled = false;
		skipCurrentRun = false;
		Ticker ticker = new Ticker();

		clearGraphs();
		gui.resetCurrentRunTime();

		results = new NondominatedPopulation();

		ticker.resetTicker();
		do {
			// dumb way to pause execution
			pauseLock.lock();
			pauseLock.unlock();

			// count current step/epoch time
			ticker.getTimeDeltaLastCall();
			algorithm.step();
			double epochDuration = ticker.getTimeDeltaLastCall();

			NondominatedPopulation lastResults = algorithm.getResult();
			results.addAll(lastResults);
			// results = new NondominatedPopulation(lastResults);

			// update GUI stuff
			updateStatus(moea_run, epoch, epochDuration);
			System.out.printf("epoch\t%d\ttime\t%f\n", epoch, epochDuration);

			// update blender visualizer
//			blenderVisualizer.update(lastResult);
			double runElapsedTime = ticker.getElapsedTime() / 60.0;
			if (algorithm.isTerminated() || //
					runElapsedTime > MOEA_Config.MAX_RUN_TIME || //
					epoch >= MOEA_Config.MAX_EPOCHS || //
					canceled || //
					skipCurrentRun) {
				break; // break while loop
			}
			epoch++;
		} while (true);

		algorithm.terminate();
		gui.takeLastEpochScreenshot();
		ArrayList<Solution> arr = new ArrayList<Solution>(results.getElements());
		resultsWriter.appendResultsToFile(arr, problem, resultsFilename);
		return arr;
	}

	private void updateStatus(int moea_run, int epoch, double epochDuration) {
		// do not queue gui updates, only do one at a time
		if (guiUpdatingThreadRunning) {
			return;
		}

		Runnable updater = new Runnable() {
			// local copy of the solution list to prevent concurrent modified exception
			ArrayList<Solution> array = new ArrayList<Solution>(results.getElements());

			public void run() {
				try {
					guiUpdatingThreadRunning = true;
					gui.updateData(array, epoch, moea_run, epochDuration);
				} catch (Exception e) {
					System.err.println("updateStatus(): BOOM there goes the dynamite");
					e.printStackTrace();
				} finally {
					guiUpdatingThreadRunning = false;
				}
			}
		};
		SwingUtilities.invokeLater(updater);
	}

	private void clearGraphs() {
		Runnable updater = new Runnable() {
			public void run() {
				try {
					gui.clearGraphs();
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
				}
			}
		};
		SwingUtilities.invokeLater(updater);
	}

	public void closeGUI() {
		gui.dispose();
	}

	public Problem getProblem() {
		return problem;
	}

	public Properties getAlgorithmProperties() {
		return algorithmProperties;
	}

	public NondominatedPopulation getResults() {
		return results;
	}

	public boolean isCanceled() {
		return canceled;
	}

	/**
	 * called when the user clicks on the stop button
	 */
	public void stopOptimization() {
		// stop MOEA's loop
		if (isPaused()) {
			togglePause();
		}
		this.canceled = true; // and stops the caller
	}

	public void skipCurrentRun() {
		this.skipCurrentRun = true;
	}

	public String getWindowTitle() {
		return windowTitle;
	}

	public void saveCurrentNDS() {
		String filename = String.format("moea_results_%s_epoch_%d.tsv", dateTimeStamp, epoch);

		resultsWriter.writeFileHeader(problem, filename);
		resultsWriter.appendResultsToFile(results.getElements(), problem, filename);
	}

	public void showRandomSolution() {
		if (results == null || results.isEmpty())
			return;

		int i = random.nextInt(results.size());
		Solution solution = results.get(i);

		showAndSaveSolution(solution);
	}

	public void showBestSolution() {
		if (results == null || results.isEmpty())
			return;

		ArrayList<Solution> resultsList = new ArrayList<>();
		resultsList.addAll(results.getElements());

		// effective order is the reverse of the invocation
		// make sure smaller is better and it is according to CustomProblem!
		sortSolutionList(resultsList, 0, 1e-12); // d:numPairs
		sortSolutionList(resultsList, 7, 1e-12); // d:assymetricRelationCount
		sortSolutionList(resultsList, 1, 1e-12); // f:vitalRelationsMean
		sortSolutionList(resultsList, 5, 1e-12); // f:samePOSpairRatio

		// get first in the above order
		Solution solution = results.get(0);

		showAndSaveSolution(solution);
	}

	private void showAndSaveSolution(Solution solution) {
		// MapperMO specific
		CustomChromosome cc = (CustomChromosome) solution.getVariable(0); // unless the solution domain X has more than one dimension
		MappingStructure<String, String> mappingStructure = cc.getGene();
		DirectedMultiGraph<OrderedPair<String>, String> pairGraph = mappingStructure.getPairGraph();
		// OrderedPair<String> referencePair = mappingStructure.getReferencePair();

		// create window if non existent
		if (graphVisualizer == null) {
			graphVisualizer = new SimpleGraphVisualizer();
			graphVisualizer.setExtendedState(graphVisualizer.getExtendedState() | JFrame.MAXIMIZED_BOTH);
		}
		graphVisualizer.setVisible(true);
		graphVisualizer.toFront();
		// setup graph GUI data
		StringGraph g = new StringGraph(pairGraph);
		g = MappingAlgorithms.translateEdges(g, StaticSharedVariables.relationTranslation);
		graphVisualizer.setData(g, null, null, epoch, results.size());
		graphVisualizer.setTitle(String.format("Graph (%d)", nextFileID));
		// save graph
		try {
			String filename = "graph_" + Integer.toString(nextFileID) + ".csv";
			GraphReadWrite.writeCSV(MOEA_Config.saveFolder + File.separatorChar + filename, g);
		} catch (IOException e) {
			e.printStackTrace();
		}
		// next graph will have next ID
		nextFileID++;
	}

	private void sortSolutionList(ArrayList<Solution> resultsList, int objective, double epsilon) {
		resultsList.sort(new Comparator<Solution>() {

			@Override
			public int compare(Solution o1, Solution o2) {
				return VariousUtils.doubleCompareTo(o1.getObjective(objective), o2.getObjective(objective), epsilon);
			}
		});
	}

	public boolean isPaused() {
		return pauseLock.isLocked();
	}

	public void togglePause() {
		if (isPaused()) {
			pauseLock.unlock();
		} else {
			pauseLock.lock();
		}
	}

}
