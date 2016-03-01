package ru.amperka.matreshkaanalyticsintegration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

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
		return DriverManager.getConnection(connectionString, login, password);
	}
}
