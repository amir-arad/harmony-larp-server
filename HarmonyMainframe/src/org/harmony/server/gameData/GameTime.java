/**
 * 
 * @author Amir
 */
package org.harmony.server.gameData;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Amir
 *
 */
public enum GameTime {
	INSTASCE;

	private static final Log LOGGER = LogFactory.getLog(GameTime.class);
	private long interval;
	private DateFormat dateFormat;

	public static GameTime getInstance(){
		return INSTASCE;
	}

	public void setTime(final String gameTimeStr, final String dateFormatPattern){
		dateFormat = DateFormat.getDateTimeInstance();
		interval = 0;
		if (dateFormatPattern != null && !dateFormatPattern.isEmpty()){
			try{
				dateFormat = new SimpleDateFormat(dateFormatPattern);
			} catch(final Exception e){
				LOGGER.error("date format pattern is invalid: "+ dateFormatPattern, e);
			}
		}
		if (gameTimeStr != null){
			try{
				final Date gameTime = dateFormat.parse(gameTimeStr);
				interval = gameTime.getTime() - System.currentTimeMillis();
			} catch(final Exception e){
				LOGGER.error("start game time is invalid: "+gameTimeStr, e);
			}
		}
	}

	public Date getTime(){
		return new Date(System.currentTimeMillis() + interval);
	}

	public String getTimeStr(){
		final Date time =  new Date(System.currentTimeMillis() + interval);
		return dateFormat.format(time);
	}
}
