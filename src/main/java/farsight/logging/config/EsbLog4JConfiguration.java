package farsight.logging.config;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.Component;
import org.apache.logging.log4j.core.config.builder.api.ComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.LoggableComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.LoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.RootLoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.config.builder.impl.DefaultConfigurationBuilder;

import farsight.logging.LoggingDefaults;

public class EsbLog4JConfiguration extends BuiltConfiguration {
	
	public static class EsbLog4JConfigurationBuilder extends DefaultConfigurationBuilder<EsbLog4JConfiguration> {
		
		protected final RuntimeConf conf;
		
		//cache
		private final String defaultPattern;
		private final String basePath;
		private final String rollSuffix;
		private final String fileSize;
		private final Level grayLogLevel;
		
		private final String graylogID = "$.graylog";
		private final String graylogSource;
		
		private static String formatPath(String input) {
			if(input == null || "".equals(input) || ".".equals(input))
				return "";
			input = input.replace('\\', '/');
			return input.endsWith("/") ? input : input + "/";
		}
		

		public EsbLog4JConfigurationBuilder(RuntimeConf confRoot) {
			super(EsbLog4JConfiguration.class);
			this.conf = confRoot;
			
			defaultPattern = confRoot.getConfig("default.pattern", LoggingDefaults.LAYOUT_PATTERN);
			basePath = formatPath(confRoot.getConfig("output.basePath", LoggingDefaults.BASE_PATH));
			rollSuffix = confRoot.getConfig("fileAppender.filePattern", LoggingDefaults.ROLL_PATTERN_SUFFIX);
			fileSize = confRoot.getConfig("fileAppender.maxFileSize", LoggingDefaults.MAX_LOGFILE_SIZE);
			grayLogLevel = conf.getConfig("graylog.level", LoggingDefaults.DEFAULT_LEVEL);
			
			String source = conf.getConfig("graylog.source");
			if(source == null) {
				try {
					source = InetAddress.getLocalHost().getHostName().toUpperCase();
				} catch(UnknownHostException e) {
					source = "unknown";
				}
			}
			graylogSource = source;
			
		}
		
		protected <T extends ComponentBuilder<?>> T setLevel(T builder, Level level) {
			if(level != null)
				builder.addAttribute("level", level);
			return builder;
		}
		
		private void addFileAppender(LogEntity entity, String appenderID) {
			AppenderComponentBuilder appender = newAppender(appenderID, "RollingFile");
			
			String name = entity.file;
			if(name.endsWith(LoggingDefaults.LOGFILE_EXTENSION))
				name = name.substring(0, name.length() - LoggingDefaults.LOGFILE_EXTENSION.length());
			
			appender.addAttribute("fileName", basePath + name + LoggingDefaults.LOGFILE_EXTENSION);
			appender.addAttribute("filePattern",  basePath + name + rollSuffix + LoggingDefaults.LOGFILE_EXTENSION);
			appender.addAttribute("ignoreExceptions", "true");
			
			appender.add(newLayout("PatternLayout").addAttribute("pattern", entity.pattern != null ? entity.pattern : defaultPattern));
			appender.addComponent(newComponent("Policies").
					addComponent(newComponent("OnStartupTriggeringPolicy")).
					addComponent(newComponent("SizeBasedTriggeringPolicy").addAttribute("size", fileSize)).
					addComponent(newComponent("TimeBasedTriggeringPolicy")));
			
			add(appender);
		}
		
		protected EsbLog4JConfigurationBuilder setup() {
			//setPackages("esb.framework.logging.log4jPlugin");
			
			//Graylog
			boolean useGrayLog = conf.isSet("graylog.host");
			if(useGrayLog)
				setupGraylogAppender();

			//Root
			LogEntity root = conf.loggers.getRoot();
			if(root != null) {
				RootLoggerComponentBuilder rootBuilder = newRootLogger(root.logLevel);
				if(root.file != null) {
					addFileAppender(root, "$.file");
					rootBuilder.add(setLevel(newAppenderRef("$.file"), root.fileLevel));
				}
				if(useGrayLog) 
					addGraylogRef(rootBuilder, grayLogLevel);
					
				add(rootBuilder);
			}

			//LogEntities
			for(LogEntity entry: conf.loggers.getLogEnities()) {
				LoggerComponentBuilder logger = newLogger(entry.logger);
				setLevel(logger, entry.logLevel);
				if(entry.file != null) {
					addFileAppender(entry, entry.logger + ".file");
					logger.addAttribute("additivity", false);
					logger.add(setLevel(newAppenderRef(entry.logger + ".file"), entry.fileLevel));
					if(useGrayLog)
						addGraylogRef(logger, grayLogLevel);
				}
				add(logger);
			}
			
			return this;
		}

		
		
		private <T extends LoggableComponentBuilder<T>> void addGraylogRef(T logger, Level grayLogLevel) {
			logger.add(newAppenderRef(graylogID).addAttribute("level", grayLogLevel));
		}

		private void setupGraylogAppender() {
			add(newAppender(graylogID, "Socket")
					.addAttribute("host", conf.getConfig("graylog.host"))
					.addAttribute("port", conf.getConfig("graylog.port"))
					.addAttribute("protocol", "TCP")
					.add(newLayout("EsbGelfLayout").addAttribute("host", graylogSource))
				);			
		}		
		
	}
	
	public static EsbLog4JConfiguration createFrom(RuntimeConf conf) {
		return createBuilderFrom(conf).build();
	}
	
	public static ConfigurationBuilder<EsbLog4JConfiguration> createBuilderFrom(RuntimeConf conf) {
		return new EsbLog4JConfigurationBuilder(conf).setup();
	}

	public EsbLog4JConfiguration(LoggerContext loggerContext, ConfigurationSource source, Component rootComponent) {
		super(loggerContext, source, rootComponent);
	}
}
