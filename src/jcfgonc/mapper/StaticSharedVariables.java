package jcfgonc.mapper;

import org.apache.commons.math3.random.RandomAdaptor;

import frames.SeriarizableCache;
import graph.StringGraph;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import structures.UnorderedPair;

public class StaticSharedVariables {

	public static StringGraph inputSpace;
	public static RandomAdaptor random;
	public static Object2DoubleOpenHashMap<UnorderedPair<String>> wordPairScores;
	public static Object2DoubleOpenHashMap<String> vitalRelations;
	public static SeriarizableCache<UnorderedPair<String>, Integer> refPairInnerDistanceCache = //
			new SeriarizableCache<>(MOEA_Config.CACHE_FILENAME, MOEA_Config.CACHE_SAVE_TIMEOUT);

}
