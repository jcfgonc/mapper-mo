package jcfgonc.mapper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

public class GrammarUtils {
	/**
	 * cache of previously decoded POS for each concept
	 */
	private static SynchronizedSeriarizableMapOfSet<String, POS> cachedConceptPOS = new SynchronizedSeriarizableMapOfSet<>(MOEA_Config.POS_CACHE_FILENAME,
			MOEA_Config.CACHE_SAVE_TIMEOUT);

	/**
	 * Warning, returns null if concept is not cached. May return an empty set if the concept has no POS. Otherwise returns the list of POS for the given
	 * concept.
	 * 
	 * @param concept
	 * @return
	 */
	public static Set<POS> conceptCached(String concept) {
		return cachedConceptPOS.get(concept);
	}

	/**
	 * Returns true if the given concept is cached as the given POS type. Otherwise returns false.
	 * 
	 * @param concept
	 * @param cachedType
	 * @return
	 */
	public static boolean conceptCached(String concept, POS cachedType) {
		Set<POS> cachedPOS = conceptCached(concept);
		if (cachedPOS != null && cachedPOS.contains(cachedType)) {
			return true;
		}
		return false;
	}

	/**
	 * Check if the given concept ISA <something> with a POS in the inputspace, recursively.
	 * 
	 * @param concept
	 * @throws JWNLException
	 */
	public static Set<POS> checkPOS_InInputSpace(String concept, StringGraph inputSpace) throws JWNLException {
		// prevent futile work
		Set<POS> posType = conceptCached(concept);
		if (posType != null) { // POS has either been queried or not before
	//		System.out.println("previously resolved: " + concept + " = " + posType);
			return posType;
		}
		// POS was not queried before
		System.out.println("resolving new concept: " + concept);

		posType = new HashSet<POS>();

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
			Set<POS> conceptCached = conceptCached(current);
			if (conceptCached != null && !conceptCached.isEmpty()) {
				endingConcept = current;
				posType.addAll(conceptCached);
				break outerWhile;
			}

			// check if concept has a POS in wordnet
			Set<POS> conceptPOS_fromWordnet = getConceptPOS(current);
			if (conceptPOS_fromWordnet != null && !conceptPOS_fromWordnet.isEmpty()) {
				endingConcept = current;
				posType.addAll(conceptPOS_fromWordnet);
				break outerWhile;
			}

			// these relations should maintain POS
			Set<StringEdge> out = inputSpace.outgoingEdgesOf(current, "isa");
			out.addAll(inputSpace.outgoingEdgesOf(current, "synonym"));
			out.addAll(inputSpace.outgoingEdgesOf(current, "partof"));
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
			// get POS for each concept
			Set<POS> lPOS = checkPOS_InInputSpace(leftElement, inputSpace);
			Set<POS> rPOS = checkPOS_InInputSpace(rightElement, inputSpace);

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

	public static HashSet<POS> getConceptPOS(String concept) throws JWNLException {
		// try simple direct wordnet test
		HashSet<POS> pos = getWordNetPOS_noRules(concept);
		if (!pos.isEmpty()) {
			return pos;
		}

		// otherwise try compound noun
		pos = new HashSet<POS>(1);
		if (checkWordnetForCompoundNoun(concept)) {
			pos.add(POS.NOUN);
		}
		return pos;
	}

	/**
	 * Checks if the given string is defined as a compound noun POS in wordnet.
	 * 
	 * @param string
	 * @return
	 * @throws JWNLException
	 */
	public static boolean checkWordnetForCompoundNoun(String string) throws JWNLException {

		List<String> words = VariousUtils.arrayToArrayList(VariousUtils.fastSplit(string, ' '));
		// remove stopwords
		words.removeAll(StaticSharedVariables.stopWords);

		int numWords = words.size();
		ListOfSet<POS> possiblePOS_perWord = new ListOfSet<>();

		// assign possible POS for each word
		for (String word : words) {
			HashSet<POS> wPOS = getWordNetPOS_noRules(word);
			possiblePOS_perWord.add(wPOS);
		}

		// check for compound noun
		if (numWords == 2) {

			// test compound noun rules
			if (possiblePOS_perWord.numberOfNonEmptySets() == 2) {

				HashSet<POS> pos0 = possiblePOS_perWord.get(0);
				HashSet<POS> pos1 = possiblePOS_perWord.get(1);

				// catches two word rules with at least one noun
				if (pos0.contains(POS.NOUN) || pos1.contains(POS.NOUN))
					return true;

				// rules with no nouns
				if (pos0.contains(POS.ADJECTIVE)) {
					if (pos1.contains(POS.ADJECTIVE)) {
						return true;
					}
					if (pos1.contains(POS.VERB)) {
						return true;
					}
				}
				if (pos0.contains(POS.ADVERB)) {
					if (pos1.contains(POS.VERB)) {
						return true;
					}
				}
				if (pos0.contains(POS.VERB)) {
					if (pos1.contains(POS.ADVERB)) {
						return true;
					}
				}
			}
			// not matched with the two word rules

		}
		// not identified as a noun OR it is composed of three or more words

		// dirty generic noun test (if contains at least one Noun) - expanded from the 2 noun rule
		// obviously I am not sure if it is ok
//		for (HashSet<POS> pos : possiblePOS_perWord) {
//			if (pos.contains(POS.NOUN)) {
//				return true;
//			}
//		}

		return false;
	}

	/**
	 * Gets a set of POS for the given concept in wordnet. If the concept does not exist in wordnet an empty set is returned. Does not check for compound nouns.
	 * 
	 * @param concept
	 * @return
	 * @throws JWNLException
	 */
	public static HashSet<POS> getWordNetPOS_noRules(String concept) throws JWNLException {
		HashSet<POS> pos = new HashSet<>();
		Dictionary dictionary = StaticSharedVariables.dictionary;
		if (dictionary.getIndexWord(POS.NOUN, concept) != null) {
			pos.add(POS.NOUN);
		}
		if (dictionary.getIndexWord(POS.VERB, concept) != null) {
			pos.add(POS.VERB);
		}
		if (dictionary.getIndexWord(POS.ADJECTIVE, concept) != null) {
			pos.add(POS.ADJECTIVE);
		}
		if (dictionary.getIndexWord(POS.ADVERB, concept) != null) {
			pos.add(POS.ADVERB);
		}
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

			Set<POS> pos = checkPOS_InInputSpace(concept, graph);
			if (pos.isEmpty())
				System.out.printf("%s\t%d\t%d\t%d\t%s\n", concept, degree, inDegree, outDegree, pos);
		}
	}
}
