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

    private void handleJsonClientMsg(Document json, Socket clientSocket, BufferedReader in,BufferedWriter out) throws IOException, NoSuchAlgorithmException {
        String command = json.getString("command");
        String invalidProtocol;
        Document fileDescriptor;
        String md5;
        String lastModified;
        String fileSize;
        String pathName;
        String message;
        Long position;
        Long length;
        String content;

        boolean status;

        switch (command){

            case "FILE_CREATE_REQUEST":

                fileDescriptor = (Document) json.get("fileDescriptor");
                md5 = fileDescriptor.getString("md5");
                lastModified = fileDescriptor.getString("lastModified");
                fileSize = fileDescriptor.getString("fileSize");

                pathName = json.getString("pathName");

                // Check message credibility
                // Check lastModified, check fileSize, and pathName (not sure what md5 is)
                //if ()

                        String fileCreateResponse = this.json.getFileCreateResponse(md5, Long.parseLong(lastModified), Long.parseLong(fileSize), pathName, message, status);
                        out.write(fileCreateResponse+"\n");
                        out.flush();
                        /*
                        // Start peer thread to manage P2P communication in separate thread per peer

                        Runnable runnable = new ServerMain(serverMain, clientSocket, in, out);
                        Thread thread = new Thread(runnable);
                        thread.start();
                        log.info("P2P Connection Thread Running");

                else {
                    invalidProtocol = this.json.getInvalidProtocol("message must contain xx field");
                    log.info("Invalid message between port: "+ this.port + " @ host: "+ this.advertisedHost +" and port: " + port +" @ host: " + host + invalidProtocol);
                    out.write(invalidProtocol+"\n");
                    out.flush();
                }*/
            break;

            case "FILE_CREATE_RESPONSE":

                fileDescriptor = (Document) json.get("fileDescriptor");
                md5 = fileDescriptor.getString("md5");
                lastModified = fileDescriptor.getString("lastModified");
                fileSize = fileDescriptor.getString("fileSize");

                pathName = json.getString("pathName");
                message = json.getString("message");
                status = json.getBoolean("status");

                // Check whether it was successful
                if (status == true) {
                }
                else {
                    // retry?
                }


            case "FILE_DELETE_REQUEST":

                fileDescriptor = (Document) json.get("fileDescriptor");
                md5 = fileDescriptor.getString("md5");
                lastModified = fileDescriptor.getString("lastModified");
                fileSize = fileDescriptor.getString("fileSize");

                pathName = json.getString("pathName");

                // Check message credibility
                //if ()

                    //message="depends on checks made before this point";
                    //status; depends whether successful or not
                    String fileDeleteResponse = this.json.getFileDeleteResponse(md5, Long.parseLong(lastModified), Long.parseLong(fileSize), pathName, message, status);
                    out.write(fileDeleteResponse+"\n");
                    out.flush();

            case "FILE_DELETE_RESPONSE":

                fileDescriptor = (Document) json.get("fileDescriptor");
                md5 = fileDescriptor.getString("md5");
                lastModified = fileDescriptor.getString("lastModified");
                fileSize = fileDescriptor.getString("fileSize");

                pathName = json.getString("pathName");
                message = json.getString("message");
                status = json.getBoolean("status");

                // Check whether it was successful
                if (status == true) {
                }
                else {
                    // retry?
                }

            case "FILE_MODIFY_REQUEST":

                fileDescriptor = (Document) json.get("fileDescriptor");
                md5 = fileDescriptor.getString("md5");
                lastModified = fileDescriptor.getString("lastModified");
                fileSize = fileDescriptor.getString("fileSize");

                pathName = json.getString("pathName");

                // Check message credibility
                //if ()

                    String fileModifyResponse = this.json.getFileModifyResponse(md5, Long.parseLong(lastModified), Long.parseLong(fileSize), pathName, message, status);
                    out.write(fileModifyResponse+"\n");
                    out.flush();

            case "FILE_MODIFY_RESPONSE":

                fileDescriptor = (Document) json.get("fileDescriptor");
                md5 = fileDescriptor.getString("md5");
                lastModified = fileDescriptor.getString("lastModified");
                fileSize = fileDescriptor.getString("fileSize");

                pathName = json.getString("pathName");
                message = json.getString("message");
                status = json.getBoolean("status");

                // Check whether it was successful
                if (status == true) {
                }
                else {
                    // retry?
                }

            case "DIRECTORY_CREATE_REQUEST":

                String pathName = json.getString("pathName");

                // Check message credibility
                //if ()

                    String directoryCreateResponse = this.json.getDirectoryCreateResponse(pathName, message, status);
                    out.write(directoryCreateResponse+"\n");
                    out.flush();

            case "DIRECTORY_CREATE_RESPONSE":

                pathName = json.getString("pathName");
                message = json.getString("message");
                status = json.getBoolean("status")

                // Check whether it was successful
                if (status == true) {
                }
                else {
                    // retry?
                }

            case "DIRECTORY_DELETE_REQUEST":

                pathName = json.getString("pathName");

                // Check message credibility
                //if ()

                    String directoryDeleteResponse = this.json.getDirectoryDeleteResponse(pathName, message, status);
                    out.write(directoryDeleteResponse+"\n");
                    out.flush();

            case "DIRECTORY_DELETE_RESPONSE":

                pathName = json.getString("pathName");
                message = json.getString("message");
                status = json.getBoolean("status");

                // Check whether it was successful
                if (status == true) {
                }
                else {
                    // retry?
                }

            case "FILE_BYTES_REQUEST":

                fileDescriptor = (Document) json.get("fileDescriptor");
                md5 = fileDescriptor.getString("md5");
                lastModified = fileDescriptor.getString("lastModified");
                fileSize = fileDescriptor.getString("fileSize");

                pathName = json.getString("pathName");
                position = json.getLong("position");
                length = json.getLong("length");

            case "FILE_BYTES_RESPONSE":

                fileDescriptor = (Document) json.get("fileDescriptor");
                md5 = fileDescriptor.getString("md5");
                lastModified = fileDescriptor.getString("lastModified");
                fileSize = fileDescriptor.getString("fileSize");

                pathName = json.getString("pathName");
                position = json.getLong("position");
                length = json.getLong("length");
                message = json.getString("message");
                content = json.getString("content");
                status = json.getBoolean("status");

                // Check whether it was successful
                if (status == true) {
                }
                else {
                    // retry?
                }



            /*default:
                invalidProtocol = this.json.getInvalidProtocol("Expected HANDSHAKE_REQUEST");
                log.info("Invalid message between port: "+ this.port + " @ host: "+ this.advertisedHost +" and port: " + port +" @ host: " + host + invalidProtocol);
                out.write(invalidProtocol+"\n");
                out.flush();*/

        }

    }


    private Document proocessJSONstring(String jsonMessage) {
        Document json = new Document();
        json = Document.parse(jsonMessage);
        return json;
    }

	@Override
	public void run() {

		/*BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
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

	}*/
}