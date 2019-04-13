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
import java.util.Scanner;
import java.util.logging.Logger;

import org.omg.CORBA.portable.OutputStream;

public class ServerConnection implements Runnable {

	private static Logger log = Logger.getLogger(Peer.class.getName());
	ServerSocket listeningSocket = null;
	Socket clientSocket = null;
	Socket socket = null;
	ServerConnection serverconnection;

	public ServerConnection(String port) throws IOException {

		listeningSocket = new ServerSocket(Integer.parseInt(port));
		log.info("Server listening on port " + port + " for a connection");
	}

	public ServerConnection(ServerConnection serverConnection)  {
		this.serverconnection = serverConnection;
		this.listeningSocket = serverConnection.getListeningSocket();
	}

	private ServerSocket getListeningSocket() {
		return this.listeningSocket;
	}

	public void connect() {
		try {

			// Create a stream socket bounded to any port and connect it to the
			// socket bound to localhost on port xxxx
			socket = new Socket("43.240.97.106", 3000);
			// Get the input/output streams for reading/writing data from/to the socket
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));

			Messages json = new Messages();
			
			out.write(json.getHandshakeRequest("dimefox.eng.unimelb.edu.au", 8111));
			out.flush();
				

				// Receive the reply from the server by reading from the socket input stream
				String received = in.readLine(); // This method blocks until there
				// is something to read from the
				// input stream
				System.out.println("Message received: " + received);
			 

			System.out.println("Connection established");


		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
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






	@Override
	public void run() {
		while(true) {
			//Connection Management Thread
			Socket clientSocket = null;

			try {
				//Create a server socket listening on port 4444

				int i = 0; //counter to keep track of the number of clients


				//Listen for incoming connections for ever 
				while (true) {

					//Accept an incoming client connection request 
					clientSocket = listeningSocket.accept(); //This method will block until a connection request is received
					i++;
				//	System.out.println("Client conection number " + i + " accepted:");
				//	System.out.println("Remote Port: " + clientSocket.getPort());
				//	System.out.println("Remote Hostname: " + clientSocket.getInetAddress().getHostName());
				//	System.out.println("Local Port: " + clientSocket.getLocalPort());

					//Get the input/output streams for reading/writing data from/to the socket
					BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
					BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"));


					//Read the message from the client and reply
					//Notice that no other connection can be accepted and processed until the last line of 
					//code of this loop is executed, incoming connections have to wait until the current
					//one is processed unless...we use threads!
					String clientMsg = null;
					try {
						while((clientMsg = in.readLine()) != null) {
							System.out.println("Message from client " + i + ": " + clientMsg);
							out.write("Server Ack " + clientMsg + "\n");
							out.flush();
							System.out.println("Response sent");
						}}
					catch(SocketException e) {
						System.out.println("closed...");
					}
					clientSocket.close();
				}
			} catch (SocketException ex) {
				ex.printStackTrace();
			}catch (IOException e) {
				e.printStackTrace();
			} 
			finally {
				if(listeningSocket != null) {
					try {
						listeningSocket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}

	}


}
