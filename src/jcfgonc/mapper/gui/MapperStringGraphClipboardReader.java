package jcfgonc.mapper.gui;

import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import graph.GraphAlgorithms;
import graph.GraphReadWrite;
import graph.StringEdge;
import graph.StringGraph;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import linguistics.AnalogyToText;
import utils.VariousUtils;
import visual.StringClipBoardListener;
import visual.VisualGraph;

public class MapperStringGraphClipboardReader {

	public static void main(String[] args) throws IOException {

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					createAndShowGUI();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	protected static void createAndShowGUI() throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException, IOException {
		Object2DoubleOpenHashMap<String> vitalRelations = readVitalRelations("data/vital_relations.tsv");
		HashSet<String> relevantRelations = selectRelevantRelations(vitalRelations, 0.9);

		System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");
		System.setProperty("org.graphstream.ui", "org.graphstream.ui.swingViewer.util.SwingDisplay");
		System.setProperty("org.graphstream.ui", "swing");

		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

		VisualGraph vg = new VisualGraph(0);

		JFrame mainFrame = new JFrame("StringGraph Clipboard Reader");
		mainFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		mainFrame.setPreferredSize(new Dimension(320, 240));
		mainFrame.pack();
		mainFrame.setVisible(true);

		mainFrame.add(vg.getDefaultView());

		mainFrame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				close(mainFrame);
			}
		});

		mainFrame.addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent e) {
			}

			@Override
			public void keyPressed(KeyEvent e) {
				int key = e.getKeyCode();

				if (key == KeyEvent.VK_R) {
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							vg.resetView();
						}
					});
				} else if (key == KeyEvent.VK_K) {
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							vg.shakeLayout();
						}
					});
				} else if (key == KeyEvent.VK_S) {
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							String filename = VariousUtils.generateCurrentDateAndTimeStamp() + ".tgf";
							try {
								vg.saveCurrentGraphToFile(filename);
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					});
				} else if (key == KeyEvent.VK_ESCAPE) {
					close(mainFrame);
				}

			}

			@Override
			public void keyReleased(KeyEvent e) {
			}

		});

		StringClipBoardListener cl = new StringClipBoardListener(clipboardText -> {
			try {
				handlePaste(vg, clipboardText, relevantRelations);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		});
		cl.start();
	}

	private static HashSet<String> selectRelevantRelations(Object2DoubleOpenHashMap<String> vitalRelations, double threshold) {
		HashSet<String> relevantRelations = new HashSet<>();
		for (it.unimi.dsi.fastutil.objects.Object2DoubleMap.Entry<String> entry : vitalRelations.object2DoubleEntrySet()) {
			String relation = entry.getKey();
			double priority = entry.getDoubleValue();
			if (priority >= threshold - 1e-3) {
				relevantRelations.add(relation);
			}
		}
		return relevantRelations;
	}

	public static Object2DoubleOpenHashMap<String> readVitalRelations(String path) throws IOException {
		Object2DoubleOpenHashMap<String> relationToImportance = new Object2DoubleOpenHashMap<String>();
		BufferedReader br = new BufferedReader(new FileReader(path, StandardCharsets.UTF_8), 1 << 24);
		String line;
		boolean firstLine = true;
		while ((line = br.readLine()) != null) {
			if (firstLine) {
				firstLine = false;
				continue;
			}
			String[] cells = VariousUtils.fastSplit(line, '\t');
			String relation = cells[0];
			double importance = Double.parseDouble(cells[1]);
			relationToImportance.put(relation, importance);
		}
		br.close();
		System.out.printf("using the definition of %d vital relations from %s\n", relationToImportance.size(), path);
		return relationToImportance;
	}

	private static void handlePaste(VisualGraph vg, String clipboardText, HashSet<String> relevantRelations) throws NoSuchFileException, IOException {
		StringGraph graph = null;
		String refpair = null;
		String[] columns = VariousUtils.fastSplit(clipboardText, '\t');
		if (columns.length == 1) {
			// only the mapping
			graph = GraphReadWrite.readCSVFromString(columns[0]);
			refpair = GraphAlgorithms.getHighestDegreeVertex(graph);
		} else if (columns.length == 2) {
			// ref pair followed by the mapping
			refpair = columns[0];
			graph = GraphReadWrite.readCSVFromString(columns[1]);
		}
		if (graph != null && !graph.isEmpty()) {
			StringGraph croppedGraph = selectRelevantRelations(graph, refpair, relevantRelations, 4);
			AnalogyToText.textifyAnalogy(refpair, croppedGraph);
			vg.refreshGraph(croppedGraph);
		}
	}

	private static StringGraph selectRelevantRelations(StringGraph graph, String refpair, HashSet<String> relevantRelations, int maxEdges) {
		StringGraph newGraph = new StringGraph();
		ArrayDeque<String> openSet = new ArrayDeque<>();
		HashSet<String> closedSet = new HashSet<>();
		// start in a given vertex
		openSet.add(refpair);
		// expand all neighbors
		outer: while (openSet.size() > 0) {
			String current = openSet.removeFirst();
			ArrayList<StringEdge> edgesOfCurrent = new ArrayList<>(graph.edgesOf(current));
			Collections.shuffle(edgesOfCurrent);
			for (StringEdge edge : edgesOfCurrent) {
				String label = edge.getLabel();
				if (relevantRelations.contains(label)) {
					newGraph.addEdge(edge);
					if (newGraph.numberOfEdges() >= maxEdges) {
						break outer;
					}
					String opposite = edge.getOppositeOf(current);
					if (!closedSet.contains(opposite)) {
						openSet.add(opposite);
					}
				}
			}
			closedSet.add(current);
			// openSet.addAll(newVertices);
			// openSet.removeAll(closedSet);
		}
		return newGraph;
	}

	private static void close(JFrame mainFrame) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				mainFrame.dispose();
				System.exit(0);
			}
		});
	}

}
