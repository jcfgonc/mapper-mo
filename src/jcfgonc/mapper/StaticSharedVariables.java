package jcfgonc.mapper;

import java.util.HashSet;

import org.apache.commons.math3.random.RandomAdaptor;

import graph.StringGraph;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.dictionary.Dictionary;
import structures.SynchronizedSeriarizableCache;
import structures.UnorderedPair;

public class StaticSharedVariables {

	public static StringGraph inputSpace;
	public static RandomAdaptor random;
	public static Object2DoubleOpenHashMap<UnorderedPair<String>> wordPairScores;
	public static Object2DoubleOpenHashMap<String> vitalRelations;
	public static SynchronizedSeriarizableCache<UnorderedPair<String>, Integer> refPairInnerDistanceCache = //
			new SynchronizedSeriarizableCache<>(MOEA_Config.CACHE_FILENAME, MOEA_Config.CACHE_SAVE_TIMEOUT);
	public static Dictionary dictionary; // wordnet dictionary
	public static HashSet<String> stopWords;
	public static StringGraph inputSpaceForPOS;

	static {
		try {
			dictionary = Dictionary.getDefaultResourceInstance();
		} catch (JWNLException e) {
			e.printStackTrace();
		}
	}
}
