package com.lightspeedeps.util.common;


public class Log4jConfigurator {

	//This ensures singleton instance of configurator
	private final static Log4jConfigurator INSTANCE = new Log4jConfigurator();

	//private static final Log log = LogFactory.getLog(Log4jConfigurator.class);

	public static Log4jConfigurator getInstance() {
		return INSTANCE;
	}

	public void initialize(final String file) {
		try {
			//Create the watch service thread and start it.
			Log4jChangeWatcherService listener = new Log4jChangeWatcherService(file);

			//Start the thread
			new Thread(listener).start();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
