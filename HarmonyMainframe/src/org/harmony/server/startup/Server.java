/**
 * 
 * @author Amir
 */
package org.harmony.server.startup;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;

import net.wimpi.telnetd.BootException;
import net.wimpi.telnetd.TelnetD;
import net.wimpi.telnetd.util.PropertiesLoader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.harmony.server.bl.User;
import org.harmony.server.bl.User.Difficulcy;
import org.harmony.server.commands.ControlProccessBars;
import org.harmony.server.commands.Help;
import org.harmony.server.commands.Repair;
import org.harmony.server.commands.Time;
import org.harmony.server.gameData.GameTime;
import org.harmony.server.hackGame.Hack;
import org.harmony.server.shells.commandShell.Command;

/**
 * main server class for startup and shutdown, and configurations access point
 * 
 * @author Amir
 * 
 */
public final class Server {

	private static final Log LOGGER = LogFactory.getLog(Server.class);

	private static TelnetD server = null;
	private static Properties messages = null;
	private static Properties variables = null;
	private static Map<String, Command> commands = null;
	private static Map<String, User> users = null;
	private static final Timer timer = new Timer("timer", true);

	private Server() {}

	/**
	 * main startup method.
	 */
	public static void main(final String[] args) {
		try {
			// 1. load properties and create server
			setServerEnvironment();
			final Properties telnetProperties = loadProperties("telnetd.properties");
			server = TelnetD.createTelnetD(telnetProperties);
			// 2.start serving/accepting connections
			server.start();
			LOGGER.info("server is up.");
		} catch (final Exception e) {
			LOGGER.error("error starting server up", e);
			System.exit(1);
		}
	}// main

	public static void setServerEnvironment() throws BootException {
		messages = loadProperties("messages.properties");
		variables = loadProperties("variables.properties");
		commands = loadCommands();
		users = loadUsers();
		GameTime.getInstance().setTime(
				variables.getProperty("system.startGameTime"),
				variables.getProperty("system.dateFormatPattern"));
	}

	public static Map<String, Command> loadCommands() {
		final Map<String, Command> result = new HashMap<String, Command>(4);
		Command c = new Time();
		result.put(c.getName(), c);
		c = new Hack();
		result.put(c.getName(), c);
		c = new Repair();
		result.put(c.getName(), c);
		c = new ControlProccessBars();
		result.put(c.getName(), c);
		c = new Help();
		result.put(c.getName(), c);
		return result;
	}

	public static Collection<Command> getCommands(){
		return commands.values();
	}

	public static Map<String, User> loadUsers() {
		final Map<String, User> result = new HashMap<String, User>();
		try {
			// Get the object of DataInputStream
			final DataInputStream in = new DataInputStream(new FileInputStream(ResourceLoader.getResource("users.csv").getFile()));
			LOGGER.info("Loading users");
			final BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			// Read File Line By Line
			while ((strLine = br.readLine()) != null) {
				final String[] lineArr = strLine.split(",");
				assert lineArr.length == 3;
				final User u = new User(
						lineArr[0].trim(),
						lineArr[1].trim(),
						Difficulcy.valueOf(lineArr[2].trim()));
				result.put(u.getUserName(), u);
			}
			// Close the input stream
			in.close();
		} catch (final Exception e) {// Catch exception if any
			LOGGER.error("error loading commands", e);
		}
		LOGGER.info("Done loading commands");
		return result;
	}

	private static Properties loadProperties(final String name) throws BootException {
		// try to load properties from classpath
		final URL url = ResourceLoader.getResource(name);
		LOGGER.info("Loading properties, url=" + url);
		if (url == null)
			throw new BootException("Failed to load configuration file " + name);
		try {
			return PropertiesLoader.loadProperties(url);
		} catch (final IOException e) {
			throw new BootException(
					"Failed to load configuration file " + name, e);
		}
	}

	public static void shutDown() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				server.stop();
			}
		}).start();
	}

	public static String getStringVar(final String key) {
		return getStringValue(key, variables);
	}

	public static char getCharVar(final String key) {
		return getStringValue(key, variables).charAt(0);
	}

	public static Float getFloatVar(final String key) {
		return Float.valueOf(getStringValue(key, variables));
	}

	public static Integer getIntVar(final String key) {
		try {
			return Integer.valueOf(getStringValue(key, variables));
		} catch(final RuntimeException e){
			LOGGER.error("error getting "+key);
			throw e;
		}
	}

	public static Boolean getBooleanVar(final String key) {
		return Boolean.valueOf(getStringValue(key, variables));
	}

	public static Object setVariable(final String key, final Serializable value) {
		return variables.setProperty(key, value.toString());
	}

	public static String getMessage(final String key) {
		return getStringValue(key, messages);
	}

	public static User getUser(final String userName) {
		return users.get(userName);
	}

	public static Command getCommand(final String commandName) {
		return commands.get(commandName);
	}

	private static String getStringValue(final String key,
			final Properties props) {
		if (key == null) {
			LOGGER.debug("returning null for property " + key
					+ ", attaching stacktrace for lookup", new Exception());
			return null;
		}
		final String result = props.getProperty(key);
		return result;
	}

	/**
	 * @return the corrupted
	 */
	public static boolean isCorrupted() {
		final File repairFile = new File(getStringVar("system.repairedFilePath"));
		return !repairFile.exists();
	}

	/**
	 * @param corrupted the corrupted to set
	 */
	public synchronized static void setNotCorrupted() {
		final File repairFile = new File(getStringVar("system.repairedFilePath"));
		if (repairFile.exists())
			return;
		try {
			repairFile.createNewFile();
		} catch (final Exception e) {
			LOGGER.error("cannot create repaired file "+repairFile, e);
		}
	}

	/**
	 * @return the timer
	 */
	public static Timer getTimer() {
		return timer;
	}
}
