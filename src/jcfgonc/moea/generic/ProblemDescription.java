package jcfgonc.moea.generic;

/**
 * Used to add descriptions to the problem, i.e., describe the objectives, constraints, variables and the problem itself
 * 
 * @author jcfgonc@gmail.com
 *
 */
public interface ProblemDescription {

	public String getProblemDescription();

	public String getVariableDescription(int varid);

	public String getObjectiveDescription(int varid);

	public String getConstraintsDescription(int varid);
}
