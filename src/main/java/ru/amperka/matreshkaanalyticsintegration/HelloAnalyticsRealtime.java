package ru.amperka.matreshkaanalyticsintegration;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import com.google.api.services.analytics.Analytics;
import com.google.api.services.analytics.Analytics.Data.Realtime.Get;
import com.google.api.services.analytics.AnalyticsScopes;
import com.google.api.services.analytics.model.Account;
import com.google.api.services.analytics.model.Accounts;
import com.google.api.services.analytics.model.Profiles;
import com.google.api.services.analytics.model.RealtimeData;
import com.google.api.services.analytics.model.RealtimeData.ColumnHeaders;
import com.google.api.services.analytics.model.RealtimeData.ProfileInfo;
import com.google.api.services.analytics.model.RealtimeData.Query;
import com.google.api.services.analytics.model.Webproperties;
import com.google.api.services.analytics.model.Webproperty;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A simple example of how to access the Google Analytics API using a service
 * account.
 */
public class HelloAnalyticsRealtime {
	private static final Logger logger = Logger.getLogger(HelloAnalyticsRealtime.class.getName());
//	static {
//		logger.setLevel(Level.FINEST);
//		ConsoleHandler handler = new ConsoleHandler();
//		// PUBLISH this level
//		handler.setLevel(Level.FINEST);
//		logger.addHandler(handler);
//	}
	private static final String APPLICATION_NAME = "Matreshka Analytics Integration";

	public static void main(String[] args) {
		
		String keyFileLocation = args[0];
		String serviceAccountEmail = args[1];
		String url = args[2];
		int usersOnline = getUsersOnline(serviceAccountEmail, keyFileLocation, url);
		System.out.println("usersOnline: " + usersOnline);
	}
	public static int getUsersOnline(String serviceAccountEmail, String keyFileLocation, String url) {
		try {
			Analytics analytics = initializeAnalytics(serviceAccountEmail, keyFileLocation);

			String profile = getWebresourceProfileId(analytics, url);
			logger.log(Level.FINE, "First Profile Id: {0}", profile);
			List<Map<String, String>> realtimeReport = getRealtimeReport(getResults(analytics, profile, "activeUsers"));
			//realtime report can be empty (if nobody is online)
			//or should have one row
			if (realtimeReport == null || realtimeReport.isEmpty()) {
				return 0;
			}
			assert realtimeReport.size() == 1;
			String activeUsers = realtimeReport.get(0).get("rt:activeUsers");
			return Integer.valueOf(activeUsers);
		} catch (IOException | GeneralSecurityException ex) {
			logger.log(Level.SEVERE, null, ex);
		}
		return 0;
	}
	
	private static Analytics initializeAnalytics(String SERVICE_ACCOUNT_EMAIL, String KEY_FILE_LOCATION) throws IOException, GeneralSecurityException {
		// Initializes an authorized analytics service object.
		GsonFactory jsonFactory = GsonFactory.getDefaultInstance();
		// Construct a GoogleCredential object with the service account email
		// and p12 file downloaded from the developer console.
		HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
		ArrayList<String> allAnalyticsScopes = new ArrayList<>();
		allAnalyticsScopes.add(AnalyticsScopes.ANALYTICS);
		allAnalyticsScopes.add(AnalyticsScopes.ANALYTICS_READONLY);
		GoogleCredential credential = new GoogleCredential.Builder()
				.setTransport(httpTransport)
				.setJsonFactory(jsonFactory)
				.setServiceAccountId(SERVICE_ACCOUNT_EMAIL)
				.setServiceAccountPrivateKeyFromP12File(new File(KEY_FILE_LOCATION))
				.setServiceAccountScopes(allAnalyticsScopes)
				.build();

		// Construct the Analytics service object.
		return new Analytics.Builder(httpTransport, jsonFactory, credential)
				.setApplicationName(APPLICATION_NAME).build();
	}

	private static String getWebresourceProfileId(Analytics analytics, String url) throws IOException {
		// Query for the list of all accounts associated with the service account.
		Accounts accounts = analytics.management().accounts().list().execute();

		if (accounts.getItems().isEmpty()) {
			logger.severe("No accounts found");
		} else {
			for (Account account: accounts.getItems()){
				String accountId = account.getId();

				// Query for the list of properties associated with the first account.
				Webproperties properties = analytics.management().webproperties()
						.list(accountId).execute();

				if (properties.getItems().isEmpty()) {
					logger.severe("No Webproperties found");
				} else {
					for(Webproperty webproperty: properties.getItems()) {
						if(!url.equals(webproperty.getWebsiteUrl())) {
							continue;
						}
						String webpropertyId = webproperty.getId();

						// Query for the list views (profiles) associated with the property.
						Profiles profiles = analytics.management().profiles()
								.list(accountId, webpropertyId).execute();

						if (profiles.getItems().isEmpty()) {
							logger.severe("No views (profiles) found");
						} else {
							// Return the first (view) profile associated with the property.
							return profiles.getItems().get(0).getId();
						}
					}
				}
			}
		}
		return null;
	}

	private static RealtimeData getResults(Analytics analytics, String profileId, String parameter) throws IOException {
		// Query the Core Reporting API for the number of sessions
		// in the past seven days.
		Get get = analytics.data().realtime().get("ga:" + profileId, "rt:" + parameter);
		return get.execute();
	}

	private static List<Map<String, String>> getRealtimeReport(RealtimeData realtimeData) {
		logger.log(Level.FINE, "Response: id={0}, realtimeData Kind=", new Object[]{realtimeData.getId(), realtimeData.getKind()});

		logQueryInfo(realtimeData.getQuery());
		logProfileInfo(realtimeData.getProfileInfo());
		logPaginationInfo(realtimeData);
		return getDataTable(realtimeData);
	}

	private static void logQueryInfo(Query query) {
		logger.log(Level.FINE, "Query Info:\nIds: {0}\nMetrics: {1}\nDimensions: {2}\nSort: {3}\nFilters: {4}\nMax results: {5}", new Object[]{query.getIds(), query.getMetrics(), query.getDimensions(), query.getSort(), query.getFilters(), query.getMaxResults()});
	}

	private static void logProfileInfo(ProfileInfo profileInfo) {
		logger.log(Level.FINE, "Info:\nAccount ID:{0}\nWeb Property ID: {1}\nProfile ID: {2}\nProfile Name: {3}\nTable Id: {4}", new Object[]{profileInfo.getAccountId(), profileInfo.getWebPropertyId(), profileInfo.getProfileId(), profileInfo.getProfileName(), profileInfo.getTableId()});
	}

	private static void logPaginationInfo(RealtimeData realtimeData) {
		logger.log(Level.FINE, "Pagination info:\nSelf link: {0}\nTotal Results: {1}", new Object[]{realtimeData.getSelfLink(), realtimeData.getTotalResults()});
	}

	private static List<Map<String, String>> getDataTable(RealtimeData realtimeData) {
		List<Map<String, String>> result = new ArrayList<>();
		if (realtimeData.getRows() == null || realtimeData.getRows().isEmpty()) {
			return result;
		}
		List<String> headers = new ArrayList<>();
		for (ColumnHeaders header : realtimeData.getColumnHeaders()) {
			logger.log(Level.FINE, "{0} ({1})", new Object[]{header.getName(), header.getDataType()});
			headers.add(header.getName());
		}
		for (List<String> row : realtimeData.getRows()) {
			Map<String, String> rowOutout = new HashMap<>();
			for (int i = 0; i < row.size(); i++) {
				String element = row.get(i);
				logger.fine(element);
				rowOutout.put(headers.get(i), element);
			}
			result.add(rowOutout);
		}
		return result;
	}
}
