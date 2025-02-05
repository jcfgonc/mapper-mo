package jcfgonc.mapper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import graph.DirectedMultiGraph;
import graph.StringEdge;
import graph.StringGraph;
import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.data.POS;
import net.sf.extjwnl.dictionary.Dictionary;
import structures.ListOfSet;
import structures.ObjectCount;
import structures.ObjectCounter;
import structures.OrderedPair;
import structures.SynchronizedSeriarizableMapOfSet;
import utils.VariousUtils;

public class GrammarUtilsWordNet {
	private static Dictionary dictionary;

	static {
		try {
			dictionary = Dictionary.getDefaultResourceInstance();
		} catch (JWNLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * concepts with at least one of these words have no POS
	 */
	private static final Set<String> uselessWords = Set.of(VariousUtils.fastSplitWhiteSpace(""));
	private static final HashSet<String> stopWords = new HashSet<String>(VariousUtils.readFileRows("data/english stop words basic.txt"));
	private static final HashSet<String> prepositions = new HashSet<String>(VariousUtils.readFileRows("data/english prepositions.txt"));
	private static final HashSet<String> determiners = new HashSet<String>(VariousUtils.readFileRows("data/english determiners.txt"));
	private static final HashSet<String> pronouns = new HashSet<String>(VariousUtils.readFileRows("data/english pronouns.txt"));

	/**
	 * cache of previously decoded POS for each concept
	 */
	private static SynchronizedSeriarizableMapOfSet<String, MyPOS> cachedConceptPOS = new SynchronizedSeriarizableMapOfSet<>("posCache.dat",
			MOEA_Config.CACHE_SAVE_TIMEOUT);

	/**
	 * Warning, returns null if concept is not cached. May return an empty set if the concept has no POS. Otherwise returns the list of POS for the given
	 * concept.
	 * 
	 * @param concept
	 * @return
	 */
	private static Set<MyPOS> conceptCached(String concept) {
		return cachedConceptPOS.get(concept);
	}

	/**
	 * Check if the given concept ISA <something> with a POS in the inputspace, recursively.
	 * 
	 * @param concept
	 * @throws JWNLException
	 */
	public static Set<MyPOS> getConceptPOS_includingInputSpace(String concept, StringGraph inputSpace) throws JWNLException {
		// prevent futile work
		Set<MyPOS> posType = conceptCached(concept);
		if (posType != null) { // POS has either been queried or not before
			// System.out.println("previously resolved: " + concept + " = " + posType);
			return posType;
		}
		// POS was not queried before
//		System.out.println("resolving new concept: " + concept);

		posType = new HashSet<MyPOS>();

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
			Set<MyPOS> conceptCached = conceptCached(current);
			if (conceptCached != null && !conceptCached.isEmpty()) {
				endingConcept = current;
				posType.addAll(conceptCached);
				break outerWhile;
			}

			// check if concept has a POS in wordnet
			Set<MyPOS> conceptPOS_fromWordnet = getConceptPOS(current);
			if (conceptPOS_fromWordnet != null && !conceptPOS_fromWordnet.isEmpty()) {
				endingConcept = current;
				posType.addAll(conceptPOS_fromWordnet);
				break outerWhile;
			}

			// these relations should maintain POS
			Set<StringEdge> out = inputSpace.outgoingEdgesOf(current, "isa");
			out.addAll(inputSpace.outgoingEdgesOf(current, "synonym"));
			// out.addAll(inputSpace.outgoingEdgesOf(current, "partof"));
			HashSet<String> targets = StringGraph.edgesTargets(out);

			for (String target : targets) {
				if (closedSet.contains(target))
					continue;

				// remember which concept came before
				cameFrom.put(target, current);
				openSet.addLast(target); // later expand next ISA target
			} // went through all targets
		}
		// posType defined OR not
		if (posType.isEmpty()) {
			// none of the concepts in the cameFrom map were able to be resolved
			// back-propagate failed resolve using code below
			for (String key : cameFrom.keySet()) {
				cachedConceptPOS.add(key, posType);
			}
			cachedConceptPOS.add(concept, posType);
		} else {
			// store middle targets POS by starting at endingConcept and going back the camefrom path
			// get path
			String prior;
			String current = endingConcept;
			while (true) {
				cachedConceptPOS.add(current, posType);
				prior = cameFrom.get(current);
				if (prior == null)
					break;
				current = prior;
			}
		}
		return posType;
	}

	public static boolean sameWordPOS(OrderedPair<String> pair, StringGraph inputSpace) {
		String leftElement = pair.getLeftElement();
		String rightElement = pair.getRightElement();

		// compound concepts are expected to be space separated
		try {

			// complex concepts with useless words have no valid POS
			if (containsUselessWords(leftElement) || containsUselessWords(rightElement))
				return false;

			// get POS for each concept
			Set<MyPOS> lPOS = getConceptPOS_includingInputSpace(leftElement, inputSpace);
			Set<MyPOS> rPOS = getConceptPOS_includingInputSpace(rightElement, inputSpace);

			if (lPOS.isEmpty()) {
				// System.out.println("could not get POS: " + leftElement + "\tdegree: " + inputSpace.degreeOf(leftElement));
			}
			if (rPOS.isEmpty()) {
				// System.out.println("could not get POS: " + rightElement + "\tdegree: " + inputSpace.degreeOf(rightElement));
			}

			if (!lPOS.isEmpty() && !rPOS.isEmpty()) {
				boolean intersects = VariousUtils.intersects(lPOS, rPOS);
				// if (!intersects)
				// System.lineSeparator();
				return intersects;
			}

			// if both concepts have the same POS, return true
			// otherwise return false
		} catch (JWNLException e) {
			e.printStackTrace();
			System.exit(-1);
		}

//		System.out.println("could not get word classes for pair " + pair);

		return false;
	}

	private static boolean containsUselessWords(String concept) {
		String[] words = VariousUtils.fastSplit(concept, MOEA_Config.CONCEPT_WORD_SEPARATOR);
		for (String word : words) {
			if (uselessWords.contains(word))
				return true;
		}
		return false;
	}

	public static HashSet<MyPOS> getConceptPOS(String concept) throws JWNLException {
		// try simple direct wordnet test
		HashSet<MyPOS> pos = getWordNetPOS(concept);
		if (!pos.isEmpty()) {
			return pos;
		}

		// try compound noun or more advanced structures
		pos = advancedCheck(concept);
		return pos;
	}

	/**
	 * Looks up possible POS type in local files. Probably only works for single words.
	 * 
	 * @param word
	 * @return
	 */
	public static HashSet<MyPOS> lookUpPOS_inLocalFiles(String word) {
		HashSet<MyPOS> possible_poses = new HashSet<MyPOS>();
		if (GrammarUtilsWordNet.determiners.contains(word)) {
			possible_poses.add(MyPOS.DETERMINER);
		}
		if (GrammarUtilsWordNet.prepositions.contains(word)) {
			possible_poses.add(MyPOS.PREPOSITION);
		}
		if (GrammarUtilsWordNet.pronouns.contains(word)) {
			possible_poses.add(MyPOS.PRONOUN);
		}
		return possible_poses;
	}

	/**
	 * Checks if the given string is defined as a compound noun POS in wordnet. To be called by getConceptPOS().
	 * 
	 * @param string
	 * @return
	 * @throws JWNLException
	 */
	private static HashSet<MyPOS> advancedCheck(String string) throws JWNLException {

		List<String> words = VariousUtils.arrayToArrayList(VariousUtils.fastSplit(string, MOEA_Config.CONCEPT_WORD_SEPARATOR));
		// remove stopwords
		// words.removeAll(stopWords);

		int numWords = words.size();
		ListOfSet<MyPOS> possiblePOS_perWord = new ListOfSet<>();

		// assign possible POS for each word
		for (String word : words) {
			HashSet<MyPOS> wPOS = getWordNetPOS(word);
			possiblePOS_perWord.add(wPOS);
		}

		HashSet<MyPOS> retval = new HashSet<MyPOS>();

		if (numWords == 1) {
			return possiblePOS_perWord.get(0);

		} else if (numWords == 2) {
			// test compound noun rules
			if (possiblePOS_perWord.numberOfNonEmptySets() == 2) {

				HashSet<MyPOS> pos0 = possiblePOS_perWord.get(0);
				HashSet<MyPOS> pos1 = possiblePOS_perWord.get(1);

				// catches two word rules with at least one noun
				if (pos0.contains(MyPOS.NOUN) && pos1.contains(MyPOS.NOUN)) {
					retval.add(MyPOS.NOUN); // MORE THAN OK
				}

				// determiner noun
				if (pos0.contains(MyPOS.DETERMINER) && pos1.contains(MyPOS.NOUN)) {
					retval.add(MyPOS.NOUN); // MORE THAN OK
				}

				// rules with no nouns
				if (pos0.contains(MyPOS.ADJECTIVE)) {
					if (pos1.contains(MyPOS.NOUN)) {
						retval.add(MyPOS.NOUN); // OK
					}
					if (pos1.contains(MyPOS.ADJECTIVE)) {
						retval.add(MyPOS.NOUN); // OK
					}
					if (pos1.contains(MyPOS.VERB)) {
						retval.add(MyPOS.NOUN); // PARTIALLY OK
					}
				}
				if (pos0.contains(MyPOS.ADVERB)) {
					if (pos1.contains(MyPOS.VERB)) {
						retval.add(MyPOS.NOUN); // PARTIALLY OK
					}
				}
				if (pos0.contains(MyPOS.VERB)) {
					if (pos1.contains(MyPOS.ADVERB)) {
						retval.add(MyPOS.NOUN); // NOT SURE
					}
					if (pos1.contains(MyPOS.DETERMINER)) {
						retval.add(MyPOS.VERB);
					}
					if (pos1.contains(MyPOS.NOUN)) {
						retval.add(MyPOS.VERB);
					}
				}
			}
			System.lineSeparator();
			// not matched with the two word rules
		} else if (numWords == 3) {
			HashSet<MyPOS> pos0 = possiblePOS_perWord.get(0);
			HashSet<MyPOS> pos1 = possiblePOS_perWord.get(1);
			HashSet<MyPOS> pos2 = possiblePOS_perWord.get(2);

			// go to school
			// [[VERB, NOUN, ADJECTIVE], [PREPOSITION], [VERB, NOUN]]
			if (pos1.size() == 1 && pos1.contains(MyPOS.PREPOSITION)) {
				if (pos0.contains(MyPOS.VERB) && (pos2.contains(MyPOS.NOUN) || pos2.contains(MyPOS.PRONOUN))) {
					retval.add(MyPOS.VERB);
				}
			}

			// maintain good health
			// [[VERB], [ADVERB, NOUN, ADJECTIVE], [NOUN]]
			if (pos0.contains(MyPOS.VERB) && //
					pos1.contains(MyPOS.ADJECTIVE) && //
					pos2.contains(MyPOS.NOUN)) {
				retval.add(MyPOS.VERB);
			}
			
			// sleep at night
			// [[VERB], [PREPOSITION], [NOUN]]
			if (pos0.contains(MyPOS.VERB) && //
					pos1.contains(MyPOS.PREPOSITION) && //
					pos2.contains(MyPOS.NOUN)) {
				retval.add(MyPOS.VERB);
			}
			
			// go see film
			// [[VERB], [VERB], [NOUN]]
			if (pos0.contains(MyPOS.VERB) && //
					pos1.contains(MyPOS.VERB) && //
					pos2.contains(MyPOS.NOUN)) {
				retval.add(MyPOS.VERB);
			}
			
			// light sport aircraft
			//[[ADJECTIVE], [NOUN], [NOUN]]
			if (pos0.contains(MyPOS.ADJECTIVE) && //
					pos1.contains(MyPOS.NOUN) && //
					pos2.contains(MyPOS.NOUN)) {
				retval.add(MyPOS.NOUN);
			}
		
			System.lineSeparator();
		
		} else {
			System.lineSeparator();
//				if (pos0.contains(POS.VERB)) {
//
//				}		
		}
		return retval;
	}

	/**
	 * Gets a set of POS for the given concept in wordnet. If the concept does not exist in wordnet an empty set is returned. Probably only works for single
	 * words. 9+
	 * 
	 * 
	 * 
	 * @param concept
	 * @return
	 * @throws JWNLException
	 */
	private static HashSet<MyPOS> getWordNetPOS(String concept) throws JWNLException {
		HashSet<MyPOS> pos = new HashSet<>();
		if (dictionary.getIndexWord(POS.NOUN, concept) != null) {
			pos.add(MyPOS.NOUN);
		}
		if (dictionary.getIndexWord(POS.VERB, concept) != null) {
			pos.add(MyPOS.VERB);
		}
		if (dictionary.getIndexWord(POS.ADJECTIVE, concept) != null) {
			pos.add(MyPOS.ADJECTIVE);
		}
		if (dictionary.getIndexWord(POS.ADVERB, concept) != null) {
			pos.add(MyPOS.ADVERB);
		}

		// complete with additional POSes using local lookup files
		pos.addAll(lookUpPOS_inLocalFiles(concept));

		// if empty, do morphological processing
//		if (pos.isEmpty()) {
//			System.lineSeparator();
//			if (dictionary.lookupIndexWord(POS.NOUN, concept) != null) {
//				pos.add(MyPOS.NOUN);
//			}
//			if (dictionary.lookupIndexWord(POS.VERB, concept) != null) {
//				pos.add(MyPOS.VERB);
//			}
//			if (dictionary.lookupIndexWord(POS.ADJECTIVE, concept) != null) {
//				pos.add(MyPOS.ADJECTIVE);
//			}
//			if (dictionary.lookupIndexWord(POS.ADVERB, concept) != null) {
//				pos.add(MyPOS.ADVERB);
//			}
//		}
		return pos;
	}

	static void studyStringGraphVerticesPOS(StringGraph graph) throws JWNLException {
		ObjectCounter<String> degreeCounter = new ObjectCounter<>();
		for (String concept : graph.getVertexSet()) {
			degreeCounter.addObject(concept, graph.degreeOf(concept));
		}
		ArrayList<ObjectCount<String>> sortedCount = degreeCounter.getSortedCount();
		for (ObjectCount<String> oc : sortedCount) {
			String concept = oc.getId();
			int inDegree = graph.getInDegree(concept);
			int outDegree = graph.getOutDegree(concept);
			int degree = oc.getCount();

			Set<MyPOS> pos = getConceptPOS_includingInputSpace(concept, graph);
			if (pos.isEmpty())
				System.out.printf("%s\t%d\t%d\t%d\t%s\n", concept, degree, inDegree, outDegree, pos);
		}
	}

	/**
	 * Calculates the ratio of same POS concept pairs in the overall mapping. Called from the genetic algorithm's fitness evaluation.
	 * 
	 * @param pairGraph
	 * @param inputSpace
	 * @return
	 */
	public static double calculateSamePOS_pairsPercentage(DirectedMultiGraph<OrderedPair<String>, String> pairGraph, StringGraph inputSpace) {
		if (pairGraph.getNumberOfVertices() == 0)
			return 0;
		int samePOScount = 0;
		for (OrderedPair<String> pair : pairGraph.vertexSet()) {
			if (sameWordPOS(pair, inputSpace)) {
				samePOScount++;
			}
		}
		double ratio = (double) samePOScount / pairGraph.getNumberOfVertices();
		return ratio;
	}
}
