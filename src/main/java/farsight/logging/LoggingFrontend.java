package farsight.logging;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;

import farsight.logging.config.LogEntity;
import farsight.logging.config.RuntimeConf;
import farsight.utils.config.Configuration;

/*
 * Some Hints/Notes:
 * 
 * Currently Logger are bound to the ClassLoader of the package running this code.
 * Logger are unloaded with this packages ClassLoader.
 * 
 * CHANGE: Not using LogManger or ContextFactory anymore.
 * Instead creating an instance of LogContext on our own.
 * -> Check how IS reacts on unloads w/o proper shutdown?!
 *  -> Are files rolled properly?!
 *  
 * Why did I do this?
 * -> wm 10.5 has switched to Log4j2 itself and their usage of it is incompatible to this library (as it was before the change!)
 * -> Needed was a separate LoggingContext that can be managed programatically.
 * -> wm 10.5 uses BasicContextSelector so no chance of providing a separate context there
 *   -> could use ClassLoaderSelector
 *     -> downside: but that would need some investigation on how IS uses Log4j and weather this is working properly.
 *   -> could implement own Selector (Basic with two contexts) one for IS one for this library.
 *     -> downside: if SAG chooses to change Selector this would need adaption here 
 * 
 * 
 * Current implementations downsides
 * - Runtime Configuration changes are NOT maintained if the logger package
 *   reloads that could cause LogEvents to be lost.
 *    -> Solution may be to dump each configuration change to runtime config
 *       file. Upon reload parse this configuration first.
 * 
 * 
 *
 */
public class LoggingFrontend {

	/**
	 * Implementation of thread-save lazy loading singleton pattern.
	 */
	private static class LoggingFrontendInstanceHolder {
		private static final LoggingFrontend INSTANCE = new LoggingFrontend();
	}

	/**
	 * Returns a singleton instance of {@link LoggingFrontend}.
	 * 
	 * Notice: Hence the class constructor is not private you may instantiate
	 * other instances of this class. This is especially useful for unit tests!
	 * 
	 * @return Singleton Instance of {@link LoggingFrontend}
	 */
	public static LoggingFrontend instance() {
		return LoggingFrontendInstanceHolder.INSTANCE;
	}

	protected static String DEFAULT_RUNTIME_CONFIGURATION_PATH = "config/esbLoggingRuntimeConfig.xml";
	
	private HashMap<String, Logger> loggerMap = new HashMap<>();
	private boolean initialized = false;
	private ReentrantLock lock = new ReentrantLock();
	private RuntimeConf configuration = null;
	private LoggerContext loggingContext;
	private final String configurationFilePath;
	
	
	/* constructors */
	
	public LoggingFrontend() {
		this(DEFAULT_RUNTIME_CONFIGURATION_PATH);
	}
	
	public LoggingFrontend(String configurationFilePath) {
		this.configurationFilePath = configurationFilePath;
	}
	
	public LoggingFrontend(RuntimeConf conf) {
		this(conf, DEFAULT_RUNTIME_CONFIGURATION_PATH);
	}

	public LoggingFrontend(RuntimeConf conf, String configurationFilePath) {
		//creates Frontend initialized with conf
		this.configurationFilePath = configurationFilePath;
		this.configuration = conf;
		initializeLoggingContext();
		this.initialized = true;
	}


	/* initialization and synchronization */

	public LoggingFrontend configure(Configuration setup) {
		if(!initialized)
			autoInitialize(setup);
		else if (setup != null)
			configuration.reconfigure(setup);
		commit();
		return this;
	}
	
	public boolean configurationChanged() {
		return configuration == null ? false : configuration.hasChanged();
	}

	public void checkInitialized() {
		if (initialized)
			return;
		autoInitialize(null);
	}
	
	private void initializeLoggingContext() {
		loggingContext = new LoggerContext("ESB_Framework");
		loggingContext.start(configuration.createLog4JConfiguration());
		configuration.clearChanged();	
	}

	private void autoInitialize(Configuration setup) {
		try {
			lock.lock();
			if (!initialized) {
				try {
					configuration = restoreRuntimeConfig();
					boolean restoreFailed = configuration == null;
					
					if(restoreFailed) {
						if(setup != null) {
							configuration = createRuntimeConfigFrom(setup);
							restoreFailed = false;
						} else {
							configuration = setupMinimalLogging();
						}
					} else if(setup != null) {
						// if a new setup was given... adapt
						configuration.reconfigure(setup);
					}
					
					initializeLoggingContext();

					if(restoreFailed) {
						loggingContext.getRootLogger().warn("No runtime config found! Please fix configuration and restart logging.");
					}
					
					configuration.clearChanged();
				} catch (Exception e) {
					System.err.println("Error initializing ESB_Framework:Logging");
					e.printStackTrace();
					
					configuration = setupMinimalLogging();
					initializeLoggingContext();
					
					Logger logger;
					try {
						logger = loggingContext.getRootLogger();
					} catch(Exception e2) {
						logger = LogManager.getRootLogger();
					}
					
					logger.error("Could not initialize logging properly! Please fix configuration and restart logging.", e);
				}
			}
		} finally {
			lock.unlock();
			this.initialized = true;
		}
	}

	private RuntimeConf createRuntimeConfigFrom(Configuration setup) {
		return RuntimeConf.createFromSetup(setup);
	}

	private RuntimeConf setupMinimalLogging() {
		return RuntimeConf.createFallbackSetup();
	}

	private RuntimeConf restoreRuntimeConfig() throws Exception {
		File confFile = new File(configurationFilePath);
		if (confFile.exists() && confFile.canRead()) {
			return RuntimeConf.decodeFrom(confFile);
		}
		return null;
	}
	
	/* public API */
	
	public Logger getLogger(String logger) {
		Logger loggerCached = loggerMap.get(logger);
		if (loggerCached == null) {
			checkInitialized();
			loggerCached = loggingContext.getLogger(logger);
			loggerMap.put(logger, loggerCached);
		}
		return loggerCached;
	}
	
	public LoggerContext getContext() {
		return loggingContext;
	}

	public LoggingFrontend setupFileAppender(String logger, String filename, Level appenderLevel, String pattern) {
		checkInitialized();
		configuration.insertFileAppender(logger, filename, appenderLevel, pattern);
		
		return this;
	}

	public LoggingFrontend setupLogger(String logger, Level level) {
		checkInitialized();
		if(level == null)
			level = Level.OFF;
		
		configuration.setupLogger(logger, level);
		
		return this;
	}
	
	public LoggingFrontend setup(LogEntity entity) {
		configuration.setup(entity);
		return this;
	}
	
	public LoggingFrontend removeAppender(String logger) {
		configuration.removeAppender(logger);
		return this;
	}
	
	public LoggingFrontend removeLogger(String logger) {
		configuration.removeLogger(logger);
		return this;
	}

	public LoggingFrontend commit() {
		if (configuration.hasChanged()) {
			try {
				lock.lock();
				if (configuration.hasChanged()) {
					loggingContext.setConfiguration(configuration.createLog4JConfiguration());
					writeRuntimeConfiguration();
					configuration.clearChanged();
				}
			} finally {
				lock.unlock();
			}
		}
		return this;
	}

	public void writeRuntimeConfiguration() {
		File confFile = new File(configurationFilePath);
		try (OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(confFile), StandardCharsets.UTF_8)) {
			configuration.encodeTo(fw);
		} catch (IOException | XMLStreamException | FactoryConfigurationError e) {
			loggingContext.getRootLogger().error("Cannot write RuntimeConfiguration file: {}", e, confFile.toString());
		}
	}
	
	public RuntimeConf getConfiguration() {
		return configuration;
	}
	
	public void shutdown() {
		loggerMap.clear();
		loggingContext.terminate();
		initialized = false;
	}

	/**
	 * Changes the logLevel of an existing logger
	 * @param logger
	 * @param level
	 * @return 
	 */
	public LoggingFrontend changeLogLevel(String logger, Level level) {
		LogEntity entity = configuration.loggers.get(logger);
		if(entity == null || entity.logLevel == level)
			return this;
		
		setup(LogEntity.builder(entity).setLogLevel(level).build());
		
		return this;
	}
	
}