/**
 * 
 * @author Amir
 */
package org.harmony.server.shells.commandShell;

import java.io.IOException;
import java.util.regex.Pattern;

import net.wimpi.telnetd.io.BasicTerminalIO;
import net.wimpi.telnetd.io.toolkit.Editfield;
import net.wimpi.telnetd.io.toolkit.Label;
import net.wimpi.telnetd.shell.Shell;

import org.harmony.server.commands.Repair;
import org.harmony.server.components.ProgressBar;
import org.harmony.server.shells.AbsShell;
import org.harmony.server.shells.Role;
import org.harmony.server.startup.Server;

/**
 * @author Amir
 *
 */
public class CommandShell extends AbsShell{


	public static final String ROLE_KEY = "user role";
	private static final Pattern splitPattern = Pattern.compile("\\s+");

	private void printWelcomeMessage() throws IOException {
		getIo().eraseScreen();  //erase the screen
		getIo().homeCursor();  //place the cursor in home position
		getIo().write(Server.getMessage("shell.main.welcome"));
		if (Server.isCorrupted()) {
			getIo().write(BasicTerminalIO.CRLF).write(Server.getMessage("shell.main.systemCorruptedMessage"));
		}
	}

	private String getCommandLine() throws IOException{
		getIo().write(BasicTerminalIO.CRLF);
		String result = "";
		while (result.isEmpty()){
			int inputWidth = getConnectionData().getTerminalColumns();
			final Label prompt = new Label(getIo(), "prompt", Server.getMessage(
					isAdmin() ? "shell.main.offplayPrompt" : "shell.main.prompt"));
			prompt.draw();
			inputWidth -= prompt.getDimension().getWidth();
			final Editfield commandField = new Editfield(getIo(), "commandField", inputWidth);
			commandField.run();
			result = commandField.getValue().trim();
			getIo().write(BasicTerminalIO.CRLF);
		}
		return result.toLowerCase();
	}

	public boolean isAdmin(){
		return (Role)getConnectionData().getEnvironment().get(ROLE_KEY) == Role.ADMIN ;
	}
	private Command getCommand(final String commandName) {
		final Command result = Server.getCommand(commandName);
		if (result != null){
			if(isAdmin())
				return result;
			if (result.isAdminOnly())
				return null;
			return result;
		}
		return null;
	}

	@Override
	protected String run() throws IOException {
		printWelcomeMessage();
		while (true){
			final String[] commandLine = splitPattern.split(getCommandLine());
			if ("exit".equalsIgnoreCase(commandLine[0])) //exit shell
				return null;
			final Command result = getCommand(commandLine[0]);
			if (result == null) {
				getIo().write("\"").write(commandLine[0]).write("\" ").write(Server.getMessage("shell.main.badCommand"));
				getIo().write(BasicTerminalIO.CRLF);
			} else {
				if (Server.isCorrupted()
						&& !(result instanceof Repair)
						&& !isAdmin() ) {
					getIo().write(Server.getMessage("shell.main.systemCorruptedMessage")).write(BasicTerminalIO.CRLF);
					new ProgressBar(
							getIo(),
							"command while corrupted delay",
							Server.getIntVar("shell.commandWhileCorrupted.delay")
					).draw();
					getIo().write(Server.getMessage("shell.main.failedCommand")).write(BasicTerminalIO.CRLF);
				} else {
					result.run(this, commandLine);
				}
			}
		}
	}

	public static Shell createShell() {
		return new CommandShell();
	}
}
