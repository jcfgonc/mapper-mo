package jcfgonc.mapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantLock;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.simple.Sentence;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import graph.StringGraph;
import stream.ParallelConsumer;

public class GrammarUtilsCoreNLP {

	private static StanfordCoreNLP pipeline;
	private static boolean initialized = false;
	private static HashMap<String, String> cachedConceptPOS = new HashMap<String, String>();
	private static ReentrantLock cachedConceptPOS_lock = new ReentrantLock();

	public static void initialize() {
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

	public static Tree getConstituencyParsingSimpleNLP(String text) {
		Sentence set = new Sentence(text);
		Tree tree = set.parse();
		return tree;
	}

	public static Tree getConstituencyParsing(String text) {
		initialize();
		// build annotation for a review
		Annotation annotation = new Annotation(text);
		// annotate
		pipeline.annotate(annotation);
		// get tree
		Tree tree = annotation.get(CoreAnnotations.SentencesAnnotation.class).get(0).get(TreeCoreAnnotations.TreeAnnotation.class);
		return tree;
	}

	public static List<String> getChildrenLabels(Tree root) {
		ArrayList<String> labels = new ArrayList<String>();
		for (Tree child : root.children()) {
			labels.add(child.label().toString());
		}
		return labels;
	}

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
				System.lineSeparator();
			}
			return false;
		}
		default:
			System.err.println("Unexpected value: " + type_level1 + " for: " + concept);
			return false;
		// throw new IllegalArgumentException("Unexpected value: " + type_level1+" for "+concept);
		}
	}

	public static String getPOS(String concept) {
		String pos = cachedConceptPOS.get(concept);
		if (pos != null) {
			return pos;
		} else {
			pos = getPOS_inner(concept);
			cachedConceptPOS_lock.lock();
			cachedConceptPOS.put(concept, pos);
			cachedConceptPOS_lock.unlock();
			return pos;
		}
	}

	public static String getPOS_inner(String concept) {
		Tree root = getConstituencyParsingSimpleNLP(concept);
		Tree level1 = root.children()[0];
		String type_level1 = level1.label().toString();
		switch (type_level1) {
		case "FRAG": {
			return type_level1;
		}
		case "ADVP": {
			return type_level1;
		}
		case "VP": {
			return type_level1;
		}
		case "SQ": {
			return type_level1;
		}
		case "ADJP": {
			return type_level1;
		}
		case "LST": {
			return type_level1;
		}
		case "NP": {
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

	public static void testConceptPOSes(StringGraph inputSpace) {
		for(String concept:	inputSpace.getVertexSet()) {
			System.out.println(concept+"\t"+getPOS(concept));
		}
	}

}
