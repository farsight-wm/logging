package farsight.logging.business;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

import com.wm.data.IData;
import com.wm.lang.ns.NSName;
import com.wm.lang.ns.NSService;

import farsight.utils.idata.DataBuilder;

public class BusinessService {
	
	public final NSName service;
	public final String packageName;
	public final String contextID;
	public final long startTime = System.currentTimeMillis();
	public final LinkedHashMap<String, String> businessIDs;
	
	protected BusinessService(NSService service, LinkedHashMap<String, String> businessIDs) {
		this.service = service.getNSName();
		this.packageName = service.getPackage().getName();
		this.contextID = "GET_CONTEXT!";
		this.businessIDs = businessIDs;
	}

	public static BusinessService create(NSService businessService, LinkedHashMap<String, String> businessIDs) {
		return new BusinessService(businessService, businessIDs);
	}

	public IData getIData() {
		return DataBuilder.create()
			.put("service", service.getFullName())
			.put("package", packageName)
			.put("contextID", contextID)
			.put("businessIDs", getBusinessIDs())
			.build();
	}
	
	private IData getBusinessIDs() {
		DataBuilder builder = DataBuilder.create();
		for(Entry<String, String> entry: businessIDs.entrySet())
			builder.put(entry.getKey(), entry.getValue());
		return builder.build();
	}
	
}