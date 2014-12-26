/**
 * 
 * @author Amir
 */
package org.harmony.server.hackGame;

import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.harmony.server.hackGame.Game.Player;
import org.harmony.server.startup.Server;

/**
 * @author Amir
 * 
 */
class Rabbit extends TimerTask {
	private static final Log LOGGER = LogFactory.getLog(Rabbit.class);

	private int rabbitHeadPos;
	private final char rabbitHead;
	private final Game game;
	private final int rowNumber;

	Rabbit(final Game game, final int rowNumber) {
		this.game = game;
		this.rowNumber = rowNumber;
		rabbitHeadPos = 0;
		rabbitHead = Server.getCharVar("shell.hack.rabbitHead");
	}

	@Override
	public void run() {
		synchronized (game.getIo()) {
			//undisturbed by user's actgame.getIo()ns
			try {
				game.getIo().storeCursor();
				//advance the rabbit 1 letter
				game.getIo().setCursor(rowNumber, rabbitHeadPos);
				if (rabbitHeadPos != 0) {
					// change the rabbit head to a letter
					game.getIo().write(Character.toLowerCase(game.getStringToWrite().charAt(rabbitHeadPos - 1)));
				}
				if (rabbitHeadPos == game.getStringToWrite().length()){
					game.endGame(Player.RABBIT);
				}else {
					game.getIo().write(rabbitHead);
					rabbitHeadPos++;
				}
				//restore the cursor
				game.getIo().restoreCursor();
				game.getIo().flush();
			} catch ( final Exception e) {
				LOGGER.error("error progressing rabbit", e);
			}
		}
	}
}
