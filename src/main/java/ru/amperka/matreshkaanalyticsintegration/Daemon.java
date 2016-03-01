package ru.amperka.matreshkaanalyticsintegration;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Фоновый процесс, осуществляющий сбор данных из Google Analytics и запись их
 * в базу данных для процесса, взаимодействующего со встраиваемой электроникой.
 * 
 * @author aleksandr<kopilov.ad@gmail.com>
 */
public class Daemon implements Runnable {
	private static final ResourceBundle l10n = ResourceBundle.getBundle("ru.amperka.matreshkaanalyticsintegration.l10n");
	private static final Logger logger = Logger.getLogger(Daemon.class.getName());
//	static {
//		logger.setLevel(Level.FINEST);
//		ConsoleHandler handler = new ConsoleHandler();
//		// PUBLISH this level
//		handler.setLevel(Level.FINEST);
//		logger.addHandler(handler);
//	}
	
	private volatile boolean running = true;
	
	/**
	 * Парсинг и валидация входных параметров, запуск демона
	 * @param args параметры командной строки
	 */
	public static void main(String[] args) {
		CommandLine commandLine;
		try {
			commandLine = parseCommandLine(args);
			List<String> argList = commandLine.getArgList();
			if (commandLine.hasOption("help") || argList.isEmpty()) {
				System.out.println(l10n.getString("detailedHelp"));
				return;
			}
		} catch (ParseException ex) {
			System.out.println(ex.getLocalizedMessage());
			System.out.println(l10n.getString("help"));
			return;
		}
		Daemon daemon = new Daemon(commandLine);
		new Thread(daemon).start();
	}
	
	/**
	 * Парсинг параметров командной строки с использованием Apache Commons CLI.
	 * Ищутся опции "login", "password", "icon", "help"
	 * @param args параметры командной строки
	 * @return
	 * @throws ParseException 
	 */
	private static CommandLine parseCommandLine(String[] args) throws ParseException {
		Options options = new Options();
		options.addOption(null, "login", true, "Database login");
		options.addOption(null, "password", true, "Database password");
		options.addOption("i", "icon", false, "Display tray icon");
		options.addOption("h", "help", false, "Show help message and exit");
		CommandLineParser parser = new DefaultParser();
		return parser.parse(options, args);
	}
	
	private Daemon(CommandLine commandLine) {
		String connectionString = commandLine.getArgList().get(0);
		String login = commandLine.getOptionValue("login");
		String password = commandLine.getOptionValue("password");
		ConnectionGate.getIstance().init(connectionString, login, password);
		
		if (commandLine.hasOption("icon")) {
			displayTrayIcon();
		}
	}
	
	@Override
	public synchronized void run() {
		try (Connection connectionWrite = ConnectionGate.getIstance().getConnection()) {
		try (Connection connectionRead = ConnectionGate.getIstance().getConnection()) {
			PreparedStatement getActiveResources = connectionRead.prepareStatement(
					"select ID, URL, KEY_FILE_LOCATION, SERVICE_ACCOUNT_EMAIL, LAST_UPDATED, UPDATING_PERIOD from WEBRESOURCE\n" +
							"where IS_ACTIVE > 0 and (current_timestamp - LAST_UPDATED) * 24 * 60 * 60 > UPDATING_PERIOD");
			PreparedStatement updateResource = connectionWrite.prepareStatement(
					"update WEBRESOURCE set LAST_UPDATED = current_timestamp, USERS_ONLINE = ? where ID = ?");
			while (running) {
				getActiveResources.clearParameters();
				ResultSet resultSet = getActiveResources.executeQuery();
				while (resultSet.next()) {
					String serviceAccountEmail = resultSet.getString("SERVICE_ACCOUNT_EMAIL");
					String keyFileLocation = resultSet.getString("KEY_FILE_LOCATION");
					String url = resultSet.getString("URL");
					int usersOnline = HelloAnalyticsRealtime.getUsersOnline(serviceAccountEmail, keyFileLocation, url);
					logger.log(Level.FINE, "Online on {0}: {1}", new Object[]{url, usersOnline});
					updateResource.clearParameters();
					updateResource.setInt(1, usersOnline);
					updateResource.setInt(2, resultSet.getInt("ID"));
					updateResource.execute();
				}
				this.wait(500);
			}
		}} catch (SQLException | InterruptedException ex) {
			Logger.getLogger(Daemon.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void displayTrayIcon() {
		SystemTray systemTray= SystemTray.getSystemTray();
		Image image = new ImageIcon(this.getClass().getResource("icon.png")).getImage();
		PopupMenu menu = new PopupMenu();
		TrayIcon trayIcon = new TrayIcon(image, "Matreshka Analytics Integration", menu);
		MenuItem exitItem = new MenuItem(l10n.getString("Exit"));
		exitItem.addActionListener((ActionEvent e) -> {
			running = false;
			systemTray.remove(trayIcon);
		});
		menu.add(exitItem);
		try {
			systemTray.add(trayIcon);
		} catch (AWTException ex) {
			Logger.getLogger(Daemon.class.getName()).log(Level.SEVERE, "Could not display tray icon", ex);
		}
	}

}
