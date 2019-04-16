package unimelb.bitbox;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
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
	private Long port;
	private ServerMain serverMain;
	static HashMap<String, Long> peersConnected = new HashMap();
	
	boolean isEmpty= true;
	private Socket clientSocket;
	private BufferedReader in;
	private BufferedWriter out;
	
	private Queue<String> eventsQ = new LinkedList<>();
	
	public ServerMain() throws NumberFormatException, IOException, NoSuchAlgorithmException {
		//Main Server (self) constructor
		fileSystemManager=new FileSystemManager(Configuration.getConfigurationValue("path"),this);
	}


	public ServerMain(ServerMain serverMain, Socket clientSocket, BufferedReader in, BufferedWriter out) {
		this.serverMain = serverMain;
		this.clientSocket = clientSocket;
		this.in = in;
		this.out = out;
	}

	@Override
	public void processFileSystemEvent(FileSystemEvent fileSystemEvent) {
		//System.out.println("TEST");
		System.out.println(fileSystemEvent.event.name());
		eventsQ.add(fileSystemEvent.event.name());
		 System.out.println( eventsQ.toString());
		System.out.println( eventsQ.size());
		
	}

	@Override
	public void run() {
		/*
		BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"));
		
		while (true) {
			
			String clientMsg = null;
			try {
				while((clientMsg = in.readLine()) != null) {  
				 System.out.println("INCOMING "+ Thread.currentThread().getName() + ": " + clientMsg);
				 
				 }}
		
			catch(SocketException e) {
				System.out.println("closed...");
			}
			//clientSocket.close();
			Document json1 = proocessJSONstring(clientMsg);
			try {
				handleJsonClientMsg(json1, in,out, clientSocket);
			} catch (NoSuchAlgorithmException e) {
				 
				e.printStackTrace();
			}
			

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
	
			
			
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//System.out.println(ServerConnection.eventsQ.size());
			
			if (ServerConnection.eventsQ.size() > 0) {
				//Send Events from Q
				System.out.println(ServerConnection.eventsQ.toString());
				ServerConnection.eventsQ.remove();
				isEmpty = false;
			}else if (!isEmpty && ServerConnection.eventsQ.size() == 0) {
				System.out.println("Empty");
				isEmpty = true;
			}
			
		}
		*/
		
		
		
	}
	
}
