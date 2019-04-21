package unimelb.bitbox;

import java.io.IOException;
import java.lang.reflect.Array;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;

public class Peer 


{
	private static Logger log = Logger.getLogger(Peer.class.getName());

	public static void main( String[] args ) throws IOException, NumberFormatException, NoSuchAlgorithmException, InterruptedException
	{
		System.setProperty("java.util.logging.SimpleFormatter.format",
				"[%1$tc] %2$s %4$s: %5$s%n");
		log.info("BitBox Peer starting...");
		Configuration.getConfiguration();
		//new ServerMain();

		//Server Setup, get Port and Advertised name from properties
		String advertisedName = Configuration.getConfigurationValue("advertisedName");
		int serverPort = Integer.parseInt(Configuration.getConfigurationValue("port"));
		int maximumincomingConnections = Integer.parseInt(Configuration.getConfigurationValue("maximumincomingConnections"));
		ServerMain serverMain = new ServerMain();
		ServerConnection serverConnection = new ServerConnection(advertisedName, serverPort, maximumincomingConnections, serverMain);
		
		//Start Server Thread to accept incoming connections
		Runnable runnable = new ServerConnection(serverConnection);
		Thread thread = new Thread(runnable);
		thread.start();
		log.info("Connection Management Thread Running");


		/*
		Messages json = new Messages();
		System.out.println(json.getHandshakeRequest("bigdata.cis.unimelb.edu.au",8121));

        HashMap<String, Integer> peers  = new HashMap<String, Integer>() {{
            put("sunrise.cis.unimelb.edu.au", Integer.parseInt("8111"));
            put("bigdata.cis.unimelb.edu.au", Integer.parseInt("8500"));
        }};
        System.out.println(json.getConnectionRefused(peers, "Sorry, connection refused"));
        String md5 = "074195d72c47315efae797b69393e5e5";
        long lastModified = 1553417607;
        long fileSize = 45787;
        String pathName = "test.jpg";
        boolean status = false;
        long position = 5; 
        long length = 6; 
        String content = "aGVsbG8K";
        */

        /*
		System.out.println(json.getFileCreateRequest(md5, lastModified, fileSize, pathName));
		System.out.println(json.getFileCreateResponse(md5, lastModified, fileSize, pathName, status));
		System.out.println(json.getInvalidProtocol());
		System.out.println(json.getFileBytesRequest(md5, lastModified, fileSize, pathName, position, length));
         */
		
		//Get peers string from properties and process peers in an Array
		//Splitting and Comma, and removing white spaces
		String[] peersArray = Configuration.getConfigurationValue("peers").split("\\s*,\\s*");


		// List<Document> peersList = new ArrayList<>();

		//Create HasMap<Peer,<host,port>>
		HashMap<String,HashMap<String,String>> peersMap = new HashMap<String, HashMap<String, String>>();
		HashMap<String,String> hostPort = new HashMap<String,String>();

		//Peer index
		int peerIndex = 0;

		// Iterate over peers in Peers Array, and split at : to get Port and Host, and add each to peersMap
		for (String peer: peersArray) {
			peerIndex++;
			String[] peerHostPort = peer.split(":");
			String host = peerHostPort[0];
			String port = peerHostPort[1];
			hostPort.put(host,port);
			peersMap.put("Peer" + peerIndex, new HashMap<>(hostPort));
			hostPort.clear();
		}

		// Iterate over Peers and connect to server.
		for (Map.Entry<String, HashMap<String,String>> peerMapEntry : peersMap.entrySet()) {
			//String peer = peerMapEntry.getKey();
			HashMap<String,String> hostPortMap = new HashMap<String,String>(peerMapEntry.getValue());

			for (Map.Entry<String, String> hostPortMapEntry : hostPortMap.entrySet()) {
				String host = hostPortMapEntry.getKey();
				String port = hostPortMapEntry.getValue();

				serverConnection.connect(host,port);
				Thread.sleep(1000);
				//System.out.println(value);
			}
						
		}
	}
}
