package ru.ardu_cris.mai;

import java.sql.SQLException;
import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

/**
 * Общий функционал модулей, работающих с БД
 * @author aleksandr
 */
public abstract class BaseApplicationModule implements ApplicationModule {
	private CommandLine commandLine;
	
	@Override
	public String validateParameters(CommandLine commandLine) {
		List<String> argList = commandLine.getArgList();
		if (argList.isEmpty()) {
			return "No connection string found";
		}
		String connectionString = argList.get(0);
		String login = commandLine.getOptionValue("login", null);
		String password = commandLine.getOptionValue("password", null);
		try {
			boolean success = ConnectionGate.tryToConnect(connectionString, login, password);
			if (!success) {
				return "Could not connect to database";
			}
		} catch (SQLException ex) {
			return "Could not connect to database\n" +ex.getMessage();
		}
		return null;
	}

	@Override
	public Options getOptionsList() {
		Options options = new Options();
		options.addOption(null, "login", true, "Database login");
		options.addOption(null, "password", true, "Database password");
		options.addOption("h", "help", false, "Show help message and exit");
		return options;
	}
	@Override
	public int execute(CommandLine commandLine) {
		this.commandLine = commandLine;
		List<String> argList = commandLine.getArgList();
		String connectionString = argList.get(0);
		String login = commandLine.getOptionValue("login", null);
		String password = commandLine.getOptionValue("password", null);
		ConnectionGate.getIstance().init(connectionString, login, password);
		return 0;
	}
	
	public CommandLine getCommandLine() {
		return commandLine;
	}
}
