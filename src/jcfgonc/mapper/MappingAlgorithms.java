package jcfgonc.mapper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.math3.random.RandomGenerator;

import graph.DirectedMultiGraph;
import graph.GraphAlgorithms;
import graph.GraphEdge;
import graph.StringEdge;
import graph.StringGraph;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import jcfgonc.mapper.structures.MappingStructure;
import structures.MapOfList;
import structures.MapOfSet;
import structures.OrderedPair;
import structures.UnorderedPair;
import utils.VariousUtils;

public class MappingAlgorithms {

	/**
	 * map edge label (+=outgoing, -=incoming, relative to the reference) to the set of connected neighbors
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

	public static MapOfList<String, OrderedPair<String>> expandConceptPair(StringGraph inputSpace, DirectedMultiGraph<OrderedPair<String>, String> pairGraph, RandomGenerator random, OrderedPair<String> refPair, HashSet<String> usedConcepts) {
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
		MapOfList<String, OrderedPair<String>> pairs = matchLeftRightMapsAndSelectRandom(leftmap, rightmap, random, usedConcepts);
		// update the pair/mapping graph
		for (String dirLabel : pairs.keySet()) {
			if (pairGraph.getNumberOfVertices() >= MOEA_Config.MAXIMUM_NUMBER_OF_CONCEPT_PAIRS)
				break;
			List<OrderedPair<String>> listPairs = pairs.get(dirLabel);

			// dirLabel always start with + OR -
			String label = dirLabel.substring(1);

			for (OrderedPair<String> nextPair : listPairs) {

				if (dirLabel.charAt(0) == '-') {
					pairGraph.addEdge(nextPair, refPair, label);
				} else { // must be '+'
					pairGraph.addEdge(refPair, nextPair, label);
				}
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
	private static MapOfList<String, OrderedPair<String>> matchLeftRightMapsAndSelectRandom(MapOfSet<String, String> leftmap, MapOfSet<String, String> rightmap, RandomGenerator random, HashSet<String> usedConcepts) {
		MapOfList<String, OrderedPair<String>> relationToPairs = new MapOfList<String, OrderedPair<String>>();
		Set<String> leftDirLabels = leftmap.keySet();
		Set<String> rightDirLabels = rightmap.keySet();
		HashSet<String> commonDirLabels = VariousUtils.intersection(leftDirLabels, rightDirLabels);
		for (String dirLabel : commonDirLabels) {

			Set<String> leftInputSet = leftmap.get(dirLabel);
			Set<String> rightInputSet = rightmap.get(dirLabel);

			// create both sets as list excluding closed concepts
			ArrayList<String> leftNeighbors = VariousUtils.toListExcluding(leftInputSet, usedConcepts);
			ArrayList<String> rightNeighbors = VariousUtils.toListExcluding(rightInputSet, leftInputSet, usedConcepts); // what's on the left can't be on the right

			if (leftNeighbors.isEmpty() || rightNeighbors.isEmpty())
				continue;

			Collections.shuffle(leftNeighbors);
			Collections.shuffle(rightNeighbors); // one shuffle is probably enough but two is better

			int smallerSize = Math.min(leftNeighbors.size(), rightNeighbors.size());

			for (int i = 0; i < smallerSize; i++) {
				String leftNeighbor = leftNeighbors.get(i);
				String rightNeighbor = rightNeighbors.get(i);
				OrderedPair<String> pair = new OrderedPair<String>(leftNeighbor, rightNeighbor);
				usedConcepts.add(leftNeighbor);
				usedConcepts.add(rightNeighbor);
				relationToPairs.add(dirLabel, pair);
			}
		}
		return relationToPairs;
	}

	public static Object2IntOpenHashMap<OrderedPair<String>> createIsomorphism(StringGraph inputSpace, DirectedMultiGraph<OrderedPair<String>, String> pairGraph, RandomGenerator random, OrderedPair<String> refPair) {
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
			if (closedSet.contains(currentPair.getLeftElement()))
				continue;
			if (closedSet.contains(currentPair.getRightElement()))
				continue;
			// get the vertex neighbors not in the closed set
			MapOfList<String, OrderedPair<String>> expansion = expandConceptPair(inputSpace, pairGraph, random, currentPair, usedConcepts);
			for (List<OrderedPair<String>> pairs : expansion.values()) {
				for (OrderedPair<String> nextPair : pairs) {
					if (closedSet.contains(nextPair.getLeftElement()))
						continue;
					if (closedSet.contains(nextPair.getRightElement()))
						continue;
					// put the neighbors in the open set
					openSet.addLast(nextPair);
					pairDeepness.put(nextPair, nextDeepness);
				}
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
	 * calculates the distance (in hops on the given graph) between the two concepts of the given pair. Uses distanceBetweenVertices() from GraphAlgorithms but with prior caching.
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
	 * Calculates the number of children per sub tree for the given graph, starting at the root vertex. Used to calculate the balanced sub-tree(s) at the root vertex. IE, for each vertex calculates the number of hanging vertices from it to the terminal
	 * vertices, beginning at the root vertice.
	 * 
	 * @param graph
	 * @param root
	 * @return
	 */
	public static Object2IntOpenHashMap<OrderedPair<String>> countNumberOfChildrenPerSubTree(DirectedMultiGraph<OrderedPair<String>, String> graph, OrderedPair<String> root) {
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

	public static <T> double closenessCentrality(T referencePair, DirectedMultiGraph<T, String> pairGraph) {
		int nvert = pairGraph.getNumberOfVertices();
		Object2IntOpenHashMap<T> deepness = getVerticesDeepness(referencePair, pairGraph);
		double deepSum = 0;
		for (int deep : deepness.values()) {
			deepSum += deep;
		}
		double centrality = (double) (nvert - 1) / deepSum;
		return centrality;
	}

	private static <T> Object2IntOpenHashMap<T> getVerticesDeepness(T referencePair, DirectedMultiGraph<T, String> pairGraph) {
		HashSet<T> closedSet = new HashSet<>();
		ArrayDeque<T> openSet = new ArrayDeque<>();
		Object2IntOpenHashMap<T> deepness = new Object2IntOpenHashMap<>();
		deepness.put(referencePair, 0);

		// ---------init
		openSet.addLast(referencePair);

		while (!openSet.isEmpty()) {
			T current = openSet.removeFirst();
			closedSet.add(current);
			int nextDeepness = deepness.getInt(current) + 1;
			HashSet<GraphEdge<T, String>> touchingE = pairGraph.edgesOf(current);
			for (GraphEdge<T, String> edge : touchingE) {
				T other = edge.getOppositeOf(current);
				if (!closedSet.contains(other)) {
					openSet.addLast(other);
					deepness.put(other, nextDeepness);
				}
			}
		}
		return deepness;
	}

	public static void calculatePathsFromOrigin(DirectedMultiGraph<OrderedPair<String>, String> pairGraph, //
			OrderedPair<String> referencePair, MapOfList<OrderedPair<String>, String> priorRelations, HashSet<OrderedPair<String>> terminalSet) {
		HashSet<OrderedPair<String>> closedSet = new HashSet<>();
		ArrayDeque<OrderedPair<String>> openSet = new ArrayDeque<>();
		openSet.add(referencePair);
		while (!openSet.isEmpty()) {
			OrderedPair<String> currentVertex = openSet.removeLast();
			closedSet.add(currentVertex);
			// check for terminal vertices
			if (!currentVertex.equals(referencePair) && pairGraph.degreeOf(currentVertex) == 1) {
				terminalSet.add(currentVertex);
			}
			List<String> currentPreviousRelations = priorRelations.get(currentVertex);
			HashSet<GraphEdge<OrderedPair<String>, String>> edgesOf = pairGraph.edgesOf(currentVertex);
			for (GraphEdge<OrderedPair<String>, String> edge : edgesOf) {
				OrderedPair<String> neighboringVertex = edge.getOppositeOf(currentVertex);
				if (closedSet.contains(neighboringVertex))
					continue;
				String relation;
				if (edge.outgoesFrom(currentVertex)) {
					relation = "+" + edge.getLabel();
				} else {
					relation = "-" + edge.getLabel();
				}
				if (currentPreviousRelations != null)
					priorRelations.add(neighboringVertex, currentPreviousRelations);
				priorRelations.add(neighboringVertex, relation);
				openSet.add(neighboringVertex);
			}
		}
	}

	/**
	 * Converts the relations to their corresponding translations
	 * 
	 * @param g
	 * @param relationTranslation
	 * @return
	 */
	public static StringGraph translateEdges(StringGraph g, HashMap<String, String> relationTranslation) {
		StringGraph newG = new StringGraph();
		for (StringEdge edge : g.edgeSet()) {
			String edgeSource = edge.getSource();
			String edgeTarget = edge.getTarget();
			String edgeLabel = edge.getLabel();
			String translation = relationTranslation.get(edgeLabel);
			if (translation == null) { // no translation -> mantain original
				translation = edgeLabel;
			}
			newG.addEdge(edgeSource, edgeTarget, translation);
		}
		return newG;
	}

	public static DirectedMultiGraph<OrderedPair<String>, String> createGraphAroundRefPair(DirectedMultiGraph<OrderedPair<String>, String> pairGraph, OrderedPair<String> referencePair) {
		DirectedMultiGraph<OrderedPair<String>, String> nearby = new DirectedMultiGraph<>();
		HashSet<GraphEdge<OrderedPair<String>, String>> edgesOf = pairGraph.edgesOf(referencePair);
		nearby.addEdges(edgesOf);
		return nearby;
	}
}
