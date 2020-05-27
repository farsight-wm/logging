package farsight.logging.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Node;

import farsight.common.conf.AbstractConfComponent;
import farsight.common.conf.ConfComponentDecoder;

public class LoggersSection extends AbstractConfComponent<LoggersSection> {
	
	private LogEntity root;
	private final LinkedHashMap<String, LogEntity> entities = new LinkedHashMap<>();
	
	public LoggersSection() {
	}

	@Override
	public void encodeTo(XMLStreamWriter w) throws XMLStreamException {
		// section loggers
		w.writeStartElement("loggers");
		if(root != null)
			root.encodeTo(w, "root");
		for(LogEntity entity: entities.values()) {
			entity.encodeTo(w);
		}
			
		w.writeEndElement();
		
	}
	
	public static ConfComponentDecoder<LoggersSection> getDecoder() {
		return new ConfComponentDecoder<LoggersSection>() {
			@Override
			public LoggersSection decodeFrom(Node element) {
				final ConfComponentDecoder<LogEntity> decoder = LogEntity.getDecoder();
				final LoggersSection result = new LoggersSection();
				vistChildElements(element, logger -> {
					LogEntity entity = decoder.decodeFrom(logger);
					if(logger.getLocalName().equals("root")) {
						result.setRoot(entity);
					} else {
						result.add(entity);
					}
					
				});
				return result;
			}
		};
	}
	
	protected void add(LogEntity logEntity) {
		this.entities.put(logEntity.logger, logEntity);
	}
	
	protected void addAll(Collection<LogEntity> list) {
		for(LogEntity entity: list) {
			add(entity);
		}
	}

	public List<LogEntity> getLogEnities() {
		return new ArrayList<>(entities.values());
	}
	
	public void setRoot(LogEntity root) {
		this.root = root;
	}
	
	public LogEntity getRoot() {
		return root;
	}

	public LogEntity get(String logger) {
		return this.entities.get(logger);
	}

	public void put(LogEntity logEntity) {
		this.entities.put(logEntity.logger, logEntity);
	}

	public void remove(String logger) {
		this.entities.remove(logger);
	}
}
