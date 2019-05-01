package unimelb.bitbox;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.FileSystemManager.FileDescriptor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.logging.Logger;

public class PeerServer implements Runnable {
    private static Logger log = Logger.getLogger(PeerServer.class.getName());

    private FileSystemManager fileSystemManager;
    private BufferedReader in;
    private BufferedWriter out;
    private boolean closed;
    private long blockSize;

    /**
     * PeerServer constructor
     * @param fileSystemManager the file system manager
     * @param in input buffer for connection
     * @param out output buffer for connection
     */
    public PeerServer(FileSystemManager fileSystemManager, BufferedReader in, BufferedWriter out) {
        this.fileSystemManager = fileSystemManager;
        this.in = in;
        this.out = out;
        this.closed = false;
        this.blockSize = Long.parseLong(Configuration.getConfigurationValue("blockSize"));
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
                // This method sends FILE_BYTE_RESPONSE, and
                // possibly FILE_BYTE_REQUEST messages inside it.
                // TODO: Make all these methods consistent
                sendFileCreateResponse(clientMsg);
                break;
            case "FILE_DELETE_REQUEST":
                response = fileDeleteResponse(clientMsg);
                send(response);
                break;
            case "FILE_MODIFY_REQUEST":
                sendFileModifyResponse(clientMsg);
                break;
            case "FILE_BYTES_REQUEST":
                sendFileBytesResponse(clientMsg);
                break;
            case "DIRECTORY_CREATE_RESPONSE":
            case "DIRECTORY_DELETE_RESPONSE":
            case "FILE_CREATE_RESPONSE":
            case "FILE_DELETE_RESPONSE":
            case "FILE_MODIFY_RESPONSE":
                log.info("No implementation yet for " + command);
                break;
            case "FILE_BYTES_RESPONSE":
                processFileBytesResponse(clientMsg);
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

    private void sendFileCreateResponse(Document request) throws NoSuchAlgorithmException, IOException {
        String pathName = request.getString("pathName");
        Document fileDescriptor = (Document) request.get("fileDescriptor");
        String md5 = fileDescriptor.getString("md5");
        Long lastModified = fileDescriptor.getLong("lastModified");
        Long fileSize = fileDescriptor.getLong("fileSize");
        /*
        log.info("MD5 Hash: " + md5);
        log.info("Last Modified: " + lastModified);
        log.info("File Size: " + fileSize);
         */

        // Validate the file create request and an appropriate response
        String message;
        String response;
        if (!fileSystemManager.isSafePathName(pathName)) {
            // Path name is unsafe
            message = "unsafe pathname given";
            response = Messages.getFileCreateResponse(md5, lastModified, fileSize, pathName, message, false);
            send(response);
        } else if (fileSystemManager.fileNameExists(pathName)) {
            // File must not already exist
            message = "pathname already exists";
            response = Messages.getFileCreateResponse(md5, lastModified, fileSize, pathName, message, false);
            send(response);
        } else {
            // Request was successful
            message = "file loader ready";
            response = Messages.getFileCreateResponse(md5, lastModified, fileSize, pathName, message, true);
            // Create new file loader
            boolean created = fileSystemManager.createFileLoader(pathName, md5, fileSize, lastModified);
            if (!created) {
                message = "there was a problem creating the file";
                response = Messages.getFileCreateResponse(md5, lastModified, fileSize, pathName, message, false);
            }
            // TODO: Fix this sending logic
            send(response);

            // First, check if we can use a local copy. If
            // not, start sending file bytes requests
            if (!fileSystemManager.checkShortcut(pathName)) {
                sendFileBytesRequests(pathName, md5, lastModified, fileSize);
            }
        }
    }

    private String fileDeleteResponse(Document request) {
        String pathName = request.getString("pathName");
        Document fileDescriptor = (Document) request.get("fileDescriptor");
        String md5 = fileDescriptor.getString("md5");
        Long lastModified = fileDescriptor.getLong("lastModified");
        Long fileSize = fileDescriptor.getLong("fileSize");

        String message;
        boolean status;
        if (!fileSystemManager.isSafePathName(pathName)) {
            // Path name is unsafe
            message = "unsafe pathname given";
            status = false;
        } else if (!fileSystemManager.fileNameExists(pathName, md5)) {
            // File does not exist
            message = "pathname does not exist";
            status = false;
        } else {
            // Attempt to delete the file
            status = fileSystemManager.deleteFile(pathName, lastModified, md5);
            message =  status ? "file deleted" : "there was a problem deleting the file";
        }
        return Messages.getFileDeleteResponse(md5, lastModified, fileSize, pathName, message, status);
    }

    private void sendFileModifyResponse(Document request) throws NoSuchAlgorithmException, IOException {
        String pathName = request.getString("pathName");
        Document fileDescriptor = (Document) request.get("fileDescriptor");
        String md5 = fileDescriptor.getString("md5");
        Long lastModified = fileDescriptor.getLong("lastModified");
        Long fileSize = fileDescriptor.getLong("fileSize");

        String message;
        boolean status;
        if (!fileSystemManager.isSafePathName(pathName)) {
            message = "unsafe pathname given";
            status = false;
        } else if (!fileSystemManager.fileNameExists(pathName)) {
            message = "pathname does not exist";
            status = false;
        } else if (fileSystemManager.fileNameExists(pathName, md5)) {
            message = "file already exists with matching contents";
            status = false;
        } else {
            // Request was successful
            message = "file loader ready";
            status = true;
            // Create new file loader
            boolean created = fileSystemManager.modifyFileLoader(pathName, md5, lastModified);
            if (!created) {
                message = "there was a problem modifying the file";
                status = false;
            }
        }

        // Send the response
        String response = Messages.getFileModifyResponse(md5, lastModified, fileSize, pathName, message, status);
        send(response);

        // If file modify request was successful
        // then attempt to get the modified file
        if (status) {
            // But first, check if we can use a local copy to avoid
            // the other peer sending us a file we already have
            if (!fileSystemManager.checkShortcut(pathName)) {
                sendFileBytesRequests(pathName, md5, lastModified, fileSize);
            }
        }
    }

    private void sendFileBytesResponse(Document request) throws IOException, NoSuchAlgorithmException {
        String pathName = request.getString("pathName");
        Document fileDescriptor = (Document) request.get("fileDescriptor");
        String md5 = fileDescriptor.getString("md5");
        Long lastModified = fileDescriptor.getLong("lastModified");
        Long fileSize = fileDescriptor.getLong("fileSize");
        long position = request.getLong("position");
        long length = request.getLong("length");

        String message;
        boolean status;

        // Try to read the file
        ByteBuffer buffer = fileSystemManager.readFile(md5, position, length);
        if (buffer != null) {
            message = "successful read";
            status = true;
        } else {
            message = "unsuccessful read";
            status = false;
        }

        // If read was successful, encode content
        String content;
        if (status) {
            // Encode the file bytes in base 64
            byte[] encodedBuffer = Base64.getEncoder().encode(buffer.array());
            content = new String(encodedBuffer, StandardCharsets.UTF_8);
            //log.info("Encoded 'content': " + content);
        } else {
            content = "";
        }
        // Create and send response
        String response
                = Messages.getFileBytesResponse(md5, lastModified, fileSize, pathName, position, length, content, message, status);
        send(response);
    }


    private void processFileBytesResponse(Document response) throws NoSuchAlgorithmException, IOException {
        String pathName = response.getString("pathName");
        Document fileDescriptor = (Document) response.get("fileDescriptor");
        String md5 = fileDescriptor.getString("md5");
        Long lastModified = fileDescriptor.getLong("lastModified");
        Long fileSize = fileDescriptor.getLong("fileSize");
        Long position = response.getLong("position");
        Long length = response.getLong("length");
        boolean status = response.getBoolean("status");

        if (!status) {
            fileSystemManager.cancelFileLoader(pathName);
        } else {
            String content = response.getString("content");
            byte[] decodedBytes = Base64.getDecoder().decode(content);
            log.info("Decoded content string: " + new String(decodedBytes, StandardCharsets.UTF_8));
            ByteBuffer decodedByteBuffer = ByteBuffer.wrap(decodedBytes); // TODO: Check if this is correct

            boolean success = fileSystemManager.writeFile(pathName, decodedByteBuffer, position);
            if (success) {
                fileSystemManager.checkWriteComplete(pathName);
            } else {
                log.warning("Failed to write to file");
            }
        }
    }


    /**
     * Sends all the file byte request messages
     * for a given file 'pathName' and its
     * size 'fileSize'.
     * @param pathName the path to the file
     * @param fileSize the size of the file
     */
    private void sendFileBytesRequests(String pathName, String md5, long lastModified, long fileSize) throws IOException {
        long position = 0;
        long length;
        for (int i = 0; i < fileSize / blockSize; i++) {
            length = blockSize;
            String fileBytesRequest =
                    Messages.getFileBytesRequest(md5, lastModified, fileSize, pathName, position, length);
            send(fileBytesRequest);
            position += blockSize;
        }
        // Request last block, which may be smaller than 'blockSize'
        if ((length = fileSize % blockSize) > 0) {
            String fileBytesRequest =
                    Messages.getFileBytesRequest(md5, lastModified, fileSize, pathName, position, length);
            send(fileBytesRequest);
        }
    }


    private FileDescriptor getFileDescriptor(Document request) {
        // TODO: Use this to avoid duplicate code
        return null;
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
