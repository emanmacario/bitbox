package unimelb.bitbox;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
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
        new ServerMain();
        
        //Testing code
        
        
        ServerConnection serverConnection = new ServerConnection( Configuration.getConfigurationValue("port"));
      
        Runnable runnable = new ServerConnection(serverConnection);
        Thread thread = new Thread(runnable);
        thread.start();
        
        log.info("Connection Management Thread Running");
        
        Messages json = new Messages();
        
        //System.out.println(json.getHandshakeRequest("bigdata.cis.unimelb.edu.au",8121));
           
        HashMap<String, String> peers  = new HashMap<String, String>() {{
            put("8111", "sunrise.cis.unimelb.edu.au");
            put("8500", "sunrise.cis.unimelb.edu.au");
        }};
        
       // System.out.println(json.getConnectionRefused(peers));
        String md5 = "074195d72c47315efae797b69393e5e5";
        long lastModified = 1553417607;
        long fileSize = 45787;
        String pathName = "test.jpg";
        
        boolean status = false;
        
        long position = 5; 
        long length = 6; 
        
        String content = "aGVsbG8K";
        
        
        //System.out.println(json.getFileCreateRequest(md5, lastModified, fileSize, pathName));
        
       // System.out.println(json.getFileCreateResponse(md5, lastModified, fileSize, pathName, status));
       // System.out.println(json.getInvalidProtocol());
      
       // System.out.println(json.getFileBytesRequest(md5, lastModified, fileSize, pathName, position, length));
       // for (int i = 0; i < 10;i++)
         serverConnection.connect();
        
  }
    
}
