package ru.ardu_cris.mai;

import java.util.Arrays;
import java.util.ResourceBundle;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import ru.ardu_cris.mai.daemon.Daemon;
import ru.ardu_cris.mai.face.Face;
import ru.ardu_cris.mai.face.FaceModule;

/**
 * Запуск программы Matreshka Analytics Integration.
 * Первый параметр командной строки должен соответствовать одному из модулей приложения,
 * остальные передаются данному модулю. Список модулей: daemon, face, query
 * @author aleksandr
 */
public class Main {
	private static final ResourceBundle l10n = ResourceBundle.getBundle("ru.ardu_cris.mai.l10n");
	
	public static void main(String[] args) {
		CommandLine commandLine;
		try {
			if (args.length == 0) {
				System.out.println(l10n.getString("detailedHelpMain"));
				return;
			}
			String moduleName = args[0];
			String[] moduleArgs = Arrays.copyOfRange(args, 1, args.length);
			ApplicationModule module = null;
			switch (moduleName) {
				case "query":
					module = new Query(); break;
				case "daemon":
					module = new Daemon(); break;
				case "face": case "gui":
					module = new FaceModule(); break;
				case "http":
					//coming soon...
				default:
					System.err.println("Unknown module: " + moduleName);
					System.exit(1);
			}
			commandLine = parseCommandLine(moduleArgs, module.getOptionsList());
			String validMessage = module.validateParameters(commandLine);
			if (validMessage != null) {
				System.out.println(validMessage);
				return;
			}
			if (commandLine.hasOption("help")) {
				System.out.println(module.getHelpMessage());
				return;
			}
			module.execute(commandLine);
		} catch (ParseException ex) {
			System.out.println(ex.getLocalizedMessage());
			//System.out.println(l10n.getString("help"));
			return;
		}
	}

	/**
	 * Парсинг параметров командной строки с использованием Apache Commons CLI.
	 * Ищутся указанные опции
	 * @param args параметры командной строки
	 * @return
	 * @throws ParseException 
	 */
	private static CommandLine parseCommandLine(String[] args, Options options) throws ParseException {
		CommandLineParser parser = new DefaultParser();
		return parser.parse(options, args);
	}
	
}
