package nl.defensie.adacta.utils;

import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;


public class SyncLogger {

	private static Logger synclogger = null;
	private static String syncFileName = "sync-batch.log";

	public static void info(String log) {
		logger(Level.INFO);
		synclogger.info(log);
	}
	public static void trace(String log) {
		logger(Level.TRACE);
		synclogger.trace(log);
	}
	public static void error(String log, Throwable t) {
		logger(Level.ERROR);
		String m = t.getMessage();
		if (m==null && t.getCause() != null){
			m = t.getCause().getMessage();
		if (m==null && t.getMessage() != null){
			m = t.getMessage();
		}
		}
		synclogger.error(log+" "+m);
	}
	public static void debug(String log) {
		logger(Level.DEBUG);
		synclogger.debug(log);
	}
	public static void reset(){
		synclogger = null;
	}

	public static void setLoggerPath(String path) {
		syncFileName = path;
	}

	/**
	 * Create the custom batch logger.
	 * 
	 * @return Logger the logger
	 */
	private static Logger logger(Level level) {
		if (synclogger == null) {
			synclogger = Logger.getLogger(syncFileName);
			synclogger.setLevel(level);
			try {
				PatternLayout layout = new PatternLayout();
				String conversionPattern = "[%p] %d %c %M - %m%n";
				layout.setConversionPattern(conversionPattern);
				DailyRollingFileAppender appender = new DailyRollingFileAppender(layout, syncFileName, "'.'yyyy-MM-dd");
				synclogger.removeAllAppenders();
				synclogger.addAppender(appender);
				synclogger.setAdditivity(false);		
			} catch (Exception e) {
				synclogger.error(e);
			}
		}
		return synclogger;
	}
}