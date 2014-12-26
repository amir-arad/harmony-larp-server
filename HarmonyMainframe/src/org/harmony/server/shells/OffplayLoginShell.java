/**
 * 
 * @author Amir
 */
package org.harmony.server.shells;

import java.io.IOException;

import net.wimpi.telnetd.io.BasicTerminalIO;
import net.wimpi.telnetd.io.toolkit.Editfield;
import net.wimpi.telnetd.io.toolkit.Label;
import net.wimpi.telnetd.shell.Shell;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.harmony.server.shells.commandShell.CommandShell;
import org.harmony.server.startup.Server;

/**
 * @author Amir
 *
 */
public class OffplayLoginShell extends AbsShell {
	private static final Log LOGGER = LogFactory.getLog(OffplayLoginShell.class);


	private void printWelcomeMessage() throws IOException {
		getIo().eraseScreen();  //erase the screen
		getIo().homeCursor();  //place the cursor in home position
		getIo().write(Server.getMessage("shell.login.welcome"));
	}

	private String getPassword() throws IOException {
		final Label label = new Label(getIo(), "offPlayPasswordLabel", "offplay password: ");
		final Editfield field = new Editfield(getIo(), "offPlayPasswordField", 60);
		label.setLocation(0, 3);
		field.setLocation(label.getDimension().getWidth(), 3);
		label.draw(); //draw the label on the screen
		field.run(); //read
		return field.getValue();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected String run() throws IOException {
		printWelcomeMessage();
		final String password = getPassword();
		if (password.equals(Server.getStringVar("shell.offplayLogin.password"))){
			LOGGER.warn("offplay logged in");
			getConnectionData().getEnvironment().put(CommandShell.ROLE_KEY,Role.ADMIN);
			return chooseNextShell();
		}
		LOGGER.error("----->>  bad offplay login! (tried '"+password+ "')  <<-----");
		return null;
	}

	private String chooseNextShell() throws IOException{
		getIo().write(BasicTerminalIO.CRLF);
		getIo().write("offplay log in. please choose shell. 1:main console, 9:dummy.");
		while (true){
			final Editfield menu = new Editfield(getIo(), "menuField", 50);
			menu.run(); //read menu choice
			switch (Integer.valueOf(menu.getValue())){
			case 1: return "command";
			case 9: return "dummy";
			}
		}
	}


	public static Shell createShell() {
		return new OffplayLoginShell();
	}
}
