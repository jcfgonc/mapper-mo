package jcfgonc.mapper;

import org.apache.commons.math3.random.RandomGenerator;

import graph.GraphAlgorithms;
import graph.StringGraph;
import jcfgonc.mapper.structures.MappingStructure;
import structures.OrderedPair;
import utils.VariousUtils;

public class MapperGeneticOperations {

	public static MappingStructure<String, String> initializeGenes(StringGraph inputSpace, RandomGenerator random) {
		OrderedPair<String> refPair = createReferencePair(inputSpace, random);
		// set mapping structure
		MappingStructure<String, String> mappingStruct = new MappingStructure<>(refPair);
		// do the isomorphism using the concept pair
		MappingAlgorithms.updateMappingGraph(inputSpace, mappingStruct, random);
		return mappingStruct;
	}

	@SuppressWarnings("unused")
	private static OrderedPair<String> createReferencePair(StringGraph inputSpace, RandomGenerator random) {
		boolean fixedLeft = MOEA_Config.fixedConceptLeft != null;
		boolean fixedRight = MOEA_Config.fixedConceptRight != null;
		if (fixedLeft) { // L fixed
			if (fixedRight) { // LR fixed
				return new OrderedPair<String>(MOEA_Config.fixedConceptLeft, MOEA_Config.fixedConceptRight);
			} else { // L fixed R free
				String randomConcept;
				do { // make sure L and R are different
					randomConcept = VariousUtils.getRandomElementFromCollection(inputSpace.getVertexSet(), random);
				} while (randomConcept.equals(MOEA_Config.fixedConceptLeft));
				return new OrderedPair<String>(MOEA_Config.fixedConceptLeft, randomConcept);
			}
		} else {// L free
			if (fixedRight) { // L free R fixed
				String randomConcept;
				do { // make sure L and R are different
					randomConcept = VariousUtils.getRandomElementFromCollection(inputSpace.getVertexSet(), random);
				} while (randomConcept.equals(MOEA_Config.fixedConceptRight));
				return new OrderedPair<String>(randomConcept, MOEA_Config.fixedConceptRight);
			} else { // L free R free
				// get a random concept pair
				return MappingAlgorithms.getRandomConceptPair(inputSpace, random);
			}
		}
	}

	@SuppressWarnings("unused")
	public static MappingStructure<String, String> mutateGenes(MappingStructure<String, String> genes, StringGraph inputSpace, RandomGenerator random) {
		OrderedPair<String> refPair = genes.getReferencePair();
		String leftElement = refPair.getLeftElement();
		String rightElement = refPair.getRightElement();

		// mutate the reference pair?
		boolean leftFixed = MOEA_Config.fixedConceptLeft != null;
		boolean rightFixed = MOEA_Config.fixedConceptRight != null;
		if (leftFixed && rightFixed) {
			// do not mutate
		} else {
			if (random.nextDouble() < MOEA_Config.REFPAIR_MUTATION_PROBABILITY) { // 0.125

				int numberTries = 0;

				// unalign/rearrange a new refpair from the existing
				// do a random walk on either left or right concepts (or both)
				do {
					if (numberTries > MOEA_Config.NUMBER_MUTATION_TRIES) { // 16
						System.err.println("mutation reached maximum number of tries (" + numberTries + ") without making a change");
						break;
					}
					if (random.nextBoolean() && !leftFixed) { // shift left element
						int hops = 1;
						if (MOEA_Config.REFPAIR_JUMP_RANGE > 1) {
							double r = Math.pow(random.nextDouble(), MOEA_Config.JUMP_PROBABILITY_POWER); // 5.2
							hops = (int) Math.ceil(r * MOEA_Config.REFPAIR_JUMP_RANGE); // 1
							if (hops < 1)
								hops = 1;
						}
						leftElement = GraphAlgorithms.getVertexFromRandomWalk(random, leftElement, inputSpace, hops);
					} else { // shift right element
						int hops = 1;
						if (MOEA_Config.REFPAIR_JUMP_RANGE > 1) {
							double r = Math.pow(random.nextDouble(), MOEA_Config.JUMP_PROBABILITY_POWER); // 5.2
							hops = (int) Math.ceil(r * MOEA_Config.REFPAIR_JUMP_RANGE); // 1
							if (hops < 1)
								hops = 1;
						}
						rightElement = GraphAlgorithms.getVertexFromRandomWalk(random, rightElement, inputSpace, hops);
					}
					numberTries++;
				}
				// prevent left and right from being equals
				while (leftElement.equals(rightElement));

				// store refpair back in the gene
				genes.setReferencePair(new OrderedPair<String>(leftElement, rightElement));
			}
		}

//		if (refPair.getLeftElement().equals(leftElement) && // debug
//				refPair.getRightElement().equals(rightElement)) {
//			System.err.printf("reference pair %s did not change\n", refPair);
//		}

		// create random left-right isomorphism
		MappingAlgorithms.updateMappingGraph(inputSpace, genes, random);
		return genes;
	}

}
