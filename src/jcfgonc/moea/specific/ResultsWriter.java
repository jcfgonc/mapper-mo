package jcfgonc.moea.specific;

import java.io.IOException;

import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.Problem;

public interface ResultsWriter {

	/**
	 * Opens the given file in append mode and writes the results header.
	 * 
	 * @param filename
	 * @param problem
	 * @throws IOException
	 */
	public void writeFileHeader(String filename, Problem problem);

	/**
	 * Opens the given file in append mode and appends the given results.
	 * 
	 * @param filename
	 * @param results
	 * @param problem
	 * @throws IOException
	 */
	public void appendResultsToFile(String filename, NondominatedPopulation results, Problem problem);

	public void close();
}
