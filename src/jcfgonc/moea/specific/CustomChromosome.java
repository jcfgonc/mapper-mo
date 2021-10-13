package jcfgonc.moea.specific;

import org.moeaframework.core.Variable;

import graph.DirectedMultiGraph;
import jcfgonc.mapper.MapperGeneticOperations;
import jcfgonc.mapper.StaticSharedVariables;
import jcfgonc.mapper.structures.MappingStructure;
import structures.OrderedPair;

/**
 * This class represents the problem domain X, as a single (1D) dimension stored on a single custom gene.
 * 
 * @author jcfgonc@gmail.com
 *
 */
public class CustomChromosome implements Variable {

	private static final long serialVersionUID = 1449562469642194509L;

	private MappingStructure<String, String> mappingStruct;

	/**
	 * Called by CustomProblem.newSolution(). Custom code required here.
	 * 
	 */
	public CustomChromosome() {
		super();
	//	System.out.println("CustomChromosome()");

		mappingStruct = MapperGeneticOperations.initializeGenes(StaticSharedVariables.inputSpace, StaticSharedVariables.random);
	}

	/**
	 * Called by the copy() function below. Custom code required here.
	 * 
	 */
	public CustomChromosome(CustomChromosome other) {
		super();
//		System.out.println("CustomChromosome(copy)");

		MappingStructure<String, String> otherGene = other.getGene();
		OrderedPair<String> otherRefPair = otherGene.getReferencePair();

		mappingStruct = new MappingStructure<String, String>();
		mappingStruct.setReferencePair(new OrderedPair<>(otherRefPair)); // very important to copy
		mappingStruct.setPairGraph(new DirectedMultiGraph<>(otherGene.getPairGraph())); // very important to copy
	}

	@Override
	/**
	 * Invoked by MOEA when copying a solution and by this class' copy constructor. Custom code may be required here.
	 */
	public CustomChromosome copy() {
		CustomChromosome newcc = new CustomChromosome(this);
		return newcc;
	}

	/**
	 * Invoked by CustomMutation.evolve(). Custom code required here.
	 */
	public void mutate() {
	//	System.out.println("mutate()");
		MapperGeneticOperations.mutateGenes(mappingStruct, StaticSharedVariables.inputSpace, StaticSharedVariables.random);
	}

	@Override
	/**
	 * Invoked by MOEA framework when creating a new solution in the first epoch. Custom code required here.
	 */
	public void randomize() {
	//	System.out.println("randomize()");
		mutate();
	}

	@Override
	/**
	 * Custom toString() code helpful here.
	 */
	public String toString() {
		return mappingStruct.toString();
	}

	public MappingStructure<String, String> getGene() {
		return mappingStruct;
	}

}
