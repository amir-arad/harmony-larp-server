package org.harmony.server.shells.commandShell;


/**
 * a command parameter.
 * 
 * @author Amir
 */
public interface Parameter {
	/**
	 * the name of the parameter, to be used in the command implementation and displayed to user
	 * @return
	 */
	String getName();
	/**
	 * description of the parameter to be shown in help
	 * @return
	 */
	String getDescription();
	/**
	 * is this parameter mandatory
	 * @return
	 */
	boolean isMandatory();
	/**
	 * translate a string to the parameter value
	 */
	Object getValue(String str) throws CommandError;
}