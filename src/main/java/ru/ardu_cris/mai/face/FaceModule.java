package ru.ardu_cris.mai.face;

import org.apache.commons.cli.CommandLine;
import ru.ardu_cris.mai.BaseApplicationModule;

/**
 *
 * @author aleksandr
 */
public class FaceModule extends BaseApplicationModule{
	private boolean startedFromDaemon = false;
	
	public FaceModule() {
		super();
	}
	
	public FaceModule(boolean startedFromDaemon) {
		super();
		this.startedFromDaemon = startedFromDaemon;
	}
	
	@Override
	public int execute(CommandLine commandLine) {
		super.execute(commandLine);
		Face face = new Face(this, startedFromDaemon);
		face.setVisible(true);
		return 0;
	}

	@Override
	public String getHelpMessage() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}
	
}
