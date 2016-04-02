package ru.ardu_cris.mai.daemon;

import ru.ardu_cris.mai.Query;
import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import ru.ardu_cris.mai.BaseApplicationModule;
import ru.ardu_cris.mai.ConnectionGate;
import ru.ardu_cris.mai.face.FaceModule;

/**
 * Фоновый процесс, осуществляющий сбор данных из Google Analytics и запись их
 * в базу данных для процесса, взаимодействующего со встраиваемой электроникой.
 * 
 * @author aleksandr<kopilov.ad@gmail.com>
 */
public class Daemon extends BaseApplicationModule implements Runnable {
	private static final ResourceBundle l10n = ResourceBundle.getBundle("ru.ardu_cris.mai.l10n");
	private static final Logger logger = Logger.getLogger(Daemon.class.getName());
	private boolean startedFromGui = false;
	
	public Daemon() {
		super();
	}
	
	public Daemon(boolean startedFromGui) {
		this.startedFromGui = startedFromGui;
	}
//	static {
//		logger.setLevel(Level.FINEST);
//		ConsoleHandler handler = new ConsoleHandler();
//		// PUBLISH this level
//		handler.setLevel(Level.FINEST);
//		logger.addHandler(handler);
//	}
	
	private volatile boolean running = true;
	
	@Override
	public Options getOptionsList() {
		Options options = super.getOptionsList();
		options.addOption("i", "icon", false, "Display tray icon");
		return options;
	}
	
	@Override
	public int execute(CommandLine commandLine) {
		super.execute(commandLine);
		if (startedFromGui || commandLine.hasOption("icon")) {
			displayTrayIcon();
		}
		TomcatStartup.start();
		new Thread(this).start();
		return 0;
	}
	
	@Override
	public synchronized void run() {
		try (Connection connectionWrite = ConnectionGate.getIstance().getConnection()) {
		try (Connection connectionRead = ConnectionGate.getIstance().getConnection()) {
			PreparedStatement getActiveResources = connectionRead.prepareStatement(
					"select ID, URL, KEY_FILE_LOCATION, SERVICE_ACCOUNT_EMAIL, LAST_UPDATED, UPDATING_PERIOD from WEBRESOURCE\n" +
							"where IS_ACTIVE > 0 and (\n" +
							"	(current_timestamp - LAST_UPDATED) * 24 * 60 * 60 > UPDATING_PERIOD\n" +
							"	or LAST_UPDATED is null\n" +
							")");
			PreparedStatement updateResource = connectionWrite.prepareStatement(
					"update WEBRESOURCE set LAST_UPDATED = current_timestamp, USERS_ONLINE = ? where ID = ?");
			while (running) {
				getActiveResources.clearParameters();
				ResultSet resultSet = getActiveResources.executeQuery();
				while (resultSet.next()) {
					String serviceAccountEmail = resultSet.getString("SERVICE_ACCOUNT_EMAIL");
					String keyFileLocation = resultSet.getString("KEY_FILE_LOCATION");
					String url = resultSet.getString("URL");
					try {
						int usersOnline = Query.getUsersOnline(serviceAccountEmail, keyFileLocation, url);
						logger.log(Level.FINE, "Online on {0}: {1}", new Object[]{url, usersOnline});
						updateResource.clearParameters();
						updateResource.setInt(1, usersOnline);
						updateResource.setInt(2, resultSet.getInt("ID"));
						updateResource.execute();
					} catch (GeneralSecurityException | IOException e) {
						logger.log(Level.WARNING, "Could not get data from API", e);
					}
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
		
		MenuItem faceItem = new MenuItem(l10n.getString("settings"));
		faceItem.addActionListener((ActionEvent e) -> {
			FaceModule faceModule = new FaceModule(true);
			CommandLine commandLine = getCommandLine();
			faceModule.execute(commandLine);
		});
		menu.add(faceItem);
		
		MenuItem exitItem = new MenuItem(l10n.getString("exit"));
		exitItem.addActionListener((ActionEvent e) -> {
			running = false;
			systemTray.remove(trayIcon);
			TomcatStartup.stop();
		});
		menu.add(exitItem);
		
		try {
			systemTray.add(trayIcon);
		} catch (AWTException ex) {
			Logger.getLogger(Daemon.class.getName()).log(Level.SEVERE, "Could not display tray icon", ex);
		}
	}

	@Override
	public String getHelpMessage() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}
}
