/**
 * 
 * @author qballer
 */
package org.harmony.server.commands;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import net.wimpi.telnetd.io.BasicTerminalIO;
import net.wimpi.telnetd.io.toolkit.Editfield;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.harmony.server.shells.AbsShell;
import org.harmony.server.shells.commandShell.Command;
import org.harmony.server.shells.commandShell.CommandError;
import org.harmony.server.shells.commandShell.CommandShell;
import org.harmony.server.startup.Server;

/**
 * @author qballer
 * 
 */
public class ControlProccessBars extends Command {
	private static final Log LOGGER = LogFactory.getLog(ControlProccessBars.class);

	private static ControlProccessBars single;

	private final ArrayList<IControlable> ProBarList;

	private enum InternalCommandList{
		SHOW("Write 'show' to see all active proccess bars", "show") ,
		STOP("write 'stop' to pick a procces bar to stop","stop") ,
		LEAVE("write 'leave' to exit conpbs.","leave"),
		NOSUCHCMD("Dummy command, if you read this something is wrong","NOSUCHCMD");


		private String description;
		private String Text;

		public String getDesc(){
			return this.description;
		}
		public String getText(){
			return this.Text;
		}

		InternalCommandList (final String Desc, final String Text) {
			this.description = Desc;
			this.Text = Text;
		}
		public static InternalCommandList fromString(final String text) {
			InternalCommandList ret = InternalCommandList.NOSUCHCMD;
			for (final InternalCommandList iter : InternalCommandList.values()) {
				if (text.equalsIgnoreCase(iter.getText())) {
					ret = iter;
					break;
				}
			}
			return ret;
		}
	}
	/**
	 * This command relies on the fact that each command is singleton
	 * 
	 * @return - the control reference
	 */
	public static ControlProccessBars GetControl(){
		return single;
	}

	public ControlProccessBars(){
		super("conpbs", "control process bars", true);
		ProBarList = new ArrayList<IControlable>();
		single = this;
	}
	/*abstract implemntaion*/
	@Override
	protected void executeCommand(final CommandShell shell, final Map<String, Object> args) throws IOException, CommandError{
		IControlable[] lastPrinted = ControlablelListToArray();
		InternalCommandList perform;
		boolean stopConpbs = false;
		shell.getIo().eraseScreen();
		writeWelcomConpbsMsg(shell);
		while(!stopConpbs)	{
			writeExplainMsg(shell);
			shell.getIo().write(this.getName()).write(Server.getMessage("shell.main.prompt")).flush();
			perform = getInput(shell);
			switch (perform){
			case SHOW:
				lastPrinted = ControlablelListToArray();
				showList(shell, lastPrinted);
				break;
			case STOP :
				stopProc(shell, lastPrinted);
				break;
			case LEAVE:
				stopConpbs = true;
				break;
			default:
				shell.getIo().write(BasicTerminalIO.CRLF).write(this.getName() + " doesn't support this command").write(BasicTerminalIO.CRLF);
			}

		}

	}


	/**
	 * @param shell
	 * @param lastPrinted
	 * @throws IOException
	 */
	private void showList(final AbsShell shell, final IControlable[] lastPrinted) throws IOException {
		shell.getIo().eraseScreen();
		for (int i = 0; i<lastPrinted.length; i++ ){
			if (lastPrinted[i].IsAcive()){
				shell.getIo().write( String.valueOf(i+1)).write(") ").write(lastPrinted[i].Display()).write(BasicTerminalIO.CRLF);
			}
		}
		if (lastPrinted.length == 0 ){
			shell.getIo().write("list is empty!").write(BasicTerminalIO.CRLF).write(BasicTerminalIO.CRLF);

		}
		shell.getIo().flush();
	}

	/**
	 * @param shell
	 * @param lastPrinted
	 */
	private void stopProc(final AbsShell shell, final IControlable[] lastPrinted) throws IOException {
		int index = -1;
		shell.getIo().write(BasicTerminalIO.CRLF).write("Enter proccess bar number:").flush();
		final Editfield inputNumber = new Editfield(shell.getIo(), "inputNumber", 10);
		inputNumber.run(); //read user name
		try{
			index = Integer.parseInt(inputNumber.getValue().trim()) - 1;
		}
		catch (final NumberFormatException nfe)
		{
			shell.getIo().write(BasicTerminalIO.CRLF).write("Error: No such number").write(BasicTerminalIO.CRLF);
			return;
		}
		if (index >= 0 && index <lastPrinted.length ){
			if(lastPrinted[index].IsAcive()){
				lastPrinted[index].Release();
				shell.getIo().write(BasicTerminalIO.CRLF).write("Proccess Bar terminated, write show to get the latest list.").write(BasicTerminalIO.CRLF);
			}else{
				shell.getIo().write(BasicTerminalIO.CRLF).write("Error: proccess bar already terminated.").write(BasicTerminalIO.CRLF);
			}

		}else {
			shell.getIo().write(BasicTerminalIO.CRLF).write("Error: number out of bounds").write(BasicTerminalIO.CRLF);
		}
	}

	/**
	 * @param shell
	 * @return
	 * @throws IOException
	 */
	private InternalCommandList getInput(final AbsShell shell) throws IOException {
		final Editfield inputCommand = new Editfield(shell.getIo(), "inputCommand", 10);
		inputCommand.run();
		return InternalCommandList.fromString(inputCommand.getValue().trim());
	}

	/**
	 * @param shell
	 */
	private void writeExplainMsg(final AbsShell shell) throws IOException{
		for (final InternalCommandList iter : InternalCommandList.values()) {
			if (iter != InternalCommandList.NOSUCHCMD) {
				shell.getIo().write(iter.getDesc()).write(BasicTerminalIO.CRLF);
			}
		}
	}

	/**
	 * @return
	 */
	private synchronized IControlable[] ControlablelListToArray() {
		final IControlable[] retList = new IControlable[ProBarList.size()];
		ProBarList.toArray(retList);
		return retList;
	}

	/**
	 * @param shell
	 */
	private void writeWelcomConpbsMsg(final AbsShell shell) throws IOException {
		shell.getIo().write("Wellcome to " + this.getName()+ "!").write(BasicTerminalIO.CRLF);
	}

	public synchronized void RegisterControlable( final IControlable toRegister){
		if (!ProBarList.contains(toRegister)){
			ProBarList.add(toRegister);
		}else{
			LOGGER.error("register failed : " + toRegister);
		}
	}
	public synchronized void RemoveControlable( final IControlable toRemove){
		ProBarList.remove(toRemove);
	}

}
