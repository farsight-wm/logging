package farsight.logging.tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Simple TCP proxy to test (and watch) communication to GRAYLOG.
 */
public class GraylogProxy {

	// setup
	public static final boolean FORWARD_TO_GRAYLOG_ENABLED = true;
	public static final String GRAYLOG_HOST = "graylog.host"; //FIXME get from config (properties?)
	public static final int GRAYLOG_PORT = 1516;

	// forwarding pipe
	private static class Pipe implements Runnable {
		
		private final boolean dump;
		private final Socket src;
		private final Socket to;
		
		public Pipe(Socket src, Socket to, boolean dump) {
			this.to = to;
			this.src = src;
			this.dump = dump;
		}

		@Override
		public void run() {
			if(to == null)
				runSink();
			else
				runTunnel();
		}
		
		private void runTunnel() {
			try (final InputStream in = src.getInputStream()) {
				try (final OutputStream out = to.getOutputStream()) {
					int ch;
					while((ch = in.read()) != -1) {
						if(dump) dump(ch);
						out.write(ch);
					}
				}
			} catch(IOException e) {
				e.printStackTrace();
			}			
		}
		
		private void runSink() {
			try (final InputStream in = src.getInputStream()) {
				int ch;
				while((ch = in.read()) != -1) {
					if(dump) dump(ch);
				}
			} catch(IOException e) {
				e.printStackTrace();
			}						
		}
		
		private void dump(int ch) {
			System.out.print((char) ch);
			if(ch == '\0')
				System.out.println();			
		}
	}
	
	public static void startListener() throws IOException {
		System.out.println("Start linstening...");
		try (ServerSocket serverSocket = new ServerSocket(1516)) {
			while(true) {
				handleConnection(serverSocket.accept());
			}
		} finally {
			System.out.println("Listener Stopped");
		}
	}

	private static void handleConnection(Socket client) throws IOException {
		System.out.println("New Connection");
		//try to tunnel through
		Socket remote = FORWARD_TO_GRAYLOG_ENABLED ? new Socket(GRAYLOG_HOST, GRAYLOG_PORT) : null;
		new Thread(new Pipe(client, remote, true)).start();
	}
	
	public static void main(String[] args) throws IOException {
		startListener();
	}

}
