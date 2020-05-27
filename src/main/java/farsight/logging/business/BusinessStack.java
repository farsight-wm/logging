package farsight.logging.business;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Stack;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

import com.softwareag.util.IDataMap;
import com.wm.data.IData;
import com.wm.lang.ns.NSService;

import farsight.logging.graylog.GelfMessage;
import farsight.logging.graylog.GelfMessage.Builder;
import farsight.logging.services.LoggingServices;
import farsight.utils.services.InvokeUtils;



// static and concrete very chaotic!
public class BusinessStack {
	
	protected static final String BUSINESS_LOG_DOMAIN = "business";
	private static final String PIPELINE_KEY = "BusinessStack";
	
	private static final BusinessStack EMPTY_STACK = new BusinessStack() {
		
		@Override
		public boolean isEmpty() {
			return true;
		}
		
		@Override
		public BusinessService pop() {
			return null;
		}	
		
		@Override
		public void push(NSService businessService, LinkedHashMap<String, String> businessIDs) {
			//exception ??
		}
		
		@Override
		public BusinessService peek() {
			return null;
		}
		
		@Override
		public Logger getLogger(String domain, String caller) {
			return LoggingServices.getLogger(caller, domain);
		};
		
		@Override
		protected GelfMessage appendBusinessData(Builder builder) {
			return builder.build(); // no extra data
		};
	};
	
	private Stack<BusinessService> fStack = new Stack<>();
	
	private BusinessStack() {
	}
	
	// stack modifier / access
	
	public BusinessService pop() {
		return fStack.pop();
	}

	public void push(NSService businessService, LinkedHashMap<String, String> businessIDs) {
		fStack.push(BusinessService.create(businessService, businessIDs));
	}
	
	public BusinessService peek() {
		return fStack.isEmpty() ? null : fStack.peek();
	}
	
	public boolean isEmpty() {
		return fStack.isEmpty();
	}

	public void validate(LinkedList<NSService> callStack) {
		if(fStack.isEmpty())
			return; //nothing to do
		
		//TODO implement!
	}
	

	
	// REVIEW
	
	public String getContextID() {
		return null;//esb.framework.logging.metadata.getRootContextID();
	}
	
	public IData[] getIData() {
		IData[] result = new IData[fStack.size()];
		int p = 0;
		for(BusinessService service: fStack) {
			result[p++] = service.getIData();
		}
		return result;
	}

	// implementation
	



	// log implementation
	
	public Logger getLogger(String domain, String caller) {
		if(isEmpty())
			return EMPTY_STACK.getLogger(domain, caller);
		return LoggingServices.getLogger(peek().service.getFullName(), domain);
	}
	
	protected GelfMessage appendBusinessData(GelfMessage.Builder builder) {
		
		builder.appendMetadata("BusinessService", peek().service.getFullName(), true);
		builder.appendMetadata("BusinessStack", "TODO", true);
		builder.appendMetadata(peek().businessIDs, true);
		
		return builder.build();
	}

	// static API
	
	public static BusinessStack current() {
		return current(false);
	}
	
	/**
	 * Returns the current BusinessStack
	 * 
	 * Result may be a special instance EmptyStack.
	 * If create is true a new Stack is created for adding new Services. EmptyStack will only be returned if create is false.
	 * 
	 * @param create
	 * @return
	 */
	public static BusinessStack current(boolean create) {
		Object o = InvokeUtils.getPrivateData(PIPELINE_KEY);
		if(o instanceof BusinessStack) {
			return (BusinessStack) o;
		}
		if(create) {
			BusinessStack stack = new BusinessStack();
			InvokeUtils.setPrivateData(PIPELINE_KEY, stack);
			return stack;
		} else {
			return EMPTY_STACK;
		}
	}
	
	// static Logging API (like LoggingService) 

	public static void logMessage(Level level, IData pipeline) {
		logMessage(level, new IDataMap(pipeline));
	}
	
	
	//FIXME multiple calls to current()
	public static void logMessage(Level level, IDataMap p) {
		String caller = LoggingServices.getCaller(p);
		Logger logger = getLogger(caller, p);
		if(level == null) level = LoggingServices.getLevel(p);
		if(logger.isEnabled(level)) {
			Builder builder = LoggingServices.createMessageBuilder(caller, null, p);
			logger.log(level, current().appendBusinessData(builder));
		}
	}
	
	public static Logger getLogger(String caller, IDataMap p) {
		// logger overridden? 
		String overrideLogger = p.getAsString(LoggingServices.P_KEY_OVERRIDE_LOGGER);
		if(overrideLogger != null)
			return LoggingServices.getLogger(overrideLogger);
		
		// caller overridden?
		caller = p.getAsString(LoggingServices.P_KEY_OVERRIDE_CALLER, caller);
		if(caller == null)
			caller = LoggingServices.getCaller(p);
		
		return current().getLogger(BUSINESS_LOG_DOMAIN, caller);
	}

	public void logMessage(Level level, String caller, String message) {
		Logger logger = current().getLogger(BUSINESS_LOG_DOMAIN, caller);
		if(level == null)
			level = Level.INFO;
		logger.log(level, appendBusinessData(GelfMessage.builder(message, caller)));
	}
	




	
	
	

}
