/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.ardu_cris.mai.daemon;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Produces;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import org.firebirdsql.jdbc.FBDriver;
import ru.ardu_cris.mai.ConnectionGate;

/**
 * REST Web Service
 *
 * @author aleksandr
 */
@Path("webresourceaccess")
public class WebResourceAccess {
	
	@Context
	private UriInfo context;

	
	@GET
	@Produces("text/html")
	public String getHtml(@QueryParam("url") String url) {
		StringBuilder html = new StringBuilder("<!DOCTYPE html>\n");
		html.append("	<head>\n");
		html.append("		<title>Display Page</title>\n");
		html.append("		<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\n");
		html.append("	</head>\n");
		html.append("	<body>\n");
		html.append("		<h1>Данные из Google Analytics</h1>\n");
		String body = getCsv(url);
		body = body.replaceAll(";", "<br/>");
		html.append("<p>Status: ");
		html.append(body);
		html.append("</p>\n");
		html.append("	</body>\n");
		html.append("</html>\n");
		return html.toString();
	}
	
	/**
	 * Retrieves representation of an instance of me.kopilov.httpfacadeformai.GenericResource
	 * @param url
	 * @return an instance of java.lang.String
	 */
	@GET
	@Produces("text/csv")
	public String getCsv(@QueryParam("url") String url) {
		JsonArray data = getJson(url);
		if (data.size() == 0) {
			return "failed;no webresource available";
		} else if (data.size() > 1) {
			return "failed;several webresource available";
		} else {
			JsonObject webresourceData = (JsonObject) data.get(0);
			return "success;" + webresourceData.getInt("usersOnline") + " users online";
		}
	}
	
	@GET
	@Produces("application/json")
	public JsonArray getJson(@QueryParam("url") String url) {
		JsonArrayBuilder result = Json.createArrayBuilder();
		Logger logger = Logger.getLogger(WebResourceAccess.class.getName());
		logger.info(url);
		try (Connection connection = ConnectionGate.getIstance().getConnection()){
			PreparedStatement statement;
			String sql = "select NAME, URL, USERS_ONLINE, USERS_ONLINE_PREFERRED, LAST_UPDATED, UPDATING_PERIOD from WEBRESOURCE where IS_ACTIVE > 0";
			if (url == null || url.equals("")) {
				statement = connection.prepareStatement(sql);
			} else {
				sql = sql + " and URL = ?";
				statement = connection.prepareStatement(sql);
				statement.setString(1, url);
			}
			ResultSet resultSet = statement.executeQuery();
			while (resultSet.next()) {
				JsonObjectBuilder resource = Json.createObjectBuilder();
				Utls.addJsonString(resource, "name", resultSet.getString("NAME"));
				Utls.addJsonString(resource, "url", resultSet.getString("URL"));
				resource.add("usersOnline", resultSet.getInt("USERS_ONLINE"));
				resource.add("usersOnlinePreferred", resultSet.getInt("USERS_ONLINE_PREFERRED"));
				Timestamp lastUpdated = resultSet.getTimestamp("LAST_UPDATED");
				if (lastUpdated == null) { 
					resource.addNull("lastUpdated");
				} else {
					String lastUpdatedStr = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(lastUpdated);
					resource.add("lastUpdated", lastUpdatedStr);
				}
				resource.add("updatingPeriod", resultSet.getInt("UPDATING_PERIOD"));
				result.add(resource);
			}
		} catch (SQLException ex) {
			logger.log(Level.SEVERE, null, ex);
		}
		return result.build();
	}
}
