/*
 * Copyright 1999-2005 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.harmony.server.startup;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Load resources (or images) from various sources.
 * 
 * @author Ceki G&uuml;lc&uuml;
 * 
 *         (Very small adaptations by Michael Meyling and Amir Arad.)
 */

public class ResourceLoader {

	static final String TSTR = "Caught Exception while in Loader.getResource. This may be innocuous.";

	/** Trace logger. */
	private static Log trace = LogFactory.getLog(ResourceLoader.class);

	// We conservatively assume that we are running under Java 1.x
	static private boolean java1 = true;

	static private boolean ignoreTCL = false;

	static {
		final String prop = System.getProperty("java.version", null);

		if (prop != null) {
			final int i = prop.indexOf('.');
			if (i != -1) {
				if (prop.charAt(i + 1) != '1') {
					java1 = false;
				}
			}
		}
		final String ignoreTCLProp = System
		.getProperty("log4j.ignoreTCL", null);
		if (ignoreTCLProp != null) {
			ignoreTCL = toBoolean(ignoreTCLProp, true);
		}
	}


	/**
	 * This method will search for <code>resource</code> in different places.
	 * The search order is as follows:
	 * 
	 * <ol>
	 * 
	 * <p>
	 * <li>Search for <code>resource</code> using the thread context class
	 * loader under Java2. If that fails, search for <code>resource</code> using
	 * the class loader that loaded this class (<code>Loader</code>). Under JDK
	 * 1.1, only the the class loader that loaded this class (
	 * <code>Loader</code>) is used.
	 * 
	 * <p>
	 * <li>Try one last time with
	 * <code>ClassLoader.getSystemResource(resource)</code>, that is is using
	 * the system class loader in JDK 1.2 and virtual machine's built-in class
	 * loader in JDK 1.1.
	 * 
	 * </ol>
	 */
	static public URL getResource(final String resource) {
		ClassLoader classLoader = null;
		URL url = null;

		try {
			if (!java1) {
				classLoader = getTCL();
				if (classLoader != null) {
					trace.debug("Trying to find [" + resource
							+ "] using context classloader " + classLoader
							+ ".");
					url = classLoader.getResource(resource);
					if (url != null)
						return url;
				}
			}

			// We could not find resource. Ler us now try with the
			// classloader that loaded this class.
			classLoader = ResourceLoader.class.getClassLoader();
			if (classLoader != null) {
				trace.debug("Trying to find [" + resource + "] using "
						+ classLoader + " class loader.");
				url = classLoader.getResource(resource);
				if (url != null)
					return url;
			}
		} catch (final Throwable t) {
			trace.warn(TSTR, t);
		}

		// Last ditch attempt: get the resource from the class path. It
		// may be the case that clazz was loaded by the Extentsion class
		// loader which the parent of the system class loader. Hence the
		// code below.
		trace.debug("Trying to find [" + resource
				+ "] using ClassLoader.getSystemResource().");
		return ClassLoader.getSystemResource(resource);
	}

	/**
	 * Are we running under JDK 1.x?
	 */
	public static boolean isJava1() {
		return java1;
	}

	/**
	 * Get the Thread Context Loader which is a JDK 1.2 feature. If we are
	 * running under JDK 1.1 or anything else goes wrong the method returns
	 * <code>null<code>.
	 * 
	 * */
	private static ClassLoader getTCL() throws IllegalAccessException,
	InvocationTargetException {

		// Are we running on a JDK 1.2 or later system?
		Method method = null;
		try {
			method = Thread.class.getMethod("getContextClassLoader");
		} catch (final NoSuchMethodException e) {
			// We are running on JDK 1.1
			return null;
		}

		return (ClassLoader) method.invoke(Thread.currentThread());
	}

	/**
	 * If running under JDK 1.2 load the specified class using the
	 * <code>Thread</code> <code>contextClassLoader</code> if that fails try
	 * Class.forname. Under JDK 1.1 only Class.forName is used.
	 * 
	 */
	static public Class<?> loadClass(final String clazz) throws ClassNotFoundException {
		// Just call Class.forName(clazz) if we are running under JDK 1.1
		// or if we are instructed to ignore the TCL.
		if (java1 || ignoreTCL)
			return Class.forName(clazz);
		else {
			try {
				return getTCL().loadClass(clazz);
			} catch (final Throwable e) {
				// we reached here because tcl was null or because of a
				// security exception, or because clazz could not be loaded...
				// In any case we now try one more time
				return Class.forName(clazz);
			}
		}
	}

	/**
	 * If <code>value</code> is "true", then <code>true</code> is returned. If
	 * <code>value</code> is "false", then <code>true</code> is returned.
	 * Otherwise, <code>default</code> is returned.
	 * 
	 * <p>
	 * Case of value is unimportant.
	 */
	public static boolean toBoolean(final String value, final boolean dEfault) {
		if (value == null)
			return dEfault;
		final String trimmedVal = value.trim();
		if ("true".equalsIgnoreCase(trimmedVal))
			return true;
		if ("false".equalsIgnoreCase(trimmedVal))
			return false;
		return dEfault;
	}

}
