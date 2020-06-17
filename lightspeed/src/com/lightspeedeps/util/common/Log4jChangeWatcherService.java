package com.lightspeedeps.util.common;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
//import org.apache.log4j.xml.DOMConfigurator;
import org.apache.log4j.PropertyConfigurator;

public class Log4jChangeWatcherService implements Runnable {

	//private String configFileName = null;
	private String fullFilePath = null;
	private static final Log log = LogFactory.getLog(Log4jConfigurator.class);

	public Log4jChangeWatcherService(final String filePath) {
		this.fullFilePath = filePath;
	}

	//This method will be called each time the log4j configuration is changed
	public void configurationChanged(final String file)
	{
		System.out.println("Log4j configuration file changed. Reloading logging levels !!");
		PropertyConfigurator.configure(file);
	}

	@Override
	public void run() {
		try {
			log.error("");
			register(this.fullFilePath);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void register(final String file) throws IOException {
		configurationChanged(file);
		log.error("<<< THREAD INTERUPPTED >>>");
		Thread.currentThread().interrupt();
	}

}