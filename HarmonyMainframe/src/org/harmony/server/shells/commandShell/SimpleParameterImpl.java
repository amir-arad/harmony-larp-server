/**
 * 
 * @author Amir
 */
package org.harmony.server.shells.commandShell;


public class SimpleParameterImpl implements Parameter{
	private final String name;
	private final String description;
	private final boolean isMandatory;

	public SimpleParameterImpl(final String name, final String description, final boolean isMandatory) {
		this.name = name;
		this.description = description;
		this.isMandatory = isMandatory;
	}
	@Override public String getName() {return name;}
	@Override public String getDescription() {return description;}
	@Override public boolean isMandatory() {return isMandatory;}
	@Override public Object getValue(final String str) throws CommandError {return str;}
}