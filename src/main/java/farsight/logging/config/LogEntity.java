package farsight.logging.config;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.logging.log4j.Level;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import farsight.common.conf.AbstractConfComponent;
import farsight.common.conf.ConfComponentDecoder;

public class LogEntity extends AbstractConfComponent<LogEntity> {
	
	public static class Builder {
		private String logger;
		private String file;
		private Level logLevel, fileLevel;
		private String pattern;
		
		public Builder() {
		}
		
		public Builder(LogEntity entity) {
			this.logger = entity.logger;
			this.file = entity.file;
			this.logLevel = entity.logLevel;
			this.fileLevel = entity.fileLevel;
			this.pattern = entity.pattern;
		}
		
		public Builder setLogger(String logger) {
			this.logger = logger;
			return this;
		}
		
		public Builder setFile(String file) {
			this.file = file;
			return this;
		}
		
		public Builder setLogLevel(Level level) {
			this.logLevel = level;
			return this;
		}
		
		public Builder setFileLevel(Level level) {
			this.fileLevel = level;
			return this;
		}
		
		public Builder setPattern(String pattern) {
			this.pattern = pattern;
			return this;
		}
		
		public Builder setNoneNullValuesFrom(LogEntity entity) {
			if(entity.logger != null) logger = entity.logger;
			if(entity.logLevel != null) logLevel = entity.logLevel;
			if(entity.file != null) file = entity.file;
			if(fileLevel != null) fileLevel = entity.fileLevel;
			if(entity.pattern != null) pattern = entity.pattern;
			return this;
		}
		
		public LogEntity build() {
			return new LogEntity(logger, logLevel, file, fileLevel, pattern);
		}
		
	}
	
	public final String logger;
	public final String file;
	public final Level logLevel, fileLevel;
	public final String pattern;

	public LogEntity(String logger, Level logLevel, String file, Level fileLevel, String pattern) {
		this.logger = logger;
		this.logLevel = logLevel;
		this.file = file;
		this.fileLevel = fileLevel;
		this.pattern = pattern;
	}
	
	public String toString() {
		return "[LogEntity logger=" + logger + "]";
	}

	@Override
	public void encodeTo(XMLStreamWriter w) throws XMLStreamException {
		encodeTo(w, "logger");
	}
	
	public void encodeTo(XMLStreamWriter w, String elementName) throws XMLStreamException {
		w.writeEmptyElement(elementName);
		writeAttribute(w, "logger", logger);
		if(logLevel != null)
			writeAttribute(w, "logLevel", logLevel.name());
		
		if(file != null) {
			w.writeAttribute("file", file);
			if(fileLevel != null) {
				w.writeAttribute("fileLevel", fileLevel.name());
			}
			writeAttribute(w, "pattern", pattern);
		}
	}   
	
	protected static Level toLevel(String strLevel) {
    	if(strLevel == null)
    		return null;
    	return Level.toLevel(strLevel);
    }

	static public ConfComponentDecoder<LogEntity> getDecoder() {
		return new ConfComponentDecoder<LogEntity>() {
			@Override
			public LogEntity decodeFrom(Node element) {
				NamedNodeMap attrs = element.getAttributes();
				String logger = getAttribute(attrs, "logger");
				Level logLevel = toLevel(getAttribute(attrs, "logLevel"));
				String file = getAttribute(attrs, "file");
				Level fileLevel = toLevel(getAttribute(attrs, "fileLevel"));
				String pattern = getAttribute(attrs, "pattern");
			
				return new LogEntity(logger, logLevel, file, fileLevel, pattern);
			}
		};
	}
	
	public LogEntity createChanged(String logger, Level logLevel, String file, Level fileLevel, String pattern) {
		return checkChanged(logger, logLevel, file, fileLevel, pattern) ? new Builder(this)
				.setNoneNullValuesFrom(new LogEntity(logger, logLevel, file, fileLevel, pattern)).build() : this;
	}
	
	public boolean checkChanged(String logger, Level logLevel, String file, Level fileLevel, String pattern) {
		if(logger != null && !logger.equals(this.logger))
			return true;
		if(logLevel != null && logLevel != this.logLevel)
			return true;
		if(file != null && !file.equals(this.file))
			return true;
		if(fileLevel != null && fileLevel != this.fileLevel)
			return true;
		if(pattern != null && !pattern.equals(this.pattern))
			return true;
		return false;
	}

	public boolean hasLogger() {
		return logLevel == null;
	}
	
	public boolean hasAppender() {
		return file != null;
	}

	public static Builder buidler() {
		return new Builder();
	}
	
	public static Builder builder(LogEntity entity) {
		return new Builder(entity);
	}
	
}
