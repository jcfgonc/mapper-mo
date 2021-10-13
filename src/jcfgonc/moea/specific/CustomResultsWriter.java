package jcfgonc.moea.specific;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.Problem;
import org.moeaframework.core.Solution;

import graph.DirectedMultiGraph;
import jcfgonc.mapper.structures.MappingStructure;
import structures.OrderedPair;

public class CustomResultsWriter implements ResultsWriter {

	/**
	 * opens the given file in append mode and writes the results header. It is hard-coded for the blender
	 * 
	 * @param filename
	 * @param problem
	 * @throws IOException
	 */
	public void writeFileHeader(String filename, Problem problem) {
		try {
			FileWriter fw = new FileWriter(filename, true);
			BufferedWriter bw = new BufferedWriter(fw);

			CustomProblem cp = (CustomProblem) problem;
			int numberOfObjectives = problem.getNumberOfObjectives();
			for (int i = 0; i < numberOfObjectives; i++) {
				String objectiveDescription = cp.getObjectiveDescription(i);
				fw.write(String.format("%s\t", objectiveDescription));
			}
			// remaining headers
			bw.write("s:Reference Pair\t");

			// graph column
			bw.write(cp.getVariableDescription(0));

			bw.newLine();
			bw.close();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// this is hard-coded for the blender
	public void appendResultsToFile(String filename, NondominatedPopulation results, Problem problem) {
		try {

			// nothing to save
			if (results == null || results.isEmpty())
				return;

			FileWriter fw = new FileWriter(filename, true);
			BufferedWriter bw = new BufferedWriter(fw);
			CustomProblem cp = (CustomProblem) problem;

			int numberOfObjectives = cp.getNumberOfObjectives();

			for (Solution solution : results) {
				// write objectives
				for (int i = 0; i < numberOfObjectives; i++) {
					bw.write(Double.toString(solution.getObjective(i)));
					bw.write('\t');
				}

				// graph data
				// hard-coded for the blender
				CustomChromosome cc = (CustomChromosome) solution.getVariable(0); // unless the solution domain X has more than one dimension
				MappingStructure<String, String> mappingStructure = cc.getGene();
				DirectedMultiGraph<OrderedPair<String>, String> pairGraph = mappingStructure.getPairGraph();

				// remaining headers
				bw.write(String.format("%s", mappingStructure.getReferencePair()));
				bw.write('\t');
				// graph column
				bw.write(String.format("%s", pairGraph));
				bw.newLine();
			}
			bw.close();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void close() {
		// not needed
	}

}
