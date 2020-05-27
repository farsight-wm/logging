package farsight.logging.xmlUtils;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public class IndentingXMLWriter implements XMLStreamWriter {
	
	
	private final XMLStreamWriter w;
	private final String INDENT;
	private final String EOL;
	
	private int level = 0;

	public IndentingXMLWriter(XMLStreamWriter outputWriter) {
		this(outputWriter, "\t", "\n");
	}

	public IndentingXMLWriter(XMLStreamWriter outputWriter, String indention, String lineSeparator) {
		this.w = outputWriter;
		this.INDENT = indention;
		this.EOL = lineSeparator;
	}
	
	private void indent() throws XMLStreamException {
		w.writeCharacters(EOL);
		for(int i = 0; i < level; i++)
			w.writeCharacters(INDENT);
	}
	
	@Override
	public void writeStartElement(String paramString) throws XMLStreamException {
		indent();
		w.writeStartElement(paramString);
		level++;
	}

	@Override
	public void writeStartElement(String paramString1, String paramString2) throws XMLStreamException {
		indent();
		w.writeStartDocument(paramString1, paramString2);
		level++;
	}

	@Override
	public void writeStartElement(String paramString1, String paramString2, String paramString3)
			throws XMLStreamException {
		indent();
		w.writeStartElement(paramString1, paramString2, paramString3);
		level++;
	}

	@Override
	public void writeEmptyElement(String namespaceURI, String localName) throws XMLStreamException {
		indent();
		w.writeEmptyElement(namespaceURI, localName);
	}

	@Override
	public void writeEmptyElement(String prefix, String localName, String namespaceURI)
			throws XMLStreamException {
		indent();
		w.writeEmptyElement(prefix, localName, namespaceURI);
	}

	@Override
	public void writeEmptyElement(String localName) throws XMLStreamException {
		indent();
		w.writeEmptyElement(localName);
	}

	@Override
	public void writeEndElement() throws XMLStreamException {
		level--;
		indent();
		w.writeEndElement();
	}

	@Override
	public void writeEndDocument() throws XMLStreamException {
		level = 0;
		w.writeEndDocument();
	}

	@Override
	public void close() throws XMLStreamException {
		w.close();
	}

	@Override
	public void flush() throws XMLStreamException {
		w.flush();
	}

	@Override
	public void writeAttribute(String localName, String value) throws XMLStreamException {
		w.writeAttribute(localName, value);		
	}

	@Override
	public void writeAttribute(String prefix, String namespaceURI, String localName, String value)
			throws XMLStreamException {
		w.writeAttribute(prefix, namespaceURI, localName, value);
	}

	@Override
	public void writeAttribute(String namespaceURI, String localName, String value)
			throws XMLStreamException {
		w.writeAttribute(namespaceURI, localName, value);
	}

	@Override
	public void writeNamespace(String prefix, String namespaceURI) throws XMLStreamException {
		w.writeNamespace(prefix, namespaceURI);
	}

	@Override
	public void writeDefaultNamespace(String namespaceURI) throws XMLStreamException {
		w.writeDefaultNamespace(namespaceURI);
	}

	@Override
	public void writeComment(String comment) throws XMLStreamException {
		indent();
		w.writeComment(comment);
	}

	@Override
	public void writeProcessingInstruction(String paramString) throws XMLStreamException {
		w.writeProcessingInstruction(paramString);
	}

	@Override
	public void writeProcessingInstruction(String paramString1, String paramString2) throws XMLStreamException {
		w.writeProcessingInstruction(paramString1, paramString2);		
	}

	@Override
	public void writeCData(String paramString) throws XMLStreamException {
		w.writeCData(paramString);
	}

	@Override
	public void writeDTD(String paramString) throws XMLStreamException {
		w.writeDTD(paramString);
	}

	@Override
	public void writeEntityRef(String paramString) throws XMLStreamException {
		w.writeEntityRef(paramString);
	}

	@Override
	public void writeStartDocument() throws XMLStreamException {
		level = 0;
		w.writeStartDocument();
	}

	@Override
	public void writeStartDocument(String version) throws XMLStreamException {
		level = 0;
		w.writeStartDocument(version);
	}

	@Override
	public void writeStartDocument(String encoding, String version) throws XMLStreamException {
		level = 0;
		w.writeStartDocument(encoding, version);
	}

	@Override
	public void writeCharacters(String paramString) throws XMLStreamException {
		w.writeCharacters(paramString);
	}

	@Override
	public void writeCharacters(char[] paramArrayOfChar, int paramInt1, int paramInt2) throws XMLStreamException {
		w.writeCharacters(paramArrayOfChar, paramInt1, paramInt2);
	}

	@Override
	public String getPrefix(String paramString) throws XMLStreamException {
		return getPrefix(paramString);
	}

	@Override
	public void setPrefix(String paramString1, String paramString2) throws XMLStreamException {
		w.setPrefix(paramString1, paramString2);		
	}

	@Override
	public void setDefaultNamespace(String paramString) throws XMLStreamException {
		w.setDefaultNamespace(paramString);
	}

	@Override
	public void setNamespaceContext(NamespaceContext paramNamespaceContext) throws XMLStreamException {
		w.setNamespaceContext(paramNamespaceContext);
	}

	@Override
	public NamespaceContext getNamespaceContext() {
		return w.getNamespaceContext();
	}

	@Override
	public Object getProperty(String paramString) throws IllegalArgumentException {
		return w.getProperty(paramString);
	}
	
	
	

}
