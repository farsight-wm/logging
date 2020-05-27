package farsight.logging.services;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

import com.softwareag.util.IDataMap;
import com.wm.app.b2b.server.InvokeState;
import com.wm.data.IData;
import com.wm.lang.ns.NSService;

import farsight.logging.LoggingFrontend;
import farsight.logging.graylog.GelfMessage;
import farsight.logging.graylog.GelfMessage.Builder;
import farsight.utils.services.InvokeUtils;

public class LoggingServices {
	
	
	public static final String P_KEY_MESSAGE = "message";
	public static final String P_KEY_LEVEL = "severity";
	public static final String P_KEY_DETAIL = "detail";
	public static final String P_KEY_OVERRIDE_LOGGER = "$logger";
	public static final String P_KEY_OVERRIDE_LEVEL = "$level";
	public static final String P_KEY_OVERRIDE_CALLER = "$caller";
	
	public static final String DEFAULT_LOG_ROOT = "service";
	public static final Level DEFAULT_LEVEL = Level.INFO;
	
	
	public static void logMessage(String domain, Level level, IData pipeline) {
		logMessage(domain, level, new IDataMap(pipeline));
	}
	
	public static void logMessage(String domain, Level level, IDataMap p) {
		String caller = getCaller(p);
		Logger logger = getLogger(domain, caller, p);
		if(level == null) level = getLevel(p);
		if(logger.isEnabled(level)) {
			logger.log(level, createMessage(caller, null, p));
		}
	}
	
	public static void logMessage(String loggerName, Level level, String message) {
		Logger logger = getLogger(loggerName);
		if(level == null) level = DEFAULT_LEVEL;
		logger.log(level, message);
	}
	
	public static GelfMessage.Builder createMessageBuilder(String caller, String message, IDataMap p) {
		Builder builder = GelfMessage.builder(message == null ? p.getAsString("messageText", "") : message, caller);
		
		builder.appendMetadata("contextID", getRootContextID(), true);
		builder.appendMetadata("rootService", getRootServiceName(), true);
		
		IDataMap details = p.getAsIDataMap("detail");
		if(details != null) {
			IDataMap error = details.getAsIDataMap("lastError");
			if(error != null) {
				builder.appendMetadata("lastError", error.getAsString("error"), false);
				builder.appendMetadata("failedService", error.getAsString("service"), false);
			}
			builder.appendMetadata(details.getAsIData("businessData"), false);
			builder.appendMetadata(details.getAsIData("extended"), true);
		}
		
		return builder;
	}
	
	public static GelfMessage createMessage(String caller, String message, IDataMap p) {
		return createMessageBuilder(caller, message, p).build();
	}

	public static Level getLevel(IDataMap p) {
		String levelString = p.getAsString(P_KEY_LEVEL);
		if(levelString == null)
			return DEFAULT_LEVEL;
		Level level = Level.getLevel(levelString);
		return level == null ? DEFAULT_LEVEL : level;
	}
	
	public static String getLoggerName(String caller, String domain) {
		String name = null;
		if(caller != null) {
			int pos = caller.indexOf(".service");
			if(pos == -1) {
				pos = caller.indexOf('.');
			}
			if(pos == -1) {
				pos = caller.indexOf(':');
			}
			name = caller.substring(0, pos);
		}
		if(domain == null) domain = DEFAULT_LOG_ROOT; 
		return name == null ? domain : (domain + "." + name);
	}
	
	public static Logger getLogger(String loggerName) {
		return LoggingFrontend.instance().getLogger(loggerName);
	}
	
	public static Logger getLogger(String caller, String domain) {
		return getLogger(getLoggerName(caller, domain));
	}
	
	public static Logger getLogger(String domain, String caller, IDataMap p) {
		// logger overridden? 
		String overrideLogger = p.getAsString(P_KEY_OVERRIDE_LOGGER);
		if(overrideLogger != null)
			return getLogger(overrideLogger);
		
		// caller overridden?
		String overrideCaller = p.getAsString(P_KEY_OVERRIDE_CALLER);
		if(overrideCaller != null)
			return getLogger(getLoggerName(overrideCaller, domain));
		
		// get caller!
		if(caller == null)
			caller = getCaller(p);

		return getLogger(caller, domain);
	}
	
	public static String getCaller(IDataMap p) {
		return getCaller(p.getAsInteger(P_KEY_OVERRIDE_LEVEL, 0) + 1);
	}

	private static String getCaller(int level) {
		NSService service = InvokeUtils.getCallingService(level);
		return service == null ? null : service.getNSName().getFullName();
	}
	
	
	// extended Data
	
	public static String getRootContextID() {
		try{
			//FIXME check if root service is a trigger! triggers have only one root ctx -> use sub ctx of main handler
			String[] contextStack = InvokeState.getCurrentState().getAuditRuntime().getContextStack();
			if(contextStack != null && contextStack.length > 0)
				return contextStack[0];
		} catch(Exception ex) {
			// ignore exception, just return no context Id
		}
		return null;
	}
	
	public static String getRootServiceName() {
		NSService root = InvokeUtils.getRootService();
		return root != null ? root.getNSName().getFullName() : null;
	}


}
