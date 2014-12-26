/**
 * 
 * @author Amir
 */
package org.harmony.server.bl;

/**
 * @author Amir
 *
 */
public class User {
	public enum Difficulcy{EASY, MEDIUM, HARD}

	private final String userName;
	private final String password;
	private final Difficulcy difficulcy;


	/**
	 * @param userName
	 * @param password
	 * @param difficulcy
	 */
	public User(final String userName, final String password, final Difficulcy difficulcy) {
		this.userName = userName;
		this.password = password;
		this.difficulcy = difficulcy;
	}


	/**
	 * @return the userName
	 */
	public String getUserName() {
		return userName;
	}


	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}


	/**
	 * @return the difficulcy
	 */
	public Difficulcy getDifficulcy() {
		return difficulcy;
	}


}
