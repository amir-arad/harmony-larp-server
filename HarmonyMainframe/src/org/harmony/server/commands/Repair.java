/**
 * 
 * @author Amir
 */
package org.harmony.server.commands;

import java.io.IOException;
import java.util.Map;

import org.harmony.server.components.ProgressBar;
import org.harmony.server.shells.commandShell.Command;
import org.harmony.server.shells.commandShell.CommandError;
import org.harmony.server.shells.commandShell.CommandShell;
import org.harmony.server.startup.Server;

/**
 * @author Amir
 */
public class Repair extends Command{

	public Repair() {
		super("repair", "Repair utility to maintain and repair system.", false);
	}

	@Override
	public void executeCommand(final CommandShell shell, final Map<String, Object> args) throws IOException, CommandError {
		writeln(shell, "Repairing system.");
		process(shell, "Searching for nodes...", "Searching for done. found 83.6% of system nodes.", 10);
		process(shell, "Initializing corruption resolver", "Initializing corruption resolver FAILED!\nChecksum persistency missing.", 4);
		writeln(shell, "Backup Repair Operation (TM) console.");
		writeln(shell, "--------------------------------------");
		process(shell, "Loading B.R.O(TM) agents...", "B.R.O(TM) agents loaded.", 4);
		process(shell, "Activating B.R.O(TM) agents", "B.R.O(TM) agents active.", 4);
		writeln(shell, "waiting for B.R.O(TM) agents to report on system status...");
		if (Server.isCorrupted()){
			new ProgressBar(
					shell.getIo(),
					"repair long delay",
					Server.getIntVar("shell.repair.delay")
					,null).draw();
			writeln(shell, "B.R.O agents minimized corruption to 8.35%");
			Server.setNotCorrupted();
			writeln(shell, "System partially repaired and is operational.");
		} else {
			new ProgressBar(
					shell.getIo(),
					"already repaired short delay (5)",
					5 ,null
			).draw();
			writeln(shell, "system has less than 12% repairable corruption. B.R.O(TM) cannot improve system state.");
		}
		writeln(shell, "Backup Repair Operation (TM) completed.");
	}
}
