package farsight.logging;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

import com.softwareag.util.IDataMap;
import com.wm.app.b2b.server.ServiceException;
import com.wm.data.IData;

import farsight.logging.services.LoggingServices;

public final class log {

	final static log _instance = new log();

	static log _newInstance() {
		return new log();
	}

	static log _cast(Object o) {
		return (log) o;
	}

	static final String LOG_ROOT = "service";

	public static final void debug(IData pipeline) throws ServiceException {
		LoggingServices.logMessage(LOG_ROOT, Level.DEBUG, pipeline);
	}

	public static final void error(IData pipeline) throws ServiceException {
		LoggingServices.logMessage(LOG_ROOT, Level.ERROR, pipeline);
	}

	public static final void fatal(IData pipeline) throws ServiceException {
		LoggingServices.logMessage(LOG_ROOT, Level.FATAL, pipeline);
	}

	public static final void info(IData pipeline) throws ServiceException {
		LoggingServices.logMessage(LOG_ROOT, Level.INFO, pipeline);
	}

	public static final void trace(IData pipeline) throws ServiceException {
		LoggingServices.logMessage(LOG_ROOT, Level.TRACE, pipeline);
	}

	public static final void warn(IData pipeline) throws ServiceException {
		LoggingServices.logMessage(LOG_ROOT, Level.WARN, pipeline);
	}
	
	public static final void isEnabled(IData pipeline) throws ServiceException {
		IDataMap p = new IDataMap(pipeline);
		Logger logger = LoggingServices.getLogger(LOG_ROOT, p.getAsString("$caller"), p);
		Level level = LoggingServices.getLevel(p);
		p.put("isEnabled", String.valueOf(logger.isEnabled(level)));
	}
	
	public static final void getName(IData pipeline) throws ServiceException {
		IDataMap p = new IDataMap(pipeline);
		p.put("logger", LoggingServices.getLogger(LOG_ROOT, p.getAsString("$caller"), p).getName());
	}
	
	public static final void logMessage(IData pipeline) throws ServiceException {
		IDataMap p = new IDataMap(pipeline);
		LoggingServices.logMessage(p.getAsString("$domain", LOG_ROOT), null, pipeline);
	}

}
