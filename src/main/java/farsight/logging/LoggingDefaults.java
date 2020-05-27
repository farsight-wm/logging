package farsight.logging;

import org.apache.logging.log4j.Level;

public interface LoggingDefaults {
	
	public static final String LAYOUT_PATTERN = "%d %-5p [%c] - %m%n";
	public static final String BASE_PATH = "logs";
	public static final Level DEFAULT_LEVEL = Level.INFO;
	public static final Level ROOT_FILE_LEVEL = Level.WARN;
	public static final String ROOT_FILE = "root";
	public static final String MAX_LOGFILE_SIZE = "50MB";
	public static final String ROLL_PATTERN_SUFFIX = "-%d{yyyyMMdd}-%i";
	
	public static final String LOGFILE_EXTENSION = ".log";
	
}
