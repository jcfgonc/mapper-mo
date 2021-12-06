package jcfgonc.mapper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.math3.random.RandomGenerator;

import graph.DirectedMultiGraph;
import graph.GraphAlgorithms;
import graph.StringEdge;
import graph.StringGraph;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import jcfgonc.mapper.structures.MappingStructure;
import structures.MapOfSet;
import structures.OrderedPair;
import structures.UnorderedPair;
import utils.VariousUtils;

public class MappingAlgorithms {

	/**
	 * map edge label (+=outgoing, -=incoming) to the set of connected neighbors
	 * 
	 * @param reference
	 * @param inputSpace
	 * @param usedConcepts
	 * @return
	 */
	private static MapOfSet<String, String> mapEgdeLabelsDirToNeighbors(String reference, StringGraph inputSpace, HashSet<String> usedConcepts) {
		MapOfSet<String, String> dirLabelMap = new MapOfSet<>();
		Set<StringEdge> edges = inputSpace.edgesOf(reference);
		for (StringEdge edge : edges) {
			String oppositeConcept = edge.getOppositeOf(reference);
			if (usedConcepts.contains(oppositeConcept))
				continue;
			String label = edge.getLabel();
			String dirLabel;
			if (edge.incomesTo(reference)) {
				dirLabel = "-" + label;
			} else {
				dirLabel = "+" + label;
			}
			dirLabelMap.add(dirLabel, oppositeConcept);
		}
		return dirLabelMap;
	}

	public static HashMap<String, OrderedPair<String>> expandConceptPair(StringGraph inputSpace, DirectedMultiGraph<OrderedPair<String>, String> pairGraph,
			RandomGenerator random, OrderedPair<String> refPair, HashSet<String> usedConcepts) {
		// do the expansion
		HashMap<String, OrderedPair<String>> pairs;
		// get left/right edges
		String left = refPair.getLeftElement();
		String right = refPair.getRightElement();

		// left and right are expected to be different
		assert !left.equals(right) : "left and right concepts of a pair are expected to be different";

		usedConcepts.add(left);
		usedConcepts.add(right);
		// create left/right edge maps according to labels+direction
		// map edge label(+=outgoing, -=incoming) to set of connected neighbors
		MapOfSet<String, String> leftmap = mapEgdeLabelsDirToNeighbors(left, inputSpace, usedConcepts);
		MapOfSet<String, String> rightmap = mapEgdeLabelsDirToNeighbors(right, inputSpace, usedConcepts);

//		removeUselessRelations(leftmap);
//		removeUselessRelations(rightmap);

		// intersect maps' keys randomly
		pairs = matchLeftRightMapsAndSelectRandom(leftmap, rightmap, random, usedConcepts);
		// update the pair/mapping graph
		for (String dirLabel : pairs.keySet()) {
			if (pairGraph.getNumberOfVertices() >= MOEA_Config.MAXIMUM_NUMBER_OF_CONCEPT_PAIRS)
				break;
			OrderedPair<String> nextPair = pairs.get(dirLabel);

			// take care of tagged +- label
			String label = dirLabel;
			if (dirLabel.startsWith("+") || dirLabel.startsWith("-")) {
				label = dirLabel.substring(1);
			}

			if (dirLabel.charAt(0) == '-') {
				pairGraph.addEdge(nextPair, refPair, label);
			} else { // must be '+'
				pairGraph.addEdge(refPair, nextPair, label);
			}
		}
		return pairs;
	}

	/**
	 * Used by expandConceptPair() to remove useless relations (stored in MOEA_Config.uselessRelations). Currently not used.
	 * 
	 * @param map
	 */
	@SuppressWarnings("unused")
	private static void removeUselessRelations(MapOfSet<String, String> map) {
		Iterator<String> keyIterator = map.keySet().iterator();
		while (keyIterator.hasNext()) {
			String key = keyIterator.next();
			// remove + or - to match the list of useless relations
			if (key.startsWith("+") || key.startsWith("-")) {
				key = key.substring(1);
			}
			if (MOEA_Config.uselessRelations.contains(key)) {
				keyIterator.remove();
			}
		}
	}

	/**
	 * For each labeldir in common from both left/right maps (labeldir around each pair), select a random orderedpair of touching concepts.
	 * 
	 * @param leftmap
	 * @param rightmap
	 * @param random
	 * @param usedConcepts
	 * @return
	 */
	private static HashMap<String, OrderedPair<String>> matchLeftRightMapsAndSelectRandom(MapOfSet<String, String> leftmap, MapOfSet<String, String> rightmap,
			RandomGenerator random, HashSet<String> usedConcepts) {
		HashMap<String, OrderedPair<String>> relationToPair = new HashMap<>();
		Set<String> leftDirLabels = leftmap.keySet();
		Set<String> rightDirLabels = rightmap.keySet();
		for (String label : leftDirLabels) {
			// TODO ter cuidado aqui se avancar com relacoes nao direccionais
			if (rightDirLabels.contains(label)) {
				Set<String> leftInputSet = leftmap.get(label);
				Set<String> rightInputSet = rightmap.get(label);

				ArrayList<String> leftNeighbors = setToListExcluding(leftInputSet, usedConcepts);
				ArrayList<String> rightNeighbors = setToListExcluding(rightInputSet, leftInputSet, usedConcepts); // what's on the left can't be on the right

				if (leftNeighbors.isEmpty() || rightNeighbors.isEmpty())
					continue;

				// any of the left concepts can be paired with any of the right concepts
				String leftNeighbor = VariousUtils.getRandomElementFromCollection(leftNeighbors, random);
				String rightNeighbor = VariousUtils.getRandomElementFromCollection(rightNeighbors, random);

				usedConcepts.add(leftNeighbor);
				usedConcepts.add(rightNeighbor);

				OrderedPair<String> pair = new OrderedPair<String>(leftNeighbor, rightNeighbor);
				relationToPair.put(label, pair);
			}
		}
		return relationToPair;
	}

	private static <T> ArrayList<T> setToListExcluding(Set<T> inputSet, Set<T> exclusionSet) {
		ArrayList<T> asList = new ArrayList<>();
		for (T element : inputSet) {
			if (exclusionSet.contains(element))
				continue;
			asList.add(element);
		}
		return asList;
	}

	private static <T> ArrayList<T> setToListExcluding(Set<T> inputSet, Set<T> exclusionSet0, Set<T> exclusionSet1) {
		ArrayList<T> asList = new ArrayList<>();
		for (T element : inputSet) {
			if (exclusionSet0.contains(element) || exclusionSet1.contains(element))
				continue;
			asList.add(element);
		}
		return asList;
	}

	public static Object2IntOpenHashMap<OrderedPair<String>> createIsomorphism(StringGraph inputSpace,
			DirectedMultiGraph<OrderedPair<String>, String> pairGraph, RandomGenerator random, OrderedPair<String> refPair) {

		Object2IntOpenHashMap<OrderedPair<String>> pairDeepness = null;
		pairDeepness = new Object2IntOpenHashMap<>();
		pairDeepness.defaultReturnValue(-1);
		HashSet<String> closedSet = new HashSet<>();
		HashSet<String> usedConcepts = new HashSet<>();
		ArrayDeque<OrderedPair<String>> openSet = new ArrayDeque<>();
		openSet.addLast(refPair);
		pairDeepness.put(refPair, 0);
		// ---------init
		while (!openSet.isEmpty()) {
			if (pairGraph.getNumberOfVertices() >= MOEA_Config.MAXIMUM_NUMBER_OF_CONCEPT_PAIRS)
				break;

			OrderedPair<String> currentPair = openSet.removeFirst();
			// if (deepnessLimit >= 0) {
			int deepness = pairDeepness.getInt(currentPair);
			if (deepness >= MOEA_Config.REFPAIR_ISOMORPHISM_MAX_DEEPNESS)
				continue;
			int nextDeepness = deepness + 1;

			// expand a vertex not in the closed set
			if (closedSet.contains(currentPair.getLeftElement()) || closedSet.contains(currentPair.getRightElement()))
				continue;
			// get the vertex neighbors not in the closed set
			HashMap<String, OrderedPair<String>> expansion = expandConceptPair(inputSpace, pairGraph, random, currentPair, usedConcepts);
			for (OrderedPair<String> nextPair : expansion.values()) {
				if (closedSet.contains(currentPair.getLeftElement()) || closedSet.contains(currentPair.getRightElement()))
					continue;
				// put the neighbors in the open set
				openSet.addLast(nextPair);
				pairDeepness.put(nextPair, nextDeepness);
			}
			// vertex from the open set explored, remove it from further exploration
			closedSet.add(currentPair.getLeftElement());
			closedSet.add(currentPair.getRightElement());
		}

//		System.out.println("pairGraph's concepts:"+pairGraph.getNumberOfVertices());

		return pairDeepness;
	}

	/**
	 * Finds an isomorphism starting at the mapping structure's reference pair and stores it (as a graph) back in the structure.
	 * 
	 * @param inputSpace
	 * @param mappingStruct
	 * @param deepnessLimit
	 * @param random
	 */
	public static void updateMappingGraph(StringGraph inputSpace, MappingStructure<String, String> mappingStruct, RandomGenerator random) {
		DirectedMultiGraph<OrderedPair<String>, String> pairGraph = new DirectedMultiGraph<>();
		OrderedPair<String> referencePair = mappingStruct.getReferencePair();
		// create a random mapping using the reference pair
		@SuppressWarnings("unused")
		Object2IntOpenHashMap<OrderedPair<String>> pairDeepness;
		for (int tries = 0; tries < 10; tries++) {
			pairDeepness = MappingAlgorithms.createIsomorphism(inputSpace, pairGraph, random, referencePair);
			int nVertices = pairGraph.getNumberOfVertices();
			if (nVertices > 1) {
				break;
			}
		}
		// store results in the MappingStructure
		mappingStruct.setPairGraph(pairGraph);
	}

	/**
	 * } Returns true if both concepts have the same labels in their incoming/outgoing edges. Returns false otherwise.
	 * 
	 * @param inputSpace
	 * @param leftConcept
	 * @param rightConcept
	 * @param out
	 * @return
	 */
	public static boolean containsCommonEdgeLabels(StringGraph inputSpace, String leftConcept, String rightConcept, boolean out) {
		Set<StringEdge> leftEdges;
		Set<StringEdge> rightEdges;
		if (out) {
			leftEdges = inputSpace.outgoingEdgesOf(leftConcept);
			rightEdges = inputSpace.outgoingEdgesOf(rightConcept);
		} else {
			leftEdges = inputSpace.incomingEdgesOf(leftConcept);
			rightEdges = inputSpace.incomingEdgesOf(rightConcept);
		}
		HashSet<String> leftLabels = GraphAlgorithms.getEdgesLabelsAsSet(leftEdges);
		HashSet<String> rightLabels = GraphAlgorithms.getEdgesLabelsAsSet(rightEdges);
		boolean commonLabels = VariousUtils.intersects(leftLabels, rightLabels);
		return commonLabels;
	}

	/**
	 * Selects randomly from the inputspace an ordered pair of distinct concepts which have at least one relation (of a given label) in common.
	 * 
	 * @param inputSpace
	 * @param random
	 * @return
	 */
	public static OrderedPair<String> getRandomConceptPair(StringGraph inputSpace, RandomGenerator random) {
		Set<String> concepts = inputSpace.getVertexSet();

		// create a new concept match which has at least one relation with the same label in common
		String leftConcept;
		String rightConcept;
		do {
			leftConcept = VariousUtils.getRandomElementFromCollection(concepts, random);
			do {
				rightConcept = VariousUtils.getRandomElementFromCollection(concepts, random);
			} while (rightConcept.equals(leftConcept));
			// try matching labels from edges OUT/IN
		} while (!(containsCommonEdgeLabels(inputSpace, leftConcept, rightConcept, false) || // ---
				containsCommonEdgeLabels(inputSpace, leftConcept, rightConcept, true)));

		OrderedPair<String> initial = new OrderedPair<String>(leftConcept, rightConcept);
		return initial;
	}

	/**
	 * calculates the distance (in hops on the given graph) between the two concepts of the given pair. Uses distanceBetweenVertices() from GraphAlgorithms but
	 * with prior caching.
	 * 
	 * @param pair
	 * @return
	 */
	public static int calculateReferencePairInnerDistance(StringGraph graph, OrderedPair<String> pair, int maximumDistance) {
		// distance from a to b is equal to distance from b to a
		// convert directional to bidirectional and get it from the cache
		UnorderedPair<String> uPair = new UnorderedPair<String>(pair);
		Integer value = (Integer) StaticSharedVariables.refPairInnerDistanceCache.get(uPair);
		if (value != null) { // cache HIT
			return value.intValue();
		} else { // cache MISS
			int refPairInnerDistance = GraphAlgorithms.getDistance(graph, pair.getLeftElement(), pair.getRightElement(), maximumDistance);
			if (refPairInnerDistance == 0) {
				System.err.println("refPairInnerDistance=0 for OrderedPair " + pair);
			}
			if (refPairInnerDistance == Integer.MAX_VALUE)
				refPairInnerDistance = maximumDistance + 1;
			StaticSharedVariables.refPairInnerDistanceCache.put(uPair, refPairInnerDistance);
			return refPairInnerDistance;
		}
	}

	/**
	 * Calculates the number of children per sub tree for the given graph, starting at the root vertex. Used to calculate the balanced sub-tree(s) at the root
	 * vertex.
	 * 
	 * @param graph
	 * @param root
	 * @return
	 */
	public static Object2IntOpenHashMap<OrderedPair<String>> countNumberOfChildrenPerSubTree(DirectedMultiGraph<OrderedPair<String>, String> graph,
			OrderedPair<String> root) {
		HashMap<OrderedPair<String>, OrderedPair<String>> cameFrom = new HashMap<>();
		HashSet<OrderedPair<String>> closedSet = new HashSet<>();
		ArrayDeque<OrderedPair<String>> stack = new ArrayDeque<>();
		ArrayDeque<OrderedPair<String>> output = new ArrayDeque<>();
		// depth first expansion so that later we can go back from the terminal nodes to the root (in the output deque)
		stack.push(root);
		while (!stack.isEmpty()) {
			OrderedPair<String> node = stack.pop();
			closedSet.add(node);
			output.push(node);
			Set<OrderedPair<String>> neighborhood = graph.getNeighborVertices(node);
			for (OrderedPair<String> neighbor : neighborhood) {
				if (!closedSet.contains(neighbor)) {
					stack.push(neighbor);
					cameFrom.put(neighbor, node);
				}
			}
		}
		Object2IntOpenHashMap<OrderedPair<String>> numChildren = new Object2IntOpenHashMap<>(output.size() * 2);
		while (!output.isEmpty()) {
			OrderedPair<String> node = output.pop();
			// children of this node is its neighborhood except ancestor
			Set<OrderedPair<String>> children = graph.getNeighborVertices(node);
			OrderedPair<String> ancestor = cameFrom.get(node);
			children.remove(ancestor);
			int childCount = 0;
			for (OrderedPair<String> child : children) {
				// add number of children for each "child" including self
				childCount += numChildren.getInt(child) + 1;
			}
			numChildren.put(node, childCount);
		}
		return numChildren;
	}
}
