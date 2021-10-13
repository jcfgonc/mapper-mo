package jcfgonc.mapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import graph.DirectedMultiGraph;
import graph.GraphAlgorithms;
import graph.StringEdge;
import graph.StringGraph;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import structures.Mapping;
import structures.OrderedPair;
import structures.UnorderedPair;
import utils.VariousUtils;

public class LogicUtils {

	/**
	 * creates a mapping from each element of the given concept set to a unique and consecutive variable named Xi, i in 0...set size.
	 * 
	 * @param pattern
	 * @return
	 */
	public static HashMap<String, String> createConceptToVariableMapping(Set<String> vertexSet) {
		HashMap<String, String> conceptToVariable = new HashMap<>(vertexSet.size() * 2);
		int varCounter = 0;
		for (String concept : vertexSet) {
			String varName = "X" + varCounter;
			conceptToVariable.put(concept, varName);
			varCounter++;
		}
		return conceptToVariable;
	}

	public static DescriptiveStatistics calculatePresenceVitalRelations(DirectedMultiGraph<OrderedPair<String>, String> graph,
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
	 * Calculates the arithmetic mean of the scores of the relations present in the blend space. Used to calculate the vital relation score of the blend space.
	 * 
	 * @param graph
	 * @param relationWeights
	 * @return
	 */
	public static DescriptiveStatistics calculatePresenceVitalRelations(StringGraph graph, Object2DoubleOpenHashMap<String> relationWeights) {
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

	public static double calculateMeanImportantanceVitalRelations(StringGraph blendSpace, Object2DoubleOpenHashMap<String> vitalRelations) {
		double accum = 0;
		HashSet<String> checkedLabels = new HashSet<String>();
		for (StringEdge edge : blendSpace.edgeSet()) {
			String label = edge.getLabel();
			if (checkedLabels.contains(label)) {
				continue;
			}
			checkedLabels.add(label);
			double weight = vitalRelations.getDouble(label);
			accum += weight;
		}
		accum = accum / checkedLabels.size();
		return accum;
	}

	/**
	 * if 1 means both input spaces are equally present in the blend and in the other end 0 if one or both are not present
	 * 
	 * @param blendSpace
	 * @param mapping
	 * @return
	 */
	public static double calculateInputSpacesBalance(StringGraph blendSpace, Mapping<String> mapping) {
		Set<String> leftConcepts = mapping.getLeftConcepts();
		Set<String> rightConcepts = mapping.getRightConcepts();
		int leftCount = 0;
		int rightCount = 0;
		for (String concept : blendSpace.getVertexSet()) {
			if (concept.indexOf('|') >= 0) { // blended concept
				String[] concepts = VariousUtils.fastSplit(concept, '|');
				String left = concepts[0];
				String right = concepts[1];
				// debug: this could happen if the blended concept is of the sort right|left instead of left|right
				if (!leftConcepts.contains(left)) {
					System.err.printf("leftConcepts does not contain the left part (%s) of the blended concept\n", left);
				}
				if (!rightConcepts.contains(right)) {
					System.err.printf("rightConcepts does not contain the right part (%s) of the blended concept\n", right);
				}
				leftCount += blendSpace.degreeOf(concept);
				rightCount += blendSpace.degreeOf(concept);
			} else {
				if (leftConcepts.contains(concept)) { // used in the mapping as a left concept
					leftCount += blendSpace.degreeOf(concept);
				} else if (rightConcepts.contains(concept)) {// used in the mapping as a right concept
					rightCount += blendSpace.degreeOf(concept);
				} else { // not referenced in the mapping
				}
			}
		}
		// prevent divide by zero
		if (leftCount == 0 || rightCount == 0) {
			return 0; // return 0 because we want both input spaces to be somewhat present
		}
		double u = (double) Math.min(leftCount, rightCount) / Math.max(leftCount, rightCount);
		return u;
	}

	/**
	 * Calculates statistics about the number of relations of each type in the blend. Returns an array with the measurements mean, stddev and number of
	 * relations of each type.
	 * 
	 * @param graph
	 * @return
	 */
	public static double[] calculateRelationStatistics(StringGraph graph) {
		double[] stats = new double[3];
		Object2IntOpenHashMap<String> relHist = GraphAlgorithms.countRelations(graph);
		int numRelations = relHist.size();
		double mean = 0;
		double stddev = 1.1; // higher value means there is a relation with a higher frequency than the other relations
		if (numRelations == 0) {
			throw new RuntimeException("empty relation histogram, unable to compute statistics");
		} else if (numRelations == 1) {
		} else {
			DescriptiveStatistics ds = GraphAlgorithms.getRelationStatisticsNormalized(relHist, graph.numberOfEdges());
//			System.out.printf("%d\t%f\t%f\t%f\t%f\t%s\n", ds.getN(), ds.getMin(), ds.getMean(), ds.getMax(), ds.getStandardDeviation(),
//					relHist.toString());
			mean = ds.getMean(); // 0...1
			stddev = ds.getStandardDeviation();
		}
		stats[0] = mean;
		stats[1] = stddev;
		stats[2] = numRelations;
		return stats;
	}

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

	/**
	 * calculates mixing of nearby concepts belonging to different subsets (left/right) of the mapping
	 * 
	 * @param blendSpace
	 * @param mapping
	 * @return
	 */
	public static int calculateMappingMix(StringGraph blendSpace, Mapping<String> mapping) {
		int mix = 0;
		HashSet<UnorderedPair<String>> checkedPairs = new HashSet<UnorderedPair<String>>(16, 0.333f);
		Set<String> leftConcepts = mapping.getLeftConcepts();
		Set<String> rightConcepts = mapping.getRightConcepts();
		for (String concept0 : blendSpace.getVertexSet()) {
			Set<String> neighSet = blendSpace.getNeighborVertices(concept0);
			for (String concept1 : neighSet) {

				UnorderedPair<String> neighPar = new UnorderedPair<String>(concept0, concept1);
				if (checkedPairs.contains(neighPar))
					continue;
				checkedPairs.add(neighPar);

				if (isBlend(concept0)) { // blended concept0
					if (isBlend(concept1)) { // blended concept0 connected to another blended concept1
						mix++;
					} else { // blended concept0 connected to a normal concept1
						if (mapping.containsConcept(concept1)) { // concept1 present in the mapping?
							mix++;
						}
					}
				} else { // normal concept0
					if (isBlend(concept1)) { // normal concept0 connected to a blended concept1
						if (mapping.containsConcept(concept0)) { // concept0 present in the mapping?
							mix++;
						}
					} else { // normal concept0 connected to a normal concept1
						if (leftConcepts.contains(concept0) && rightConcepts.contains(concept1) || //
								leftConcepts.contains(concept1) && rightConcepts.contains(concept0)) {
							mix++;
						}
					}
				}
			}
		}
		return mix;
	}

	public static boolean isBlend(String concept) {
		return concept.indexOf('|') >= 0;
	}

	public static double calculateNovelty(StringGraph blendSpace, StringGraph inputSpace) {
		int uniqueEdges = 0;
		for (StringEdge edge : blendSpace.edgeSet()) {
			if (!inputSpace.containsEdge(edge)) {
				uniqueEdges++;
			}
		}
		int numberOfEdges = blendSpace.numberOfEdges();
		double novelty = (double) uniqueEdges / numberOfEdges;
		return novelty;
	}

	public static double calculateBlendedConceptsPercentage(StringGraph blendSpace) {
		int count = GraphAlgorithms.countBlendedConcepts(blendSpace);
		double p = (double) count / blendSpace.numberOfVertices();
		return p;
	}

	/**
	 * Calculates the mean of the number of words per concept in the blend space. Words are assumed to be separated by a single underscore, eg, "two_words".
	 * 
	 * @param blendSpace
	 * @return
	 */
	public static double calculateMeanOfWordsPerConcept(StringGraph blendSpace) {
		double accum = 0;
		int[] conceptWords = calculateWordsPerConcept(blendSpace);
		for (int numWords : conceptWords) {
			accum += numWords;
		}
		double mean = accum / (double) conceptWords.length;
		return mean;
	}

	public static double calculateWordsPerConceptScore(StringGraph blendSpace) {
		double accum = 0;
		int[] conceptWords = calculateWordsPerConcept(blendSpace);
		for (int numWords : conceptWords) {
			if (numWords == 3) { // two words per concept is acceptable, make the same as one
				numWords = 1;
			}
			accum += numWords;
		}
		double mean = accum / (double) conceptWords.length;
		return mean;
	}

	public static int[] calculateWordsPerConcept(StringGraph blendSpace) {
		ArrayList<String> concepts = new ArrayList<String>(blendSpace.numberOfVertices() * 2);
		// create list of simple concepts (split blended into the two components)
		for (String concept : blendSpace.getVertexSet()) {
			if (concept.indexOf('|') >= 0) {
				String[] bconcepts = VariousUtils.fastSplit(concept, '|');
				concepts.add(bconcepts[0]);
				concepts.add(bconcepts[1]);
			} else {
				concepts.add(concept);
			}
		}
		int numConcepts = concepts.size();
		int[] conceptWords = new int[numConcepts];
		for (int i = 0; i < numConcepts; i++) {
			String concept = concepts.get(i);
			String[] words = VariousUtils.fastSplit(concept, '_');
			int numWords = words.length;
			conceptWords[i] = numWords;
		}
		return conceptWords;
	}

	public static int countChars(String str, char c) {
		int count = 0;
		for (int i = 0; i < str.length(); i++) {
			if (str.charAt(i) == c) {
				count++;
			}
		}
		return count;
	}

}
