package org.harmony.server.commands;

import java.io.IOException;
import java.util.Map;

import net.wimpi.telnetd.io.BasicTerminalIO;

import org.harmony.server.gameData.GameTime;
import org.harmony.server.shells.commandShell.Command;
import org.harmony.server.shells.commandShell.CommandError;
import org.harmony.server.shells.commandShell.CommandShell;

/**
 * @author Amir
 *
 */
public class Time extends Command{

	public Time() {
		super("time", "Utility to display system time.", false);
	}

	@Override
	protected void executeCommand(final CommandShell shell, final Map<String, Object> args) throws IOException, CommandError {
		shell.getIo().write(GameTime.getInstance().getTimeStr());
		shell.getIo().write(BasicTerminalIO.CRLF);
	}
}
