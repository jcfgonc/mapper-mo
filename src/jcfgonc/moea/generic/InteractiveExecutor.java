package jcfgonc.moea.generic;

import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.moeaframework.core.Algorithm;
import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.Problem;
import org.moeaframework.core.Solution;
import org.moeaframework.core.spi.AlgorithmFactory;

import jcfgonc.mapper.MOEA_Config;
import jcfgonc.mapper.gui.InteractiveExecutorGUI;
import jcfgonc.moea.specific.ResultsWriter;
import structures.Ticker;
import utils.VariousUtils;

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
	private int epoch;
	private ReentrantLock pauseLock;

	public InteractiveExecutor(Problem problem, Properties algorithmProperties, ResultsWriter resultsWriter, String windowTitle) {
		this.problem = problem;
		this.algorithmProperties = algorithmProperties;
		this.resultsWriter = resultsWriter;
		this.windowTitle = windowTitle;
//		this.blenderVisualizer = new BlenderVisualizer(populationSize);
		this.gui = new InteractiveExecutorGUI(this);
		this.gui.initializeTheRest();
		this.gui.setVisible(true);

		this.dateTimeStamp = VariousUtils.generateCurrentDateAndTimeStamp();
		this.resultsFilename = String.format("moea_results_%s.tsv", dateTimeStamp);
		this.resultsWriter.writeFileHeader(problem, resultsFilename);
		this.pauseLock = new ReentrantLock();
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

			// count algorithm time
			ticker.getTimeDeltaLastCall();
			algorithm.step();
			double epochDuration = ticker.getTimeDeltaLastCall();

			NondominatedPopulation lastResults = algorithm.getResult();
			results.addAll(lastResults);
			// results = new NondominatedPopulation(lastResults);

			// update GUI stuff
			updateStatus(moea_run, epoch, epochDuration);

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

	public void dumpRandomSolution() {
		// TODO Auto-generated method stub

	}

	public boolean isPaused() {
		return pauseLock.isLocked();
	}

	public void pause() {
		if (isPaused()) {
			pauseLock.unlock();
		} else {
			pauseLock.lock();
		}
	}
}
