package unimelb.bitbox;

import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

public class PeerServer implements Runnable {
    private static Logger log = Logger.getLogger(PeerServer.class.getName());

    private FileSystemManager fileSystemManager;
    private BufferedReader in;
    private BufferedWriter out;
    private boolean closed;

    // Thread constructor
    public PeerServer(FileSystemManager fileSystemManager, BufferedReader in, BufferedWriter out) {
        this.fileSystemManager = fileSystemManager;
        this.in = in;
        this.out = out;
        this.closed = false;
    }

    /**
     * Responsible for handling incoming requests from
     * a single peer, until that connection is closed.
     */
    @Override
    public void run() {

        while (!closed) {
            log.info("PeerServer Thread still running...");

            // Read any incoming request from the input buffer.
            // This blocks until an incoming message is received.
            String clientMessage = null;
            try {
                while ((clientMessage = in.readLine()) != null) {
                    // Logging
                    log.info("INCOMING " + Thread.currentThread().getName() + ": " + clientMessage);

                    // Parse the request into a JSON object
                    Document json = Document.parse(clientMessage);

                    // Handle the incoming request
                    try {
                        handleIncomingClientMessage(json);
                    } catch (NoSuchAlgorithmException | IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Handles an incoming client message, sends a response.
     * @param clientMsg incoming client message in JSON
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    private void handleIncomingClientMessage(Document clientMsg) throws NoSuchAlgorithmException, IOException {
        String command = clientMsg.getString("command");
        String response;
        switch (command) {
            case "DIRECTORY_CREATE_REQUEST":
                response = directoryCreateResponse(clientMsg);
                send(response);
                break;
            case "DIRECTORY_DELETE_REQUEST":
                response = directoryDeleteResponse(clientMsg);
                send(response);
                break;
            case "FILE_CREATE_REQUEST":
            case "FILE_DELETE_REQUEST":
            case "FILE_MODIFY_REQUEST":
            case "FILE_BYTES_REQUEST":
                // TODO: Process above four request types
                log.info("No implementation yet for " + command);
                break;
            case "DIRECTORY_CREATE_RESPONSE":
            case "DIRECTORY_DELETE_RESPONSE":
            case "FILE_CREATE_RESPONSE":
            case "FILE_DELETE_RESPONSE":
            case "FILE_MODIFY_RESPONSE":
            case "FILE_BYTES_RESPONSE":
                // TODO: Process all response types
                log.info("INCOMING RESPONSE: " + clientMsg.toString());
                break;
            default:
                // Invalid protocol
                response = Messages.getInvalidProtocol("invalid command");
                send(response);
                break;
        }
    }

    /**
     * Get response to the client's request for creating a directory.
     * @param request
     * @return response
     */
    private String directoryCreateResponse(Document request) {
        String message;
        String response;
        String pathName = request.getString("pathName");
        if (fileSystemManager.fileNameExists(pathName)) {
            message = "pathname already exists";
            response = Messages.getDirectoryCreateResponse(pathName, message, false);
        } else if (!fileSystemManager.isSafePathName(pathName)) {
            message = "unsafe pathname given";
            response = Messages.getDirectoryCreateResponse(pathName, message, false);
        } else {
            message = "directory created";
            response = Messages.getDirectoryCreateResponse(pathName, message, true);
            fileSystemManager.makeDirectory(pathName);
        }
        return response;
    }

    /**
     * Get response to the client's request for deleting a directory.
     * @param request
     * @return response
     */
    private String directoryDeleteResponse(Document request) {
        String message;
        String response;
        String pathName = request.getString("pathName");
        if (!fileSystemManager.isSafePathName(pathName)) {
            message = "unsafe pathname given";
            response = Messages.getDirectoryDeleteResponse(pathName, message, false);
        } else if (!fileSystemManager.fileNameExists(pathName)) {
            message = "pathname does not exist";
            response = Messages.getDirectoryDeleteResponse(pathName, message, false);
        } else {
            // Attempt to delete the directory
            boolean status = fileSystemManager.deleteDirectory(pathName);
            message =  status ? "directory deleted" : "there was a problem deleting the directory";
            response = Messages.getDirectoryDeleteResponse(pathName, message, status);
        }
        return response;
    }

    public void close() {
        this.closed = true;
    }

    /**
     * Sends a request/response string to the connected peer.
     * Appends a newline char to the outgoing message.
     * @param message request or response message
     * @throws IOException
     */
    private void send(String message) throws IOException {
        out.write(message + '\n');
        out.flush();
        log.info("Sending: " + message);
    }
}
