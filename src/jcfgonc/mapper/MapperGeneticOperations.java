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
		MappingAlgorithms.updateMappingGraph(inputSpace, mappingStruct, random);
		return mappingStruct;
	}

	public static MappingStructure<String, String> mutateGenes(MappingStructure<String, String> genes, StringGraph inputSpace, RandomGenerator random) {
		OrderedPair<String> refPair = genes.getReferencePair();
		String leftElement = refPair.getLeftElement();
		String rightElement = refPair.getRightElement();

		// mutate the reference pair?
		if (random.nextDouble() < MOEA_Config.REFPAIR_MUTATION_PROBABILITY) {

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
				// shift left element
				if (random.nextBoolean()) {
					int hops = 1;
					if (MOEA_Config.REFPAIR_JUMP_RANGE > 1) {
						double r = Math.pow(random.nextDouble(), MOEA_Config.JUMP_PROBABILITY_POWER);
						hops = (int) Math.ceil(r * MOEA_Config.REFPAIR_JUMP_RANGE);
						if (hops < 1)
							hops = 1;
					}
					leftElement = GraphAlgorithms.getVertexFromRandomWalk(random, leftElement, inputSpace, hops);
				} else
				// shift right element
				// if (random.nextBoolean()) //
				{
					int hops = 1;
					if (MOEA_Config.REFPAIR_JUMP_RANGE > 1) {
						double r = Math.pow(random.nextDouble(), MOEA_Config.JUMP_PROBABILITY_POWER);
						hops = (int) Math.ceil(r * MOEA_Config.REFPAIR_JUMP_RANGE);
						if (hops < 1)
							hops = 1;
					}
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

			// store refpair back in the gene
			genes.setReferencePair(new OrderedPair<String>(leftElement, rightElement));
		}

//		if (refPair.getLeftElement().equals(leftElement) && //
//				refPair.getRightElement().equals(rightElement)) {
//			System.err.printf("reference pair %s did not change\n", refPair);
//		}

		// create random left-right isomorphism
		MappingAlgorithms.updateMappingGraph(inputSpace, genes, random);
		return genes;
	}

}
