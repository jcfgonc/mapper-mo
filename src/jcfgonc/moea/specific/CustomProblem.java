package jcfgonc.moea.specific;

import org.moeaframework.core.Problem;
import org.moeaframework.core.Solution;

import graph.DirectedMultiGraph;
import jcfgonc.mapper.GrammarUtils;
import jcfgonc.mapper.LogicUtils;
import jcfgonc.mapper.MOEA_Config;
import jcfgonc.mapper.MappingAlgorithms;
import jcfgonc.mapper.StaticSharedVariables;
import jcfgonc.mapper.structures.MappingStructure;
import jcfgonc.moea.generic.ProblemDescription;
import structures.OrderedPair;

public class CustomProblem implements Problem, ProblemDescription {

	/**
	 * Invoked by Custom Launcher when creating this problem. Custom code typically required here.
	 * 
	 */
	public CustomProblem() {
		// nothing to do
	}

	@Override
	/**
	 * Invoked by MOEA when initializing/filling the solution population. Custom code may required here. Typically only invoked in the initial phase of the
	 * algorithm.
	 */
	public Solution newSolution() {
		// !!! do not touch this unless the solution domain X has more than ONE dimension
		Solution solution = new Solution(getNumberOfVariables(), getNumberOfObjectives(), getNumberOfConstraints());
		solution.setVariable(0, new CustomChromosome());
		return solution;
	}

	@Override
	/**
	 * Invoked by MOEA when evaluating a solution. Custom code required here. Remember to define the variable's descriptions in MOConfig.OBJECTIVE_DESCRIPTION
	 */
	public void evaluate(Solution solution) {
		CustomChromosome cc = (CustomChromosome) solution.getVariable(0); // unless the solution domain X has more than one dimension
		MappingStructure<String, String> mappingStructure = cc.getGene();
		DirectedMultiGraph<OrderedPair<String>, String> pairGraph = mappingStructure.getPairGraph();
		OrderedPair<String> referencePair = mappingStructure.getReferencePair();

		int numPairs = pairGraph.getNumberOfVertices();
		boolean emptyGraph = numPairs <= 1;

		// maximize the presence of vital/important relations
		double vitalRelationsMean = 0;
		if (!emptyGraph) {
			vitalRelationsMean = LogicUtils.calculateVitalRelationsStatistics(pairGraph, StaticSharedVariables.vitalRelations).getMean();
		}

		// relation statistics
		double relationStdDev;
		int numRelations;
		relationStdDev = 100;
		numRelations = 0;
		if (!emptyGraph) {
			double[] rs = LogicUtils.calculateRelationStatistics(pairGraph);
			relationStdDev = rs[1]; // 0...1 : 0 = equal amount of relation labels
			numRelations = (int) rs[2];
		}

		int degreeOfReferencePair = 0;
		if (!emptyGraph) {
			degreeOfReferencePair = pairGraph.degreeOf(referencePair);
		}

		int refPairInnerDistance = 0;
		if (!emptyGraph) {
		//	refPairInnerDistance = MappingAlgorithms.calculateReferencePairInnerDistance(StaticSharedVariables.inputSpace, referencePair, 10);
		}

		double meanWordsPerConcept = 100;
		if (!emptyGraph) {
			double[] wpcs = LogicUtils.calculateWordsPerConceptStatistics(pairGraph);
//			stats[0] = ds.getMean();
//			stats[1] = ds.getStandardDeviation();
//			stats[2] = ds.getMin();
//			stats[3] = ds.getMax();
			meanWordsPerConcept = wpcs[0];
		}
		
		boolean sameWordClass = GrammarUtils.sameWordPOS(referencePair);

		// set solution's objectives here
		int obj_i = 0;
		solution.setObjective(obj_i++, -numPairs);
		solution.setObjective(obj_i++, -vitalRelationsMean);
		solution.setObjective(obj_i++, relationStdDev);
		solution.setObjective(obj_i++, -numRelations);
		solution.setObjective(obj_i++, -degreeOfReferencePair);
		solution.setObjective(obj_i++, -refPairInnerDistance);
		solution.setObjective(obj_i++, meanWordsPerConcept);

		// violated constraints are set to 1, otherwise set to 0
		if (numPairs < 3 || numPairs > MOEA_Config.MAXIMUM_NUMBER_OF_CONCEPT_PAIRS) { // limit the number of vertices in the blend space
			solution.setConstraint(0, 1); // violated
		} else {
			solution.setConstraint(0, 0); // not violated
		}

		if (numRelations < 3) {
			solution.setConstraint(1, 1); // violated
		} else {
			solution.setConstraint(1, 0); // not violated
		}

		if (refPairInnerDistance < 2) {
			solution.setConstraint(2, 1); // violated
		} else {
			solution.setConstraint(2, 0); // not violated
		}
	}

	private String[] objectivesDescription = { //
			"d:numPairs", //
			"f:vitalRelationsMean", //
			"f:relationStdDev", //
			"d:numRelations", //
			"d:degreeOfReferencePair", //
			"d:refPairInnerDistance", //
			"f:meanWordsPerConcept", //
	};

	private String[] constraintsDescription = { //
			"required numPairs", //
			"required numRelations", //
			"required refPairInnerDistance", //
	};

	@Override
	/**
	 * The number of objectives defined by this problem.
	 */
	public int getNumberOfObjectives() {
		return objectivesDescription.length;
	}

	@Override
	public String getObjectiveDescription(int varid) {
		return objectivesDescription[varid];
	}

	@Override
	/**
	 * The number of constraints defined by this problem.
	 */
	public int getNumberOfConstraints() {
		return constraintsDescription.length;
	}

	@Override
	public String getConstraintsDescription(int varid) {
		return constraintsDescription[varid];
	}

	@Override
	/**
	 * NOT IMPLEMENTED: this is supposed to be used somewhere, I don't know where (probably in the GUI's title?)
	 */
	public String getProblemDescription() {
		return "Conceptual Mapper: Multi-Objective version";
	}

	@Override
	public String getVariableDescription(int varid) {
		return "g:mapping";
	}

	@Override
	public String getName() {
		return getClass().getSimpleName();
	}

	@Override
	/**
	 * The number of variables defined by this problem.
	 */
	public int getNumberOfVariables() {
		return 1; // blend space/graph
	}

	@Override
	public void close() {
	}

}
