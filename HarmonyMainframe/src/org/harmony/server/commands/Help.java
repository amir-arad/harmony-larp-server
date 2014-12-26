/**
 * 
 * @author Amir
 */
package org.harmony.server.commands;

import java.io.IOException;
import java.util.Map;

import net.wimpi.telnetd.io.BasicTerminalIO;

import org.harmony.server.shells.commandShell.Command;
import org.harmony.server.shells.commandShell.CommandError;
import org.harmony.server.shells.commandShell.CommandShell;
import org.harmony.server.shells.commandShell.Parameter;
import org.harmony.server.shells.commandShell.SimpleParameterImpl;
import org.harmony.server.startup.Server;

/**
 * @author Amir
 *
 */
public class Help extends Command{
	private static final String COMMAND_NAME_PARAM = "command";

	private static final int SPACES = 16;

	public Help(){
		super("help", "Displays system commands", false);
		addParam(new SimpleParameterImpl(COMMAND_NAME_PARAM, "A specific command to display help for", true){
			@Override public Object getValue(final String commandName) throws CommandError {
				final Command result = Server.getCommand(commandName);
				if (result == null) {
					handleCommandNotFound(commandName);
				}
				return result;
			}
		});
	}

	private void handleCommandNotFound(final String commandName) throws CommandError{
		throw new CommandError(Server.getMessage("shell.help.commandDoesNotExist")+" "+commandName);
	}
	/* (non-Javadoc)
	 * @see org.harmony.server.shells.commandShell.Command#executeCommand(org.harmony.server.shells.AbsShell, java.util.Map)
	 */
	@Override
	protected void executeCommand(final CommandShell shell, final Map<String, Object> args) throws IOException, CommandError {
		final Command command = (Command) args.get(COMMAND_NAME_PARAM);
		if (command == null) {
			showAllCommands(shell);
		} else {
			if (command.isAdminOnly() && !shell.isAdmin()) {
				handleCommandNotFound(command.getName());
			}
			showCommand(shell.getIo(), command);
		}
	}

	private void showAllCommands(final CommandShell shell) throws IOException{
		final BasicTerminalIO io = shell.getIo();
		io.write("command name");
		io.moveRight(SPACES-"command name".length());
		io.write("command description");
		io.write(BasicTerminalIO.CRLF);
		for (final Command c: Server.getCommands()){
			if (!c.isAdminOnly() || shell.isAdmin()) {
				showCommandHeader(io, c);
			}
		}
	}

	private void showCommandHeader(final BasicTerminalIO io, final Command command) throws IOException{
		io.write(command.getName());
		io.moveRight(SPACES-command.getName().length());
		io.write(command.getDescription());
		if (command.isAdminOnly()) {
			io.write(" (offplay command)");
		}
		io.write(BasicTerminalIO.CRLF);
	}
	private void showCommand(final BasicTerminalIO io, final Command command) throws IOException{
		showCommandHeader(io, command);
		if (command.getParams().isEmpty())
			return;
		io.write(BasicTerminalIO.CRLF);
		io.write("parameter");
		io.moveRight(SPACES-"parameter".length());
		io.write("description");
		io.write(BasicTerminalIO.CRLF);

		for (final Parameter param : command.getParams()) {
			io.write(param.getName());
			io.moveRight(SPACES-param.getName().length());
			io.write(param.getDescription());
			if (param.isMandatory()) {
				io.write(" (mandatory)");
			}
			io.write(BasicTerminalIO.CRLF);
		}
	}
}
