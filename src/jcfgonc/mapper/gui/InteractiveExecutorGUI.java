package jcfgonc.mapper.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Properties;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.StandardChartTheme;
import org.moeaframework.core.Problem;
import org.moeaframework.core.Solution;

import jcfgonc.mapper.MOEA_Config;
import jcfgonc.moea.generic.InteractiveExecutor;

public class InteractiveExecutorGUI extends JFrame {

	private static final long serialVersionUID = 5577378439253898247L;
	private JPanel contentPane;
	private NonDominatedSetPanel nonDominatedSetPanel;
	private JPanel technicalPanel;
	private final InteractiveExecutor interactiveExecutor;
	private final int numberOfObjectives;
	private final Problem problem;
	private StatusPanel statusPanel;
	private OptimisationControlPanel optimisationControlPanel;
	private BarChartPanel timeEpochPanel;
	private JPanel upperPanel;
	private JPanel upperLeftPanel;
	private BarChartPanel ndsSizePanel;
	private ObjectivesChartPanel objectivesBoxplotPanel;
	private SettingsPanel settingsPanel;
	private JPanel fillPanel;
	private final DecimalFormat screenshotFilenameDecimalFormat = new DecimalFormat("0000");
	private int epoch;
	private int run;
	private double epochDuration;
	private Collection<Solution> nds;
	/**
	 * used to update the gui before taking a screenshot
	 */
	private boolean guiUpdated;
	private JPanel timeSeriesPanel;

	/**
	 * Create the frame.
	 * 
	 * @param properties
	 * @param interactiveExecutor
	 * 
	 * @param k
	 * @param j
	 * @param i
	 */
	public InteractiveExecutorGUI(InteractiveExecutor interactiveExecutor) {
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				abortOptimization();
			}
		});

		this.interactiveExecutor = interactiveExecutor;
		this.problem = interactiveExecutor.getProblem();
		this.numberOfObjectives = problem.getNumberOfObjectives();
		this.nds = null;
		this.epoch = -1;
		this.run = -1;
		this.epochDuration = -1;

		initialize();
	}

	private void initialize() {
		setTitle("Blender 2.0 - Multiple Objective Optimization");
		setName("MOEA");
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		contentPane = new JPanel();
		setContentPane(contentPane);
		contentPane.setLayout(new GridLayout(2, 0, 0, 0));

		upperPanel = new JPanel();
		contentPane.add(upperPanel);
		upperPanel.setLayout(new BoxLayout(upperPanel, BoxLayout.X_AXIS));

		upperLeftPanel = new JPanel();
		upperPanel.add(upperLeftPanel);
		upperLeftPanel.setLayout(new GridLayout(1, 0, 0, 0));

		timeSeriesPanel = new JPanel();
		upperLeftPanel.add(timeSeriesPanel);
		timeSeriesPanel.setLayout(new GridLayout(1, 0, 0, 0));

		objectivesBoxplotPanel = new ObjectivesChartPanel(null, null, problem);
		upperLeftPanel.add(objectivesBoxplotPanel);

		timeEpochPanel = new BarChartPanel("Time vs Epoch", "Epoch", "Time (s)", new Color(200, 0, 100));
		timeSeriesPanel.add(timeEpochPanel);

		ndsSizePanel = new BarChartPanel("Non Dominated Set Size vs Epoch", "Epoch", "Size of the Non Dominated Set", new Color(0, 200, 100));
		timeSeriesPanel.add(ndsSizePanel);

		technicalPanel = new JPanel();
		upperPanel.add(technicalPanel);
		technicalPanel.setLayout(new BoxLayout(technicalPanel, BoxLayout.Y_AXIS));

		statusPanel = new StatusPanel();
		technicalPanel.add(statusPanel);

		settingsPanel = new SettingsPanel();
		technicalPanel.add(settingsPanel);

		optimisationControlPanel = new OptimisationControlPanel(this);
		technicalPanel.add(optimisationControlPanel);

		fillPanel = new JPanel();
		technicalPanel.add(fillPanel);
		fillPanel.setLayout(new BorderLayout(0, 0));

		nonDominatedSetPanel = new NonDominatedSetPanel(problem, Color.BLACK);
		contentPane.add(nonDominatedSetPanel);

		addComponentListener(new ComponentAdapter() { // window resize event
			@Override
			public void componentResized(ComponentEvent e) {
				windowResized(e);
			}
		});
	}

	public void abortOptimization() {
		// default icon, custom title
		int n = JOptionPane.showConfirmDialog(null, "Aborting optimization will discard the results of the current epoch.\nAre you sure?",
				"Abort Optimization", JOptionPane.YES_NO_OPTION);
		if (n != 0)
			return;
		setVisible(false);
		interactiveExecutor.abortOptimization();
	}

	@Override
	public void dispose() {
		super.dispose();
	}

	@SuppressWarnings("unused")
	private int proportionOfInt(int value, double proportion) {
		double newval = (double) value * proportion;
		return (int) newval;
	}

	/**
	 * contains the rest of the stuff which cannot be initialized in the initialize function (because of the windowbuilder IDE)
	 */
	public void initializeTheRest() {
		ChartFactory.setChartTheme(StandardChartTheme.createLegacyTheme());
		// ChartFactory.setChartTheme(StandardChartTheme.createDarknessTheme());

		nonDominatedSetPanel.initialize();
//		timeEpochPanel.initialize(new Color(255, 106, 181));
//		ndsSizePanel.initialize(new Color(83, 255, 169));
		timeEpochPanel.initialize();
		ndsSizePanel.initialize();
		objectivesBoxplotPanel.initialize();

//		this.setLocationRelativeTo(null); // center jframe

		settingsPanel.setNumberEpochs(MOEA_Config.MAX_EPOCHS);
		settingsPanel.setNumberRuns(MOEA_Config.MOEA_RUNS);
		settingsPanel.setPopulationSize(MOEA_Config.POPULATION_SIZE);
		settingsPanel.setRunTimeLimit(MOEA_Config.MAX_RUN_TIME);

		statusPanel.initializedTimeCounters();
		// display static properties
		statusPanel.setObjectives(Integer.toString(numberOfObjectives));
		statusPanel.setAlgorithm(MOEA_Config.ALGORITHM);

//		setLocation(-6, 0);
		windowResized(null);
		pack();
		setLocationRelativeTo(null);
//		setPreferredSize(new Dimension(1920, 512));
//		setMinimumSize(new Dimension(800, 640));
		setExtendedState(getExtendedState() | JFrame.MAXIMIZED_BOTH);
	}

	/**
	 * Updates data structures and the GUI.
	 * 
	 * @param nds
	 * @param epoch
	 * @param run
	 * @param epochDuration
	 */
	public void updateData(Collection<Solution> nds, int epoch, int run, double epochDuration) {
		// changed with a new run
		if (run != this.run) {
			statusPanel.setCurrentRun(Integer.toString(run));
			statusPanel.setPopulationSize(getAlgorithmProperties().getProperty("populationSize")); // get directly from the algorithm's properties
		}

		this.nds = nds;
		this.epoch = epoch;
		this.run = run;
		this.epochDuration = epochDuration;
		guiUpdated = false;

		// update text boxes with dynamic properties
		statusPanel.setEpoch(Integer.toString(epoch));
		statusPanel.setNDS_Size(Integer.toString(nds.size()));
		statusPanel.setLastEpochDuration(epochDuration);
		statusPanel.setNumberRuns(Integer.toString(MOEA_Config.MOEA_RUNS));
		statusPanel.setNumberEpochs(Integer.toString(MOEA_Config.MAX_EPOCHS));
		statusPanel.setRunTimeLimit(Double.toString(MOEA_Config.MAX_RUN_TIME));

		if (settingsPanel.isGraphsEnabled()) {
			updateGUI();
		}

		if (settingsPanel.isScreenshotsEnabled()) {
			takeScreenshot();
		}
	}

	private void updateGUI() {
		if (nds != null) {
			nonDominatedSetPanel.updateGraphs(nds);
			timeEpochPanel.addSample(epoch, epochDuration);
			ndsSizePanel.addSample(epoch, nds.size());
			objectivesBoxplotPanel.addValues(nds);
		}
		guiUpdated = true;
	}

	private void takeScreenshot() {
		// maximize window
		setExtendedState(getExtendedState() | JFrame.MAXIMIZED_BOTH);
		// update gui if out-dated
		if (!guiUpdated) {
			updateGUI();
		}
		// take the screenshot
		new File(MOEA_Config.screenshotsFolder).mkdir();
		String filename = String.format("run_%s_epoch_%s", screenshotFilenameDecimalFormat.format(run),
				screenshotFilenameDecimalFormat.format(epoch));

		saveScreenShotPNG(MOEA_Config.screenshotsFolder + File.separator + filename + ".png");
	}

	public void takeLastEpochScreenshot() {
		try {
			if (settingsPanel.isLastEpochScreenshotEnabled()) {
				Runnable updater = new Runnable() {
					public void run() {
						try {
							takeScreenshot();
						} catch (Exception e) {
							e.printStackTrace();
						} finally {
						}
					}
				};
				SwingUtilities.invokeAndWait(updater);
			}
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void clearGraphs() {
		nonDominatedSetPanel.clearData();
		ndsSizePanel.clearData();
		timeEpochPanel.clearData();
		objectivesBoxplotPanel.clearData();
	}

	protected void windowResized(ComponentEvent e) {
		// position horizontal divider to give space to the right pane
		// horizontalPane.setDividerLocation(horizontalPane.getWidth() - rightPanel.getMinimumSize().width);
//		int uw = upperPanel.getWidth();
//		int rw = rightPanel.getWidth();
//		ndsPanel.setMaximumSize(new Dimension(uw - rw, Integer.MAX_VALUE));
	}

	/**
	 * Saves the entire GUI to a file. Filename contains the extension/file format.
	 */
	public void saveScreenShotPNG(String filename) {
		int w = contentPane.getWidth();
		int h = contentPane.getHeight();
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		contentPane.paint(img.getGraphics());
		GUI_Utils.saveScreenShotPNG(img, filename);
	}

	public void stopOptimization() {
		interactiveExecutor.stopOptimization();
	}

	public void printNonDominatedSet() {
		nonDominatedSetPanel.printNonDominatedSet();
	}

	public void skipCurrentRun() {
		interactiveExecutor.skipCurrentRun();
	}

	public Properties getAlgorithmProperties() {
		return interactiveExecutor.getAlgorithmProperties();
	}

	public void resetCurrentRunTime() {
		statusPanel.resetCurrentRunTime();
	}

	public int getEpoch() {
		return epoch;
	}

	public int getRun() {
		return run;
	}
}
