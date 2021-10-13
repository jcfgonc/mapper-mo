package jcfgonc.mapper;

import org.apache.commons.math3.random.RandomGenerator;

import graph.GraphAlgorithms;
import graph.StringGraph;
import jcfgonc.mapper.structures.MappingStructure;
import structures.OrderedPair;

public class MapperGeneticOperations {


	public static MappingStructure<String, String> initializeGenes(StringGraph inputSpace, RandomGenerator random) {
		// get a random concept pair
		OrderedPair<String> refPair = MappingAlgorithms.getRandomConceptPair(inputSpace, random);
		// set mapping structure
		MappingStructure<String, String> mappingStruct = new MappingStructure<>(refPair);
		// do the isomorphism using the concept pair
		MappingAlgorithms.updateMappingGraph(inputSpace, mappingStruct, MOEA_Config.DEEPNESS_LIMIT, random);
		return mappingStruct;
	}

	public static MappingStructure<String, String> mutateGenes(MappingStructure<String, String> genes, StringGraph inputSpace, RandomGenerator random) {
		OrderedPair<String> refPair = genes.getReferencePair();
		String leftElement = refPair.getLeftElement();
		String rightElement = refPair.getRightElement();

		int numberTries = 0;

		// unalign/rearrange a new refpair from the existing
		// -------------------------------------------------
//		// offset locally?
//		if (random.nextDouble() < LOCAL_JUMP_PROBABILITY) {
		// do a random walk on either left or right concepts (or both)
		do {
			if (numberTries > MOEA_Config.NUMBER_MUTATION_TRIES) {
				System.err.println("mutation reached maximum number of tries (" + numberTries + ") without making a change");
				break;
			}

			if (random.nextBoolean()) {
				double r = Math.pow(random.nextDouble(), MOEA_Config.JUMP_PROBABILITY_POWER);
				int hops = (int) Math.ceil(r * MOEA_Config.REFPAIR_JUMP_RANGE);
				leftElement = GraphAlgorithms.getVertexFromRandomWalk(random, leftElement, inputSpace, hops);
			}
			if (random.nextBoolean()) {
				double r = Math.pow(random.nextDouble(), MOEA_Config.JUMP_PROBABILITY_POWER);
				int hops = (int) Math.ceil(r * MOEA_Config.REFPAIR_JUMP_RANGE);
				rightElement = GraphAlgorithms.getVertexFromRandomWalk(random, rightElement, inputSpace, hops);
			}
			numberTries++;
		}
		// prevent left and right from being equals
		while (leftElement.equals(rightElement));

//		}
		// better not to mutate that much, thats why this code is commented out

//		// offset globally
//		else {
//			// do a random shift to far away on either left or right concepts (or both)
//			do {
//				if (random.nextBoolean()) {
//					leftElement = VariousUtils.getRandomElementFromCollection(vertexSetAsList, random);
//				}
//				if (random.nextBoolean()) {
//					rightElement = VariousUtils.getRandomElementFromCollection(vertexSetAsList, random);
//				}
//			}
//			// prevent left and right from being equals
//			while (leftElement.equals(rightElement));
//		}

		// if (refPair.getLeftElement().equals(leftElement) && //
		// refPair.getRightElement().equals(rightElement)) {
		// System.err.println("reference pair did not change");
		// }
		// store refpair back in the gene
		genes.setReferencePair(new OrderedPair<String>(leftElement, rightElement));
		MappingAlgorithms.updateMappingGraph(inputSpace, genes, MOEA_Config.DEEPNESS_LIMIT, random);
		return genes;
	}

}
