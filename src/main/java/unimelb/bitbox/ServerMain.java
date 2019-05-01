package unimelb.bitbox;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.logging.Logger;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.FileSystemObserver;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;


/**
 * TODO: Close connections when invalid protocol is sent
 * TODO: Fix maximumIncomingConnections logic
 * TODO: Tidy up peers arrays/hashmaps/lists etc.
 */

public class ServerMain implements FileSystemObserver, Runnable {
	private static Logger log = Logger.getLogger(ServerMain.class.getName());
	private static FileSystemManager fileSystemManager; // MADE STATIC SO ALL THREADS CAN ACCESS IT, ALSO PRIVATE
	private static List<ServerMain> threads;            // SAME REASONING AS ABOVE ^

	// ServerMain object instance variables
	private String host;
	private Integer port;
	boolean isEmpty= true;

	// Thread instance variables (only accessible via threads)
	private ServerMain serverMain;
	private Socket clientSocket;
	private BufferedReader in;
	private BufferedWriter out;
	private Queue<FileSystemEvent> events;

	public ServerMain() throws NumberFormatException, IOException, NoSuchAlgorithmException {
		// Main Server (self) constructor
		fileSystemManager = new FileSystemManager(Configuration.getConfigurationValue("path"),this);
		threads = new ArrayList<>();
	}

	// Thread constructor
	public ServerMain(ServerMain serverMain, Socket clientSocket, BufferedReader in, BufferedWriter out) {
		this.serverMain = serverMain;
		this.clientSocket = clientSocket;
		this.in = in;
		this.out = out;
		// Add this thread to the list of threads
		threads.add(this);
		// Initalise a queue for file system events
		this.events = new LinkedList<>();
	}

	@Override
	public void processFileSystemEvent(FileSystemEvent fileSystemEvent) {
		// Enqueue the file system event in every thread
		log.info("Name: " + fileSystemEvent.event.name());
		for (ServerMain sm : threads) {
			sm.events.add(fileSystemEvent);
			System.out.println("Size of Q: " + sm.events.size());
		}
	}

	/**
	 * Responsible for handling incoming requests from
	 * a single peer, until that connection is closed
	 */
	@Override
	public void run() {
		System.out.println("isEmpty: " + isEmpty);

		try {
			while (true) {
				// DEBUGGING
				try {
					Thread.sleep(1500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				// Process file system events, relay them
				// as outgoing messages to connected peers

				FileSystemEvent event;
				try {
					if ((event = this.events.poll()) != null) {
						log.info("NEW FILE SYSTEM EVENT");
						handleOutgoingClientMessage(event);
					}
				} catch (NoSuchAlgorithmException | IOException e) {
					e.printStackTrace();
				}


				// Read any incoming request from the input buffer.
				// This blocks until an incoming message is received.
				// TODO: Figure out how to get this to NOT block
				String clientMsg = null;
				try {
					while ((clientMsg = in.readLine()) != null) {
						// Logging
						log.info("INCOMING " + Thread.currentThread().getName() + ": " + clientMsg);

						// Parse the request into a JSON object
						Document json = Document.parse(clientMsg);

						// Handle the incoming request
						try {
							handleIncomingClientMessage(json);
						} catch (NoSuchAlgorithmException | IOException e) {
							System.out.println("Socket closed");
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
					threads.remove(this);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Handles an incoming client message
	 * @param clientMsg incoming client message in JSON
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
	private void handleIncomingClientMessage(Document clientMsg) throws NoSuchAlgorithmException, IOException {

		String command = clientMsg.getString("command");
		String response;
		switch (command) {
			case "DIRECTORY_CREATE_REQUEST":
				response = directoryCreateResponse(clientMsg);
				send(response);
				break;
			case "DIRECTORY_DELETE_REQUEST":
				response = directoryDeleteResponse(clientMsg);
				send(response);
				break;
			case "FILE_CREATE_REQUEST":
			case "FILE_DELETE_REQUEST":
			case "FILE_MODIFY_REQUEST":
			case "FILE_BYTES_REQUEST":
				// TODO: Process above four request types
				break;
			case "DIRECTORY_CREATE_RESPONSE":
			case "DIRECTORY_DELETE_RESPONSE":
			case "FILE_CREATE_RESPONSE":
			case "FILE_DELETE_RESPONSE":
			case "FILE_MODIFY_RESPONSE":
			case "FILE_BYTES_RESPONSE":
				// TODO: Process all response types
				log.info("INCOMING RESPONSE: " + clientMsg.toString());
				break;
			default:
				// Invalid protocol
				response = Messages.getInvalidProtocol("invalid command");
				send(response);
				break;
		}
	}

	/**
	 * Get response to the client's request for creating a directory.
	 * @param request
	 * @return response
	 */
	private String directoryCreateResponse(Document request) {
		String message;
		String response;
		String pathName = request.getString("pathName");
		if (fileSystemManager.fileNameExists(pathName)) {
			message = "pathname already exists";
			response = Messages.getDirectoryCreateResponse(pathName, message, false);
		} else if (!fileSystemManager.isSafePathName(pathName)) {
			message = "unsafe pathname given";
			response = Messages.getDirectoryCreateResponse(pathName, message, false);
		} else {
			message = "directory created";
			response = Messages.getDirectoryCreateResponse(pathName, message, true);
			fileSystemManager.makeDirectory(pathName);
		}
		return response;
	}

	/**
	 * Get response to the client's request for deleting a directory.
	 * @param request
	 * @return response
	 */
	private String directoryDeleteResponse(Document request) {
		String message;
		String response;
		String pathName = request.getString("pathName");
		if (!fileSystemManager.isSafePathName(pathName)) {
			message = "unsafe pathname given";
			response = Messages.getDirectoryDeleteResponse(pathName, message, false);
		} else if (!fileSystemManager.fileNameExists(pathName)) {
			message = "pathname does not exist";
			response = Messages.getDirectoryDeleteResponse(pathName, message, false);
		} else {
			// Attempt to delete the directory
			boolean status = fileSystemManager.deleteDirectory(pathName);
			message =  status ? "directory deleted" : "there was a problem deleting the directory";
			response = Messages.getDirectoryDeleteResponse(pathName, message, status);
		}
		return response;
	}

	/**
	 * Handles an outgoing reply to the client,
	 * generated by a new file system event.
	 * @param fileSystemEvent the file system event
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
	private void handleOutgoingClientMessage(FileSystemEvent fileSystemEvent) throws NoSuchAlgorithmException, IOException {
		String command = fileSystemEvent.event.name();
		FileSystemManager.FileDescriptor fileDescriptor = fileSystemEvent.fileDescriptor;
		String pathName = fileSystemEvent.pathName;

		// Only for file events
		String md5 = null;
		Long lastModified = null;
		Long fileSize = null;

		// Construct and send outgoing request
		String request;
		switch (command) {
			case "DIRECTORY_CREATE":
				request = Messages.getDirectoryCreateRequest(pathName);
				send(request);
				break;
			case "DIRECTORY_DELETE":
				request = Messages.getDirectoryDeleteRequest(pathName);
				send(request);
				break;
			case "FILE_CREATE":
			case "FILE_DELETE":
			case "FILE_MODIFY":
				assert(fileDescriptor != null);
				md5 = fileDescriptor.md5;
				lastModified = fileDescriptor.lastModified;
				fileSize = fileDescriptor.lastModified;
				switch (command) {
					case "FILE_CREATE":
						request = Messages.getFileCreateRequest(md5, lastModified, fileSize, pathName);
						send(request);
						break;
					case "FILE_DELETE":
						request = Messages.getFileDeleteRequest(md5, lastModified, fileSize, pathName);
						send(request);
						break;
					case "FILE_MODIFY":
						request = Messages.getFileModifyRequest(md5, lastModified, fileSize, pathName);
						send(request);
						break;
					default:
						break;
				}
				break;
			default:
				log.warning("Invalid command arose from file system event");
				break;
		}
	}

	/**
	 * Sends a request/response string to the connected peer.
	 * Appends a newline char to the outgoing message.
	 * @param message request or response message
	 * @throws IOException
	 */
	private void send(String message) throws IOException {
		out.write(message + '\n');
		out.flush();
		log.info("Sending: " + message);
	}
}