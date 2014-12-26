//License
/***
 * Java TelnetD library (embeddable telnet daemon)
 * Copyright (c) 2000-2005 Dieter Wimberger
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of the author nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDER AND CONTRIBUTORS ``AS
 * IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ***/

package net.wimpi.telnetd.io;

import java.io.IOException;

import net.wimpi.telnetd.io.terminal.Terminal;
import net.wimpi.telnetd.io.terminal.TerminalManager;
import net.wimpi.telnetd.net.Connection;
import net.wimpi.telnetd.net.ConnectionData;
import net.wimpi.telnetd.net.ConnectionEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Class for Terminal specific I/O.
 * It represents the layer between the application layer and the generic telnet I/O.
 * Terminal specific I/O is achieved via pluggable terminal classes
 *
 * @author Dieter Wimberger , minor changes by Amir Arad
 * @version 2.01 (02/07/2011)
 * @see net.wimpi.telnetd.io.TelnetIO
 * @see net.wimpi.telnetd.io.terminal.Terminal
 */
public class TerminalIO implements BasicTerminalIO {

	private static Log log = LogFactory.getLog(TerminalIO.class);
	private TelnetIO m_TelnetIO;					//low level I/O

	private final Connection m_Connection;			//the connection this instance is working for
	private final ConnectionData m_ConnectionData;	//holds data of the connection
	private Terminal m_Terminal;					//active terminal object

	//Members
	private boolean m_AcousticSignalling;		//flag for accoustic signalling
	private boolean m_Autoflush;				//flag for autoflushing mode
	private boolean m_ForceBold;				//flag for forcing bold output
	private boolean m_LineWrapping;

	/**
	 * Constructor of the TerminalIO class.
	 *
	 * @param con Connection the instance will be working for
	 */
	public TerminalIO(final Connection con) {
		m_Connection = con;
		m_AcousticSignalling = true;
		m_Autoflush = true;

		//store the associated  ConnectionData instance
		m_ConnectionData = m_Connection.getConnectionData();
		try {
			//create a new telnet io
			m_TelnetIO = new TelnetIO();
			m_TelnetIO.setConnection(con);
			m_TelnetIO.initIO();
		} catch (final Exception ex) {
			//handle, at least log
		}

		//set default terminal
		try {
			setDefaultTerminal();
		} catch (final Exception ex) {
			log.error("TerminalIO()", ex);
			throw new RuntimeException();
		}
	}//constructor



	/************************************************************************
	 * Visible character I/O methods   				                        *
	 ************************************************************************/

	/**
	 * Read a single character and take care for terminal function calls.
	 *
	 * @return <ul>
	 *         <li>character read
	 *         <li>IOERROR in case of an error
	 *         <li>DELETE,BACKSPACE,TABULATOR,ESCAPE,COLORINIT,LOGOUTREQUEST
	 *         <li>UP,DOWN,LEFT,RIGHT
	 *         </ul>
	 */
	@Override
	public synchronized int read() throws IOException {
		return asyncRead();
	}//read

	/**
	 * Read a single character and take care for terminal function calls.
	 *
	 * @return <ul>
	 *         <li>character read
	 *         <li>IOERROR in case of an error
	 *         <li>DELETE,BACKSPACE,TABULATOR,ESCAPE,COLORINIT,LOGOUTREQUEST
	 *         <li>UP,DOWN,LEFT,RIGHT
	 *         </ul>
	 */
	@Override
	public int asyncRead() throws IOException {
		int i = m_TelnetIO.read();
		//translate possible control sequences
		i = m_Terminal.translateControlCharacter(i);

		//catch & fire a logoutrequest event
		if (i == LOGOUTREQUEST) {
			m_Connection.processConnectionEvent(new ConnectionEvent(m_Connection, ConnectionEvent.CONNECTION_LOGOUTREQUEST));
			i = HANDLED;
		} else if (i > 256 && i == ESCAPE) {
			//translate an incoming escape sequence
			i = handleEscapeSequence(i);
		}

		//return i holding a char or a defined special key
		return i;
	}//read

	@Override
	public synchronized BasicTerminalIO write(final byte b) throws IOException {
		m_TelnetIO.write(b);
		if (m_Autoflush) {
			flush();
		}
		return this;
	}//write

	@Override
	public synchronized BasicTerminalIO write(final char ch) throws IOException {
		m_TelnetIO.write(ch);
		if (m_Autoflush) {
			flush();
		}
		return this;
	}//write(char)

	@Override
	public synchronized BasicTerminalIO write(final String str) throws IOException {
		if (m_ForceBold) {
			m_TelnetIO.write(m_Terminal.formatBold(str));
		} else {
			m_TelnetIO.write(m_Terminal.format(str));
		}
		if (m_Autoflush) {
			flush();
		}
		return this;
	}//write(String)


	/*** End of Visible character I/O methods  ******************************/



	/**
	 * *********************************************************************
	 * Erase methods							                            *
	 * **********************************************************************
	 */

	@Override
	public synchronized BasicTerminalIO eraseToEndOfLine() throws IOException {
		doErase(EEOL);
		return this;
	}//eraseToEndOfLine

	@Override
	public synchronized BasicTerminalIO eraseToBeginOfLine() throws IOException {
		doErase(EBOL);
		return this;
	}//eraseToBeginOfLine

	@Override
	public synchronized BasicTerminalIO eraseLine() throws IOException {
		doErase(EEL);
		return this;
	}//eraseLine

	@Override
	public synchronized BasicTerminalIO eraseToEndOfScreen() throws IOException {
		doErase(EEOS);
		return this;
	}//eraseToEndOfScreen

	@Override
	public synchronized BasicTerminalIO eraseToBeginOfScreen() throws IOException {
		doErase(EBOS);
		return this;
	}//eraseToBeginOfScreen

	@Override
	public synchronized BasicTerminalIO eraseScreen() throws IOException {
		doErase(EES);
		return this;
	}//eraseScreen

	private synchronized void doErase(final int funcConst) throws IOException {

		m_TelnetIO.write(m_Terminal.getEraseSequence(funcConst));
		if (m_Autoflush) {
			flush();
		}
	}//erase

	/*** End of Erase methods  **********************************************/

	/**
	 * *********************************************************************
	 * Cursor related methods							                    *
	 * **********************************************************************
	 */

	@Override
	public synchronized BasicTerminalIO moveCursor(final int direction, final int times) throws IOException {

		m_TelnetIO.write(m_Terminal.getCursorMoveSequence(direction, times));
		if (m_Autoflush) {
			flush();
		}
		return this;
	}//moveCursor

	@Override
	public synchronized BasicTerminalIO moveLeft(final int times) throws IOException {
		moveCursor(LEFT, times);
		return this;
	}//moveLeft

	@Override
	public synchronized BasicTerminalIO moveRight(final int times) throws IOException {
		moveCursor(RIGHT, times);
		return this;
	}//moveRight

	@Override
	public synchronized BasicTerminalIO moveUp(final int times) throws IOException {
		moveCursor(UP, times);
		return this;
	}//moveUp

	@Override
	public synchronized BasicTerminalIO moveDown(final int times) throws IOException {
		moveCursor(DOWN, times);
		return this;
	}//moveDown

	@Override
	public synchronized BasicTerminalIO setCursor(final int row, final int col) throws IOException {
		final int[] pos = new int[2];
		pos[0] = row;
		pos[1] = col;
		m_TelnetIO.write(m_Terminal.getCursorPositioningSequence(pos));
		if (m_Autoflush) {
			flush();
		}
		return this;
	}//setCursor

	@Override
	public synchronized BasicTerminalIO homeCursor() throws IOException {
		m_TelnetIO.write(m_Terminal.getCursorPositioningSequence(HOME));
		if (m_Autoflush) {
			flush();
		}
		return this;
	}//homeCursor

	@Override
	public synchronized BasicTerminalIO storeCursor() throws IOException {
		m_TelnetIO.write(m_Terminal.getSpecialSequence(STORECURSOR));
		return this;
	}//store Cursor

	@Override
	public synchronized BasicTerminalIO restoreCursor() throws IOException {
		m_TelnetIO.write(m_Terminal.getSpecialSequence(RESTORECURSOR));
		return this;
	}//restore Cursor

	/*** End of cursor related methods **************************************/


	/**
	 * *********************************************************************
	 * Special terminal function methods							        *
	 * **********************************************************************
	 */


	@Override
	public synchronized void setSignalling(final boolean bool) {
		m_AcousticSignalling = bool;
	}//setAcousticSignalling


	@Override
	public synchronized boolean isSignalling() {
		return m_AcousticSignalling;
	}//isAcousticSignalling

	/**
	 * Method to write the NVT defined BEL onto the stream.
	 * If signalling is off, the method simply returns, without
	 * any action.
	 */
	@Override
	public synchronized BasicTerminalIO bell() throws IOException {
		if (m_AcousticSignalling) {
			m_TelnetIO.write(BEL);
		}
		if (m_Autoflush) {
			flush();
		}
		return this;
	}//bell

	/**
	 * EXPERIMENTAL, not defined in the interface.
	 */
	@Override
	public synchronized boolean defineScrollRegion(final int topmargin, final int bottommargin) throws IOException {
		if (m_Terminal.supportsScrolling()) {
			m_TelnetIO.write(m_Terminal.getScrollMarginsSequence(topmargin, bottommargin));
			flush();
			return true;
		} else
			return false;
	}//defineScrollRegion

	@Override
	public synchronized BasicTerminalIO setForegroundColor(final int color) throws IOException {
		if (m_Terminal.supportsSGR()) {
			m_TelnetIO.write(m_Terminal.getGRSequence(FCOLOR, color));
			if (m_Autoflush) {
				flush();
			}
		}
		return this;
	}//setForegroundColor

	@Override
	public synchronized BasicTerminalIO setBackgroundColor(final int color) throws IOException {
		if (m_Terminal.supportsSGR()) {
			//this method adds the offset to the fg color by itself
			m_TelnetIO.write(m_Terminal.getGRSequence(BCOLOR, color + 10));
			if (m_Autoflush) {
				flush();
			}
		}
		return this;
	}//setBackgroundColor

	@Override
	public synchronized BasicTerminalIO setBold(final boolean b) throws IOException {
		if (m_Terminal.supportsSGR()) {
			if (b) {
				m_TelnetIO.write(m_Terminal.getGRSequence(STYLE, BOLD));
			} else {
				m_TelnetIO.write(m_Terminal.getGRSequence(STYLE, BOLD_OFF));
			}
			if (m_Autoflush) {
				flush();
			}
		}
		return this;
	}//setBold

	@Override
	public synchronized BasicTerminalIO forceBold(final boolean b) {
		m_ForceBold = b;
		return this;
	}//forceBold

	@Override
	public synchronized BasicTerminalIO setUnderlined(final boolean b) throws IOException {
		if (m_Terminal.supportsSGR()) {
			if (b) {
				m_TelnetIO.write(m_Terminal.getGRSequence(STYLE, UNDERLINED));
			} else {
				m_TelnetIO.write(m_Terminal.getGRSequence(STYLE, UNDERLINED_OFF));
			}
			if (m_Autoflush) {
				flush();
			}

		}
		return this;
	}//setUnderlined

	@Override
	public synchronized BasicTerminalIO setItalic(final boolean b) throws IOException {
		if (m_Terminal.supportsSGR()) {
			if (b) {
				m_TelnetIO.write(m_Terminal.getGRSequence(STYLE, ITALIC));
			} else {
				m_TelnetIO.write(m_Terminal.getGRSequence(STYLE, ITALIC_OFF));
			}
			if (m_Autoflush) {
				flush();
			}
		}
		return this;
	}//setItalic


	@Override
	public synchronized BasicTerminalIO setBlink(final boolean b) throws IOException {
		if (m_Terminal.supportsSGR()) {
			if (b) {
				m_TelnetIO.write(m_Terminal.getGRSequence(STYLE, BLINK));
			} else {
				m_TelnetIO.write(m_Terminal.getGRSequence(STYLE, BLINK_OFF));
			}
			if (m_Autoflush) {
				flush();
			}
		}
		return this;
	}//setItalic

	@Override
	public synchronized BasicTerminalIO resetAttributes() throws IOException {
		if (m_Terminal.supportsSGR()) {
			m_TelnetIO.write(m_Terminal.getGRSequence(RESET, 0));
		}
		return this;
	}//resetGR

	/*** End of special terminal function methods ***************************/

	/************************************************************************
	 * Auxiliary I/O methods						                        *
	 ************************************************************************/

	/**
	 * Method that parses forward for escape sequences
	 */
	private int handleEscapeSequence(final int i) throws IOException {
		if (i == ESCAPE) {
			final int[] bytebuf = new int[m_Terminal.getAtomicSequenceLength()];
			//fill atomic length
			//FIXME: ensure CAN, broken Escapes etc.
			for (int m = 0; m < bytebuf.length; m++) {
				bytebuf[m] = m_TelnetIO.read();
			}
			return m_Terminal.translateEscapeSequence(bytebuf);
		}
		if (i == BYTEMISSING) {
			//FIXME:longer escapes etc...
		}

		return HANDLED;
	}//handleEscapeSequence

	/**
	 * Accessor method for the autoflushing mechanism.
	 */
	@Override
	public boolean isAutoflushing() {
		return m_Autoflush;
	}//isAutoflushing

	@Override
	public synchronized void resetTerminal() throws IOException {
		m_TelnetIO.write(m_Terminal.getSpecialSequence(DEVICERESET));
	}

	@Override
	public synchronized void setLinewrapping(final boolean b) throws IOException {
		if (b && !m_LineWrapping) {
			m_TelnetIO.write(m_Terminal.getSpecialSequence(LINEWRAP));
			m_LineWrapping = true;
			return;
		}
		if (!b && m_LineWrapping) {
			m_TelnetIO.write(m_Terminal.getSpecialSequence(NOLINEWRAP));
			m_LineWrapping = false;
			return;
		}
	}//setLineWrapping

	@Override
	public boolean isLineWrapping() {
		return m_LineWrapping;
	}//

	/**
	 * Mutator method for the autoflushing mechanism.
	 */
	@Override
	public synchronized void setAutoflushing(final boolean b) {
		m_Autoflush = b;
	}//setAutoflushing


	/**
	 * Method to flush the Low-Level Buffer
	 */
	@Override
	public synchronized BasicTerminalIO flush() throws IOException {
		m_TelnetIO.flush();
		return this;
	}//flush (implements the famous iToilet)


	@Override
	public synchronized void close() {
		m_TelnetIO.closeOutput();
		m_TelnetIO.closeInput();
	}//close

	/*** End of Auxiliary I/O methods  **************************************/



	/************************************************************************
	 * Terminal management specific methods			                        *
	 ************************************************************************/

	/**
	 * Accessor method to get the active terminal object
	 *
	 * @return Object that implements Terminal
	 */
	public Terminal getTerminal() {
		return m_Terminal;
	}//getTerminal

	/**
	 * Sets the default terminal ,which will either be
	 * the negotiated one for the connection, or the systems
	 * default.
	 */
	@Override
	public void setDefaultTerminal() throws IOException {
		//set the terminal passing the negotiated string
		setTerminal(m_ConnectionData.getNegotiatedTerminalType());
	}//setDefaultTerminal

	/**
	 * Mutator method to set the active terminal object
	 * If the String does not name a terminal we support
	 * then the vt100 is the terminal of selection automatically.
	 *
	 * @param terminalName String that represents common terminal name
	 */
	@Override
	public void setTerminal(final String terminalName) throws IOException {

		m_Terminal = TerminalManager.getReference().getTerminal(terminalName);
		//Terminal is set we init it....
		initTerminal();
		//debug message
		log.debug("Set terminal to " + m_Terminal.toString());
	}//setTerminal


	/**
	 * Terminal initialization
	 */
	private synchronized void initTerminal() throws IOException {
		m_TelnetIO.write(m_Terminal.getInitSequence());
		flush();
	}//initTerminal

	/**
	 *
	 */
	@Override
	public int getRows() {
		return m_ConnectionData.getTerminalRows();
	}//getRows

	/**
	 *
	 */
	@Override
	public int getColumns() {
		return m_ConnectionData.getTerminalColumns();
	}//getColumns


	/**
	 * Accessor Method for the terminal geometry changed flag
	 */
	public boolean isTerminalGeometryChanged() {
		return m_ConnectionData.isTerminalGeometryChanged();
	}//isTerminalGeometryChanged

	/*** End of terminal management specific methods  ***********************/

	/** Constants Declaration  **********************************************/

	/**
	 * Terminal independent representation constants for terminal
	 * functions.
	 */
	public static final int[] HOME = {0, 0};

	public static final int
	IOERROR = -1;		//IO error
	public static final int// Positioning 10xx
	UP = 1001; 		//one up
	public static final int DOWN = 1002; 		//one down
	public static final int RIGHT = 1003; 	//one left
	public static final int LEFT = 1004; 		//one right
	//HOME=1005,		//Home cursor pos(0,0)


	public static final int// Functions 105x
	STORECURSOR = 1051;	//store cursor position + attributes
	public static final int RESTORECURSOR = 1052;	//restore cursor + attributes

	public static final int// Erasing 11xx
	EEOL = 1100;		//erase to end of line
	public static final int EBOL = 1101;		//erase to beginning of line
	public static final int EEL = 1103;		//erase entire line
	public static final int EEOS = 1104;		//erase to end of screen
	public static final int EBOS = 1105;		//erase to beginning of screen
	public static final int EES = 1106;		//erase entire screen

	public static final int// Escape Sequence-ing 12xx
	ESCAPE = 1200;		//Escape
	public static final int BYTEMISSING = 1201;	//another byte needed
	public static final int UNRECOGNIZED = 1202;	//escape match missed

	public static final int// Control Characters 13xx
	ENTER = 1300;		//LF is ENTER at the moment
	public static final int TABULATOR = 1301;	 	//Tabulator
	public static final int DELETE = 1302;		//Delete
	public static final int BACKSPACE = 1303;		//BACKSPACE
	public static final int COLORINIT = 1304;		//Color inited
	public static final int HANDLED = 1305;
	public static final int LOGOUTREQUEST = 1306;		//CTRL-D beim login

	/**
	 * Internal UpdateType Constants
	 */
	public static final int
	LineUpdate = 475,
	CharacterUpdate = 476,
	ScreenpartUpdate = 477;

	/**
	 * Internal BufferType Constants
	 */
	public static final int
	EditBuffer = 575,
	LineEditBuffer = 576;

	/**
	 * Network Virtual Terminal Specific Keys
	 * Thats what we have to offer at least.
	 */
	public static final int BEL = 7;
	public static final int BS = 8;
	public static final int DEL = 127;
	public static final int CR = 13;
	public static final int LF = 10;

	public static final int FCOLOR = 10001;
	public static final int BCOLOR = 10002;
	public static final int STYLE = 10003;
	public static final int RESET = 10004;
	public static final int BOLD = 1;
	public static final int BOLD_OFF = 22;
	public static final int ITALIC = 3;
	public static final int ITALIC_OFF = 23;
	public static final int BLINK = 5;
	public static final int BLINK_OFF = 25;
	public static final int UNDERLINED = 4;
	public static final int UNDERLINED_OFF = 24;
	public static final int DEVICERESET = 10005;
	public static final int LINEWRAP = 10006;
	public static final int NOLINEWRAP = 10007;

	/** end Constants Declaration  ******************************************/

}//class TerminalIO
