package unimelb.bitbox.connection;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import unimelb.bitbox.util.*;


public class ClientConnectionHandler implements Runnable {
	private static Logger log = Logger.getLogger(ClientConnectionHandler.class.getName());
	private int clientPort;
	private String[] authorizedKeys;
	private ServerSocket clientListeningSocket;
	private HashMap<String,String> keysMap;
	private SecretKey skey;
	private ClientHandler clientHandler;
	private static String secretKey;

	public ClientConnectionHandler(ClientHandler clientHandler) throws Exception {
		if (clientHandler == null) {
			log.warning("NULL CLIENT HANDLER");
		}

		int clientPort = Integer.parseInt(Configuration.getConfigurationValue("clientPort"));
		String[] authorizedKeys = Configuration.getConfigurationValue("authorized_keys").split("\\s*,\\s*");
		this.clientHandler = clientHandler;
		this.clientPort = clientPort;
		this.authorizedKeys = authorizedKeys;
		this.clientListeningSocket = new ServerSocket(clientPort);
		keysMap = new HashMap<String,String>();
		sortKeys(authorizedKeys);

	}

	private void sortKeys(String[] authorizedKeys)  {
		for (String singleKey: authorizedKeys) {
			String[] keyIdentity = singleKey.split(" ");
			String key = keyIdentity[1];
			String identity  = keyIdentity[2];
			this.keysMap.put(identity, key);
		}
	}

	@Override
	public void run() {
		try {
			while (true) {
				// Accept an incoming client connection request (blocking call)
				Socket clientSocket = clientListeningSocket.accept();
				BufferedReader in =
						new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
				BufferedWriter out =
						new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8));

				try {
					String clientMessage = in.readLine(); // Blocking receive call
					Document clientMessageJSON = Document.parse(clientMessage);
					try {
						//This is how we handle the messages, expecting Auth Request, followed by Request then Close Connection
						if (handleJSONAuthRequest(clientMessageJSON, clientSocket, out)) {
							clientMessage = in.readLine();
							clientMessageJSON = Document.parse(clientMessage);
							handleJSONClientCommand(clientMessageJSON, clientSocket, out);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				} catch (SocketException e) {
					e.printStackTrace();
					log.warning("peer socket unexpectedly closed");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (clientListeningSocket != null) {
				try {
					clientListeningSocket.close();
					log.info("server listening socket closed");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	
	private boolean handleJSONAuthRequest(Document clientMessageJSON, Socket clientSocket, BufferedWriter out) throws Exception {
		boolean success;

		if (clientMessageJSON.getString("command").equals("AUTH_REQUEST") ) {
			String identity = clientMessageJSON.getString("identity");

			if (identity != null) {
				log.info("received command [" + "AUTH_REQUEST" + "] from Client: " + identity);
				
				if (keysMap.containsKey(identity)) {
					success = true;
					System.out.println(identity);
					PublicKey publicKey = Crypto.loadPublicKey(keysMap.get(identity));
					secretKey = Crypto.loadSecretKey();
					String encryptedSekretKey = Crypto.encrypt(secretKey, publicKey);
					String authResponse = Messages.getAuthResponse(encryptedSekretKey, success);
					send(authResponse, out);
				}else {
					//send key not found
					success = false;
					String authResponse = Messages.getAuthResponse(success,"public key not found");
					send(authResponse, out);
				}

			}else {
				//send identity not found
				success = false;
				String authResponse = Messages.getAuthResponse(success, "No identity field");
				send(authResponse, out);
			}

		}else {
			//send expected auth request
			success = false;
			String authResponse = Messages.getAuthResponse(success, "expected Auth request");
			send(authResponse, out);
		}
		return success;
	}
	

	private void handleJSONClientCommand(Document clientMessageJSON, Socket clientSocket, BufferedWriter out) throws IOException, UnsupportedEncodingException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
		
			String payload = clientMessageJSON.getString("payload");
			if (payload != null) {
			String decryptedPayload = Crypto.decryptAES(payload, secretKey);
			System.out.println(decryptedPayload);
			Document decryptedPayloadDocument = Document.parse(decryptedPayload);
			handleJSONpayload(decryptedPayloadDocument, out);
			}else {
				log.info("NULL Payload");
			}
		
	}
	
	private void handleJSONpayload(Document payload, BufferedWriter out) throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException {
		String command = payload.getString("command");
		String host;
		int port;
		String response;
		String encryptedResponse;
		String message;
		String responsePayload;
		

		switch (command) {
		case "LIST_PEERS_REQUEST":
			List<HostPort> peers = clientHandler.listPeers();
			response = Messages.getListPeersResponse(peers);
			encryptedResponse = Crypto.encryptAES(response, secretKey);
			responsePayload = Messages.getPayload(encryptedResponse);
			send(responsePayload, out);
			break;
		case "CONNECT_PEER_REQUEST":
			host = payload.getString("host");
			port = (int) payload.getLong("port");
			boolean connected = clientHandler.connectPeer(host, port);
			if (connected) {
				message = "connected to peer";
			} else {
				message = "connection failed";
			}
			response = Messages.getConnectPeerResponse(host, port, connected, message);
			encryptedResponse = Crypto.encryptAES(response, secretKey);
			responsePayload = Messages.getPayload(encryptedResponse);
			send(responsePayload, out);
			break;
		case "DISCONNECT_PEER_REQUEST":
			host = payload.getString("host");
			port = (int) payload.getLong("port");
			boolean disconnected = clientHandler.disconnectPeer(host, port);
			if (disconnected) {
				message = "disconnected from peer";
			} else {
				message = "connection not active";
			}
			response = Messages.getDisconnectPeerResponse(host, port, disconnected, message);
			log.info("Sending response " + response);
			encryptedResponse = Crypto.encryptAES(response, secretKey);
			responsePayload = Messages.getPayload(encryptedResponse);
			send(responsePayload, out);
			break;
		default:
			log.info("incorrect command");
			break;
		}
	}

	/**
	 * Sends a request/response string to the connected peer.
	 * Appends a newline char to the outgoing message.
	 * @param message request or response message
	 * @throws IOException
	 */
	private void send(String message, BufferedWriter out) throws IOException {
		out.write(message + '\n');
		out.flush();
	}
}
	





