/**
 * 
 * @author Amir
 */
package org.harmony.server.shells.commandShell;

@SuppressWarnings("serial")
public class CommandError extends Exception{
	public CommandError(final String arg0, final Throwable arg1) {super(arg0, arg1);}
	public CommandError(final String arg0) {super(arg0);}
}