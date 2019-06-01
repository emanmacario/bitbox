package unimelb.bitbox;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.util.logging.Logger;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import unimelb.bitbox.connection.ClientHandler;
import unimelb.bitbox.util.Crypto;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.Messages;

public class Client {
	private static Logger log = Logger.getLogger(ClientHandler.class.getName());
	private static PrivateKey privateKey;
	private static String secretKey;
	private static  BufferedWriter out;
	private static BufferedReader in;
	public static void main(String[] args) throws Exception {

		privateKey = Crypto.loadPrivateKey("bitboxclient_rsa");
		CmdLineArgs argsBean = new CmdLineArgs();
		//Parser provided by args4j
		CmdLineParser parser = new CmdLineParser(argsBean);

		try {
			//Parse the arguments
			parser.parseArgument(args);
			//After parsing, the fields in argsBean have been updated with the given
			//command line arguments
			System.out.println("Command: " + argsBean.getCommand());
			System.out.println("Server: " + argsBean.getServer());
			System.out.println("Identity: " + argsBean.getIdentity());
			System.out.println("Peer: " + argsBean.getPeer());

			String command = argsBean.getCommand();
			String server = argsBean.getServer();
			String identity = argsBean.getIdentity();
			String peer = argsBean.getPeer();
			
			String[] hostPort = server.split(":");
			String host = hostPort[0];
			int port = Integer.parseInt(hostPort[1]);

			String[] hostPortPeer;
			String hostPeer = null;
			Integer portPeer = null;
			if (peer != null) {
				hostPortPeer = peer.split(":");
				hostPeer = hostPortPeer[0];
				portPeer = Integer.parseInt(hostPortPeer[1]);
			}

			String encyrptedRequest;
			String payload;
			String commandResponse;
			Document commandResponseJSON;
			
			switch (command){
				case "list_peers":
					sendAuthRequest(identity, host, port);
					if (secretKey == null) {
						break;
					}
					encyrptedRequest = Crypto.encryptAES(Messages.getListPeersRequest(), secretKey);
				    payload = Messages.getPayload(encyrptedRequest);
					send(payload);
				    commandResponse = in.readLine();
				    commandResponseJSON = Document.parse(commandResponse);
					String encryptedPayload = commandResponseJSON.getString("payload");
					String response = Crypto.decryptAES(encryptedPayload, secretKey);
					log.info(response);
					break;
				case "connect_peer":
					sendAuthRequest(identity, host, port);
					if (secretKey == null) {
						break;
					}
					if (portPeer == null || hostPeer == null) {
						log.warning("Please set peer field");
						break;
					}
				    encyrptedRequest = Crypto.encryptAES(Messages.getConnectPeer(hostPeer,portPeer), secretKey);
					payload = Messages.getPayload(encyrptedRequest);
					send(payload);
				    commandResponse = in.readLine();
				    commandResponseJSON = Document.parse(commandResponse);
				    encryptedPayload = commandResponseJSON.getString("payload");
				    response = Crypto.decryptAES(encryptedPayload, secretKey);
					log.info(response);
					break;
				case "disconnect_peer":
					sendAuthRequest(identity, host, port);
					if (secretKey == null) {
						break;
					}
					if (portPeer == null || hostPeer == null) {
						log.warning("Please set peer field");
						break;
					}
				    encyrptedRequest = Crypto.encryptAES(Messages.getDisconnectPeer(hostPeer,portPeer), secretKey);
					payload = Messages.getPayload(encyrptedRequest);
					send(payload);
				    commandResponse = in.readLine();
				    commandResponseJSON = Document.parse(commandResponse);
				    encryptedPayload = commandResponseJSON.getString("payload");
				    response = Crypto.decryptAES(encryptedPayload, secretKey);
				    log.info(response);
					break;
				default:
					System.out.println("invalid command");
					break;
			}
		} catch (CmdLineException e) {
			System.err.println(e.getMessage());
			//Print the usage to help the user understand the arguments expected
			//by the program
			parser.printUsage(System.err);
		}
	}


	private static void sendAuthRequest(String identity, String host, int port) throws Exception {
		try {
			// Create client socket, get input and output buffers
			Socket clientSocket = new Socket(host, port);
			in =
					new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
			out =
					new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8));

			// Attempt to perform a handshake with the peer
			String authRequest = Messages.getAuthRequest(identity);
			send(authRequest);
			log.info("sending to " + host + ":" + "port " + authRequest);
			String authResponse = in.readLine(); // Blocking receive call
			Document authResponseJSON = Document.parse(authResponse);

			String encryptedSecretKey = handleJSONServerMessage(authResponseJSON);
			if (encryptedSecretKey != null) {
				secretKey = Crypto.decrypt(encryptedSecretKey, privateKey);
			} else {
				log.info(authResponseJSON.toJson());
			}
		} catch (IOException e) {
			log.warning("while connecting to " + host + ":" + port + " connection refused");
		}
	}

	private static String handleJSONServerMessage(Document authResponseJSON) {
		String AES128 = authResponseJSON.getString("AES128");
		return AES128;
	}

	private static void send(String message) throws IOException {
		out.write(message + '\n');
		out.flush();
	}
}