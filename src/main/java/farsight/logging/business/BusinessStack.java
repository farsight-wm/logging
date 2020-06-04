package farsight.logging.business;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Stack;

import com.wm.data.IData;
import com.wm.lang.ns.NSService;

import farsight.logging.log4j2.ISMessage;
import farsight.logging.log4j2.ISMessage.Builder;
import farsight.utils.services.InvokeUtils;

// static and concrete very chaotic!
public class BusinessStack {

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
			// exception ??
		}

		@Override
		public BusinessService peek() {
			return null;
		}

		@Override
		public ISMessage appendBusinessData(Builder builder) {
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
		if (fStack.isEmpty())
			return; // nothing to do

		// TODO check that there is no garbage left from previous service calls
		// processed by this thread
	}

	public IData[] getIData() {
		IData[] result = new IData[fStack.size()];
		int p = 0;
		for (BusinessService service : fStack) {
			result[p++] = service.getIData();
		}
		return result;
	}

	public String getName() {
		return isEmpty() ? null : peek().service.getFullName();
	}

	public ISMessage appendBusinessData(ISMessage.Builder builder) {

		builder.appendMetadata("BusinessService", peek().service.getFullName(), true);
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
	 * Result may be a special instance EmptyStack. If create is true a new Stack is
	 * created for adding new Services. EmptyStack will only be returned if create
	 * is false.
	 * 
	 * @param create
	 * @return
	 */
	public static BusinessStack current(boolean create) {
		Object o = InvokeUtils.getPrivateData(PIPELINE_KEY);
		if (o instanceof BusinessStack) {
			return (BusinessStack) o;
		}
		if (create) {
			BusinessStack stack = new BusinessStack();
			InvokeUtils.setPrivateData(PIPELINE_KEY, stack);
			return stack;
		} else {
			return EMPTY_STACK;
		}
	}

}
