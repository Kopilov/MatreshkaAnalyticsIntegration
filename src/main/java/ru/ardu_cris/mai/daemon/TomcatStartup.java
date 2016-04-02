package ru.ardu_cris.mai.daemon;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import org.apache.catalina.LifecycleException;

import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;

/**
 * Запуск встроенного серера Tomcat.
 * Код из примера https://devcenter.heroku.com/articles/create-a-java-web-application-using-embedded-tomcat
 */
public class TomcatStartup {
	private static Tomcat tomcat;
	public static void start() {
		try {
			if (tomcat != null) {
				return;
			}
			tomcat = new Tomcat();
			String webappDirLocation = "webapp/";
			
			//The port that we should run on can be set into an environment variable
			//Look for that variable and default to 8080 if it isn't there.
			String webPort = System.getenv("MAI_HTTP_PORT");
			if(webPort == null || webPort.isEmpty()) {
				webPort = "8080";
			}
			
			tomcat.setPort(Integer.valueOf(webPort));
			
			File webappDir = new File("./" + webappDirLocation);
			webappDir.mkdirs();
			StandardContext ctx = (StandardContext) tomcat.addWebapp("/", webappDir.getAbsolutePath());
			System.out.println("configuring app with basedir: " + webappDir.getAbsolutePath());
			
			// Declare an alternative location for your "WEB-INF/classes" dir
			// Servlet 3.0 annotation will work
			File additionWebInfClasses = new File(".");
			WebResourceRoot resources = new StandardRoot(ctx);
			resources.addPreResources(new DirResourceSet(resources, "/WEB-INF/classes",
					additionWebInfClasses.getAbsolutePath(), "/"));
			ctx.setResources(resources);
			
			tomcat.start();
		} catch (ServletException | LifecycleException ex) {
			Logger.getLogger(TomcatStartup.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	public static void stop() {
		if (tomcat != null) {
			try {
				tomcat.stop();
			} catch (LifecycleException ex) {
				Logger.getLogger(TomcatStartup.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}
}