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
import net.sf.extjwnl.data.IndexWord;
import net.sf.extjwnl.data.POS;
import net.sf.extjwnl.dictionary.Dictionary;
import structures.ListOfSet;
import structures.ObjectCount;
import structures.ObjectCounter;
import structures.OrderedPair;
import structures.SynchronizedMapOfSet;
import utils.VariousUtils;

public class GrammarUtils {
	/**
	 * cache of previously decoded POS for each concept
	 */
	private static SynchronizedMapOfSet<String, POS> cachedConceptPOS = new SynchronizedMapOfSet<>();

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
		if (posType != null && !posType.isEmpty())
			return posType;

		posType = new HashSet<POS>();

		HashSet<String> closedSet = new HashSet<>();
		ArrayDeque<String> openSet = new ArrayDeque<>();
		HashMap<String, String> cameFrom = new HashMap<>();
		String endingTarget = null;

		// ---------init
		openSet.addLast(concept);

		outerWhile: while (!openSet.isEmpty()) {
			String current = openSet.removeFirst();

			closedSet.add(current);
			// these relations should maintain POS
			Set<StringEdge> out = inputSpace.outgoingEdgesOf(current, "isa");
			out.addAll(inputSpace.outgoingEdgesOf(current, "synonym"));
			out.addAll(inputSpace.outgoingEdgesOf(current, "partof"));
//			out.addAll(inputSpace.outgoingEdgesOf(current, "derivedfrom"));
			HashSet<String> targets = StringGraph.edgesTargets(out);

			for (String target : targets) {
				if (closedSet.contains(target))
					continue;

				// remember which concept came before
				cameFrom.put(target, current);
				openSet.addLast(target); // later expand next ISA target

				// check if target has been decoded before
				Set<POS> targetCached = conceptCached(target);
				if (targetCached != null && !targetCached.isEmpty()) {
					endingTarget = target;
					posType.addAll(targetCached);
					break outerWhile;
				}

				// check if target has a POS in wordnet
				Set<POS> conceptPOS_fromWordnet = getConceptPOS_fromWordnet(target);
				if (conceptPOS_fromWordnet != null && !conceptPOS_fromWordnet.isEmpty()) {
					endingTarget = target;
					posType.addAll(conceptPOS_fromWordnet);
					break outerWhile;
				}
			} // went through all targets
			if (!openSet.isEmpty()) { // there is yet stuff to explore
				System.lineSeparator();
			}
		}
		// posType defined OR not
		if (posType.isEmpty()) {
//			System.out.println("could not resolve " + concept + " through ISA");
		} else {
			// store middle targets POS by starting at endingTarget and going back the camefrom path
			// get path
			String prior;
			String current = endingTarget;
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
			Set<POS> lPOS = getConceptPOS(leftElement, inputSpace);
			Set<POS> rPOS = getConceptPOS(rightElement, inputSpace);

			if (lPOS.isEmpty())
				System.out.println("could not get POS: " + leftElement + "\tdegree: " + inputSpace.degreeOf(leftElement));
			if (rPOS.isEmpty())
				System.out.println("could not get POS: " + rightElement + "\tdegree: " + inputSpace.degreeOf(rightElement));

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

	/**
	 * Gets concept POS list from wordnet and using ISA hierarchy in the inputspace. Calls getConceptPOS_fromWordnet().
	 * 
	 * @param concept
	 * @return
	 * @throws JWNLException
	 */
	public static Set<POS> getConceptPOS(String concept, StringGraph inputSpace) throws JWNLException {
		Set<POS> posList = getConceptPOS_fromWordnet(concept);
		// if wordnet does not know anything, try using ISA
		if (posList.isEmpty()) {
			posList = checkPOS_InInputSpace(concept, inputSpace);
		}
		return posList;
	}

	/**
	 * Gets concept POS list from wordnet only. Tries both simple and compound cases.
	 * 
	 * @param concept
	 * @return
	 * @throws JWNLException
	 */
	public static Set<POS> getConceptPOS_fromWordnet(String concept) throws JWNLException {
		// was that concept's POS previously identified?
		Set<POS> cachedPOS = conceptCached(concept);
		if (cachedPOS != null && !cachedPOS.isEmpty()) {
			// YES
			return cachedPOS;
		} else {
			// NO
			HashSet<POS> pos = new HashSet<>();
			Dictionary dictionary = StaticSharedVariables.dictionary;
			if (isNounInWordnet(concept)) {
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
			cachedConceptPOS.add(concept, pos);
			return pos;
		}
	}

	/**
	 * Self-explanatory. Checks if the given string is defined as a noun POS (simple or compound) in wordnet, nothing more.
	 * 
	 * @param string
	 * @return
	 * @throws JWNLException
	 */
	public static boolean isNounInWordnet(String string) throws JWNLException {
		Dictionary dictionary = StaticSharedVariables.dictionary;

		// check for easy/simple/existing identifiable noun
		IndexWord indexWord = dictionary.getIndexWord(POS.NOUN, string);
		if (indexWord != null)
			return true;

		List<String> words = VariousUtils.arrayToArrayList(VariousUtils.fastSplit(string, ' '));
		// remove stopwords
		words.removeAll(StaticSharedVariables.stopWords);

		int numWords = words.size();
		ListOfSet<POS> possiblePOS_perWord = new ListOfSet<>();

		// assign possible POS for each word
		for (String word : words) {
			HashSet<POS> wPOS = getWordNetPOS_simple(word);
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
		for (HashSet<POS> pos : possiblePOS_perWord) {
			if (pos.contains(POS.NOUN)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Gets a set of POS for the given concept in wordnet. If the concept does not exist in wordnet an empty set is returned. Does not check for compound nouns.
	 * 
	 * @param concept
	 * @return
	 * @throws JWNLException
	 */
	public static HashSet<POS> getWordNetPOS_simple(String concept) throws JWNLException {
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

			Set<POS> pos = getConceptPOS(concept, graph);
			if (pos.isEmpty())
				System.out.printf("%s\t%d\t%d\t%d\t%s\n", concept, degree, inDegree, outDegree, pos);
		}
	}
}
