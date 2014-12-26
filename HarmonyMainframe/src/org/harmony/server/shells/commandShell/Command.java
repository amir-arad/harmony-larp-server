/**
 * 
 * @author Amir
 */
package org.harmony.server.shells.commandShell;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import net.wimpi.telnetd.io.BasicTerminalIO;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.harmony.server.components.ProgressBar;
import org.harmony.server.shells.AbsShell;
import org.harmony.server.startup.Server;

/**
 * 
 * main shell commands are implemented as subclasses of this abstract class.
 * 
 * the command contract requires that all concrete commands supply a no-arguments constructor
 * and be registered in the commands file.
 * 
 * @author Amir
 *
 */
public abstract class Command{
	private static final Log LOGGER = LogFactory.getLog(Command.class);

	private final String name;
	private final String description;
	private final boolean adminOnly;
	private final List<Parameter> params;


	protected Command(final String name, final String description, final boolean adminOnly) {
		this.name = name;
		this.description = description;
		this.adminOnly = adminOnly;
		params = new Vector<Parameter>();
	}

	public void run(final CommandShell shell, final String[] commandLine) throws IOException {
		try {
			final Map<String, Object> args = getArgs(commandLine);
			executeCommand(shell, args);
		} catch (final CommandError e) {
			shell.getIo().write(BasicTerminalIO.CRLF).write(e.getMessage()).write(BasicTerminalIO.CRLF);
			if (e.getCause() != null){
				LOGGER.error("error running "+name, e.getCause());
			}
		}
	}

	private Map<String, Object> getArgs(final String[] commandLine) throws CommandError{
		if (commandLine.length <= 1)
			return Collections.emptyMap();
		if (commandLine.length > params.size() + 1)
			throw new CommandError(Server.getMessage("shell.main.tooManyParams"));
		final Map<String, Object> result = new HashMap<String, Object>(commandLine.length);
		for (int i=1;i <= params.size();i++){
			final Parameter p = params.get(i-1);
			if (commandLine.length <= i+1){
				result.put(p.getName(), p.getValue(commandLine[i]));
			} else if (p.isMandatory())
				throw new CommandError(Server.getMessage("shell.main.missingMandatoryParam")+ " " +p.getName());
		}
		return Collections.unmodifiableMap(result);
	}

	abstract protected void executeCommand(final CommandShell shell, Map<String, Object> args) throws IOException, CommandError;

	public String getName() {
		return name;
	}

	public boolean isAdminOnly() {
		return adminOnly;
	}

	/**
	 * use in subclass constructor to populate the parameters list
	 * @param p
	 */
	protected void addParam(final Parameter p){
		params.add(p);
	}

	protected void writeln(final AbsShell shell, final String toWrite) throws IOException{
		shell.getIo().write(toWrite).write(BasicTerminalIO.CRLF);
	}

	protected void write(final AbsShell shell, final String toWrite) throws IOException{
		shell.getIo().write(toWrite);
	}

	protected void process(final AbsShell shell, final String before, final String after, final int delay) throws IOException{
		write(shell, before);
		new ProgressBar( shell.getIo(), getName() +" short delay ("+delay+")", delay, null).draw();
		writeln(shell, after);
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}


	/**
	 * @return the params
	 */
	public List<Parameter> getParams() {
		return params;
	}

}
