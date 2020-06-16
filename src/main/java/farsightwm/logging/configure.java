package farsightwm.logging;

import java.util.List;

import org.apache.logging.log4j.Level;

import com.softwareag.util.IDataMap;
import com.wm.app.b2b.server.ServiceException;
import com.wm.data.IData;

import farsight.logging.LoggingFrontend;
import farsight.logging.config.LogEntity;
import farsight.utils.config.ConfigurationStore;

public final class configure {

	final static configure _instance = new configure();

	static configure _newInstance() {
		return new configure();
	}

	static configure _cast(Object o) {
		return (configure) o;
	}

	public static final void changeLogLevel(IData pipeline) throws ServiceException {
		IDataMap p = new IDataMap(pipeline);

		String logger = p.getAsString("logger");
		Level level = getLevel(p, "level");

		if (logger == null)
			return;

		if (p.getAsBoolean("commitImmediatly", false)) {
			LoggingFrontend.instance().changeLogLevel(logger, level).commit();
		} else {
			LoggingFrontend.instance().changeLogLevel(logger, level);
		}

	}

	public static final void commitChanges(IData pipeline) throws ServiceException {
		LoggingFrontend.instance().commit();

	}

	public static final void initializeLogging(IData pipeline) throws ServiceException {
		IDataMap p = new IDataMap(pipeline);

		String domain = p.getAsString("domain");
		if (domain == null)
			throw new ServiceException("logging domain must be set");

		String nsPrefix = p.getAsString("nsPrefix");
		Level initialLevel = getLevel(p, "initialLevel");

		String logFile = p.getAsString("logFile");
		String pattern = p.getAsString("pattern");
		String logger = loggerID(domain, nsPrefix);

		LoggingFrontend lf = LoggingFrontend.instance();
		lf.checkInitialized();
		LogEntity entity = lf.getConfiguration().getLogEntity(logger);

		if (entity == null) {
			lf.setup(new LogEntity(logger, initialLevel, logFile, logFile == null ? null : initialLevel, pattern));
		} else {
			Level logLevel = entity.logLevel == null ? initialLevel : entity.logLevel;
			Level fileLevel = entity.fileLevel == null ? (logFile == null ? null : initialLevel) : entity.fileLevel;

			LogEntity changed = entity.createChanged(logger, logLevel, logFile, fileLevel, pattern);
			if (changed != entity)
				lf.setup(changed);
		}

	}

	public static final void listLoggers(IData pipeline) throws ServiceException {
		IDataMap p = new IDataMap(pipeline);

		List<LogEntity> list = LoggingFrontend.instance().getConfiguration().loggers.getLogEnities();

		IData[] result = new IData[list.size()];
		int pos = 0;
		for (LogEntity entity : list) {
			IDataMap item = new IDataMap();
			item.put("logger", entity.logger);
			if (entity.logLevel != null) {
				item.put("logLevel", entity.logLevel.name());
			}
			if (entity.file != null) {
				item.put("file", entity.file);
			}
			if (entity.fileLevel != null) {
				item.put("fileLevel", entity.fileLevel.name());
			}
			if (entity.pattern != null) {
				item.put("pattern", entity.pattern);
			}
			result[(pos++)] = item.getIData();
		}
		p.put("loggers", result);

	}

	public static final void reconfigure(IData pipeline) throws ServiceException {
		IDataMap p = new IDataMap(pipeline);
		LoggingFrontend.instance().configure(new ConfigurationStore().fill(p.getAsIData("logging"), 3));

	}

	public static final void removeLogger(IData pipeline) throws ServiceException {

		IDataMap p = new IDataMap(pipeline);
		String logger = p.getAsString("logger");
		if (logger == null)
			return;
		LoggingFrontend.instance().removeLogger(logger).commit();

	}

	private static String loggerID(String domain, String nsPrefix) {
		return nsPrefix == null ? domain : domain + "." + nsPrefix;
	}

	private static Level getLevel(IDataMap p, String key) {
		String str = p.getAsString(key);
		if (str == null)
			return null;
		return Level.toLevel(str);
	}

	public static final void dumpLog4J2Config(IData pipeline) throws ServiceException {
		IDataMap p = new IDataMap(pipeline);
		p.put("Log4J2ConfigXML",
				LoggingFrontend.instance().getConfiguration().getLog4JXMLConfiguration().replace("&#xd;", "\r"));
	}

	public static final void dumpRuntimeConfig(IData pipeline) throws ServiceException {
		IDataMap p = new IDataMap(pipeline);
		boolean writeToDisc = p.getAsBoolean("writeToDisc", false);

		p.put("RuntimeConfigXML", LoggingFrontend.instance().getConfiguration().encodeToString());

		if (writeToDisc) {
			LoggingFrontend.instance().writeRuntimeConfiguration();
		}
	}
}
