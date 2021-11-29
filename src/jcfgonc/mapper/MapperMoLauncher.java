package jcfgonc.mapper;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.HashSet;
import java.util.Properties;

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
import net.sf.extjwnl.JWNLException;
import structures.OrderedPair;
import structures.Ticker;
import structures.UnorderedPair;
import utils.VariousUtils;
import wordembedding.WordEmbeddingUtils;

public class MapperMoLauncher {

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
			UnsupportedLookAndFeelException, InterruptedException, JWNLException {

//		for (int i = 0; i < 10; i++) {
//			System.out.println(MapperUtils.minimumRadialDistanceFunc(3, 1, i));
//		}
//		System.exit(0);

		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

		RandomAdaptor random = new RandomAdaptor(new SynchronizedRandomGenerator(new Well44497b()));

		// read input space
		StringGraph inputSpace = readInputSpace(MOEA_Config.inputSpacePath);
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

		// read vital relations importance
		Object2DoubleOpenHashMap<String> vitalRelations = MapperUtils.readVitalRelations(MOEA_Config.vitalRelationsPath);

		// read pre-calculated semantic scores of word/relation pairs
		Object2DoubleOpenHashMap<UnorderedPair<String>> wps = WordEmbeddingUtils.readWordPairScores(MOEA_Config.wordPairScores_filename);

		StaticSharedVariables.stopWords = new HashSet<String>(VariousUtils.readFileRows(MOEA_Config.stopWordsPath));

		// Set<POS> x = GrammarUtils.checkPOS_InInputSpace("walk in closet", StaticSharedVariables.inputSpaceForPOS);
		// System.out.println(x);

//		GrammarUtils.checkPOS_InInputSpace("bathurst inlet", StaticSharedVariables.inputSpaceForPOS);
//		GrammarUtils.studyStringGraphVerticesPOS(StaticSharedVariables.inputSpaceForPOS);
//		Set<POS> pos = GrammarUtils.getConceptPOS("become strong", StaticSharedVariables.inputSpaceForPOS);
//		System.exit(0);

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
		properties.setProperty("divisionsOuter", Integer.toString(MOEA_Config.NSGA3_divisionsOuter));
		properties.setProperty("divisionsInner", Integer.toString(MOEA_Config.NSGA3_divisionsInner));

		String dateTimeStamp = VariousUtils.generateCurrentDateAndTimeStamp();
		String resultsFilename = String.format("moea_results_%s.tsv", dateTimeStamp);

		// personalize your results writer here
		ResultsWriter resultsWriter = new CustomResultsWriter();

		StaticSharedVariables.vitalRelations = vitalRelations;
		StaticSharedVariables.wordPairScores = wps;
		StaticSharedVariables.random = random;

//		GrammarUtils.getConceptPOS("drown underwater");

		// personalize your constructor here
		CustomProblem problem = new CustomProblem();

		InteractiveExecutor ie = new InteractiveExecutor(problem, properties, resultsFilename, resultsWriter,
				"MapperMO - Multiple Objective Conceptual Mapper");

		resultsWriter.writeFileHeader(resultsFilename, problem);

		// do 'k' runs of 'n' epochs
		int totalRuns = MOEA_Config.MOEA_RUNS;
		// ArrayList<NondominatedPopulation> allResults = new ArrayList<NondominatedPopulation>(totalRuns);
		for (int moea_run = 0; moea_run < totalRuns; moea_run++) {
			if (ie.isCanceled())
				break;
			// properties.setProperty("maximumPopulationSize", Integer.toString(MOEA_Config.POPULATION_SIZE * 2)); // default is 10 000
			properties.setProperty("populationSize", Integer.toString(MOEA_Config.POPULATION_SIZE));

			// do one run of 'n' epochs
			NondominatedPopulation currentResults = ie.execute(moea_run);

			// allResults.add(currentResults);
			resultsWriter.appendResultsToFile(resultsFilename, currentResults, problem);
//			saveIndividualSolutions(currentResults, moea_run);
		}
		resultsWriter.close();
		ie.closeGUI();
		// mergeAndSaveResults(String.format("moea_results_%s_merged.tsv", dateTimeStamp), allResults, problem, 0.01);

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
}
