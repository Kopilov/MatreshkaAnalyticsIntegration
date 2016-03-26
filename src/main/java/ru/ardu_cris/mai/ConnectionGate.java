package ru.ardu_cris.mai;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author aleksandr
 */
public class ConnectionGate {
	
	private ConnectionGate(){};
	private static final ConnectionGate singleInstance = new ConnectionGate();
	
	public static ConnectionGate getIstance() {
		return singleInstance;
	}
	
	private String connectionString, login, password;
	
	public void init(String connectionString, String login, String password) {
		this.connectionString = connectionString;
		this.login = login;
		this.password = password;
	}
	
	public Connection getConnection() throws SQLException {
		if (login != null || password != null) {
			return DriverManager.getConnection(connectionString, login, password);
		} else {
			return DriverManager.getConnection(connectionString);
		}
	}
	
	public static boolean tryToConnect(String connectionString, String login, String password) throws SQLException {
		Logger.getLogger(ConnectionGate.class.getName()).log(Level.INFO, "Connecting to {0} with login = {1}, password = {2}", new Object[]{connectionString, login, password});
		Connection connection;
		if (login != null || password != null) {
			connection = DriverManager.getConnection(connectionString, login, password);
		} else {
			connection = DriverManager.getConnection(connectionString);
		}
		boolean success = !connection.isClosed() && connection.isValid(100);
		connection.close();
		return success;
	}
}
