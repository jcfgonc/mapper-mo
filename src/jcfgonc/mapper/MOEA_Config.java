package jcfgonc.mapper;

import java.io.File;
import java.util.Set;

import org.ini4j.Profile.Section;
import org.ini4j.Wini;

import utils.VariousUtils;

public class MOEA_Config {

	private static Section INI_SECTION;
	static {
		try {
			Wini ini = new Wini(new File("data/config.ini"));
			MOEA_Config.INI_SECTION = ini.get("MOEA_Config");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static final String WINDOW_TITLE = INI_SECTION.get("WINDOW_TITLE", String.class);
	/**
	 * name of the algorithm to be used by the MOEA-Framework
	 */
	public static final String ALGORITHM = INI_SECTION.get("ALGORITHM", String.class);
	public static final double eNSGA2_epsilon = INI_SECTION.get("eNSGA2_epsilon", Double.class);
	public static final int eNSGA2_windowSize = INI_SECTION.get("eNSGA2_windowSize", Integer.class);
	public static final int eNSGA2_maxWindowSize = INI_SECTION.get("eNSGA2_maxWindowSize", Integer.class);
	public static final int NSGA3_divisionsOuter = INI_SECTION.get("NSGA3_divisionsOuter", Integer.class);
	public static final int NSGA3_divisionsInner = INI_SECTION.get("NSGA3_divisionsInner", Integer.class);
	/**
	 * number of (constant) solutions in the population
	 */
	public static int POPULATION_SIZE = INI_SECTION.get("POPULATION_SIZE", Integer.class);
	/**
	 * maximum number of epochs/generations to iterate
	 */
	public static int MAX_EPOCHS = INI_SECTION.get("MAX_EPOCHS", Integer.class);
	/**
	 * maximum number of MOEA runs (each run iterates max_epochs)
	 */
	public static int MOEA_RUNS = INI_SECTION.get("MOEA_RUNS", Integer.class);
	/*
	 * maximum amount of time (minutes) allowed for each MOEA run
	 */
	public static double MAX_RUN_TIME = INI_SECTION.get("MAX_RUN_TIME", Double.class);

	public static final String inputSpacePath = INI_SECTION.get("inputSpacePath", String.class);
//	public static final String wordembedding_filename = INI_SECTION.get("wordembedding_filename", String.class); // used in the blenderMO
//	public static final String synonyms_filename = INI_SECTION.get("synonyms_filename", String.class); // used in the blenderMO
//	public static final String wordPairScores_filename = INI_SECTION.get("wordPairScores_filename", String.class); // used in the blenderMO
	public static final String vitalRelationsPath = INI_SECTION.get("vitalRelationsPath", String.class);
	/**
	 * relations which direction is irrelevant (used map opposing left/right edges from the concept pairs)
	 */
	public static final Set<String> undirectedRelations = Set.of(VariousUtils.fastSplitWhiteSpace(INI_SECTION.get("undirectedRelations", String.class)));
	/**
	 * these relations are removed from the KB
	 */
	public static final Set<String> uselessRelations = Set.of(VariousUtils.fastSplitWhiteSpace(INI_SECTION.get("uselessRelations", String.class)));
	/**
	 * concepts with at least one of these words have no POS
	 */
	public static final Set<String> uselessWords = Set.of(VariousUtils.fastSplitWhiteSpace(INI_SECTION.get("uselessWords", String.class)));

	public static String fixedConceptLeft = null;
	public static String fixedConceptRight = null;
	/**
	 * char separating words in the KB, "this is a concept containing words separated by space"
	 */
	public static final char CONCEPT_WORD_SEPARATOR = INI_SECTION.get("CONCEPT_WORD_SEPARATOR", String.class).charAt(1);

	public static final String stopWordsPath = INI_SECTION.get("stopWordsPath", String.class);
	public static final String screenshotsFolder = INI_SECTION.get("screenshotsFolder", String.class);

	public static final boolean GRAPHS_ENABLED = true;
	public static final boolean SCREENSHOTS_ENABLED = false;
	public static final boolean LAST_EPOCH_SCREENSHOT = false;

	public static final int MAXIMUM_NUMBER_OF_CONCEPT_PAIRS = INI_SECTION.get("MAXIMUM_NUMBER_OF_CONCEPT_PAIRS", Integer.class);
	// mutation controls
	public static final double REFPAIR_MUTATION_PROBABILITY = INI_SECTION.get("REFPAIR_MUTATION_PROBABILITY", Double.class);
	public static final double JUMP_PROBABILITY_POWER = INI_SECTION.get("JUMP_PROBABILITY_POWER", Double.class);
	public static final int REFPAIR_JUMP_RANGE = INI_SECTION.get("REFPAIR_JUMP_RANGE", Integer.class);
	public static final int REFPAIR_ISOMORPHISM_MAX_DEEPNESS = INI_SECTION.get("REFPAIR_ISOMORPHISM_MAX_DEEPNESS", Integer.class);
	public static final int NUMBER_MUTATION_TRIES = INI_SECTION.get("NUMBER_MUTATION_TRIES", Integer.class);
	public static final int REFERENCE_PAIRINNER_DISTANCE_CALCULATION_LIMIT = INI_SECTION.get("REFERENCE_PAIRINNER_DISTANCE_CALCULATION_LIMIT", Integer.class);
	public static final int CACHE_SAVE_TIMEOUT = INI_SECTION.get("CACHE_SAVE_TIMEOUT", Integer.class);

	public static final String REFPAIR_CACHE_FILENAME = INI_SECTION.get("REFPAIR_CACHE_FILENAME", String.class);
	public static final String POS_CACHE_FILENAME = INI_SECTION.get("POS_CACHE_FILENAME", String.class);
	public static final String MOEA_ICON_PATH = INI_SECTION.get("MOEA_ICON_PATH", String.class);
	public static final String saveFolder = INI_SECTION.get("saveFolder", String.class);
}
