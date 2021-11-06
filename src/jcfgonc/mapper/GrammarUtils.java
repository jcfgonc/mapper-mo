package jcfgonc.mapper;

import java.util.ArrayDeque;
import java.util.Arrays;
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
import structures.OrderedPair;
import structures.SynchronizedMapOfSet;
import utils.VariousUtils;

public class GrammarUtils {
	/**
	 * cache of previously decoded POS for each concept
	 */
	private static SynchronizedMapOfSet<String, POS> cachedConceptPOS = new SynchronizedMapOfSet<>();

	private boolean conceptCached(String concept, POS cachedType) {
		Set<POS> cachedPOS = cachedConceptPOS.get(concept);
		if (cachedPOS != null && cachedPOS.contains(cachedType)) {
			return true;
		}
		return false;
	}

	private boolean conceptCachedAsNoun(String concept) {
		Set<POS> cachedPOS = cachedConceptPOS.get(concept);
		if (cachedPOS != null && cachedPOS.contains(POS.NOUN)) {
			return true;
		}
		return false;
	}

	@SuppressWarnings("unused")
	/**
	 * check if the given concept ISA <something> noun in the inputspace - should be recursive
	 * 
	 * @param concept
	 * @throws JWNLException
	 */
	public static boolean checkISA_NounInInputSpace(String concept) throws JWNLException {
		Dictionary dictionary = StaticSharedVariables.dictionary;
		StringGraph inputSpace = StaticSharedVariables.inputSpace;

		HashSet<String> closedSet = new HashSet<>();
		ArrayDeque<String> openSet = new ArrayDeque<>();
		HashMap<String, String> cameFrom = new HashMap<>();
		String endingTarget = null;

		openSet.addLast(concept);
		// ---------init
		POS posType = null;

		outerWhile: while (!openSet.isEmpty()) {
			String current = openSet.removeFirst();
			closedSet.add(current);
			Set<StringEdge> in = inputSpace.incomingEdgesOf(current, "isa");
			Set<StringEdge> out = inputSpace.outgoingEdgesOf(current, "isa");
			HashSet<String> targets = StringGraph.edgesTargets(out);

			for (String target : targets) {
				if (closedSet.contains(target))
					continue;
				// check if target has been decoded before
				// otherwise check if target is
				// found an ISA target which is a noun
				HashSet<POS> wordPOS = getWordNetPOS(target);
				if (wordPOS.contains(POS.NOUN)) {
					posType = POS.NOUN;
					endingTarget = target;
					break outerWhile;
				}
				openSet.addLast(target); // later expand upper ISA level
				cameFrom.put(target, concept);
			}
			System.lineSeparator();
		}
		// posType defined OR not
		if (posType == null) {
			System.out.println("could not resolve " + concept + " through ISA");
		}

		// store middle targets POS by starting at endingTarget and going back the camefrom path
		// get path
		String prior;
		String current = endingTarget;
		while (true) {
			prior = cameFrom.get(current);
			if (prior == null)
				break;
			current = prior;
		}
		System.lineSeparator();
		return false;
	}

	public static boolean sameWordPOS(OrderedPair<String> pair) {
		String leftElement = pair.getLeftElement();
		String rightElement = pair.getRightElement();

		// compound concepts are expected to be space separated
		try {
			// get POS for each concept
			Set<POS> lPOS = getConceptPOS_fromWordnet(leftElement);
			Set<POS> rPOS = getConceptPOS_fromWordnet(rightElement);

			if (lPOS.isEmpty())
				System.out.println("could not get POS for " + leftElement);
			if (rPOS.isEmpty())
				System.out.println("could not get POS for " + rightElement);

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
	public static Set<POS> getConceptPOS(String concept) throws JWNLException {
		Set<POS> posList = getConceptPOS_fromWordnet(concept);
		if (checkISA_NounInInputSpace(concept)) {
			posList.add(POS.NOUN);
		}
		return posList;
	}

	/**
	 * Gets concept POS list from wordnet only. Called by getConceptPOS().
	 * 
	 * @param concept
	 * @return
	 * @throws JWNLException
	 */
	public static Set<POS> getConceptPOS_fromWordnet(String concept) throws JWNLException {
		// was that concept's POS previously identified?
		Set<POS> cachedPOS = cachedConceptPOS.get(concept);
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
	 * Self-explanatory. Checks if the given string is defined as a noun POS in wordnet, nothing more.
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

		List<String> words = Arrays.asList(VariousUtils.fastSplit(string, ' '));
		// remove stopwords
		words.removeAll(StaticSharedVariables.stopWords);

		int numWords = words.size();
		if (numWords == 2) {// otherwise check for compound noun

			ListOfSet<POS> possiblePOS_perWord = new ListOfSet<>();
			// assign possible POS for each word
			for (String word : words) {
				HashSet<POS> wPOS = getWordNetPOS(word);
				possiblePOS_perWord.add(wPOS);
			}

			// test compound noun rules
			if (possiblePOS_perWord.numberOfNonEmptySets() == 2) {

				HashSet<POS> pos0 = possiblePOS_perWord.get(0);
				HashSet<POS> pos1 = possiblePOS_perWord.get(1);

				if (pos0.contains(POS.ADJECTIVE)) {
					if (pos1.contains(POS.ADJECTIVE)) {
						return true;
					}
					if (pos1.contains(POS.NOUN)) {
						return true;
					}
					if (pos1.contains(POS.VERB)) {
						return true;
					}
				}
				if (pos0.contains(POS.ADVERB)) {
					if (pos1.contains(POS.NOUN)) {
						return true;
					}
					if (pos1.contains(POS.VERB)) {
						return true;
					}
				}
				if (pos0.contains(POS.NOUN)) {
					if (pos1.contains(POS.ADJECTIVE)) {
						return true;
					}
					if (pos1.contains(POS.ADVERB)) {
						return true;
					}
					if (pos1.contains(POS.NOUN)) {
						return true;
					}
					if (pos1.contains(POS.VERB)) {
						return true;
					}
				}
				if (pos0.contains(POS.VERB)) {
					if (pos1.contains(POS.ADVERB)) {
						return true;
					}
					if (pos1.contains(POS.NOUN)) {
						return true;
					}
				}
			}
			// not matched with the two word rules
		}
		// not identified as a noun OR it is composed of three or more words
		return false;
	}

	/**
	 * Gets a set of POS for the given concept in wordnet. If the concept does not exist in wordnet an empty set is returned.
	 * 
	 * @param concept
	 * @return
	 * @throws JWNLException
	 */
	public static HashSet<POS> getWordNetPOS(String concept) throws JWNLException {
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

}
