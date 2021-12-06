package jcfgonc.moea.specific;

import org.moeaframework.core.Problem;
import org.moeaframework.core.Solution;

import graph.DirectedMultiGraph;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import jcfgonc.mapper.GrammarUtils;
import jcfgonc.mapper.MOEA_Config;
import jcfgonc.mapper.MapperUtils;
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
		CustomChromosome cc = new CustomChromosome();
		solution.setVariable(0, cc);
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
			vitalRelationsMean = MapperUtils.calculateVitalRelationsStatistics(pairGraph, StaticSharedVariables.vitalRelations).getMean();
		}

		// relation statistics
		double relationStdDev;
		int numRelations;
		relationStdDev = 100;
		numRelations = 0;
		if (!emptyGraph) {
			double[] rs = MapperUtils.calculateRelationStatistics(pairGraph);
			relationStdDev = rs[1]; // 0...1 : 0 = equal amount of relation labels
			numRelations = (int) rs[2];
		}

//		int degreeOfReferencePair = 0;
//		if (!emptyGraph) {
//			degreeOfReferencePair = pairGraph.degreeOf(referencePair);
//		}

		int refPairInnerDistance = 10;
		if (!emptyGraph) {
			int dist = MappingAlgorithms.calculateReferencePairInnerDistance(StaticSharedVariables.inputSpace, referencePair,
					MOEA_Config.REFERENCE_PAIRINNER_DISTANCE_CALCULATION_LIMIT);
			refPairInnerDistance = (int) MapperUtils.minimumRadialDistanceFunc(3, 1, dist);
		}

		double meanWordsPerConcept = 10;
		if (!emptyGraph) {
			double[] wpcs = MapperUtils.calculateWordsPerConceptStatistics(pairGraph);
//			stats[0] = ds.getMean();
//			stats[1] = ds.getStandardDeviation();
//			stats[2] = ds.getMin();
//			stats[3] = ds.getMax();
			meanWordsPerConcept = wpcs[0];
		}

		double posRatio = 0;
		if (!emptyGraph) {
			posRatio = GrammarUtils.calculateSamePOS_pairsPercentage(pairGraph, StaticSharedVariables.inputSpaceForPOS);
		}

		double closenessCentrality = 0;
		if (!emptyGraph) {
			closenessCentrality = MapperUtils.closenessCentrality(referencePair, pairGraph);
		}

//		double coiso = 0;
		if (!emptyGraph) {
			Object2IntOpenHashMap<OrderedPair<String>> childrenTree = MappingAlgorithms.countNumberOfChildrenPerSubTree(pairGraph, referencePair);
			System.lineSeparator();
		}

		// set solution's objectives here
		int obj_i = 0;
		solution.setObjective(obj_i++, -numPairs);
		solution.setObjective(obj_i++, -vitalRelationsMean);
		solution.setObjective(obj_i++, relationStdDev);
		solution.setObjective(obj_i++, -numRelations);
//		solution.setObjective(obj_i++, -degreeOfReferencePair);
		solution.setObjective(obj_i++, refPairInnerDistance);
		solution.setObjective(obj_i++, meanWordsPerConcept);
		solution.setObjective(obj_i++, -posRatio);
		solution.setObjective(obj_i++, -closenessCentrality);

//		obj_i = 0;
//		// violated constraints are set to 1, otherwise set to 0
//		if (numPairs < 3 || numPairs > MOEA_Config.MAXIMUM_NUMBER_OF_CONCEPT_PAIRS) { // limit the number of vertices in the blend space
//			solution.setConstraint(obj_i++, 1); // violated
//		} else {
//			solution.setConstraint(obj_i++, 0); // not violated
//		}
//
//		if (numRelations < 3) {
//			solution.setConstraint(obj_i++, 1); // violated
//		} else {
//			solution.setConstraint(obj_i++, 0); // not violated
//		}
//
//		if (degreeOfReferencePair < (MOEA_Config.MAXIMUM_NUMBER_OF_CONCEPT_PAIRS / 2)) {
//			solution.setConstraint(obj_i++, 1); // violated
//		} else {
//			solution.setConstraint(obj_i++, 0); // not violated
//		}
//
//		if (posRatio < 0.67) {
//			solution.setConstraint(obj_i++, 1); // violated
//		} else {
//			solution.setConstraint(obj_i++, 0); // not violated
//		}
//
//		if (vitalRelationsMean < 0.67) {
//			solution.setConstraint(obj_i++, 1); // violated
//		} else {
//			solution.setConstraint(obj_i++, 0); // not violated
//		}
	}

	private String[] objectivesDescription = { //
			"d:numPairs", //
			"f:vitalRelationsMean", //
			"f:relationStdDev", //
			"d:numRelations", //
//			"d:degreeOfReferencePair", //
			"d:refPairInnerDistance", //
			"f:meanWordsPerConcept", //
			"f:samePOSpairRatio", //
			"f:closenessCentrality", //
	};

	private String[] constraintsDescription = { //
//			"required numPairs", //
//			"required numRelations", //
//			"required degreeOfReferencePair", //
//			"required posRatio", //
//			"required vitalRelationsMean", //
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
