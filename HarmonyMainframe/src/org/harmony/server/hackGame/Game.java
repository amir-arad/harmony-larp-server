/**
 * 
 * @author Amir
 */
package org.harmony.server.hackGame;

import java.io.IOException;
import java.util.Timer;

import net.wimpi.telnetd.io.BasicTerminalIO;
import net.wimpi.telnetd.io.toolkit.Editfield;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.harmony.server.startup.Server;

/**
 * @author Amir
 * 
 */
class Game {
	private static final Log LOGGER = LogFactory.getLog(Game.class);
	enum Player{RABBIT, HUMAN}

	private final BasicTerminalIO io;
	private final String stringToWrite;
	private final Rabbit rabbit;
	private final Editfield userInput;
	private Player winner = null;


	Game(final BasicTerminalIO io, final String stringToWrite) {
		rabbit = new Rabbit(this, 2);
		userInput = new Editfield(io, "rabbitUserInput", stringToWrite.length());
		userInput.registerInputFilter(new HumanInputFilter(this));
		this.io = io;
		this.stringToWrite = stringToWrite;
	}

	void play(final Timer timer, final long speed) throws IOException{
		io.eraseScreen().homeCursor();
		io.write(Server.getMessage("shell.hack.game.instructions")).write(BasicTerminalIO.CRLF);
		io.setCursor(3, 0);
		io.write(stringToWrite);
		userInput.setLocation(0, 4);
		timer.scheduleAtFixedRate(rabbit, speed,  speed);
		userInput.run();
		io.setCursor(6, 0);
		LOGGER.info("game ended, "+winner+" has won.");
		LOGGER.debug("user input String was: " + userInput.getValue());
		LOGGER.debug("String to type was:    " + stringToWrite.toLowerCase());
	}

	void endGame(final Player winner){
		if (this.winner == null) {
			this.winner = winner;
		} else { //break tie in favor of the human
			this.winner = Player.HUMAN;
		}
		rabbit.cancel();
		userInput.setInterrupt(true);
	}

	BasicTerminalIO getIo() {
		return io;
	}

	String getStringToWrite() {
		return stringToWrite;
	}

	public Player getWinner() {
		return winner;
	}
}
