package jcfgonc.mapper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import graph.DirectedMultiGraph;
import graph.GraphAlgorithms;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import structures.MapOfList;
import structures.OrderedPair;
import utils.VariousUtils;

public class ObjectiveEvaluationUtils {

	public static DescriptiveStatistics calculateVitalRelationsStatistics(DirectedMultiGraph<OrderedPair<String>, String> graph,
			Object2DoubleOpenHashMap<String> relationWeights) {
		DescriptiveStatistics ds = new DescriptiveStatistics();
		// histogram of relations
		Object2IntOpenHashMap<String> relHist = GraphAlgorithms.countRelations(graph);

		for (Object2IntMap.Entry<String> entry : relHist.object2IntEntrySet()) {
			String relation = entry.getKey();
			int occurrences = entry.getIntValue();
			double weight = relationWeights.getDouble(relation);

			for (int i = 0; i < occurrences; i++) {
				ds.addValue(weight);
			}
		}
		return ds;
	}

	/**
	 * Calculates statistics about the number of relations of each type in the blend. Returns an array with the measurements mean, stddev and number of
	 * relations of each type.
	 * 
	 * @param graph
	 * @return
	 */
	public static double[] calculateRelationStatistics(DirectedMultiGraph<OrderedPair<String>, String> graph) {
		// eg {partof=>1, isa=>7, synonym=>1, derivedfrom=>1, knownfor=>1}
		Object2IntOpenHashMap<String> relHist = GraphAlgorithms.countRelations(graph);
		int numRelations = relHist.size();
		double mean = Double.NaN;
		double stddev = Double.NaN;
		if (numRelations == 0) {
			throw new RuntimeException("empty relation histogram, unable to compute statistics");
		} else {
			DescriptiveStatistics ds = GraphAlgorithms.getRelationStatisticsNormalized(relHist, graph.getNumberOfEdges());
//			System.out.printf("%d\t%f\t%f\t%f\t%f\t%s\n", ds.getN(), ds.getMin(), ds.getMean(), ds.getMax(), ds.getStandardDeviation(),
//					relHist.toString());
			mean = ds.getMean(); // 0...1
			stddev = ds.getStandardDeviation();
		}
		double[] stats = new double[3];
		stats[0] = mean;
		stats[1] = stddev;
		stats[2] = numRelations;
		return stats;
	}

	public static <T> double[] calculateWordsPerConceptStatistics(DirectedMultiGraph<OrderedPair<T>, T> graph) {
		// array where each element is the number of words in a vertex
		int[] conceptWords = countWordsPerConcept(graph);
		double[] asDouble = new double[conceptWords.length];
		for (int i = 0; i < conceptWords.length; i++) {
			int numWords = conceptWords[i];
			if (numWords <= 3) { // 2/3 words per concept is acceptable, make them equal to one
				numWords = 1;
			}
			asDouble[i] = numWords;
		}

		if (conceptWords.length < 2) {
			throw new RuntimeException("pair graph with less than two words");
		}
		DescriptiveStatistics ds = new DescriptiveStatistics(asDouble);
		double[] stats = new double[4];
		stats[0] = ds.getMean();
		stats[1] = ds.getStandardDeviation();
		stats[2] = ds.getMin();
		stats[3] = ds.getMax();
		return stats;
	}

	public static <T> int[] countWordsPerConcept(DirectedMultiGraph<OrderedPair<T>, T> graph) {
		// list of all individual concepts extracted from the concept pairs of the graph
		ArrayList<T> concepts = new ArrayList<T>(graph.getNumberOfVertices() * 2);
		for (OrderedPair<T> vertex : graph.vertexSet()) {
			concepts.add(vertex.getLeftElement());
			concepts.add(vertex.getRightElement());
		}
		int numConcepts = concepts.size();
		int[] conceptWords = new int[numConcepts];
		for (int i = 0; i < numConcepts; i++) {
			T concept = concepts.get(i);
			String[] words = VariousUtils.fastSplit(concept.toString(), MOEA_Config.CONCEPT_WORD_SEPARATOR);
			int numWords = words.length;
			conceptWords[i] = numWords;
		}
		return conceptWords;
	}

	public static double minimumRadialDistanceFunc(double center, double diameter, double x) {
		if (x >= center - diameter && x <= center + diameter)
			return 0;
		return Math.ceil(Math.abs(center - x) - diameter);
	}

	public static double assymetricParabolaFunc(int minorPower, int higherPower, double x) {
		if (x < 0) {
			if (minorPower % 2 == 0) {
				return Math.pow(x, minorPower);
			} else {
				return -Math.pow(x, minorPower); // always positive
			}
		} else {
			return Math.pow(x, higherPower);
		}
	}

	public static double idealDegree(DirectedMultiGraph<OrderedPair<String>, String> pairGraph, OrderedPair<String> referencePair) {
		double degree = pairGraph.degreeOf(referencePair);
		final double idealDegree = 5;
		double val = assymetricParabolaFunc(4, 2, degree - idealDegree);
		return val;
	}

	public static double calculateSubTreesBalance(DirectedMultiGraph<OrderedPair<String>, String> pairGraph, OrderedPair<String> referencePair) {
		// calculates strictly nearby balance
		Object2IntOpenHashMap<OrderedPair<String>> childrenTree = MappingAlgorithms.countNumberOfChildrenPerSubTree(pairGraph, referencePair);
		Set<OrderedPair<String>> neighborVertices = pairGraph.getNeighborVertices(referencePair);
		DoubleArrayList subTreeCount = new DoubleArrayList(neighborVertices.size());
		for (OrderedPair<String> vertex : neighborVertices) {
			subTreeCount.add(childrenTree.getInt(vertex) + 1);
		}
		double[] elements = subTreeCount.elements();
		DescriptiveStatistics ds = new DescriptiveStatistics(elements);
		double stddev = ds.getStandardDeviation();
//		if (subTreeCount.size() > 1) {
//			System.lineSeparator();
//		}
		return stddev;
	}

	public static int calculateRelationAsymmetryPenalty(//
			MapOfList<OrderedPair<String>, String> priorRelations, //
			HashSet<OrderedPair<String>> terminalSet) {
		int numConflictingRelations = 0;
		// for (Entry<OrderedPair<String>, List<String>> terminal : priorRelations.entrySet()) {
		// List<String> relationList = terminal.getValue();
		for (OrderedPair<String> terminal : terminalSet) { // iterate only in terminal vertices
			List<String> relationList = priorRelations.get(terminal);
			if (containsOpposingRelations(relationList)) {
				numConflictingRelations++;
			}
		}
		return numConflictingRelations;
	}

	private static boolean containsOpposingRelations(List<String> relationList) {
		// check for ISA
		if (relationList.size() < 2) {
			return false;
		}
		return (relationList.contains("+isa") && relationList.contains("-isa")) //
				|| (relationList.contains("+partof") && relationList.contains("-partof")) //
				|| (relationList.contains("+capableof") && relationList.contains("-capableof")) //
				|| (relationList.contains("+usedfor") && relationList.contains("-usedfor")) //
		;
	}

}
