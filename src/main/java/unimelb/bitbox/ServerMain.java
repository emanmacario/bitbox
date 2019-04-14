package unimelb.bitbox;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.logging.Logger;

import unimelb.bitbox.util.Configuration;
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
	
	public ServerMain() throws NumberFormatException, IOException, NoSuchAlgorithmException {
		//Main Server (self) constructor
		fileSystemManager=new FileSystemManager(Configuration.getConfigurationValue("path"),this);
	}

	public ServerMain(String host, Long port) throws NoSuchAlgorithmException, IOException {
		// Peer2Peer constructor.
		fileSystemManager=new FileSystemManager(Configuration.getConfigurationValue("path"),this);
		this.port = port;
		this.host = host;
		peersConnected.put(host, port);
		
		
	}

	public ServerMain(ServerMain serverMain) {
		this.serverMain = serverMain;
	}

	@Override
	public FileSystemEvent processFileSystemEvent(FileSystemEvent fileSystemEvent) {
		//System.out.println("TEST");
		//System.out.println(fileSystemEvent.event.name());
		ServerConnection.eventsQ.add(fileSystemEvent.event.name());
		//System.out.println(ServerConnection.eventsQ.toString());
		//System.out.println(ServerConnection.eventsQ.size());
		return fileSystemEvent;
		
	}

	@Override
	public void run() {
		while (true) {
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
		
	}
	
}
