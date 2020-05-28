package example;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

import com.softwareag.util.IDataMap;
import com.wm.app.b2b.server.InvokeState;
import com.wm.app.b2b.server.ServiceException;
import com.wm.data.IData;
import com.wm.lang.ns.NSService;

import farsight.logging.ISMessage;
import farsight.logging.ISMessage.Builder;
import farsight.logging.LoggingDefaults;
import farsight.logging.LoggingFrontend;
import farsight.utils.services.InvokeUtils;

public final class log {

	final static log _instance = new log();

	static log _newInstance() {
		return new log();
	}

	static log _cast(Object o) {
		return (log) o;
	}
	
	// log implementation
	
	private static final String LOG_DOMAIN = "service";
	
	public static final String P_KEY_OUT_IS_ENABLED = "isEnabled";
	public static final String P_KEY_OUT_LOGGER = "logger";

	public static final String P_KEY_MESSAGE = "message";
	public static final String P_KEY_LEVEL = "severity";
	public static final String P_KEY_DETAIL = "detail";

	public static final String P_KEY_OVERRIDE_LOGGER = "$logger";
	public static final String P_KEY_OVERRIDE_LEVEL = "$level";
	public static final String P_KEY_OVERRIDE_CALLER = "$caller";
	public static final String P_KEY_OVERRIDE_DOMAIN = "$domain";

	
	/*
	 * Get name of calling service
	 */
	public static String getCaller(IDataMap p) {
		int level = p.getAsInteger(P_KEY_OVERRIDE_LEVEL, 0) + 1;
		
		NSService service = InvokeUtils.getCallingService(level);
		return service == null ? null : service.getNSName().getFullName();
	}
	
	/*
	 * Converts a service name into a logger name by searching the NSName for ".service"
	 * 
	 * e.g. A caller named "somePackage.services.pub.function:someService"
	 * would result in logger "<rootDomain>.somePackage"
	 * 
	 * If it cannot find ".service" in NSName it uses the whole interface name.
	 * 
	 * This method should be adapted to your IS structures.
	 */
	public static String getLogDomain(String domain, String caller) {
		String name = null;
		if (caller != null) {
			int pos = caller.indexOf(".service");
			if (pos == -1) {
				pos = caller.indexOf('.');
			}
			if (pos == -1) {
				pos = caller.indexOf(':');
			}
			name = caller.substring(0, pos);
		}
		return name == null ? domain : (domain + "." + name);
	}
	
	/*
	 * Get a logger based upon caller.
	 * 
	 */
	public static String getLoggerName(String domain, String caller, IDataMap p) {
		// logger overridden?
		String overrideLogger = p.getAsString(P_KEY_OVERRIDE_LOGGER);
		if (overrideLogger != null)
			return overrideLogger;

		// caller overridden?
		String overrideCaller = p.getAsString(P_KEY_OVERRIDE_CALLER);

		return getLogDomain(domain, overrideCaller == null ? caller : overrideCaller);
	}
	
	public static Level getLevel(IDataMap p) {
		String levelString = p.getAsString(P_KEY_LEVEL);
		if (levelString == null)
			return LoggingDefaults.DEFAULT_LEVEL;
		Level level = Level.getLevel(levelString);
		return level == null ? LoggingDefaults.DEFAULT_LEVEL : level;
	}
	
	// What should be logged?
	
	public static Builder createMessageBuilder(String caller, String message, IDataMap p) {
		Builder builder = ISMessage.builder(message == null ? p.getAsString("messageText", "") : message, caller);

		builder.appendMetadata("contextID", getRootContextID(), true);
		builder.appendMetadata("rootService", getRootServiceName(), true);

		IDataMap details = p.getAsIDataMap("detail");
		if (details != null) {
			IDataMap error = details.getAsIDataMap("lastError");
			if (error != null) {
				builder.appendMetadata("lastError", error.getAsString("error"), false);
				builder.appendMetadata("failedService", error.getAsString("service"), false);
			}
			builder.appendMetadata(details.getAsIData("businessData"), false);
			builder.appendMetadata(details.getAsIData("extended"), true);
		}

		return builder;
	}

	public static ISMessage createMessage(String caller, String message, IDataMap p) {
		return createMessageBuilder(caller, message, p).build();
	}
	
	// extended Data

	public static String getRootContextID() {
		try {
			// FIXME check if root service is a trigger! triggers have only one root ctx ->
			// use sub ctx of main handler
			String[] contextStack = InvokeState.getCurrentState().getAuditRuntime().getContextStack();
			if (contextStack != null && contextStack.length > 0)
				return contextStack[0];
		} catch (Exception ex) {
			// ignore exception, just return no context Id
		}
		return null;
	}

	public static String getRootServiceName() {
		NSService root = InvokeUtils.getRootService();
		return root != null ? root.getNSName().getFullName() : null;
	}
	
	private static String getDomain(IDataMap p) {
		return p.getAsString(P_KEY_OVERRIDE_DOMAIN, LOG_DOMAIN);
	}
	
	private static Logger getLogger(String caller, IDataMap p) {
		return LoggingFrontend.instance().getLogger(getLoggerName(getDomain(p), caller, p));
	}

	private static void logMessage(Level level, IData pipeline) {
		IDataMap p = new IDataMap(pipeline);
		String caller = getCaller(p);
		Logger logger = getLogger(caller, p);
		if (level == null)
			level = getLevel(p);
		if (logger.isEnabled(level)) {
			logger.log(level, createMessage(caller, null, p));
		}
	}
	
	// === Java Service Implementations ===
	
	public static final void debug(IData pipeline) throws ServiceException {
		logMessage(Level.DEBUG, pipeline);
	}

	public static final void error(IData pipeline) throws ServiceException {
		logMessage(Level.ERROR, pipeline);
	}

	public static final void fatal(IData pipeline) throws ServiceException {
		logMessage(Level.FATAL, pipeline);
	}

	public static final void info(IData pipeline) throws ServiceException {
		logMessage(Level.INFO, pipeline);
	}

	public static final void trace(IData pipeline) throws ServiceException {
		logMessage(Level.TRACE, pipeline);
	}

	public static final void warn(IData pipeline) throws ServiceException {
		logMessage(Level.WARN, pipeline);
	}
	
	public static final void logMessage(IData pipeline) throws ServiceException {
		logMessage(null, pipeline);
	}
	
	public static final void isEnabled(IData pipeline) throws ServiceException {
		IDataMap p = new IDataMap(pipeline);
		p.put(P_KEY_OUT_IS_ENABLED, 
				String.valueOf(getLogger(getCaller(p), p).isEnabled(getLevel(p))));
	}
	
	public static final void getName(IData pipeline) throws ServiceException {
		IDataMap p = new IDataMap(pipeline);
		p.put(P_KEY_OUT_LOGGER, getLoggerName(getDomain(p), getCaller(p), p));
	}
	
}
