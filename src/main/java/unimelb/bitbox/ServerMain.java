package unimelb.bitbox;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
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
    Messages json = new Messages();

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

    private void directoryDeleteContents(File file, File original) {
	    File[] contents = file.listFiles();
	    if (contents != null) {
            for (File f : contents) {
                directoryDeleteContents(f, original);
            }
        }
	    if (file != original) {
	        file.delete();
        }
	}


    private void handleJsonClientMsg(Document json, Socket clientSocket, BufferedReader in,BufferedWriter out) throws IOException, NoSuchAlgorithmException {
        Long blockSize = Long.parseLong(Configuration.getConfigurationValue("blockSize"));
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
        ByteBuffer src;

        boolean status;

        switch (command){

            case "FILE_CREATE_REQUEST":

                fileDescriptor = (Document) json.get("fileDescriptor");
                md5 = fileDescriptor.getString("md5");
                lastModified = fileDescriptor.getString("lastModified");
                fileSize = fileDescriptor.getString("fileSize");

                pathName = json.getString("pathName");
                status = false; // false until proven otherwise
                if (Long.parseLong(fileSize)<=blockSize){
                    length = Long.parseLong(fileSize);
                }else{
                    length = blockSize;
                }

                // TODO Check all this
                // Check message credibility
                if (fileSystemManager.isSafePathName(pathName)) {
                    try {
                        if (!fileSystemManager.fileNameExists(pathName)) {
                            if (fileSystemManager.createFileLoader(pathName, md5, length, Long.parseLong(lastModified))) {
                                fileSystemManager.checkShortcut(pathName);
                                status = true;
                                message = "file created";
                            } else {
                                System.out.println("Error: FileLoader creation unsuccessful");
                                message = "there was a problem creating the file";
                            }
                        }else {
                            System.out.println("Error: File " + pathName + " already exists!");
                            message = "pathname already exists";
                        }
                        /*File new_file = new File(pathName);
                        if (!fileSystemManager.fileNameExists(pathName)) {
                            new_file.createNewFile();
                            status = true;
                            System.out.println(pathName + " File Created");
                        } else {
                            System.out.println("Error: File " + pathName + " already exists!");
                            status = false;
                        }*/
                    } catch (Exception e) {
                        e.printStackTrace();
                        message = "there was a problem creating the file";
                    }
                } else {
                    System.out.println("Error: PathName " + pathName + " is unsafe");
                    message = "unsafe pathname given";
                    status = false;
                }

                String fileCreateResponse = this.json.getFileCreateResponse(md5, Long.parseLong(lastModified), Long.parseLong(fileSize), pathName, message, status);
                out.write(fileCreateResponse+"\n");
                out.flush();
                break;

            case "FILE_CREATE_RESPONSE":

                fileDescriptor = (Document) json.get("fileDescriptor");
                md5 = fileDescriptor.getString("md5");
                lastModified = fileDescriptor.getString("lastModified");
                fileSize = fileDescriptor.getString("fileSize");

                pathName = json.getString("pathName");
                message = json.getString("message");
                status = json.getBoolean("status");

                if (Long.parseLong(fileSize)<=blockSize){
                    length = Long.parseLong(fileSize);
                }else length = blockSize;

                // Check whether it was successful
                if (status) {
                    // Needs to now send a FILE_BYTES_REQUEST
                    position = Long.parseLong("0");
                    String fileBytesRequest = this.json.getFileBytesRequest(md5, Long.parseLong(lastModified), Long.parseLong(fileSize), pathName, position, length);
                    out.write(fileBytesRequest+"\n");
                    out.flush();
                    break;
                }
                else {
                    // TODO File create response else statement
                    // retry?
                }


            case "FILE_DELETE_REQUEST":

                fileDescriptor = (Document) json.get("fileDescriptor");
                md5 = fileDescriptor.getString("md5");
                lastModified = fileDescriptor.getString("lastModified");
                fileSize = fileDescriptor.getString("fileSize");

                pathName = json.getString("pathName");

                status = false;

                // Check message credibility
                if (fileSystemManager.isSafePathName(pathName)) {
                    try {
                        if (fileSystemManager.deleteFile(pathName, Long.parseLong(lastModified), md5)) {
                            status = true;
                            message = "file deleted";
                            System.out.println(pathName + " File Deleted");
                        } else {
                            System.out.println("Error: File " + pathName + " does not exist");
                            message = "file does not exist";
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        message = "there was a problem deleting the file";
                    }
                }else {
                    message = "unsafe pathname given";
                }

                String fileDeleteResponse = this.json.getFileDeleteResponse(md5, Long.parseLong(lastModified), Long.parseLong(fileSize), pathName, message, status);
                out.write(fileDeleteResponse+"\n");
                out.flush();
                break;

            case "FILE_DELETE_RESPONSE":

                fileDescriptor = (Document) json.get("fileDescriptor");
                md5 = fileDescriptor.getString("md5");
                lastModified = fileDescriptor.getString("lastModified");
                fileSize = fileDescriptor.getString("fileSize");

                pathName = json.getString("pathName");
                message = json.getString("message");
                status = json.getBoolean("status");

                // TODO File delete response
                // Check whether it was successful
                if (status) {
                }
                else {
                    // retry?
                }

            case "FILE_MODIFY_REQUEST": // not done

                fileDescriptor = (Document) json.get("fileDescriptor");
                md5 = fileDescriptor.getString("md5");
                lastModified = fileDescriptor.getString("lastModified");
                fileSize = fileDescriptor.getString("fileSize");

                pathName = json.getString("pathName");

                status = false;
                message = null;

                // TODO File modify request
                // Check message credibility
                //if ()

                String fileModifyResponse = this.json.getFileModifyResponse(md5, Long.parseLong(lastModified), Long.parseLong(fileSize), pathName, message, status);
                out.write(fileModifyResponse+"\n");
                out.flush();
                break;

            case "FILE_MODIFY_RESPONSE":

                fileDescriptor = (Document) json.get("fileDescriptor");
                md5 = fileDescriptor.getString("md5");
                lastModified = fileDescriptor.getString("lastModified");
                fileSize = fileDescriptor.getString("fileSize");

                pathName = json.getString("pathName");
                message = json.getString("message");
                status = json.getBoolean("status");

                // TODO file modify response
                // Check whether it was successful
                if (status) {
                    // TODO
                    message = "modify still needs to be done";
                }
                else {
                    // retry?
                }

            case "DIRECTORY_CREATE_REQUEST":

                pathName = json.getString("pathName");

                status = false;

                // Check message credibility
                if (fileSystemManager.isSafePathName(pathName)) {
                    try {
                        if (!fileSystemManager.dirNameExists(pathName)) {
                            status = fileSystemManager.makeDirectory(pathName);
                            if (status) {
                                System.out.println(pathName + " Directory Created");
                                message = "directory created";
                                status = true;
                            } else {
                                System.out.println(pathName + " Directory not Created");
                                message = "there was a problem creating the directory";
                            }
                        } else {
                            System.out.println("Error: Directory " + pathName + " already exists!");
                            message = "pathname already exists";
                            status = false;
                        }
                    } catch (Exception e) {
                        message = "there was a problem creating the directory";
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("Error: PathName " + pathName + " is unsafe");
                    message = "unsafe pathname given";
                    status = false;
                }

                String directoryCreateResponse = this.json.getDirectoryCreateResponse(pathName, message, status);
                out.write(directoryCreateResponse+"\n");
                out.flush();
                break;

            case "DIRECTORY_CREATE_RESPONSE":

                pathName = json.getString("pathName");
                message = json.getString("message");
                status = json.getBoolean("status");

                // TODO directory create response
                // Check whether it was successful
                if (!status) {
                    //retry?
                }


            case "DIRECTORY_DELETE_REQUEST":

                pathName = json.getString("pathName");
                status = false;
                // Check message credibility
                if (fileSystemManager.isSafePathName(pathName)) {
                    try {
                        File delete_dir = new File(pathName);
                        if (fileSystemManager.dirNameExists(pathName)) {
                            directoryDeleteContents(delete_dir, delete_dir);
                            status = fileSystemManager.deleteDirectory(pathName);
                            System.out.println(pathName + " Directory Created");
                            message = "directory created";
                        } else {
                            System.out.println("Error: Directory " + pathName + " does not exist!");
                            message = "directory/pathname does not exist";
                        }
                    } catch (Exception e) {
                        message = "there was a problem deleting the directory";
                        e.printStackTrace();
                    }
                } else {
                    message = "unsafe pathname given";
                }

                String directoryDeleteResponse = this.json.getDirectoryDeleteResponse(pathName, message, status);
                out.write(directoryDeleteResponse+"\n");
                out.flush();
                break;

            case "DIRECTORY_DELETE_RESPONSE":

                pathName = json.getString("pathName");
                message = json.getString("message");
                status = json.getBoolean("status");

                // TODO directory delete response
                // Check whether it was successful
                if (!status) {
                    //retry?
                }

            case "FILE_BYTES_REQUEST":

                fileDescriptor = (Document) json.get("fileDescriptor");
                md5 = fileDescriptor.getString("md5");
                lastModified = fileDescriptor.getString("lastModified");
                fileSize = fileDescriptor.getString("fileSize");

                pathName = json.getString("pathName");
                position = json.getLong("position");
                length = json.getLong("length");

                status = false;
                content = null;

                src = fileSystemManager.readFile(md5, position, length);
                if (src!=null) {
                    // convert src to content string
                    try {
                        content = StandardCharsets.UTF_8.decode(src).toString();
                        message = "successfully read";
                        status=true;
                    } catch (Exception e) {
                        message = "unsuccessfully read";
                        e.printStackTrace();
                    }
                } else {
                    message = "unsuccessfully read";
                }
                String fileBytesResponse = this.json.getFileBytesResponse(md5, Long.parseLong(lastModified), Long.parseLong(fileSize), pathName, position, length, content, message, status);
                out.write(fileBytesResponse+"\n");
                out.flush();
                break;

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

                // TODO surely the below is wrong? Why do it again?
                src = fileSystemManager.readFile(md5, position, length);

                // Check whether it was successful
                if (status) {
                    fileSystemManager.writeFile(pathName, src, position);
                    if (!fileSystemManager.checkWriteComplete(pathName)){
                        //TODO send another file bytes request
                        String fileBytesRequest = this.json.getFileBytesRequest(md5, Long.parseLong(lastModified), Long.parseLong(fileSize), pathName, position, length);
                        out.write(fileBytesRequest+"\n");
                        out.flush();
                        break;
                    }
                }
                else {
                    // retry?
                }



            /*default:
                invalidProtocol = this.json.getInvalidProtocol("Expected HANDSHAKE_REQUEST");
                log.info("Invalid message between port: "+ this.port + " @ host: "+ this.advertisedHost +" and port: " + port +" @ host: " + host + invalidProtocol);
                out.write(invalidProtocol+"\n");
                out.flush();*/

                break;
            default:
                invalidProtocol = this.json.getInvalidProtocol("Unexpected command");
                out.write(invalidProtocol+"\n");
                out.flush();
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
// TODO
    @Override
    public FileSystemEvent processFileSystemEvent(FileSystemEvent fileSystemEvent) {
        //System.out.println("TEST");
        System.out.println(fileSystemEvent.event.name());
        eventsQ.add(fileSystemEvent.event.name());
        System.out.println( eventsQ.toString());
        System.out.println( eventsQ.size());
        return null;
    }
}