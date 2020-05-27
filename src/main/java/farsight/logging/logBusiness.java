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
import farsight.logging.graylog.GelfMessage;
import farsight.logging.services.LoggingServices;
import farsight.utils.services.InvokeUtils;

public final class logBusiness {

	final static logBusiness _instance = new logBusiness();

	static logBusiness _newInstance() {
		return new logBusiness();
	}

	static logBusiness _cast(Object o) {
		return (logBusiness) o;
	}

	private static final String LOG_MSG_START = "BusinessService started";
	private static final String LOG_MSG_END = "BusinessService ended";
	
	private static final String P_KEY_STACK_LEVEL = "$level";
	private static final String P_KEY_CUSTOM_LOG = "customLog";
	private static final String P_KEY_BUSSINESS_IDS = "businessIDs";



	public static final void debug(IData pipeline) throws ServiceException {
		BusinessStack.logMessage(Level.DEBUG, pipeline);
	}

	public static final void error(IData pipeline) throws ServiceException {
		BusinessStack.logMessage(Level.ERROR, pipeline);
	}

	public static final void fatal(IData pipeline) throws ServiceException {
		BusinessStack.logMessage(Level.FATAL, pipeline);
	}

	public static final void info(IData pipeline) throws ServiceException {
		BusinessStack.logMessage(Level.INFO, pipeline);
	}

	public static final void trace(IData pipeline) throws ServiceException {
		BusinessStack.logMessage(Level.TRACE, pipeline);
	}

	public static final void warn(IData pipeline) throws ServiceException {
		BusinessStack.logMessage(Level.WARN, pipeline);
	}
	
	public static final void isEnabled(IData pipeline) throws ServiceException {
		IDataMap p = new IDataMap(pipeline);
		Logger logger = BusinessStack.getLogger(p.getAsString("$caller"), p);
		Level level = LoggingServices.getLevel(p);
		p.put("isEnabled", String.valueOf(logger.isEnabled(level)));
	}
	
	public static final void getName(IData pipeline) throws ServiceException {
		IDataMap p = new IDataMap(pipeline);
		p.put("logger", BusinessStack.getLogger(p.getAsString("$caller"), p).getName());
	}
	
	public static final void logMessage(IData pipeline) throws ServiceException {
		BusinessStack.logMessage(null, pipeline);
	}
	
	public static final void businessServiceStart(IData pipeline) throws ServiceException {
		IDataMap p = new IDataMap(pipeline);
		
		BusinessStack stack = BusinessStack.current(true);
		LinkedList<NSService> callStack = InvokeUtils.getCallStack();

		stack.validate(callStack);
		NSService bs = getBusinessService(callStack, getLevel(p));
		stack.push(bs, getBusinessIDs(p));
		
		stack.logMessage(Level.INFO, bs.getNSName().getFullName(), appendMessage(LOG_MSG_START, p.getAsString(P_KEY_CUSTOM_LOG)));
	}
	
	public static final void businessServiceEnd(IData pipeline) throws ServiceException {
		IDataMap p = new IDataMap(pipeline);
		
		BusinessStack stack = BusinessStack.current(true);
		LinkedList<NSService> callStack = InvokeUtils.getCallStack();

		stack.validate(callStack);
		BusinessService current = stack.peek();
		NSService self = getBusinessService(callStack, getLevel(p));
		
		if(current != null && self != null && current.service == self.getNSName()) {
			//correct business service (except in case of business service recursion (!) )
			
			long runtime = (System.currentTimeMillis() - current.startTime) / 1000;
			stack.logMessage(Level.INFO, current.service.getFullName(), "runtimeMS=" + runtime + formatHumanReadableRuntime(runtime));
			stack.logMessage(Level.INFO, current.service.getFullName(), appendMessage(LOG_MSG_END, p.getAsString(P_KEY_CUSTOM_LOG)));
			stack.pop();
		} else {
			String caller = LoggingServices.getCaller(p);
			Logger logger = LoggingServices.getLogger(log.LOG_ROOT, caller, p);
			if(current == null)
				logger.log(Level.ERROR, GelfMessage.builder("Called businesServiceEnd outside of business service (busibess stack is empty)", caller).build());
			else if(self == null)
				logger.log(Level.ERROR, GelfMessage.builder("Cannot determine caller! (Wrong $level or $caller ?)", caller).build());
			else 
				logger.log(Level.ERROR, GelfMessage.builder(String.format("tried to end business service that is not top of stack (service=%s top=%s)", self.getNSName(), current.service), caller).build());
		}
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
		
		if(days > 0)
			return String.format(" (%d days, %d hours, %d minutes)", days, hours, minutes);
		else if(hours > 0)
			return String.format(" (%d hours, %d minutes, %d seconds)", hours, minutes, seconds);
		else
			return String.format(" (%d minutes, %d.%03d seconds)", minutes, seconds, ms);
	}

	public static final void getCurrentBusinessService(IData pipeline) throws ServiceException {
		IDataMap p = new IDataMap(pipeline);
		BusinessService current = BusinessStack.current().peek();
		if(current != null) {
			p.put("currentBusinesService", current.getIData());
		}
	}
	
	public static final void getBusinessServiceStack(IData pipeline) throws ServiceException {
		IDataMap p = new IDataMap(pipeline);
		p.put("businessServiceStack", BusinessStack.current().getIData());
	}
	
	// common utils

	private static int getLevel(IDataMap p) {
		return p.getAsInteger(P_KEY_STACK_LEVEL, 0) + 1;
	}
	
	private static LinkedHashMap<String, String> getBusinessIDs(IDataMap p) {
		IDataMap ids = p.getAsIDataMap(P_KEY_BUSSINESS_IDS);
		if(ids == null || ids.size() == 0)
			return null;
		LinkedHashMap<String, String> result = new LinkedHashMap<>();
		for(String key: ids.keySet()) {
			String value = ids.getAsString(key);
			if(value != null)
				result.put(key, value);
		}
		return result;
	}
	
//	private static void logMessage(BusinessStack stack, Level level, String message) {
//		stack.getLogger().log(level, message);
//	}
	
	private static String appendMessage(String base, String custom) {
		return custom == null ? base : base + "; " + custom;
	}
	
	private static NSService getBusinessService(LinkedList<NSService> callStack, int level) {
		if(callStack == null || level < 1)
			return null;
		int pos = callStack.size() - level - 1;
		if(pos < 0)
			return null;
		return callStack.get(pos);
	}
	

}
