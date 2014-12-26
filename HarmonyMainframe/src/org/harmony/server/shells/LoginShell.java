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
import org.harmony.server.components.ProgressBar;
import org.harmony.server.shells.commandShell.CommandShell;
import org.harmony.server.startup.Server;

/**
 * @author Amir
 *
 */
public class LoginShell extends AbsShell{
	private static final Log LOGGER = LogFactory.getLog(LoginShell.class);

	public static Shell createShell() {
		return new LoginShell();
	}

	private void printWelcomeMessage() throws IOException {
		getIo().eraseScreen();  //erase the screen
		getIo().homeCursor();  //place the cursor in home position
		getIo().write(Server.getMessage("shell.login.welcome"));
	}

	private String getUserName() throws IOException {
		final Label userNameLabel = new Label(getIo(), "userNameLabel", "User Name: ");
		final Editfield userNameField = new Editfield(getIo(), "userNameField", 50);
		userNameLabel.setLocation(0, 3);
		userNameField.setLocation(userNameLabel.getDimension().getWidth(), 3);
		userNameLabel.draw(); //draw the label on the screen
		userNameField.run(); //read user name
		return userNameField.getValue();
	}

	private boolean getPassword(final String correctPassword) throws IOException {
		int attemptsRemaining = Server.getIntVar("shell.login.maxAttempts");
		final Label passwordLabel = new Label(getIo(), "passwordLabel", "Password: ");
		final Editfield passwordField = new Editfield(getIo(), "passwordField", 50);
		passwordLabel.setLocation(0, 5);
		passwordField.setLocation(passwordLabel.getDimension().getWidth(), 5);
		passwordField.setPasswordField(true);
		boolean correct = false;
		while (!correct && attemptsRemaining > 0){ //password incorrect and attempts not exhausted
			passwordField.clear();
			passwordLabel.draw(); //draw the label on the screen (+flush)
			passwordField.run(); //read password
			attemptsRemaining--;
			correct = passwordField.getValue().equalsIgnoreCase(correctPassword);
		}
		return correct;
	}

	@SuppressWarnings("unchecked")
	private void loginSuccessfull() throws IOException{
		getConnectionData().getEnvironment().put(CommandShell.ROLE_KEY,Role.USER);
		final Label accepted = new Label(getIo(), "acceptedLabel", Server.getMessage("shell.login.accepted"));
		accepted.setLocation(0, 7);
		final ProgressBar delay = new ProgressBar(getIo(), "logInDelay", Server.getIntVar("shell.login.delay"));
		delay.setLocation(0, 8);
		accepted.draw();
		delay.draw();
		getIo().eraseScreen();  //erase the screen
		getIo().homeCursor();  //place the cursor in home position
	}

	@Override
	protected String run() throws IOException {
		printWelcomeMessage();
		getUserName();
		final boolean correct = getPassword(Server.getStringVar("shell.login.password"));
		if (correct){
			LOGGER.info("logged in");
			loginSuccessfull();
			return "command";
			//progress to another shell
		} else {
			LOGGER.info("rejected");
			getIo().write(BasicTerminalIO.CRLF).write(Server.getMessage("shell.login.rejected"));
			return null;
		}
	}


}
