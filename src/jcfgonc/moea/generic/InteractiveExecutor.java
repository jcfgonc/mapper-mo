package jcfgonc.moea.generic;

import java.util.ArrayList;
import java.util.Properties;

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

public class InteractiveExecutor {
	private Properties algorithmProperties;
	private NondominatedPopulation lastResult;
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

	public InteractiveExecutor(Problem problem, Properties algorithmProperties, ResultsWriter resultsWriter, String windowTitle) {
		this.problem = problem;
		this.algorithmProperties = algorithmProperties;
		this.resultsWriter = resultsWriter;
		this.windowTitle = windowTitle;
//		this.blenderVisualizer = new BlenderVisualizer(populationSize);
		this.gui = new InteractiveExecutorGUI(this);
		this.gui.initializeTheRest();
		this.gui.setVisible(true);

		this.resultsWriter.writeFileHeader(problem);
	}

	public NondominatedPopulation execute(int moea_run) throws InterruptedException {
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

		int epoch = 0;
		Algorithm algorithm = null;
		guiUpdatingThreadRunning = false;

		algorithm = AlgorithmFactory.getInstance().getAlgorithm(MOEA_Config.ALGORITHM, algorithmProperties, problem);
		canceled = false;
		skipCurrentRun = false;
		Ticker ticker = new Ticker();

		clearGraphs();
		gui.resetCurrentRunTime();

		lastResult = new NondominatedPopulation();

		ticker.resetTicker();
		do {
			// count algorithm time
			algorithm.step();
			double epochDuration = ticker.getTimeDeltaLastCall();

			lastResult.addAll(algorithm.getResult());

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
		resultsWriter.appendResultsToFile(lastResult, problem);
		return lastResult;
	}

	private void updateStatus(int moea_run, int epoch, double epochDuration) {
		// do not queue gui updates, only do one at a time
		if (guiUpdatingThreadRunning) {
			return;
		}

		Runnable updater = new Runnable() {
			// local copy of the solution list to prevent concurrent modified exception
			ArrayList<Solution> nds = new ArrayList<Solution>(lastResult.getElements());

			public void run() {
				try {
					guiUpdatingThreadRunning = true;
					gui.updateData(nds, epoch, moea_run, epochDuration);
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

	public NondominatedPopulation getLastResult() {
		return lastResult;
	}

	public boolean isCanceled() {
		return canceled;
	}

	/**
	 * Called when the user clicks on the abort button. Saves last results and exits the JVM.
	 */
	public void abortOptimization() {
		gui.takeLastEpochScreenshot();
		if (resultsWriter != null) {
			resultsWriter.appendResultsToFile(lastResult, problem);
		}
		System.exit(-1);
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
}
