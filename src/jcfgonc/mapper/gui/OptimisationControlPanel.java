package jcfgonc.mapper.gui;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

public class OptimisationControlPanel extends JPanel {
	private static final long serialVersionUID = 2778229888241955365L;
	private InteractiveExecutorGUI gui;
	private JButton nextRunButton;
	private JButton stopButton;
	private AbstractButton bestSolutionButton;
	private JPanel panelTop;
	private JPanel panelMiddle;
	private JButton pauseButton;
	private JButton randomSolutionButton;

	public OptimisationControlPanel(InteractiveExecutorGUI interactiveExecutorGUI) {
		this.gui = interactiveExecutorGUI;

		setBorder(new TitledBorder(null, "Optimization Control", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		panelTop = new JPanel();
		FlowLayout flowLayout = (FlowLayout) panelTop.getLayout();
		flowLayout.setVgap(2);
		add(panelTop);

		panelMiddle = new JPanel();
		FlowLayout flowLayout_1 = (FlowLayout) panelMiddle.getLayout();
		flowLayout_1.setVgap(2);
		add(panelMiddle);

		bestSolutionButton = new JButton("Show Best Solution");
		panelTop.add(bestSolutionButton);
		bestSolutionButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				gui.showBestSolution(); 
			}
		});
		bestSolutionButton.setToolTipText("Dumps to a file a random solution chosen from the Non Dominated Set.");

		randomSolutionButton = new JButton("Show Random Solution");
		randomSolutionButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				gui.showRandomSolution();
			}
		});
		panelTop.add(randomSolutionButton);

		nextRunButton = new JButton("Next Run");
		panelMiddle.add(nextRunButton);
		nextRunButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				gui.skipCurrentRun();
			}
		});
		nextRunButton.setToolTipText("Stops the current optimization run and starts the next.");

		pauseButton = new JButton("Pause");
		panelMiddle.add(pauseButton);

		stopButton = new JButton("Stop Optimization");
		panelMiddle.add(stopButton);
		stopButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				gui.stopOptimization();
			}
		});
		stopButton.setToolTipText("Pauses the optimization procedure after the current epoch.");
		pauseButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				gui.togglePause();
				if (gui.isPaused()) {
					pauseButton.setText("Unpause");
				} else {
					pauseButton.setText("Pause");
				}
			}
		});
	}

}
