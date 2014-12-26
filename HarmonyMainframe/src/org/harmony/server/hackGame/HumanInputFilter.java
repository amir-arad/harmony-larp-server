/**
 * 
 * @author Amir
 */
package org.harmony.server.hackGame;

import java.io.IOException;

import net.wimpi.telnetd.io.toolkit.InputFilter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.harmony.server.hackGame.Game.Player;
import org.harmony.server.startup.Server;

/**
 * 
 * allows only the correct characters to be forwarded to the edit field.
 * beeps and sleeps if the wrong input is detected
 * @author Amir
 *
 */
public class HumanInputFilter implements InputFilter{

	private static final Log LOGGER = LogFactory.getLog(HumanInputFilter.class);

	private final Game game;
	private int correctLettersCount;

	public HumanInputFilter(final Game game) {
		this.game = game;
		correctLettersCount = 0;
	}

	@Override
	public int filterInput(final int key) throws IOException {
		final int expected = Character.toLowerCase(game.getStringToWrite().charAt(correctLettersCount));
		if (key == expected){
			//human was correct
			correctLettersCount++;
			if (correctLettersCount == game.getStringToWrite().length()){
				game.endGame(Player.HUMAN);
			}
			return key;
		}
		game.getIo().bell();
		try {
			Thread.sleep(Server.getIntVar("shell.hack.wrongAnswerDelayMilliseconds"));
		} catch (final InterruptedException e) {
			LOGGER.error("thread interrupted while slowing down user", e);
		}
		return InputFilter.INPUT_HANDLED;
	}

}
