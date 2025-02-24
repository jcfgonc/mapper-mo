package jcfgonc.moea.specific;

import java.util.HashSet;

import org.moeaframework.core.Problem;
import org.moeaframework.core.Solution;

import graph.DirectedMultiGraph;
import jcfgonc.mapper.MOEA_Config;
import jcfgonc.mapper.MappingAlgorithms;
import jcfgonc.mapper.ObjectiveEvaluationUtils;
import jcfgonc.mapper.StaticSharedVariables;
import jcfgonc.mapper.structures.MappingStructure;
import jcfgonc.moea.generic.ProblemDescription;
import linguistics.GrammarUtilsWordNet;
import structures.MapOfList;
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

//		System.out.println(referencePair + "\t" + pairGraph.getNumberOfVertices());

		int numPairs = pairGraph.getNumberOfVertices();
		boolean emptyGraph = numPairs <= 1;

		// maximize the presence of vital/important relations
		double vitalRelationsMean = 0;
		if (!emptyGraph) {
			vitalRelationsMean = ObjectiveEvaluationUtils.calculateVitalRelationsStatistics(pairGraph, StaticSharedVariables.vitalRelations).getMean();
		}

		// relation statistics
		int numRelations;
		numRelations = 0;
		if (!emptyGraph) {
			double[] rs = ObjectiveEvaluationUtils.calculateRelationStatistics(pairGraph);
			// returns the following:
//			stats[0] = mean;
//			stats[1] = stddev;
//			stats[2] = numRelations;
			numRelations = (int) rs[2]; // number of unique relations
		}

		int degreeOfReferencePair = 0;
		if (!emptyGraph) {
			degreeOfReferencePair = pairGraph.degreeOf(referencePair);
		}

//		double refPairOptimalDegree = 100; // to minimize
//		if (!emptyGraph) {
//			refPairOptimalDegree = ObjectiveEvaluationUtils.idealDegree(pairGraph, referencePair);
//		}

		int refPairInnerDistance = 10;
		if (!emptyGraph) {
			int dist = MappingAlgorithms.calculateReferencePairInnerDistance(StaticSharedVariables.inputSpace, referencePair,
					MOEA_Config.REFERENCE_PAIRINNER_DISTANCE_CALCULATION_LIMIT);
			refPairInnerDistance = (int) ObjectiveEvaluationUtils.minimumRadialDistanceFunc(3, 1, dist);
		}

		double meanWordsPerConcept = 10;
		if (!emptyGraph) {
			double[] wpcs = ObjectiveEvaluationUtils.calculateWordsPerConceptStatistics(pairGraph, MOEA_Config.MAX_ACCEPTABLE_CONCEPT_WORD_COUNT);
			meanWordsPerConcept = wpcs[0];
		}

		double posRatio = 0;
		if (!emptyGraph) {
			posRatio = GrammarUtilsWordNet.calculateSamePOS_pairsPercentage(pairGraph, StaticSharedVariables.inputSpaceForPOS);
		}

//		double closenessCentrality = 0;
//		if (!emptyGraph) {
//			closenessCentrality = MappingAlgorithms.closenessCentrality(referencePair, pairGraph);
//		}

		// initialize to maximum number of vertices
		double subTreeBal = pairGraph.getNumberOfVertices();
		if (!emptyGraph) {
			subTreeBal = ObjectiveEvaluationUtils.calculateSubTreesBalance(pairGraph, referencePair);
//			System.out.println(subTreeBal);
		}

		double assymetricRelationCount = 10;
		if (!emptyGraph) {

			if(pairGraph.getNumberOfVertices()>6) {
				System.lineSeparator();
			}
			// calculates the number of paths from the origin to the terminals containing opposing directional edges
			// ie +isa,-isa
			MapOfList<OrderedPair<String>, String> priorRelations = new MapOfList<>();
			HashSet<OrderedPair<String>> terminalSet = new HashSet<>();
			MappingAlgorithms.calculatePathsFromOrigin(pairGraph, referencePair, priorRelations, terminalSet);
			assymetricRelationCount = ObjectiveEvaluationUtils.calculateRelationAsymmetryPenalty(priorRelations, terminalSet);
		}

		// set solution's objectives here
		int obj_i = 0;
		solution.setObjective(obj_i++, -numPairs);
		solution.setObjective(obj_i++, -vitalRelationsMean);
//		solution.setObjective(obj_i++, relationStdDev);
		solution.setObjective(obj_i++, -numRelations);
//		solution.setObjective(obj_i++, -degreeOfReferencePair);
		solution.setObjective(obj_i++, refPairInnerDistance);
		solution.setObjective(obj_i++, meanWordsPerConcept);
		solution.setObjective(obj_i++, -posRatio);
//		solution.setObjective(obj_i++, -closenessCentrality);
		solution.setObjective(obj_i++, subTreeBal);
		solution.setObjective(obj_i++, assymetricRelationCount);

		obj_i = 0;
		// violated constraints are set to 1, otherwise set to 0
		if (degreeOfReferencePair == 1) {
			solution.setConstraint(obj_i++, 1); // violated
		} else {
			solution.setConstraint(obj_i++, 0); // not violated
		}
		if (posRatio < 0.95) {
			solution.setConstraint(obj_i++, 1); // violated
		} else {
			solution.setConstraint(obj_i++, 0); // not violated
		}
		if (subTreeBal > 3.5) {
			solution.setConstraint(obj_i++, 1); // violated
		} else {
			solution.setConstraint(obj_i++, 0); // not violated
		}
		if (numRelations < 3) {
			solution.setConstraint(obj_i++, 1); // violated
		} else {
			solution.setConstraint(obj_i++, 0); // not violated
		}
		if (vitalRelationsMean < 0.85) {
			solution.setConstraint(obj_i++, 1); // violated
		} else {
			solution.setConstraint(obj_i++, 0); // not violated
		}

	}

	private String[] objectivesDescription = { //
			"d:numPairs", //
			"f:vitalRelationsMean", //
			"d:numRelations", //
//			"d:degreeOfReferencePair", //
			"d:refPairInnerDistance", //
			"f:meanWordsPerConcept", //
			"f:samePOSpairRatio", //
			"f:subTreeBalance", //
			"d:assymetricRelationCount", //
	};

	private String[] constraintsDescription = { //
			"required degreeOfReferencePair", //
			"required posRatio", //
			"required subTreeBal", //
			"required numRelations", //
			"required vitalRelationsMean", //
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
