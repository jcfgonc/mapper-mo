package jcfgonc.mapper;

import java.util.HashMap;

import org.apache.commons.math3.random.RandomAdaptor;

import graph.StringGraph;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import structures.SynchronizedSeriarizableHashMap;
import structures.UnorderedPair;

public class StaticSharedVariables {

	public static StringGraph inputSpace;
	public static RandomAdaptor random;
	public static Object2DoubleOpenHashMap<UnorderedPair<String>> wordPairScores;
	public static Object2DoubleOpenHashMap<String> vitalRelations;
	public static SynchronizedSeriarizableHashMap<UnorderedPair<String>, Integer> refPairInnerDistanceCache = //
			new SynchronizedSeriarizableHashMap<>("refPairInnerDistanceCache.dat", MOEA_Config.CACHE_SAVE_TIMEOUT);
	public static StringGraph inputSpaceForPOS;
	public static HashMap<String, String> relationTranslation;

}
