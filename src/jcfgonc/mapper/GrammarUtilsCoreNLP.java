package jcfgonc.mapper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.simple.Sentence;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import graph.DirectedMultiGraph;
import graph.StringEdge;
import graph.StringGraph;
import net.sf.extjwnl.JWNLException;
import stream.ParallelConsumer;
import structures.OrderedPair;
import utils.VariousUtils;

public class GrammarUtilsCoreNLP {

	private static StanfordCoreNLP pipeline;
	private static boolean initialized = false;
	private static HashMap<String, String> cachedConceptPOS = new HashMap<String, String>();
	private static ReentrantLock cachedConceptPOS_lock = new ReentrantLock();

	private static void initialize() {
		if (initialized)
			return;
		// set up pipeline properties
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize,truecase,pos,lemma,ner,parse");
		// use faster shift reduce parser
		props.setProperty("parse.model", "edu/stanford/nlp/models/srparser/englishSR.ser.gz");
		props.setProperty("parse.maxlen", "100");
		// set up Stanford CoreNLP pipeline
		pipeline = new StanfordCoreNLP(props);
		initialized = true;
	}

	private static Tree getConstituencyParsingSimpleNLP(String text) {
		Sentence set = new Sentence(text);
		Tree tree = set.parse();
		return tree;
	}

	private static Tree getConstituencyParsing(String text) {
		initialize();
		// build annotation for a review
		Annotation annotation = new Annotation(text);
		// annotate
		pipeline.annotate(annotation);
		// get tree
		Tree tree = annotation.get(CoreAnnotations.SentencesAnnotation.class).get(0).get(TreeCoreAnnotations.TreeAnnotation.class);
		return tree;
	}

	private static List<String> getChildrenLabels(Tree root) {
		ArrayList<String> labels = new ArrayList<String>();
		for (Tree child : root.children()) {
			labels.add(child.label().toString());
		}
		return labels;
	}

	/**
	 * Returns true if in my opinion, the given concept has a valid format/constituency structure.
	 * 
	 * @param concept
	 * @return
	 */
	public static boolean validate(String concept) {
		Tree root = getConstituencyParsingSimpleNLP(concept);
		Tree level1 = root.children()[0];
		String type_level1 = level1.label().toString();
		// invalid 1st level POS [ADVP] [FRAG] [LST]
		switch (type_level1) {
		case "FRAG": {
			return false;
		}
		case "ADVP": {
			return false;
		}
		case "VP": {
			return false;
		}
		case "SQ": {
			return false;
		}
		case "ADJP": {
			return true;
		}
		case "LST": {
			return false;
		}
		case "NP": {
			return true;
		}
		case "S": {
			List<String> type_level2 = getChildrenLabels(level1);
			if (type_level2.size() == 1) {
				if (type_level2.get(0).equals("VP")) {
					return true;
				}
			} else {
				if (type_level2.size() == 2) {
					// [ADVP, VP]
					// [RB, VP]
					if (type_level2.get(0).equals("ADVP") && //
							type_level2.get(1).equals("VP")) {
						return true;
					}
					if (type_level2.get(0).equals("RB") && //
							type_level2.get(1).equals("VP")) {
						return true;
					}
				}
				// System.lineSeparator();
			}
			return false;
		}
		default:
			System.err.println("Unexpected value: " + type_level1 + " for: " + concept);
			return false;
		// throw new IllegalArgumentException("Unexpected value: " + type_level1+" for "+concept);
		}
	}

	private static String getClassificationFromCoreNLP(String concept) {
		Tree root = getConstituencyParsing(concept);
		// System.out.println(root);
		Tree level1 = root.children()[0];
		String type_level1 = level1.label().toString();
		switch (type_level1) {
		case "META":
		case "INTJ":
		case "ADVP":
		case "ADJP":
		case "NP": {
			return "NP";
		}
		case "FRAG": {
			return type_level1;
		}
		case "VP": {
			return type_level1;
		}
		case "SQ": {
			return type_level1;
		}
		case "LST": {
			return type_level1;
		}
		case "S": {
			List<String> type_level2 = getChildrenLabels(level1);
			if (type_level2.size() == 1) {
				if (type_level2.get(0).equals("VP")) {
					return "VP";
				}
			} else {
				if (type_level2.size() == 2) {
					// cases
					// [ADVP, VP]
					// [RB, VP]
					if (type_level2.get(0).equals("ADVP") && //
							type_level2.get(1).equals("VP")) {
						return "VP";
					}
					if (type_level2.get(0).equals("RB") && //
							type_level2.get(1).equals("VP")) {
						return "VP";
					}
					if (type_level2.get(0).equals("NP") && // typical sentence, NP VP
							type_level2.get(1).equals("VP")) {
						return "S";
					}
				}
			}
			return "S";
		}
		default:
			System.err.println("Unexpected value: " + type_level1 + " for: " + concept);
			return "UNKNOWN";
		}
	}

	public static void validate(StringGraph inputSpace) throws InterruptedException {
		ArrayList<String> concepts = new ArrayList<String>(inputSpace.getVertexSet());
		ArrayList<String> invalidConcepts = new ArrayList<String>();
		ReentrantLock lock = new ReentrantLock();
		// --- DEMO
		ParallelConsumer<String> pc = new ParallelConsumer<>();
		pc.parallelForEach(concepts, concept -> {
			try {
				if (!validate(concept)) {
					lock.lock();
					invalidConcepts.add(concept);
					lock.unlock();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			// System.out.println(concept);
		});
		System.out.println("waiting");
		pc.shutdown();
		System.out.println("shutdown");

		inputSpace.removeVertices(invalidConcepts);
	}

	public static void testConcepts(StringGraph graph) throws InterruptedException {
		// --- DEMO

		ArrayList<String> concepts = new ArrayList<String>(graph.getVertexSet());
//		ReentrantLock lock=new ReentrantLock();
		// ArrayList<String> toRemove=new ArrayList<String>();

		ParallelConsumer<String> pc = new ParallelConsumer<>(16);
		pc.parallelForEach(concepts, concept -> {
			try {
				int ntokens = VariousUtils.countCharOccurences(concept, ' ') + 1;
				if (ntokens > 1) {
					String classi = getClassification(concept, graph);
					System.out.println(concept + "\t" + classi);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		System.out.println("waiting");
		pc.shutdown();
		System.out.println("shutdown");
	}

	public static void testConceptPOSes(StringGraph inputSpace) {
		for (String concept : inputSpace.getVertexSet()) {
			System.out.println(concept + "\t" + getClassification(concept, inputSpace));
		}
	}

	public static boolean sameClassification(OrderedPair<String> pair, StringGraph inputSpace) {
		String leftElement = pair.getLeftElement();
		String rightElement = pair.getRightElement();

		return getClassification(leftElement, inputSpace).equals(getClassification(rightElement, inputSpace));
	}

	/**
	 * Calculates the ratio of same grammatical classification for the concept pairs in the overall mapping. Called from the genetic algorithm's fitness
	 * evaluation.
	 * 
	 * @param pairGraph
	 * @param inputSpace
	 * @return
	 */
	public static double calculateSameClassification_pairsPercentage(DirectedMultiGraph<OrderedPair<String>, String> pairGraph, StringGraph inputSpace) {
		if (pairGraph.getNumberOfVertices() == 0)
			return 0;
		int samePOScount = 0;
		for (OrderedPair<String> pair : pairGraph.vertexSet()) {
			if (sameClassification(pair, inputSpace)) {
				samePOScount++;
			}
		}
		double ratio = (double) samePOScount / pairGraph.getNumberOfVertices();
		return ratio;
	}

	public static String getClassification(String concept, StringGraph inputSpace) {
		String pos = cachedConceptPOS.get(concept);
		if (pos != null) {
			return pos;
		}
//		pos = getClassificationFromCoreNLP(concept);
//		if (pos.equals("NP")) { // NP are the simplest ones
//		} else {

		// not NP but it might, check ontology
		// mark if previously verified or not to prevent wasted calculations
		pos = checkIfConceptIs_NP(concept, inputSpace);
		if (pos == null) {
			pos = getClassificationFromCoreNLP(concept);
		}
		// pos = getClassification_inner(concept);
		cachedConceptPOS_lock.lock();
		cachedConceptPOS.put(concept, pos);
		cachedConceptPOS_lock.unlock();
		return pos;
	}

	/**
	 * Check if the given concept ISA noun in the inputspace, recursively.
	 * 
	 * @param concept
	 * @throws JWNLException
	 */
	private static String checkIfConceptIs_NP(String concept, StringGraph inputSpace) {
		String posType = null;

		HashSet<String> closedSet = new HashSet<>();
		ArrayDeque<String> openSet = new ArrayDeque<>();
		HashMap<String, String> cameFrom = new HashMap<>();
		String endingConcept = null; // last touched concept

		// ---------init
		openSet.addLast(concept);

		outerWhile: while (!openSet.isEmpty()) {
			String current = openSet.removeFirst();
			closedSet.add(current);

			// check if concept has been decoded before
			String conceptPOS = cachedConceptPOS.get(current);
			if (conceptPOS != null && !conceptPOS.equals("NP")) {
				endingConcept = current;
				posType = conceptPOS;
				break outerWhile;
			}

			// check if concept is a NP
			String potentialPOS = getClassificationFromCoreNLP(current);
			if (potentialPOS.equals("NP")) {
				endingConcept = current;
				posType = potentialPOS;
				break outerWhile;
			}

			// these relations should maintain POS
			Set<StringEdge> out = inputSpace.outgoingEdgesOf(current, "isa");
			out.addAll(inputSpace.outgoingEdgesOf(current, "synonym"));
			// out.addAll(inputSpace.outgoingEdgesOf(current, "partof"));
			if (!out.isEmpty()) {
				HashSet<String> targets = StringGraph.edgesTargets(out);

				for (String target : targets) {
					if (closedSet.contains(target))
						continue;

					// remember which concept came before
					cameFrom.put(target, current);
					openSet.addLast(target); // later expand next ISA target
				} // went through all targets
			}
		}
		// if posType is null is because it wasnt solved to NP
		if (posType != null && posType.equals("NP")) {
//			// none of the concepts in the cameFrom map were able to be resolved
//			// back-propagate failed resolve using code below
//			for (String key : cameFrom.keySet()) {
//				cachedConceptPOS.put(key, posType);
//			}
//			cachedConceptPOS.put(concept, posType);
//		} else {
			// store middle targets POS by starting at endingConcept and going back the camefrom path
			// get path
			String prior;
			String current = endingConcept;
			while (true) {
				cachedConceptPOS.put(current, posType);
				prior = cameFrom.get(current);
				if (prior == null)
					break;
				current = prior;
			}
		}
		return posType;
	}

}
