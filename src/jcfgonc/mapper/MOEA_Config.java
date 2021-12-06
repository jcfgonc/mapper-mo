package jcfgonc.mapper;

import java.util.Set;

public class MOEA_Config {
	public static final String WINDOW_TITLE = "MapperMO - Multiple Objective Conceptual Mapper";
	/**
	 * name of the algorithm to be used by the MOEA-Framework
	 */
	public static final String ALGORITHM = "eNSGA2";
	public static final double eNSGA2_epsilon = 0.01; // default is 0.01
	public static final int eNSGA2_windowSize = 256; // epoch to trigger eNSGA2 population injection
	public static final int eNSGA2_maxWindowSize = 640; // epoch to trigger eNSGA2 hard restart
	public static final int NSGA3_divisionsOuter = 4; // decrease with increasing dimensions/objectives
	public static final int NSGA3_divisionsInner = 1; // increase with increasing dimensions/objectives
	/**
	 * number of (constant) solutions in the population
	 */
	public static int POPULATION_SIZE = 256;
	/**
	 * maximum number of epochs/generations to iterate
	 */
	public static int MAX_EPOCHS = 999999;
	/**
	 * maximum number of MOEA runs (each run iterates max_epochs)
	 */
	public static int MOEA_RUNS = 999999;
	/*
	 * maximum amount of time (minutes) allowed for each MOEA run
	 */
	public static double MAX_RUN_TIME = 999999.0;

	public static final String inputSpacePath = "data/conceptnet5v45.csv";
	public static final String wordembedding_filename = "D:\\\\Temp\\\\ontologies\\\\word emb\\\\ConceptNet Numberbatch 19.08\\\\numberbatch-en.txt";
	public static final String synonyms_filename = "data/synonyms.txt";
	public static final String wordPairScores_filename = "data/relation_pair_scores.tsv";
	public static final String vitalRelationsPath = "data/vital_relations.tsv";

	/**
	 * relations which direction is irrelevant (used map opposing left/right edges from the concept pairs)
	 */
	public static final Set<String> undirectedRelations = Set.of("synonym", "antonym", "relatedto", "similarto");

	public static final Set<String> uselessRelations = Set.of("similarto", "derivedfrom", "hascontext", "relatedto");

	public static final Set<String> uselessWords = Set.of("that", "than", "this", "my", "your", "his", "her", "he", "hers", "these");

	public static final String fixedConceptLeft = null;
	public static final String fixedConceptRight = null;

	public static final char CONCEPT_WORD_SEPARATOR = ' ';

	public static final String stopWordsPath = "data/english stop words basic.txt";

	public static final String screenshotsFolder = "screenshots";
	public static final boolean GRAPHS_ENABLED = true;
	public static final boolean SCREENSHOTS_ENABLED = false;
	public static final boolean LAST_EPOCH_SCREENSHOT = true;

	public static final int MAXIMUM_NUMBER_OF_CONCEPT_PAIRS = 8;
	// mutation controls
	public static final double REFPAIR_MUTATION_PROBABILITY = 0.125;
	public static final double LOCAL_JUMP_PROBABILITY = 0.88; // local to global jump probability - currently not used
	public static final int REFPAIR_JUMP_RANGE = 2;
	public static final double JUMP_PROBABILITY_POWER = 5.2;
	public static final int REFPAIR_ISOMORPHISM_MAX_DEEPNESS = 16;
	public static final int NUMBER_MUTATION_TRIES = 10;
	public static final int REFERENCE_PAIRINNER_DISTANCE_CALCULATION_LIMIT = 16;
	public static final int CACHE_SAVE_TIMEOUT = 1 * 60;
	public static final String REFPAIR_CACHE_FILENAME = "refPairInnerDistanceCache.dat";
	public static final String POS_CACHE_FILENAME = "posCache.dat";
}
