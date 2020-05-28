package farsight.logging;

import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.util.TriConsumer;

import com.softwareag.util.IDataMap;
import com.wm.data.IData;
import com.wm.data.IDataCursor;

public class ISMessage implements Message {
	
	public static class Builder {
		private LinkedList<Metadata> list = new LinkedList<>();
		private String message = "";
		private String caller = null;
		
		public Builder() {
		}
		
		public Builder(String message, String caller) {
			this.message = message;
			this.caller = caller;
		}
		
		public Builder setCaller(String caller) {
			this.caller = caller;
			return this;
		}
		
		public Builder setMessage(String message) {
			this.message = message;
			return this;
		}
		
		public Builder appendMetadata(String key, String value, boolean extended) {
			list.add(new Metadata(key, value, extended));
			return this;
		}
		
		public Builder appendMetadata(IData data, boolean extended) {
			if(data != null) {
				IDataCursor c = data.getCursor();
				while(c.next()) {
					Object obj = c.getValue();
					if(obj != null && !(obj instanceof Object[]) && !(obj instanceof IData)) {
						list.add(new Metadata(c.getKey(), String.valueOf(obj), extended));
					}
				}
			}
			return this;
		}
		
		public Builder appendMetadata(Map<String, String> data, boolean extended) {
			if(data != null) for(Entry<String, String> set: data.entrySet()) {
				list.add(new Metadata(set.getKey(), set.getValue(), extended));
			}
			return this;
		}
		
		public Builder appendMetadata(IDataMap data, boolean extended) {
			if(data != null) appendMetadata(data.getIData(), extended);
			return this;
		}
		
		public ISMessage build() {
			return new ISMessage(message, caller, list.toArray(new Metadata[list.size()]));
		}
				
	}
	
	public static class Metadata {
		public final String key;
		public final String value;
		public final boolean extended;
		
		public Metadata(String key, String value, boolean extended) {
			this.key = key;
			this.value = value;
			this.extended = extended;
		}
	}

	private static final long serialVersionUID = -4798699501833532960L;

	private final String caller;
	private final String text;
	private final Metadata[] metadata;
	
	private String message = null;

	
	public ISMessage(String text, String caller, Metadata[] metadata) {
		this.text = text;
		this.caller = caller;
		this.metadata = metadata;
	}

	private String createMessage() {
		StringBuilder b = new StringBuilder();
		
		if(caller != null) {
			b.append('[').append(caller).append("] ");
		} else {
			b.append(' ');
		}
		b.append(text);
		boolean first = true;
		if(metadata != null) for(Metadata meta: metadata) {
			if(!meta.extended) {
				if(first) {
					b.append(" {");
					first = false;
				} else {
					b.append("; ");
				}
				b.append(meta.key).append(": ").append(meta.value);
			}
		}
		if(!first) {
			b.append("}");
		}
		
		return b.toString();
	}
	
	@Override
	public String getFormattedMessage() {
		return message == null ? message = createMessage() : message;
	}

	@Override
	public String getFormat() {
		return message;
	}

	@Override
	public Object[] getParameters() {
		return null;
	}

	@Override
	public Throwable getThrowable() {
		return null;
	}
	
	public <S> void forEach(final TriConsumer<String, Object, S> action, final S state) {
		
		if(caller != null)
			action.accept("caller", caller, state);
		if(metadata == null)
			return;
		for(int i = 0; i < metadata.length; i++) {
			action.accept(metadata[i].key, metadata[i].value, state);
		}
	}

	public String getText() {
		return text;
	}
	
	public static Builder builder() {
		return new Builder();
	}
	
	public static Builder builder(String message, String caller) {
		return new Builder(message, caller);
	}
	
	public static Message create(String text, String caller, IData data, IData extended) {
		if(caller == null && data == null)
			return new SimpleMessage(text);
		return builder(text, caller).appendMetadata(data, false).appendMetadata(extended, true).build();
	}



}
