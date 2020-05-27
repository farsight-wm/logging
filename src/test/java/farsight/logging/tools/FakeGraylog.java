package farsight.logging.tools;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.LinkedList;

/**
 * Simple FakeGrayLog
 * 
 * This class provides a mini-server that has "a graylog interface" (open tcp port 1516).
 * All incoming GELF messages are stored in a String list.
 * 
 * Startup/shutdown and error handling is very simple and not very robust!
 */
public class FakeGraylog implements Runnable {

	// setup
	public static final int PORT = 1516;
	
	
	private final LinkedList<String> messages = new LinkedList<String>();
	private final int port;
	
	public FakeGraylog() {
		this(PORT);
	}

	public FakeGraylog(int port) {
		this.port = port;
	}


	public void reset() {
		messages.clear();
	}
	
	public LinkedList<String> getMessages() {
		return messages;
	}
	
	private synchronized void addMessage(String message) {
		messages.add(message);
	}
	
	private enum State {
		STOPPED, STARTING, STARTED, STOPPING, ERROR
	}
	
	private volatile State state = State.STOPPED;
	private Thread thread = null;
	private LinkedList<Connection> connections = new LinkedList<>();

	@Override
	public void run() {
		try(ServerSocket socket = new ServerSocket(port)) {
			state = State.STARTED;
			while(state == State.STARTED) {
				handleConnection(socket.accept());
			}
			closeConnections();
			state = State.STOPPED;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private synchronized void addConnection(Connection con) {
		connections.add(con);
	}
	
	private synchronized void removeConnection(Connection con) {
		connections.remove(con);
	}
	
	private synchronized void closeConnections() {
		for(Connection con: connections)
			con.close();
	}

	public synchronized void start() {
		if(thread != null)
			throw new IllegalStateException("Already started");
		state = State.STARTING;
		thread = new Thread(this);
		thread.start();
		while(state != State.STARTED && state != State.ERROR) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				throw new RuntimeException("Interrupted in startup sequence!", e);
			}
		}
	}
	
	public void stop() {
		if(state != State.STARTED)
			throw new IllegalStateException("Cannot stop if FakeGraylog is not started!");
		state = State.STOPPING;
		closeConnections();
		while(state != State.STOPPED && state != State.ERROR) {
			try {
				if(connections.size() == 0)
					state = State.STOPPED;
				else 
					Thread.sleep(100);
			} catch (InterruptedException e) {
				throw new RuntimeException("Interrupted in shutdown sequence!", e);
			}
		}
	}
	
	private void handleConnection(Socket client) throws IOException {
		new Thread(new Connection(client)).start();
	}
	
	private class Connection implements Runnable {
		
		private Socket client;

		public Connection(Socket client) {
			this.client = client;
		}

		public void close() {
			if(client.isConnected() && !client.isClosed())
				try {
					client.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}

		@Override
		public void run() {
			if(client.isClosed())
				return;
			
			System.out.println("new connection...");
			addConnection(this);
			
			try(InputStream in = client.getInputStream()) {
				StringBuffer buf = new StringBuffer();
				int ch;
				while((ch = in.read()) != -1) {
					buf.append((char) ch);
					if(ch == 0) {
						addMessage(buf.toString());
						buf.setLength(0);
					}
				}
				if(buf.length() > 0)
					addMessage(buf.toString());
				
			} catch (SocketException e) {
				//ignore
			} catch (IOException e) {
				e.printStackTrace();				
			}
			
			System.out.println("closing connection...");
			removeConnection(this);
		}	
	}
	

}
