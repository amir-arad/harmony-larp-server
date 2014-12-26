/**
 * 
 * @author Amir
 */
package org.harmony.server.hackGame;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.Random;

import net.wimpi.telnetd.io.BasicTerminalIO;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.harmony.server.bl.User;
import org.harmony.server.components.ProgressBar;
import org.harmony.server.hackGame.Game.Player;
import org.harmony.server.shells.commandShell.Command;
import org.harmony.server.shells.commandShell.CommandError;
import org.harmony.server.shells.commandShell.CommandShell;
import org.harmony.server.shells.commandShell.SimpleParameterImpl;
import org.harmony.server.startup.Server;

/**
 * @author Amir
 *
 */
public class Hack extends Command {
	private static final Log LOGGER = LogFactory.getLog(Hack.class);

	private static final String USER_PARAM = "user name";

	private final DecimalFormat floatFormatter = new DecimalFormat("#####.##");
	private final Random randomizer  = new Random();

	/**
	 * @param name
	 * @param adminOnly
	 */
	public Hack() {
		super("hack", "Smart tools suit to get through security and retreive user passwords.", false);
		addParam(new SimpleParameterImpl(USER_PARAM, "The user name to hack", true){
			@Override public Object getValue(final String usernameToHack) throws CommandError {
				final User result = Server.getUser(usernameToHack);
				if (result == null)
					throw new CommandError(Server.getMessage("shell.hack.userDoesNotExist"));
				return result;
			}
		});
	}

	private String getRandomString(final Integer size){
		final String chars = Server.getStringVar("shell.hack.charactersPool");
		final StringBuilder sb = new StringBuilder();
		for (int i=0;i<size;i++){
			sb.append(chars.charAt(Math.abs(randomizer.nextInt()) % chars.length()));
		}
		return sb.toString();
	}

	@Override
	protected void executeCommand(final CommandShell shell, final Map<String, Object> args) throws IOException, CommandError {
		final User user = (User) args.get(USER_PARAM);
		final int requredAttempts = Server.getIntVar("shell.hack.game.attempts." + user.getDifficulcy());
		boolean success = true;
		for (int attempt= 0; success && attempt < requredAttempts; attempt++) {
			new ProgressBar(shell.getIo(),
					"hack level " +attempt, Server.getIntVar("shell.hack.game.levelUpDelay")+attempt,
					Server.getMessage("shell.hack.game.attempt."+attempt)).draw();
			if (!playGame(shell.getIo(), Server.getIntVar("shell.hack.game.length."+attempt))) {
				success = false;
			}
		}
		float correctionFactor = (float) 1.0;
		if (success){
			//make next game faster
			correctionFactor += Server.getFloatVar("shell.hack.speedCorrectionFactor");
			shell.getIo().write(Server.getMessage("shell.hack.game.passwordPrefix")).write(user.getPassword());
		}else {
			//make next game slower
			correctionFactor -= Server.getFloatVar("shell.hack.speedCorrectionFactor");
			shell.getIo().write(Server.getMessage("shell.hack.game.failed"));
		}

		LOGGER.debug("game speed changes from " + floatFormatter.format(getCurrentSpeed())
				+ " to "+ floatFormatter.format(getCurrentSpeed() * correctionFactor) +" characters per minute");
		setCurrentSpeed(getCurrentSpeed() * correctionFactor);
		shell.getIo().write(BasicTerminalIO.CRLF);
	}

	private boolean playGame(final BasicTerminalIO io, final int length) throws IOException{
		final String randomStr = getRandomString(length);
		final Game game = new Game(io, randomStr);
		game.play(Server.getTimer(), (long) (60 * 1000  / getCurrentSpeed()));
		final boolean result = game.getWinner() == Player.HUMAN;
		return result;
	}

	/**
	 * @param currentSpeed the currentSpeed to set
	 */
	private void setCurrentSpeed(final Float currentSpeed) {
		Server.setVariable("shell.hack.speed", currentSpeed);
	}

	/**
	 * @return the currentSpeed
	 */
	private float getCurrentSpeed() {
		return Server.getFloatVar("shell.hack.speed");
	}

}
