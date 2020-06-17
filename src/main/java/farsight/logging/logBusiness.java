package farsight.logging;

import java.util.LinkedHashMap;
import java.util.LinkedList;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

import com.softwareag.util.IDataMap;
import com.wm.app.b2b.server.ServiceException;
import com.wm.data.IData;
import com.wm.lang.ns.NSService;

import farsight.logging.business.BusinessService;
import farsight.logging.business.BusinessStack;
import farsight.logging.log4j2.ISMessage;
import farsight.logging.log4j2.ISMessage.Builder;
import farsight.utils.InvokeUtils;

public final class logBusiness {

	final static logBusiness _instance = new logBusiness();

	static logBusiness _newInstance() {
		return new logBusiness();
	}

	static logBusiness _cast(Object o) {
		return (logBusiness) o;
	}

	private static final String LOG_DOMAIN = "business";
	private static final String LOG_MSG_START = "BusinessService started";
	private static final String LOG_MSG_END = "BusinessService ended";

	public static final String P_KEY_CUSTOM_LOG = "customLog";
	public static final String P_KEY_OUT_BUSSINESS_IDS = "businessIDs";
	public static final String P_KEY_OUT_BUSSINESS_STACK = "businessServiceStack";
	public static final String P_KEY_OUT_BUSSINESS_SERVICE = "currentBusinesService";
	
	private static String getLoggerName(String businessServiceName) {
		return log.getLogDomain(LOG_DOMAIN, businessServiceName);
	}
	
	/*
	 * Get a logger from the current BusinessStack so that messages get logged by
	 * the logger of the running business service.
	 */
	private static Logger getLogger() {
		return LoggingFrontend.instance().getLogger(getLoggerName(BusinessStack.current().getName()));
	}

	private static void logMessage(Level level, IData pipeline) {
		IDataMap p = new IDataMap(pipeline);
		String caller = log.getCaller(p);
		Logger logger = getLogger();
		if (level == null)
			level = log.getLevel(p);
		if (logger.isEnabled(level)) {
			Builder builder = log.createMessageBuilder(caller, null, p);
			logger.log(level, BusinessStack.current().appendBusinessData(builder));
		}
	}

	private static void logMessage(Level level, String caller, String message) {
		Logger logger = getLogger();
		if (logger.isEnabled(level)) {
			Builder builder = ISMessage.builder(message, caller);
			logger.log(level, BusinessStack.current().appendBusinessData(builder));
		}
	}

	private static NSService getBusinessService(LinkedList<NSService> callStack, int level) {
		if (callStack == null || level < 1)
			return null;
		int pos = callStack.size() - level - 1;
		if (pos < 0)
			return null;
		return callStack.get(pos);
	}

	private static int getLevel(IDataMap p) {
		return p.getAsInteger(log.P_KEY_OVERRIDE_LEVEL, 0) + 1;
	}

	private static LinkedHashMap<String, String> getBusinessIDs(IDataMap p) {
		IDataMap ids = p.getAsIDataMap(P_KEY_OUT_BUSSINESS_IDS);
		if (ids == null || ids.size() == 0)
			return null;
		LinkedHashMap<String, String> result = new LinkedHashMap<>();
		for (String key : ids.keySet()) {
			String value = ids.getAsString(key);
			if (value != null)
				result.put(key, value);
		}
		return result;
	}

	private static String appendMessage(String base, String custom) {
		return custom == null ? base : base + "; " + custom;
	}

	private static String formatHumanReadableRuntime(long runtime) {
		long days = runtime / 86400000;
		runtime %= 86400000;
		long hours = runtime / 3600000;
		runtime %= 3600000;
		long minutes = runtime / 60000;
		runtime %= 60000;
		long seconds = runtime / 1000;
		long ms = runtime % 1000;

		if (days > 0)
			return String.format(" (%d days, %d hours, %d minutes)", days, hours, minutes);
		else if (hours > 0)
			return String.format(" (%d hours, %d minutes, %d seconds)", hours, minutes, seconds);
		else
			return String.format(" (%d minutes, %d.%03d seconds)", minutes, seconds, ms);
	}

	// java service implementations

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
		p.put(log.P_KEY_OUT_IS_ENABLED, String.valueOf(getLogger().isEnabled(log.getLevel(p))));
	}

	public static final void getName(IData pipeline) throws ServiceException {
		IDataMap p = new IDataMap(pipeline);
		p.put(log.P_KEY_OUT_IS_ENABLED, getLogger().getName());
	}

	public static final void businessServiceStart(IData pipeline) throws ServiceException {
		IDataMap p = new IDataMap(pipeline);
		NSService bs = getBusinessService(InvokeUtils.getCallStack(), getLevel(p));
		if (bs == null)
			return;

		BusinessStack stack = BusinessStack.current(true);
		stack.push(bs, getBusinessIDs(p));

		logMessage(Level.INFO, bs.getNSName().getFullName(),
				appendMessage(LOG_MSG_START, p.getAsString(P_KEY_CUSTOM_LOG)));
	}

	public static final void businessServiceEnd(IData pipeline) throws ServiceException {
		IDataMap p = new IDataMap(pipeline);

		BusinessStack stack = BusinessStack.current(true);
		LinkedList<NSService> callStack = InvokeUtils.getCallStack();

		BusinessService current = stack.peek();
		NSService self = getBusinessService(callStack, getLevel(p));

		if (current != null && self != null && current.service == self.getNSName()) {
			// correct business service (except in case of business service recursion (!) )
			long runtime = (System.currentTimeMillis() - current.startTime) / 1000;
			logMessage(Level.INFO, current.service.getFullName(),
					"runtimeMS=" + runtime + formatHumanReadableRuntime(runtime));
			logMessage(Level.INFO, current.service.getFullName(),
					appendMessage(LOG_MSG_END, p.getAsString(P_KEY_CUSTOM_LOG)));
			stack.pop();

		} else {
			String caller = log.getCaller(p);
			Logger logger = getLogger();
			if (current == null)
				logger.log(Level.ERROR,
						ISMessage.builder(
								"Called businesServiceEnd outside of business service (busibess stack is empty)",
								caller).build());
			else if (self == null)
				logger.log(Level.ERROR,
						ISMessage.builder("Cannot determine caller! (Wrong $level or $caller ?)", caller).build());
			else
				logger.log(Level.ERROR,
						ISMessage.builder(String.format(
								"tried to end business service that is not top of stack (service=%s top=%s)",
								self.getNSName(), current.service), caller).build());
		}
	}

	public static final void getCurrentBusinessService(IData pipeline) throws ServiceException {
		IDataMap p = new IDataMap(pipeline);
		BusinessService current = BusinessStack.current().peek();
		if (current != null) {
			p.put(P_KEY_OUT_BUSSINESS_SERVICE, current.getIData());
		}
	}

	public static final void getBusinessServiceStack(IData pipeline) throws ServiceException {
		IDataMap p = new IDataMap(pipeline);
		p.put(P_KEY_OUT_BUSSINESS_STACK, BusinessStack.current().getIData());
	}

}
