/**
 * 
 * @author Amir
 */
package org.harmony.server.components;

import java.io.IOException;

import net.wimpi.telnetd.io.BasicTerminalIO;
import net.wimpi.telnetd.io.toolkit.InertComponent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.harmony.server.commands.ControlProccessBars;
import org.harmony.server.commands.IControlable;
import org.harmony.server.startup.Server;

/**
 * @author Amir
 * 
 */
public class ProgressBar extends InertComponent implements IControlable {
	private enum ProgressBarStatus{
		ACTIVE,
		INACTIVE,
		STOPED,
		ENDED;
	}
	private static final Log LOGGER = LogFactory.getLog(ProgressBar.class);
	private ProgressBarStatus status;   //set and test me only throw ask or test
	private int timeout;
	private final String progress;
	private final long interval;
	private final String message;

	/**
	 * construct a progress bar with automatic timeout.
	 * @param io   Instance of a class implementing the BasicTerminalIO.
	 * @param name String that represents the components name.
	 */
	public ProgressBar(final BasicTerminalIO io, final String name, final int timeout) {
		this(io, name, timeout, Server.getMessage("shell.progressBar.pleaseWait"));
	}

	/**
	 * construct a progress bar with automatic timeout.
	 * @param io   Instance of a class implementing the BasicTerminalIO.
	 * @param name String that represents the components name.
	 */
	public ProgressBar(final BasicTerminalIO io, final String name, final int timeout, final String message) {
		super(io, name);
		this.message = message;
		this.timeout = timeout;
		progress = Server.getStringVar("ProgressBar.progress");
		interval = Server.getIntVar("ProgressBar.interval");
		setStatus(ProgressBarStatus.INACTIVE);
	}

	/**
	 * Method that draws the loading on the screen.
	 */
	@Override
	public void draw() throws IOException {
		if (m_Position == null) {
			runProgressBar();
		} else {
			m_IO.storeCursor();
			m_IO.setCursor(m_Position.getRow(), m_Position.getColumn());
			runProgressBar();
			m_IO.restoreCursor();
			m_IO.flush();
		}
	}// draw

	private void runProgressBar() throws IOException{
		if (message != null) {
			m_IO.write(message);
		}
		//should improve thread usage - instead of many sleeping threads use one timer (and callbacks) to advance all progress bars.
		setStatus(ProgressBarStatus.ACTIVE);
		ControlProccessBars.GetControl().RegisterControlable(this);
		if (timeout <= 0){
			timeout = Integer.MAX_VALUE;
		}
		while (timeout > 0 ){
			--timeout;
			m_IO.write(progress);
			if (askStatus(ProgressBarStatus.STOPED)){
				break;
			}
			try {
				Thread.sleep(interval);
			} catch (final InterruptedException e) {
				LOGGER.debug("interrupted", e);
			}
		}
		ControlProccessBars.GetControl().RemoveControlable(this);
		setStatus(ProgressBarStatus.ENDED);

		// finished
	}

	@Override
	public boolean IsAcive(){
		return askStatus(ProgressBarStatus.ACTIVE);
	}

	@Override
	public void Release() {
		setStatus(ProgressBarStatus.STOPED);
	}

	@Override
	public String Display(){
		final StringBuilder buffer = new StringBuilder();
		buffer.append("Name: ").append(this.getName());
		buffer.append(" Timeout: ").append(timeout).append(" sec'");
		return buffer.toString();
	}
	private synchronized boolean askStatus(final ProgressBarStatus toAsk){
		return status == toAsk;
	}
	private synchronized void setStatus(final ProgressBarStatus toSet){
		status = toSet;
	}

}
