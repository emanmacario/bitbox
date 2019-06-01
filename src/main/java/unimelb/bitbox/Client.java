package unimelb.bitbox;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.logging.Logger;

import javax.crypto.Cipher;

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


			switch (command){
				case "list_peers":
					sendAuthRequest(identity, server);
					String encyrptedRequest = Crypto.encryptAES(Messages.getListPeersRequest(), secretKey);
					String payload = Messages.getPayload(encyrptedRequest);
					send(payload);
					String commandResponse = in.readLine();
					Document commandResponseJSON = Document.parse(commandResponse);
					String encryptedPayload = commandResponseJSON.getString(payload);
					String response = Crypto.decryptAES(encryptedPayload, secretKey);
					System.out.println(response);
					break;
				case "connect_peer":
					sendAuthRequest(identity, server);
					//send encypted connected peers command
					break;
				case "disconnect_peer":
					sendAuthRequest(identity, server);
					//send encypted dsconnect peers command
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


	private static void sendAuthRequest(String identity, String server) throws Exception {

		String[] hostPort = server.split(":");
		String host = hostPort[0];
		int port = Integer.parseInt(hostPort[1]);

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
			log.info("Received response " + authResponseJSON.toJson());
			String encryptedSecretKey = handleJSONServerMessage(authResponseJSON);
			log.info("privateKey is null: " + (privateKey == null));
			secretKey = Crypto.decrypt(encryptedSecretKey, privateKey);


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
