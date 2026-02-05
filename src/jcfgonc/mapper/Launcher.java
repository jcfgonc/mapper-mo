package jcfgonc.mapper;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.util.HashMap;
import java.util.Properties;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.apache.commons.math3.random.RandomAdaptor;
import org.apache.commons.math3.random.SynchronizedRandomGenerator;
import org.apache.commons.math3.random.Well44497b;
import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.Problem;
import org.moeaframework.core.Solution;
import org.moeaframework.core.Variation;
import org.moeaframework.core.spi.OperatorFactory;
import org.moeaframework.core.spi.OperatorProvider;
import org.moeaframework.util.TypedProperties;

import graph.DirectedMultiGraph;
import graph.GraphReadWrite;
import graph.StringGraph;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import jcfgonc.mapper.structures.MappingStructure;
import jcfgonc.moea.generic.InteractiveExecutor;
import jcfgonc.moea.specific.CustomChromosome;
import jcfgonc.moea.specific.CustomMutation;
import jcfgonc.moea.specific.CustomProblem;
import jcfgonc.moea.specific.CustomResultsWriter;
import jcfgonc.moea.specific.ResultsWriter;
import net.sf.extjwnl.JWNLException;
import stream.SharedParallelConsumer;
import structures.OrderedPair;
import utils.OSTools;
import utils.VariousUtils;
import visual.OptionFrame;

public class Launcher {

	private static void registerCustomMutation() {
		OperatorFactory.getInstance().addProvider(new OperatorProvider() {
			public String getMutationHint(Problem problem) {
				return null;
			}

			public String getVariationHint(Problem problem) {
				return null;
			}

			public Variation getVariation(String name, Properties properties, Problem problem) {
				TypedProperties typedProperties = new TypedProperties(properties);

				if (name.equalsIgnoreCase("CustomMutation")) {
					double probability = typedProperties.getDouble("CustomMutation.Rate", 1.0);
					CustomMutation pm = new CustomMutation(probability);
					return pm;
				}

				// No match, return null
				return null;
			}
		});
	}

	public static void main(String[] args) throws NoSuchFileException, IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException, InterruptedException, JWNLException, URISyntaxException {

		System.out.println("Concept Mapper - Multiple Objective version");
		System.out.println("(C) Joao Goncalves / University of Coimbra");
		System.out.println("Contact: jcfgonc@gmail.com");

		String currentDir = System.getProperty("user.dir");
		System.out.println("Working directory: " + currentDir);
		RandomAdaptor random = new RandomAdaptor(new SynchronizedRandomGenerator(new Well44497b()));
		StaticSharedVariables.random = random;

		OSTools.setLowPriorityProcess();

		// read input space
		StringGraph kb = new StringGraph();
//		String kb_filename = "../UnoLibrary/new facts v3.tsv";
//		GraphReadWrite.readTSV(kb_filename, kb);
		GraphReadWrite.readTSV(MOEA_Config.inputSpacePath, kb);
		kb.showStructureSizes();

		// ------------
		System.out.println("-------------------");
//		GraphAlgorithms.printVertexDegree(kb);
//		GraphAlgorithms.printVertexDegreeIsa(kb);
//		GraphAlgorithms.printRelationHistogram("water", kb);
//		System.out.println(kb.edgesOf("water").toString().replace(", ", "\n"));
//		OpenAiLLM_Caller.saveCaches();
//		System.exit(0);
		// ------------

		SharedParallelConsumer.initialize(MOEA_Config.NUMBER_THREADS);
		System.out.println("Number of concurrent threads: " + MOEA_Config.NUMBER_THREADS);

		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

		// GraphAlgorithms.addMirroredCopyEdges(kb, MOEA_Config.undirectedRelations); // Do not exist in the new KB

		StaticSharedVariables.inputSpaceForPOS = new StringGraph(kb);

		// remove useless relations
//		kb.removeEdgesByLabel(MOEA_Config.uselessRelations); // Do not exist in the new KB
		StaticSharedVariables.inputSpace = kb;
		StringGraph inputSpace_for_RefPairInnerDistance = kb;// new StringGraph(kb);
		// remove generic concepts that usually relate lots of stuff
//		GraphAlgorithms.removeVerticesHighIsaDegree(inputSpace_for_RefPairInnerDistance, 50);

		StaticSharedVariables.inputSpace_for_RefPairInnerDistance = inputSpace_for_RefPairInnerDistance;

		// MOEA_Config.fixedConceptLeft = askUserForFixedConcept();

		// read pre-calculated semantic scores of word/relation pairs
//		Object2DoubleOpenHashMap<UnorderedPair<String>> wps = WordEmbeddingUtils.readWordPairScores(MOEA_Config.wordPairScores_filename);

		// read vital relations importance
		StaticSharedVariables.vitalRelations = readVitalRelations(MOEA_Config.vitalRelationsPath);
		StaticSharedVariables.relationTranslation = readRelationTranslation(MOEA_Config.relationTranslationPath);
//		StaticSharedVariables.wordPairScores = wps;

		// ------ MOEA SETUP

		// setup the mutation and the MOEA
		registerCustomMutation();
		Properties properties = setAlgorithmProperties();

		// personalize your results writer here
		ResultsWriter resultsWriter = new CustomResultsWriter();

		// personalize your constructor here
		CustomProblem problem = new CustomProblem();

		InteractiveExecutor ie = new InteractiveExecutor(problem, properties, resultsWriter, MOEA_Config.WINDOW_TITLE, random);

		// do 'k' runs of 'n' epochs
		// ArrayList<NondominatedPopulation> allResults = new
		// ArrayList<NondominatedPopulation>(totalRuns);
		for (int moea_run = 0; moea_run < MOEA_Config.MOEA_RUNS; moea_run++) {
			if (ie.isCanceled())
				break;
			properties.setProperty("maximumPopulationSize", Integer.toString(MOEA_Config.POPULATION_SIZE)); // default is 10 000
			properties.setProperty("populationSize", Integer.toString(MOEA_Config.POPULATION_SIZE));

			// do one run of 'n' epochs
			try {
				ie.execute(moea_run);
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}

		}
		ie.closeGUI();

		// terminate daemon threads
		System.exit(0);
	}

	private static Properties setAlgorithmProperties() {
		Properties properties = new Properties();
		properties.setProperty("operator", "CustomMutation");
		properties.setProperty("CustomMutation.Rate", "1.0");

		// eNSGA-II
		double eNSGA2_epsilon = MOEA_Config.eNSGA2_epsilon;
		if (eNSGA2_epsilon > 0)
			properties.setProperty("epsilon", Double.toString(eNSGA2_epsilon));
		int ensga2Windowsize = MOEA_Config.eNSGA2_windowSize;
		if (ensga2Windowsize > 0)
			properties.setProperty("windowSize", Integer.toString(ensga2Windowsize));
		int ensga2Maxwindowsize = MOEA_Config.eNSGA2_maxWindowSize;
		if (ensga2Maxwindowsize > 0)
			properties.setProperty("maxWindowSize", Integer.toString(ensga2Maxwindowsize));
		double injectionRate = 1.0 / 0.25;
		if (injectionRate > 0)
			properties.setProperty("injectionRate", Double.toString(injectionRate)); // population to archive ratio, default is 0.25

		// NSGA-III
		int nsga3Divisionsouter = MOEA_Config.NSGA3_divisionsOuter;
		if (nsga3Divisionsouter > 0)
			properties.setProperty("divisionsOuter", Integer.toString(nsga3Divisionsouter));
		int nsga3Divisionsinner = MOEA_Config.NSGA3_divisionsInner;
		if (nsga3Divisionsinner > 0)
			properties.setProperty("divisionsInner", Integer.toString(nsga3Divisionsinner));
		return properties;
	}

	@SuppressWarnings("unused")
	private static void saveIndividualSolutions(NondominatedPopulation currentResults, int run) {
		for (int i = 0; i < currentResults.size(); i++) {
			Solution solution = currentResults.get(i);
			CustomChromosome cc = (CustomChromosome) solution.getVariable(0); // unless the solution domain X has more
																				// than one dimension
			MappingStructure<String, String> mappingStructure = cc.getGene();
			DirectedMultiGraph<OrderedPair<String>, String> pairGraph = mappingStructure.getPairGraph();

			String filename = String.format("run%d_solution%d.tgf", run, i);
			try {
				GraphReadWrite.writeTGF(filename, pairGraph);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
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
			String[] cells = VariousUtils.fastSplitWhiteSpace(line);
			String relation = cells[0];
			double importance = Double.parseDouble(cells[1]);
			relationToImportance.put(relation, importance);
		}
		br.close();
		System.out.printf("using the definition of %d vital relations from %s\n", relationToImportance.size(), path);
		return relationToImportance;
	}

	public static HashMap<String, String> readRelationTranslation(String path) {
		HashMap<String, String> translationMap = new HashMap<>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(path, StandardCharsets.UTF_8), 1 << 24);
			String line;
			boolean firstLine = true;
			while ((line = br.readLine()) != null) {
				if (firstLine) {
					firstLine = false;
					continue;
				}
				line = line.strip();
				String[] cells = VariousUtils.fastSplit(line, '\t');
				String relation = cells[0];
				String translation = cells[1];
				translationMap.put(relation, translation);
			}
			br.close();
		} catch (IOException e) {
			// if this file reading blows up for any reason, return an empty map
			e.printStackTrace();
		}
		return translationMap;
	}

	@SuppressWarnings("unused")
	private static String askUserForFixedConcept() {
		StringGraph inputSpace = StaticSharedVariables.inputSpace;
		String imagePath = MOEA_Config.MOEA_ICON_PATH;
		ImageIcon ImageIcon = new ImageIcon(imagePath);

		String concept;
		do {
			concept = OptionFrame.showInputDialogStringRequest("MapperMO", ImageIcon.getImage(), "Type in one of the mapping's concepts\n(or nothing to select it randomly)\n\nClicking cancel exits the program.", "MapperMO");

			if (concept == null) { // cancel clicked
				System.exit(0);
			}
			concept = concept.strip();
			if (concept.isEmpty()) { // nothing entered
				return null;
			}
			// something was entered
			boolean valid = inputSpace.containsVertex(concept);
			if (valid)
				return concept;
			JOptionPane.showMessageDialog(null, "Knowledge Base does not contain the concept \"" + concept + "\"", //
					"MapperMO", JOptionPane.WARNING_MESSAGE);

		} while (true);
	}
}
