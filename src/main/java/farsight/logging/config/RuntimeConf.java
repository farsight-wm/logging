package farsight.logging.config;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.logging.log4j.Level;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import farsight.logging.LoggingDefaults;
import farsight.logging.xmlUtils.IndentingXMLWriter;
import farsight.utils.config.Configuration;
import farsight.utils.config.xml.AbstractXMLCodableComponent;
import farsight.utils.config.xml.XMLCodableComponentDecoder;
import farsight.utils.config.xml.XMLCodableKeyValueComponent;


public class RuntimeConf extends AbstractXMLCodableComponent<RuntimeConf> {
	
	public final XMLCodableKeyValueComponent config;
	public final LoggersSection loggers;
	private boolean hasChanged = false;
	
	/* Constructors and Builder */

	public RuntimeConf(XMLCodableKeyValueComponent config, LoggersSection loggers) {
		this.config = config;
		this.loggers = loggers;
	}
	
	public RuntimeConf() {
		this.config = new XMLCodableKeyValueComponent("config");
		this.loggers = new LoggersSection();
	}

	private RuntimeConf(Configuration setup) {
		this.config = new XMLCodableKeyValueComponent("config", setup);
		this.loggers = new LoggersSection();
	}
	
	public static RuntimeConf createFallbackSetup() {
		RuntimeConf fallback = new RuntimeConf();
		fallback.setConfig("graylog.enable", "false");
		fallback.loggers.setRoot(new LogEntity(null, Level.WARN, "fallback.log", Level.WARN, null));
		return fallback;
	}
	
	public static RuntimeConf createFromSetup(Configuration setup) {
		RuntimeConf conf = new RuntimeConf(setup);
		conf.setupDefaults();
		conf.setupLoggers();
		conf.clearChanged();
		return conf;
	}

	private void setupLoggers() {
		LogEntity root = new LogEntity(null,
				getConfig("default.level", LoggingDefaults.DEFAULT_LEVEL),
				getConfig("root.file", LoggingDefaults.ROOT_FILE),
				getConfig("root.level", LoggingDefaults.ROOT_FILE_LEVEL),
				null);
		this.loggers.setRoot(root);
	}

	private void setupDefaults() {
		if(!config.containsKey("graylog.host"))
			setConfig("graylog.enable", "false");
		
		setDefault("default.level", LoggingDefaults.DEFAULT_LEVEL.name());
		setDefault("default.pattern", LoggingDefaults.LAYOUT_PATTERN);

		setDefault("root.file", LoggingDefaults.ROOT_FILE);
		setDefault("root.level", LoggingDefaults.ROOT_FILE_LEVEL.name());
		
		setDefault("fileAppender.maxFileSize", LoggingDefaults.MAX_LOGFILE_SIZE);
		setDefault("fileAppender.rollPattern", LoggingDefaults.ROLL_PATTERN_SUFFIX);
		
		setDefault("ouput.basePath", LoggingDefaults.BASE_PATH);
	}
	
	/* de/serialization */

	@Override
	public void encodeTo(XMLStreamWriter w) throws XMLStreamException {
		w.writeStartDocument();
		w.writeStartElement("logging"); //root
		
		//sections
		config.encodeTo(w);
		loggers.encodeTo(w);
		
		w.writeEndElement(); // end root
		w.writeEndDocument();
	}

	static public XMLCodableComponentDecoder<RuntimeConf> getDecoder() {
		return new XMLCodableComponentDecoder<RuntimeConf>() {
			
			@Override
			public RuntimeConf decodeFrom(Node element) {
				Node child = null;
				
				XMLCodableKeyValueComponent config;
				LoggersSection loggers;
				
				//decode store
				child = firstChild(element, "config");
				if(child == null) {
					config = new XMLCodableKeyValueComponent("config");
				} else {
					config = XMLCodableKeyValueComponent.getDecoder().decodeFrom(child);
				}
				
				//decode loggers
				child = firstChild(element, "loggers");
				if(child == null) {
					loggers = new LoggersSection();
				} else {
					loggers = LoggersSection.getDecoder().decodeFrom(child);
				}
				
				return new RuntimeConf(config, loggers);
			}
		};
	}
	
	// helpers
	
	public static RuntimeConf decodeFrom(File file) throws ParserConfigurationException, SAXException, IOException {
		final DocumentBuilder documentBuilder = newDocumentBuilder();
		final Document document = documentBuilder.parse(file);
        final Element rootElement = document.getDocumentElement();
        return getDecoder().decodeFrom(rootElement);
	}
	
    static private DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setValidating(false);
		factory.setExpandEntityReferences(false);
		factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
		factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
		factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		return factory.newDocumentBuilder();
    }
    
    public void encodeTo(Writer writer) throws XMLStreamException, FactoryConfigurationError {
    	XMLStreamWriter xmlWriter = new IndentingXMLWriter(XMLOutputFactory.newInstance().createXMLStreamWriter(writer));
    	encodeTo(xmlWriter);
    }
    
    public String encodeToString() {
    	try (StringWriter sw = new StringWriter()) {
    		encodeTo(sw);
    		return sw.toString();
    	} catch (Exception e) {
    		throw new RuntimeException("Could not generate XML output", e);
		}
    }
    
    /* Configuration API */

	public EsbLog4JConfiguration createLog4JConfiguration() {
		return EsbLog4JConfiguration.createFrom(this);
	}
	
	public String getLog4JXMLConfiguration() {
		return EsbLog4JConfiguration.createBuilderFrom(this).toXmlConfiguration();
	}

	
	/* conf getters / setters */
	
	private void setChanged() {
		hasChanged = true;
	}

	public boolean hasChanged() {
		return hasChanged;
	}
    
	public void clearChanged() {
		hasChanged = false;
	}
	
	public Level getConfig(String key, Level defaultLevel) {
		return Level.toLevel(getConfig(key), defaultLevel);
	}
	
	public void setDefault(String key, String defaultValue) {
		if(!config.containsKey(key)) {
			config.put(key, defaultValue);
			setChanged();
		}
	}

	public void setConfig(String key, String value) {
		if(value == null) {
			removeConfig(key);
			return;
		}
		String currentValue = config.get(key);
		if(currentValue == null || !value.equals(currentValue)) {
			setChanged();
		} 
		config.put(key, value);
	}
	
	public void removeConfig(String key) {
		if(config.remove(key) != null) {
			setChanged();
		}
	}

	public String getConfig(String key) {
		return config.get(key);
	}	

	public String getConfig(String key, String defaultValue) {
		return config.get(key, defaultValue);
	}
	
	public boolean configIsTrue(String key) {
		return "true".equalsIgnoreCase(getConfig(key));
	}

	public boolean configIsFalse(String key) {
		return "false".equalsIgnoreCase(getConfig(key));
	}

	/* reconfiguration API */

	public void reconfigure(Configuration setup) {
		for(Entry<String, String> set: setup.entrySet()) {
			this.setConfig(set.getKey(), set.getValue());
		}
		
		if(hasChanged()) {
			//rebuild root Logger
			setupLoggers();
		}
			
	}

	public LogEntity getLogEntity(String logger) {
		return loggers.get(logger);
	}

	public void insertFileAppender(String logger, String filename, Level appenderLevel, String pattern) {
		LogEntity cur = getLogEntity(logger);
		if(cur == null) {
			loggers.put(new LogEntity(logger, null, filename, appenderLevel, pattern));
			setChanged();
		} else {
			LogEntity update = cur.createChanged(logger, null, filename, appenderLevel, pattern);
			if(update != cur) {
				loggers.put(update);
				setChanged();
			}
		}
	}
	
	public void removeAppender(String logger) {
		LogEntity cur = getLogEntity(logger);
		if(cur == null || !cur.hasAppender())
			return; //nothing to do
		
		if(cur.hasLogger()) {
			loggers.put(new LogEntity(logger, cur.logLevel, null, null, null));
		} else {
			loggers.remove(logger);
		}
		
		setChanged();
	}



	public void setupLogger(String logger, Level level) {
		LogEntity cur = getLogEntity(logger);
		if(cur == null) {
			loggers.put(new LogEntity(logger, level, null, null, null));
			setChanged();
		} else {
			LogEntity update = cur.createChanged(logger, level, null, null, null);
			if(update != cur) {
				loggers.put(update);
				setChanged();
			}
		}
	}

	public void removeLogger(String logger) {
		LogEntity cur = getLogEntity(logger);
		if(cur == null)
			return;
		loggers.remove(logger);
		setChanged();
	}

	public boolean isSet(String key) {
		return null != config.get(key);
	}

	public void setup(LogEntity entity) {
		if(entity != getLogEntity(entity.logger)) {
			loggers.put(entity);
			setChanged();
		}
	}
	


}
