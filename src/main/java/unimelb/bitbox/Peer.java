package unimelb.bitbox;

import java.awt.List;
import java.io.IOException;
import java.lang.reflect.Array;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
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
		int maximumIncommingConnections = Integer.parseInt(Configuration.getConfigurationValue("maximumIncommingConnections"));
		ServerMain serverMain = new ServerMain();
		ServerConnection serverConnection = new ServerConnection(advertisedName, serverPort, maximumIncommingConnections, serverMain);
		
		//Start Server Thread to accept incoming connections
		Runnable runnable = new ServerConnection(serverConnection);
		Thread thread = new Thread(runnable);
		thread.start();
		log.info("Connection Management Thread Running");

		/* Testing Block
		Messages json = new Messages();
		System.out.println(json.getHandshakeRequest("bigdata.cis.unimelb.edu.au",8121));
		   
        HashMap<String, String> peers  = new HashMap<String, String>() {{
            put("8111", "sunrise.cis.unimelb.edu.au");
            put("8500", "sunrise.cis.unimelb.edu.au");
        }};

        System.out.println(json.getConnectionRefused(peers));
        String md5 = "074195d72c47315efae797b69393e5e5";
        long lastModified = 1553417607;
        long fileSize = 45787;
        String pathName = "test.jpg";
        boolean status = false;
        long position = 5; 
        long length = 6; 
        String content = "aGVsbG8K";
        
		System.out.println(json.getFileCreateRequest(md5, lastModified, fileSize, pathName));
		System.out.println(json.getFileCreateResponse(md5, lastModified, fileSize, pathName, status));
		System.out.println(json.getInvalidProtocol());
		System.out.println(json.getFileBytesRequest(md5, lastModified, fileSize, pathName, position, length));
		for (int i = 0; i < 10;i++)
		*/
		
		//Get peers string from properties and process peers in an Array
		//Splitting and Comma, and removing white spaces
		
		String[] peersArray = Configuration.getConfigurationValue("peers").split("\\s*,\\s*");

		//Create HasMap<Peer,<host,port>>
		HashMap<String,HashMap<String,String>> peersMap = new HashMap<String, HashMap<String, String>>();
		HashMap <String,String> hostPort = new HashMap<String,String>();

		//Peer index
		int peerIndex = 0;

		// Iterate over Oeers in Peers Array, and split at : to get Port and Host, and add each to peersMap
		for (String peer: peersArray) {
			peerIndex++;
			String[] peerHostPort = peer.split(":"); 
			String host = peerHostPort[0];  
			String port = peerHostPort[1];  
			hostPort.put(host,port);
			peersMap.put("Peer" + peerIndex, new HashMap<String,String>(hostPort));
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
