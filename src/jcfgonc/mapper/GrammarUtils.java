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
import utils.VariousUtils;

public class GrammarUtils {

	public static void isaWho(String concept) throws JWNLException {
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
			Set<StringEdge> in = inputSpace.incomingEdgesOf(current, "isa");
			Set<StringEdge> out = inputSpace.outgoingEdgesOf(current, "isa");
			HashSet<String> targets = StringGraph.edgesTargets(out);

			for (String target : targets) {
				// found an ISA target which is a noun
				HashSet<POS> wordPOS = getWordPOS(target);
				if (wordPOS.contains(POS.NOUN)) {
					posType = POS.NOUN;
					endingTarget = target;
					break outerWhile;
				}
				if (wordPOS.contains(POS.VERB)) {
					posType = POS.VERB;
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
	}

	public static boolean sameWordClass(OrderedPair<String> pair) {
		String leftElement = pair.getLeftElement();
		String rightElement = pair.getRightElement();

		// compound concepts are expected to be space separated
		try {
			// get POS for each concept
			HashSet<POS> lPOS = getConceptPOS(leftElement);
			HashSet<POS> rPOS = getConceptPOS(rightElement);

			if (lPOS.isEmpty())
				System.out.println("could not get word class for " + leftElement);
			if (rPOS.isEmpty())
				System.out.println("could not get word class for " + rightElement);

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
		HashSet<POS> pos = new HashSet<>();
		Dictionary dictionary = StaticSharedVariables.dictionary;
		if (isNoun(concept)) {
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

	private static boolean isNoun(String string) throws JWNLException {
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
				HashSet<POS> wPOS = getWordPOS(word);
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
			// not matched with the rules
			System.lineSeparator();
		}
		// TODO check if the given concept ISA <something> noun in the inputspace
		// that could be recursive
//		isaWho(string);

		return false;
	}

	public static HashSet<POS> getWordPOS(String concept) throws JWNLException {
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
