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

import chatbots.openai.OpenAiLLM_Caller;
import graph.DirectedMultiGraph;
import graph.GraphAlgorithms;
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
import linguistics.GrammarUtilsCoreNLP;
import net.sf.extjwnl.JWNLException;
import stream.SharedParallelConsumer;
import structures.OrderedPair;
import structures.Ticker;
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

	public static void main(String[] args) throws NoSuchFileException, IOException, ClassNotFoundException, InstantiationException, IllegalAccessException,
			UnsupportedLookAndFeelException, InterruptedException, JWNLException, URISyntaxException {
		RandomAdaptor random = new RandomAdaptor(new SynchronizedRandomGenerator(new Well44497b()));
		StaticSharedVariables.random = random;

		OSTools.setLowPriorityProcess();

		// read input space
//		StringGraph inputSpace = readInputSpace(MOEA_Config.inputSpacePath);
		StringGraph inputSpace = null;// readInputSpace("verified.csv");
//		String[] toRename = "program language,meet,edit,nasa,ultralight aviation,film edit,publish,mythical be,public speak,naval aviation".split(",");
//		String[] rename = "programming language,meeting,editor,nasa personnel,ultralight aircraft,film editor,publisher,mythical being,public speaker,naval aviator"
//				.split(",");
//		for (int i = 0; i < toRename.length; i++) {
//			String concept = toRename[i];
//			String target = rename[i];
//			inputSpace.renameVertex(concept, target);
//		}

		// VariousUtils.countEdgeTargetsOf(inputSpace, "isa").toSystemOut(10);
		// System.out.println(OpenAiLLM_Caller.getMadeOf("civil engineer"));
		OpenAiLLM_Caller.runTest(inputSpace);
		// GrammarUtilsCoreNLP.testConcepts(inputSpace);

//		GraphReadWrite.writeCSV("verified.csv", inputSpace);

		System.exit(0);

		SharedParallelConsumer.initialize(MOEA_Config.NUMBER_THREADS);

		System.out.println("Concept Mapper - Multiple Objective version");
		System.out.println("(C) Joao Goncalves / University of Coimbra");
		System.out.println("Contact: jcfgonc@gmail.com");

		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

		// remove crap
		inputSpace.removeVerticesStartingWith("they ");
		inputSpace.removeVerticesStartingWith("some ");
		inputSpace.removeVerticesStartingWith("sometime");
		inputSpace.removeVerticesStartingWith("this ");
		inputSpace.removeVerticesStartingWith("you ");
		inputSpace.removeVerticesStartingWith("that ");
		inputSpace.removeVerticesStartingWith("sit on it");
		inputSpace.removeVerticesStartingWith("something you");
		GraphAlgorithms.addMirroredCopyEdges(inputSpace, MOEA_Config.undirectedRelations);
//		GraphReadWrite.writeCSV(MOEA_Config.inputSpacePath, inputSpace);

		StaticSharedVariables.inputSpaceForPOS = new StringGraph(inputSpace);

		// remove useless relations
		inputSpace.removeEdgesByLabel(MOEA_Config.uselessRelations);
		StaticSharedVariables.inputSpace = inputSpace;

		MOEA_Config.fixedConceptLeft = askUserForFixedConcept();

		// read vital relations importance
		Object2DoubleOpenHashMap<String> vitalRelations = readVitalRelations(MOEA_Config.vitalRelationsPath);
		HashMap<String, String> relationTranslation = readRelationTranslation(MOEA_Config.relationTranslationPath);

		// read pre-calculated semantic scores of word/relation pairs
//		Object2DoubleOpenHashMap<UnorderedPair<String>> wps = WordEmbeddingUtils.readWordPairScores(MOEA_Config.wordPairScores_filename);

		StaticSharedVariables.vitalRelations = vitalRelations;
//		StaticSharedVariables.wordPairScores = wps;
		StaticSharedVariables.relationTranslation = relationTranslation;

		// ------ MOEA SETUP

		// setup the mutation and the MOEA
		registerCustomMutation();
		Properties properties = new Properties();
		properties.setProperty("operator", "CustomMutation");
		properties.setProperty("CustomMutation.Rate", "1.0");

		// eNSGA-II
		properties.setProperty("epsilon", Double.toString(MOEA_Config.eNSGA2_epsilon));
		properties.setProperty("windowSize", Integer.toString(MOEA_Config.eNSGA2_windowSize));
		properties.setProperty("maxWindowSize", Integer.toString(MOEA_Config.eNSGA2_maxWindowSize));
//		properties.setProperty("injectionRate", Double.toString(1.0 / 0.25)); // population to archive ratio, default is 0.25

		// NSGA-III
//		properties.setProperty("divisionsOuter", Integer.toString(MOEA_Config.NSGA3_divisionsOuter));
//		properties.setProperty("divisionsInner", Integer.toString(MOEA_Config.NSGA3_divisionsInner));

		// personalize your results writer here
		ResultsWriter resultsWriter = new CustomResultsWriter();

		// personalize your constructor here
		CustomProblem problem = new CustomProblem();

		InteractiveExecutor ie = new InteractiveExecutor(problem, properties, resultsWriter, MOEA_Config.WINDOW_TITLE, random);

		// do 'k' runs of 'n' epochs
		// ArrayList<NondominatedPopulation> allResults = new ArrayList<NondominatedPopulation>(totalRuns);
		for (int moea_run = 0; moea_run < MOEA_Config.MOEA_RUNS; moea_run++) {
			if (ie.isCanceled())
				break;
			// properties.setProperty("maximumPopulationSize", Integer.toString(MOEA_Config.POPULATION_SIZE * 2)); // default is 10 000
			properties.setProperty("populationSize", Integer.toString(MOEA_Config.POPULATION_SIZE));

			// do one run of 'n' epochs
			ie.execute(moea_run);

		}
		ie.closeGUI();

		// terminate daemon threads
		System.exit(0);
	}

	@SuppressWarnings("unused")
	private static void saveIndividualSolutions(NondominatedPopulation currentResults, int run) {
		for (int i = 0; i < currentResults.size(); i++) {
			Solution solution = currentResults.get(i);
			CustomChromosome cc = (CustomChromosome) solution.getVariable(0); // unless the solution domain X has more than one dimension
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

	public static StringGraph readInputSpace(String inputSpacePath) throws IOException, NoSuchFileException {
		System.out.println("loading input space from " + inputSpacePath);
		StringGraph inputSpace = new StringGraph();
		Ticker ticker = new Ticker();
		GraphReadWrite.readCSV(inputSpacePath, inputSpace);
		inputSpace.showStructureSizes();
		System.out.println("loading took " + ticker.getTimeDeltaLastCall() + " s");
		System.out.println("-------");
		return inputSpace;
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
				line = line.trim();
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

	private static String askUserForFixedConcept() {
		StringGraph inputSpace = StaticSharedVariables.inputSpace;
		String imagePath = MOEA_Config.MOEA_ICON_PATH;
		ImageIcon ImageIcon = new ImageIcon(imagePath);

		String concept;
		do {
			concept = OptionFrame.showInputDialogStringRequest("MapperMO", ImageIcon.getImage(),
					"Type in one of the mapping's concepts\n(or nothing to select it randomly)\n\nClicking cancel exits the program.", "MapperMO");

			if (concept == null) { // cancel clicked
				System.exit(0);
			}
			concept = concept.trim();
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
