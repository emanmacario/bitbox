package unimelb.bitbox;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.FileSystemObserver;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;

public class ServerMain implements FileSystemObserver, Runnable {
	private static Logger log = Logger.getLogger(ServerMain.class.getName());
	protected FileSystemManager fileSystemManager;
	private String host;
	private Integer port;
	private ServerMain serverMain;

	boolean isEmpty= true;
	private Socket clientSocket;
	private BufferedReader in;
	private BufferedWriter out;

	private Queue<String> eventsQ = new LinkedList<>();

	public ServerMain() throws NumberFormatException, IOException, NoSuchAlgorithmException {
		// Main Server (self) constructor
		fileSystemManager=new FileSystemManager(Configuration.getConfigurationValue("path"),this);
	}

	// Constructor for threading
	public ServerMain(ServerMain serverMain, Socket clientSocket, BufferedReader in, BufferedWriter out) {
		this.serverMain = serverMain;
		this.clientSocket = clientSocket;
		this.in = in;
		this.out = out;
	}

	@Override
	public void processFileSystemEvent(FileSystemEvent fileSystemEvent) {
		// Responsible for handling outgoing requests

		System.out.println(fileSystemEvent.name);
		System.out.println(fileSystemEvent.path);
		System.out.println(fileSystemEvent.pathName);
		System.out.println(fileSystemEvent.event.name());
		// System.out.println(fileSystemEvent.fileDescriptor);
		//System.out.println(fileSystemEvent.event.name());
		// eventsQ.add(fileSystemEvent.event.name());
		//System.out.println( eventsQ.toString());
		//System.out.println( eventsQ.size());




	}

	@Override
	public void run() {
		// Responsible for handling incoming requests from
		// a single peer, until that connection is closed
		try {
			while (true) {
				// Read the incoming request from the input buffer
				String clientMsg = null;
				try {
					while ((clientMsg = in.readLine()) != null) {
						// Logging
						log.info("INCOMING " + Thread.currentThread().getName() + ": " + clientMsg);

						// Parse the request into a JSON object
						Document json = Document.parse(clientMsg);

						// Handle the incoming request
						try {
							handleClientRequest(json);
						} catch (NoSuchAlgorithmException | IOException e) {
							System.out.println("Socket closed");
							e.printStackTrace();
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} finally {
			if (clientSocket != null) {
				// Close the connection
				try {
					log.info("Client socket was closed");
					clientSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Handles an incoming client request
	 * @param request
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
	private void handleClientRequest(Document request) throws NoSuchAlgorithmException, IOException {

	}
}