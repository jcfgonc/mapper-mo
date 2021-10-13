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
	private AbstractButton abortButton;
	private JButton printNDS_button;
	private JPanel panelTop;
	private JPanel panelMiddle;

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

		printNDS_button = new JButton("Print Non Dominated Set");
		printNDS_button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				gui.printNonDominatedSet();
			}
		});
		panelTop.add(printNDS_button);

		nextRunButton = new JButton("Next Run");
		nextRunButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				gui.skipCurrentRun();
			}
		});
		nextRunButton.setToolTipText("stops the current moea run and starts the next.");
		panelTop.add(nextRunButton);

		stopButton = new JButton("Stop Optimization");
		stopButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				gui.stopOptimization();
			}
		});
		stopButton.setToolTipText("Waits for the current epoch to complete and returns the best results so far.");
		panelMiddle.add(stopButton);

		abortButton = new JButton("Abort Optimization");
		abortButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// System.out.println((double) (horizontalPane.getDividerLocation()) / (horizontalPane.getWidth() - horizontalPane.getDividerSize()));
				gui.abortOptimization();
			}
		});
		abortButton.setToolTipText("Aborts the optimization by discarding the current epoch's results and returns the best results so far.");
		panelMiddle.add(abortButton);
	}

}
