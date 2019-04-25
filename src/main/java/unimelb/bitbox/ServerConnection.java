package unimelb.bitbox;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;
import java.util.logging.Logger;

import org.omg.CORBA.portable.OutputStream;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;

public class ServerConnection implements Runnable {

	private static Logger log = Logger.getLogger(Peer.class.getName());

	private ServerSocket listeningSocket = null;
	private Socket clientSocket = null;
	private Socket socket = null;
	private ServerConnection serverConnection;
	private Messages json = new Messages();
	private Map<String, Integer> connectedPeers = new HashMap<>();
	private Map<String, Integer> peersPool = new HashMap<>();
	private int port;
	private int maximumIncomingConnections;
	private String advertisedHost;
	private FileSystemManager fileSystemManager;
	private ServerMain serverMain;
 
	
	public ServerConnection(String advertisedHost, int port, int maximumIncomingConnections, ServerMain serverMain) throws IOException {
		this.port = port;
		this.advertisedHost = advertisedHost;
		this.maximumIncomingConnections = maximumIncomingConnections;
		this.serverMain = serverMain;

		// Bind to the port number on advertisedHost
		this.listeningSocket = new ServerSocket(this.port);
		log.info("Server listening on port " + port + " for a connection");
	}

	// Thread constructor to pass object to Thread
	public ServerConnection(ServerConnection serverConnection)  {
		this.serverConnection = serverConnection;
		this.listeningSocket = serverConnection.getListeningSocket();
		this.port = serverConnection.getPort();
		this.maximumIncomingConnections = serverConnection.getMaximumIncomingConnections();
		this.advertisedHost = serverConnection.getAdvertisedName();
	}


	public void connect(String host, String port) throws InterruptedException {
		try {
			// Create a stream socket bounded to any port and connect
			// it to the socket bound to advertisedName on serverPort
			socket = new Socket(host, Integer.parseInt(port));

			// Get the input/output streams for reading/writing data from/to the socket
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));

			String handShake = json.getHandshakeRequest(host, Integer.parseInt(port));

			//Send JSON string to server.
			out.write(handShake + "\n");
			out.flush();
			//Thread.sleep(1000);
			String received;
			// Receive the reply from the server by reading from the socket input stream
			received = in.readLine(); // This method blocks until there
			Document json = processJSONstring(received);
			handleJsonServerMsg(json, socket, in,  out);
			System.out.println("INCOMING: " + received);
		} catch (UnknownHostException e) {
			// e.printStackTrace();
		} catch (IOException e) {
			log.warning("while connecting to " + host + ":" + port + "Connection Refused");
			// e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} finally {
			// Close the socket
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void handleJsonServerMsg(Document json, Socket socket, BufferedReader in ,BufferedWriter out) throws IOException, NoSuchAlgorithmException {
		String command = json.getString("command");
		String invalidProtocol;

		switch (command) {

		case "HANDSHAKE_RESPONSE":
			Document hostPort = (Document) json.get("hostPort");
			Integer port =  (int) hostPort.getLong("port");
			String host = hostPort.getString("host");

			// Check message credibility, ensure host field
			if (port != null && host != null) {
				//Proofing if a peer accepted an existing connection
				if (connectedPeers.containsKey(host) && connectedPeers.get(host).equals(port)){
					invalidProtocol= this.json.getInvalidProtocol("Peer already connected!");
					out.write(invalidProtocol+"\n");
					out.flush();
				}
				else {
					connectedPeers.put(host,port);
					log.info("Connection established between port: "+ this.port + " @ host: "+ this.advertisedHost +" and port: " + port +" @ host: " + host );
					// Start peer thread
					Runnable runnable = new ServerMain( serverMain,  socket,  in,  out);
					Thread thread = new Thread(runnable);
					thread.start();
					log.info("P2P Connection Thread Running");
				}					
			} else {
				invalidProtocol = this.json.getInvalidProtocol("message must contain hostPort field");
				out.write(invalidProtocol+"\n");
				out.flush();
			}
			break;

		case "CONNECTION_REFUSED":
			ArrayList<Document> peers = (ArrayList<Document>) json.get("peers");
			for (Document peer : peers) {
				port = peer.getInteger("port");
				host = peer.getString("host");
				if (host != null && port != null) {
					if (connectedPeers.containsKey(host) && connectedPeers.get(host).equals(port)){
						// Do not add to connection pool if already connected to that peer
					}
					else {
						peersPool.put(host,port);
						// Connect if connections less than 10
					}		
				}else {
					invalidProtocol = this.json.getInvalidProtocol("message must contain hostPort field");
					
					out.write(invalidProtocol+"\n");
					out.flush();
				}
			}
			break;
		default:
			invalidProtocol = this.json.getInvalidProtocol("Expected HANDSHAKE_RESPONSE");
			 out.write(invalidProtocol+"\n");
			 out.flush();
		}
	}
	
	private void handleJsonClientMsg(Document json, Socket clientSocket, BufferedReader in,BufferedWriter out) throws IOException, NoSuchAlgorithmException {
		String command = json.getString("command");
		String invalidProtocol;
		
		Document hostPort = (Document) json.get("hostPort");
		Integer port =  (int) hostPort.getLong("port");
		String host = hostPort.getString("host");

		System.out.println("== CHECKING HANDSHAKE REQUEST ==");
		switch (command){

		case "HANDSHAKE_REQUEST":
			// Check message credibility, ensure host field
			if (host != null && port != null) {
				//Proofing if a peer accepted an existing connection
				if (connectedPeers.containsKey(host) && connectedPeers.get(host).equals(port)){
					invalidProtocol= this.json.getInvalidProtocol("Peer already connected!");
					log.info("Connection Refused between port: "+ this.port + " @ host: "+ this.advertisedHost +" and port: " + port +" @ host: " + host + invalidProtocol);
					out.write(invalidProtocol+"\n");
					out.flush();
				}
				else if (connectedPeers.size() >= this.maximumIncomingConnections){
					String connectionRefused = this.json.getConnectionRefused(connectedPeers, "connection limit reached");
					log.info("Connection Refused between port: "+ this.port + " @ host: "+ this.advertisedHost +" and port: " + port +" @ host: " + host + connectionRefused);
					out.write(connectionRefused + "\n");
					out.flush();
				} else {
					connectedPeers.put(host,port);
					log.info("Connection established between port: "+ this.port + " @ host: "+ this.advertisedHost +" and port: " + port +" @ host: " + host );
					/* Debug
					System.out.println("Client conection number " + i + " accepted:");
					System.out.println("Remote Port: " + clientSocket.getPort());
					System.out.println("Remote Hostname: " + clientSocket.getInetAddress().getHostName());
					System.out.println("Local Port: " + clientSocket.getLocalPort());
					// End Debug
					*/
					String handShakeResponse = this.json.getHandshakeResponse(advertisedHost, port);
					out.write(handShakeResponse+"\n");
					out.flush();
					// Start peer thread to manage P2P communication in seperate thread per peer
					
					Runnable runnable = new ServerMain(serverMain, clientSocket, in, out);
					Thread thread = new Thread(runnable);
					thread.start();
					log.info("P2P Connection Thread Running");
				}
			} else {
				invalidProtocol = this.json.getInvalidProtocol("message must contain hostPort field");
				log.info("Invalid message between port: "+ this.port + " @ host: "+ this.advertisedHost +" and port: " + port +" @ host: " + host + invalidProtocol);
				out.write(invalidProtocol+"\n");
				out.flush();
			}
			break;

		default:
			invalidProtocol = this.json.getInvalidProtocol("Expected HANDSHAKE_REQUEST");
			log.info("Invalid message between port: "+ this.port + " @ host: "+ this.advertisedHost +" and port: " + port +" @ host: " + host + invalidProtocol);
			out.write(invalidProtocol+"\n");
			out.flush();
		}
	}

	private Document processJSONstring(String jsonMessage) {
		Document json = new Document();
		json = Document.parse(jsonMessage);
		return json;
	}

	@Override
	public void run() {

		//Connection Management Thread
		Socket clientSocket = null;

		try {
			//Create a server socket listening on port 4444
			//Get the input/output streams for reading/writing data from/to the socket

			int i = 0; //counter to keep track of the number of clients


			//Listen for incoming connections for ever 
			while (true) {

				//Accept an incoming client connection request 
				clientSocket = listeningSocket.accept(); //This method will block until a connection request is received
				BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
				BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"));


				String clientHost = clientSocket.getInetAddress().getHostName();
				Integer clientPort = clientSocket.getLocalPort();

				i++;
				String clientMsg = null;
				try {

					clientMsg = in.readLine(); // Blocking receive call
					System.out.println("NEW MESSAGE RECEIVED: " + clientMsg);
					Document json = processJSONstring(clientMsg);

					try {
						System.out.println("Handling new client message...");
						handleJsonClientMsg(json, clientSocket, in, out);
					} catch (NoSuchAlgorithmException e) {
						e.printStackTrace();
					}
				} catch(SocketException e) {
					System.out.println("Client socket has closed");
				}
			}
		} catch (SocketException ex) {
			ex.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			if (listeningSocket != null) {
				try {
					listeningSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private int getMaximumIncomingConnections() {
		return this.maximumIncomingConnections;
	}

	private String getAdvertisedName() {
		return this.advertisedHost;
	}

	private int getPort() {
		return this.port;
	}

	private ServerSocket getListeningSocket() {
		return this.listeningSocket;
	}
}