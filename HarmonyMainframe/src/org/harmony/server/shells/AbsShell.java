/**
 * 
 * @author Amir
 */
package org.harmony.server.shells;

import java.io.IOException;

import net.wimpi.telnetd.io.BasicTerminalIO;
import net.wimpi.telnetd.net.Connection;
import net.wimpi.telnetd.net.ConnectionData;
import net.wimpi.telnetd.net.ConnectionEvent;
import net.wimpi.telnetd.shell.Shell;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * abstract shell, leaving only the main logic to implement by subclasses.
 * @author Amir
 *
 */
public abstract class AbsShell implements Shell{
	private static final Log LOGGER = LogFactory.getLog(AbsShell.class);

	private Connection connection;
	private ConnectionData connectionData;
	private BasicTerminalIO io;

	/**
	 * the main shell logic to execute
	 */
	abstract protected String run() throws IOException;

	private void switchToShell(final String shellName) throws IOException{
		if(getConnection().setNextShell(shellName)) {
			getConnection().removeConnectionListener(this);
			LOGGER.info("Switching to " + shellName);
		} else {
			LOGGER.error("Could not switch to "+shellName);
		}
		getIo().flush();  //flush the output to ensure it is sent
	}

	//this implements the ConnectionListener!
	@Override
	public void connectionTimedOut(final ConnectionEvent ce) {
		try {
			getIo().write("CONNECTION TIMEDOUT");
			getIo().flush();
			//close connection
			getConnection().close();
		} catch (final Exception ex) {
			LOGGER.error("connectionTimedOut()", ex);
		}
	}//connectionTimedOut

	@Override
	public void connectionIdle(final ConnectionEvent ce) {
		try {
			getIo().write("CONNECTION_IDLE");
			getIo().flush();
		} catch (final IOException e) {
			LOGGER.error("connectionIdle()", e);
		}

	}//connectionIdle

	@Override
	public void connectionLogoutRequest(final ConnectionEvent ce) {
		try {
			getIo().write("CONNECTION LOGOUT REQUEST");
			getIo().flush();
		} catch (final Exception ex) {
			LOGGER.error("connectionLogoutRequest()", ex);
		}
	}//connectionLogout

	@Override
	public void connectionSentBreak(final ConnectionEvent ce) {
		try {
			getIo().write("CONNECTION BREAK");
			getIo().flush();
		} catch (final Exception ex) {
			LOGGER.error("connectionSentBreak()", ex);
		}
	}//connectionSentBreak

	@Override
	public void run(final Connection con) {
		setConnection(con);
		setIo(con.getTerminalIO());
		setConnectionData(con.getConnectionData());
		con.addConnectionListener(this);
		try {
			final String nextShell = run();
			if (nextShell != null){
				switchToShell(nextShell);
			} else {
				LOGGER.info("disconnecting.");
			}
		} catch (final IOException e) {
			LOGGER.error("IO exception in main logic", e);
		}
	}

	/**
	 * @return the connection
	 */
	public Connection getConnection() {
		return connection;
	}

	/**
	 * @param connection the connection to set
	 */
	public void setConnection(final Connection connection) {
		this.connection = connection;
	}


	/**
	 * @param connectionData the connectionData to set
	 */
	public void setConnectionData(final ConnectionData connectionData) {
		this.connectionData = connectionData;
	}

	/**
	 * @return the connectionData
	 */
	public ConnectionData getConnectionData() {
		return connectionData;
	}

	/**
	 * @param io the io to set
	 */
	public void setIo(final BasicTerminalIO io) {
		this.io = io;
	}

	/**
	 * @return the io
	 */
	public BasicTerminalIO getIo() {
		return io;
	}

}
