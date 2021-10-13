package jcfgonc.mapper.gui;

import java.awt.FlowLayout;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jcfgonc.mapper.MOEA_Config;

public class SettingsPanel extends JPanel {
	private static final long serialVersionUID = -321394905164631735L;
	private JSpinner numberEpochs;
	private JSpinner numberRuns;
	private JSpinner populationSize;
	private JCheckBox graphsEnabledCB;
	private JCheckBox screenshotsCB;
	private JCheckBox lastEpochScreenshotCB;
	private JSpinner runTimeLimit;

	public SettingsPanel() {
		super();

		setBorder(new TitledBorder(null, "Settings", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		JPanel panel_0 = new JPanel();
		FlowLayout flowLayout_1 = (FlowLayout) panel_0.getLayout();
		flowLayout_1.setVgap(2);
		add(panel_0);

		JLabel label_0 = new JLabel("Update Graphs each Epoch");
		panel_0.add(label_0);

		graphsEnabledCB = new JCheckBox("", MOEA_Config.GRAPHS_ENABLED);
		graphsEnabledCB.setToolTipText("If enabled the graphs are updated each epoch.");
		panel_0.add(graphsEnabledCB);
		
		JPanel panel_6 = new JPanel();
		add(panel_6);
		
		JLabel label_6 = new JLabel("Time Limit per Run (minutes)");
		panel_6.add(label_6);
		
		runTimeLimit = new JSpinner();
		runTimeLimit.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				Double value = (Double) runTimeLimit.getModel().getValue();
				MOEA_Config.MAX_RUN_TIME = value.doubleValue();
			}
		});
		runTimeLimit.setModel(new SpinnerNumberModel(15.0, 0.0, 99999.0, 1.0));
		runTimeLimit.setToolTipText("Time limit (minutes) for each run. Takes effect immediately.");
		panel_6.add(runTimeLimit);

		JPanel panel_1 = new JPanel();
		FlowLayout flowLayout = (FlowLayout) panel_1.getLayout();
		flowLayout.setVgap(2);
		add(panel_1);

		JLabel label_1 = new JLabel("Number of Epochs per Run");
		panel_1.add(label_1);

		numberEpochs = new JSpinner();
		numberEpochs.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				Integer value = (Integer) numberEpochs.getModel().getValue();
				MOEA_Config.MAX_EPOCHS = value.intValue();
			}
		});
		numberEpochs.setModel(new SpinnerNumberModel(512, 1, 999999, 1));
		numberEpochs.setToolTipText("Number of epochs to execute for each run. Takes effect immediately.");
		panel_1.add(numberEpochs);

		JPanel panel_2 = new JPanel();
		FlowLayout flowLayout_2 = (FlowLayout) panel_2.getLayout();
		flowLayout_2.setVgap(2);
		add(panel_2);

		JLabel label_2 = new JLabel("Number of Runs");
		panel_2.add(label_2);

		numberRuns = new JSpinner();
		numberRuns.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				Integer value = (Integer) numberRuns.getModel().getValue();
				MOEA_Config.MOEA_RUNS = value.intValue();
			}
		});
		numberRuns.setModel(new SpinnerNumberModel(256, 1, 999999, 1));
		numberRuns.setToolTipText("Number of runs (each of n epochs) to execute. Takes effect immediately.");
		panel_2.add(numberRuns);

		JPanel panel_3 = new JPanel();
		FlowLayout flowLayout_3 = (FlowLayout) panel_3.getLayout();
		flowLayout_3.setVgap(2);
		add(panel_3);

		JLabel label_3 = new JLabel("Population Size");
		panel_3.add(label_3);

		populationSize = new JSpinner();
		populationSize.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				Integer value = (Integer) populationSize.getModel().getValue();
				MOEA_Config.POPULATION_SIZE = value.intValue();
			}
		});
		populationSize.setModel(new SpinnerNumberModel(256, 20, 999999, 1));
		populationSize.setToolTipText("Size of the population. Takes effect on the next run.");
		panel_3.add(populationSize);

		JPanel panel_4 = new JPanel();
		FlowLayout flowLayout_4 = (FlowLayout) panel_4.getLayout();
		flowLayout_4.setVgap(2);
		add(panel_4);

		JLabel label_4 = new JLabel("Take Runtime Screenshots");
		panel_4.add(label_4);

		screenshotsCB = new JCheckBox("", MOEA_Config.SCREENSHOTS_ENABLED);
		screenshotsCB.setToolTipText("If enabled takes a screenshot of the GUI every epoch.");
		panel_4.add(screenshotsCB);

		JPanel panel_5 = new JPanel();
		FlowLayout flowLayout_5 = (FlowLayout) panel_5.getLayout();
		flowLayout_5.setVgap(2);
		add(panel_5);

		JLabel label_5 = new JLabel("Take Screenshot of the Last Epoch");
		panel_5.add(label_5);

		lastEpochScreenshotCB = new JCheckBox("", MOEA_Config.LAST_EPOCH_SCREENSHOT);
		lastEpochScreenshotCB.setToolTipText("If enabled takes a screenshot of the GUI every epoch.");
		panel_5.add(lastEpochScreenshotCB);
	}

	public boolean isGraphsEnabled() {
		return graphsEnabledCB.isSelected();
	}

	public boolean isScreenshotsEnabled() {
		return screenshotsCB.isSelected();
	}

	public boolean isLastEpochScreenshotEnabled() {
		return lastEpochScreenshotCB.isSelected();
	}

	public void setNumberEpochs(int v) {
		numberEpochs.setValue(v);
	}

	public void setNumberRuns(int v) {
		numberRuns.setValue(v);
	}

	public void setPopulationSize(int v) {
		populationSize.setValue(v);
	}
	
	public void setRunTimeLimit(double v) {
		runTimeLimit.setValue(v);
	}
}
