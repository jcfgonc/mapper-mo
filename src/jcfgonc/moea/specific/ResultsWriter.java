package jcfgonc.moea.specific;

import java.io.IOException;
import java.util.List;

import org.moeaframework.core.Problem;
import org.moeaframework.core.Solution;

public interface ResultsWriter {

	/**
	 * Creates a new file with the given filename and writes the results header. Should be the first access to the given filename. File should be closed
	 * afterwards.
	 * 
	 * @param filename
	 * @param problem
	 * @throws IOException
	 */
	public void writeFileHeader(Problem problem, String filename);

	/**
	 * Opens the given file in append mode and appends the given results to the existing contents. File should be closed afterwards.
	 * 
	 * @param filename
	 * @param lastNDS
	 * @param problem
	 * @throws IOException
	 */
	public void appendResultsToFile(List<Solution> lastNDS, Problem problem, String filename);
}
