package unimelb.bitbox.protocols;

import unimelb.bitbox.connection.ConnectionObserver;
import unimelb.bitbox.util.*;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.logging.Logger;

public class PeerServer implements FileSystemObserver, Runnable {
    private static Logger log = Logger.getLogger(PeerServer.class.getName());

    private static FileSystemManager fileSystemManager;
    private PeerClient client;
    private ConnectionObserver observer;
    private String host;
    private int port;
    private DatagramSocket socket;
    private BufferedReader in;
    private BufferedWriter out;
    private long blockSize;
    private String mode;

    /**
     * TCP PeerServer constructor
     * @param client the corresponding peer client thread object
     * @param host the peer host name
     * @param port the peer port number
     * @param socket the peer socket
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    public PeerServer(PeerClient client, String host, int port, Socket socket, ConnectionObserver observer)
            throws NoSuchAlgorithmException, IOException {
        if (fileSystemManager == null) {
            fileSystemManager = new FileSystemManager(Configuration.getConfigurationValue("path"), this);
        }
        this.client = client;
        this.host = host;
        this.port = port;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        this.observer = observer;
        this.mode = Configuration.getConfigurationValue("mode");
        if (this.mode.equals("tcp")) {
            this.blockSize = Long.parseLong(Configuration.getConfigurationValue("blockSize"));
        } else {
            this.blockSize = Math.min(Long.parseLong(Configuration.getConfigurationValue("blockSize")), 8192);
        }
    }

    // UDP PeerServer constructor
    public PeerServer(PeerClient client, String host, int port, DatagramSocket socket, ConnectionObserver observer)
            throws NoSuchAlgorithmException, IOException {
        if (fileSystemManager == null) {
            fileSystemManager = new FileSystemManager(Configuration.getConfigurationValue("path"), this);
        }
        this.client = client;
        this.host = host;
        this.port = port;
        this.socket = socket;
        this.observer = observer;
        this.mode = Configuration.getConfigurationValue("mode");
        if (this.mode.equals("tcp")) {
            this.blockSize = Long.parseLong(Configuration.getConfigurationValue("blockSize"));
        } else {
            this.blockSize = Math.min(Long.parseLong(Configuration.getConfigurationValue("blockSize")), 8192);
        }
    }

    @Override
    public void processFileSystemEvent(FileSystemEvent fileSystemEvent) {
        // Do nothing, let the client thread handle outgoing requests
    }

    /**
     * Responsible for handling incoming requests from
     * a single peer, until that connection is closed.
     */
    @Override
    public void run() {
        if (mode.equals("tcp")) {
            // Read any incoming request from the input buffer.
            // This blocks until an incoming message is received.
            String clientMessage;
            try {
                while ((clientMessage = in.readLine()) != null) {
                    // Parse the request into a JSON object
                    Document clientMessageJSON = Document.parse(clientMessage);

                    // Handle the incoming request
                    try {
                        handleIncomingClientMessage(clientMessageJSON);
                    } catch (NoSuchAlgorithmException | IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                observer.disconnect(host, port);
                log.info("Connection to " + host + ":" + port + " has been terminated, PeerServer thread has stopped");
                // Terminate the PeerClient thread as well
                this.client.close();
            }
            observer.disconnect(host, port);
            log.info("Connection to " + host + ":" + port + " has been terminated, PeerServer thread has stopped");

            // Terminate the PeerClient thread as well
            this.client.close();
        }
    }

    /**
     * Handles an incoming client message, sends a response.
     * @param clientMessage incoming client message in JSON
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    public void handleIncomingClientMessage(Document clientMessage) throws NoSuchAlgorithmException, IOException {
        String command = clientMessage.getString("command");
        String response;
        switch (command) {
            case "DIRECTORY_CREATE_REQUEST":
                sendDirectoryCreateResponse(clientMessage);
                break;
            case "DIRECTORY_DELETE_REQUEST":
                sendDirectoryDeleteResponse(clientMessage);
                break;
            case "FILE_CREATE_REQUEST":
                sendFileCreateResponse(clientMessage);
                break;
            case "FILE_DELETE_REQUEST":
                sendFileDeleteResponse(clientMessage);
                break;
            case "FILE_MODIFY_REQUEST":
                sendFileModifyResponse(clientMessage);
                break;
            case "FILE_BYTES_REQUEST":
                sendFileBytesResponse(clientMessage);
                break;
            case "DIRECTORY_CREATE_RESPONSE":
                processDirectoryCreateResponse(clientMessage);
                break;
            case "DIRECTORY_DELETE_RESPONSE":
                processDirectoryDeleteResponse(clientMessage);
                break;
            case "FILE_CREATE_RESPONSE":
                processFileCreateResponse(clientMessage);
                break;
            case "FILE_DELETE_RESPONSE":
                processFileDeleteResponse(clientMessage);
                break;
            case "FILE_MODIFY_RESPONSE":
                processFileModifyResponse(clientMessage);
                break;
            case "FILE_BYTES_RESPONSE":
                processFileBytesResponse(clientMessage);
                break;
            case "INVALID_PROTOCOL":
                processInvalidProtocol(clientMessage);
                break;
            default:
                // Invalid protocol
                response = Messages.getInvalidProtocol("invalid command");
                send(response);
                break;
        }
    }

    /**
     * Send a response to the client's request for creating a directory.
     * @param request a directory create request in JSON
     */
    private void sendDirectoryCreateResponse(Document request) throws IOException {
        String pathName = request.getString("pathName");
        String message;
        boolean status;
        if (fileSystemManager.fileNameExists(pathName)) {
            message = "pathname already exists";
            status = false;
        } else if (!fileSystemManager.isSafePathName(pathName)) {
            message = "unsafe pathname given";
            status = false;
        } else {
            // Attempt to create the directory
            status = fileSystemManager.makeDirectory(pathName);
            message = status ? "directory created" : "there was a problem creating the directory";
        }
        // Create and send response
        String response = Messages.getDirectoryCreateResponse(pathName, message, status);
        send(response);
    }

    /**
     * Send a response to the client's request for deleting a directory.
     * @param request the direct delete request
     */
    private void sendDirectoryDeleteResponse(Document request) throws IOException {
        String pathName = request.getString("pathName");
        String message;
        boolean status;
        if (!fileSystemManager.isSafePathName(pathName)) {
            message = "unsafe pathname given";
            status = false;
        } else if (!fileSystemManager.fileNameExists(pathName)) {
            message = "pathname does not exist";
            status = false;
        } else {
            // Attempt to delete the directory
            status = fileSystemManager.deleteDirectory(pathName);
            message =  status ? "directory deleted" : "there was a problem deleting the directory";
        }
        String response = Messages.getDirectoryDeleteResponse(pathName, message, status);
        send(response);
    }

    /**
     * Send a response to the client's request for creating a file.
     * @param request a file create request in JSON
     */
    private void sendFileCreateResponse(Document request) throws NoSuchAlgorithmException, IOException {
        String pathName = request.getString("pathName");
        Document fileDescriptor = (Document) request.get("fileDescriptor");
        String md5 = fileDescriptor.getString("md5");
        long lastModified = fileDescriptor.getLong("lastModified");
        long fileSize = fileDescriptor.getLong("fileSize");

        // Validate the file create request and an appropriate response
        String message;
        boolean status;
        if (!fileSystemManager.isSafePathName(pathName)) {
            // Path name is unsafe
            message = "unsafe pathname given";
            status = false;
        } else if (fileSystemManager.fileNameExists(pathName)) {
            // File must not already exist
            message = "pathname already exists";
            status = false;
        } else {
            // Request was successful, try creating a file loader
            status = fileSystemManager.createFileLoader(pathName, md5, fileSize, lastModified);
            message = status ? "file loader ready" : "there was a problem creating the file";
        }
        // Send the response
        String response = Messages.getFileCreateResponse(md5, lastModified, fileSize, pathName, message, status);
        send(response);

        // If file create request was successful
        // then attempt to get the modified file
        if (status) {
            // Check if we can use a local copy
            if (!fileSystemManager.checkShortcut(pathName)) {
                // Otherwise, start requesting file bytes
                sendFileBytesRequests(pathName, md5, lastModified, fileSize);
            }
        }
    }

    /**
     * Send a response to the client's request for deleting a file.
     * @param request a file delete request in JSON
     */
    private void sendFileDeleteResponse(Document request) throws IOException {
        String pathName = request.getString("pathName");
        Document fileDescriptor = (Document) request.get("fileDescriptor");
        String md5 = fileDescriptor.getString("md5");
        long lastModified = fileDescriptor.getLong("lastModified");
        long fileSize = fileDescriptor.getLong("fileSize");

        String message;
        boolean status;
        if (!fileSystemManager.isSafePathName(pathName)) {
            message = "unsafe pathname given";
            status = false;
        } else if (!fileSystemManager.fileNameExists(pathName, md5)) {
            message = "pathname does not exist";
            status = false;
        } else {
            status = fileSystemManager.deleteFile(pathName, lastModified, md5);
            message =  status ? "file deleted" : "there was a problem deleting the file";
        }
        String response = Messages.getFileDeleteResponse(md5, lastModified, fileSize, pathName, message, status);
        send(response);
    }

    /**
     * Send a response to the client's request for modifying a file.
     * @param request a file modify request in JSON
     */
    private void sendFileModifyResponse(Document request) throws NoSuchAlgorithmException, IOException {
        String pathName = request.getString("pathName");
        Document fileDescriptor = (Document) request.get("fileDescriptor");
        String md5 = fileDescriptor.getString("md5");
        long lastModified = fileDescriptor.getLong("lastModified");
        long fileSize = fileDescriptor.getLong("fileSize");

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
            status = fileSystemManager.modifyFileLoader(pathName, md5, lastModified);
            message = status ? "file loader ready" : "there was a problem modifying the file";
        }
        // Send the response
        String response = Messages.getFileModifyResponse(md5, lastModified, fileSize, pathName, message, status);
        send(response);

        // If file modify request was successful
        // then attempt to get the modified file
        if (status) {
            // Check if we can use a local copy
            if (!fileSystemManager.checkShortcut(pathName)) {
                // Otherwise, start requesting file bytes
                sendFileBytesRequests(pathName, md5, lastModified, fileSize);
            }
        }
    }

    /**
     * Send a response to the client's request for file bytes.
     * @param request a file bytes request in JSON
     */
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
        } else {
            content = "";
        }
        // Create and send response
        String response = Messages.getFileBytesResponse(md5, lastModified, fileSize, pathName, position, length, content, message, status);
        send(response);
    }

    /**
     * Processes a server's file bytes response.
     * @param response a file bytes response in JSON
     */
    private void processFileBytesResponse(Document response) throws NoSuchAlgorithmException, IOException {
        String command = response.getString("command");
        String pathName = response.getString("pathName");
        long position = response.getLong("position");
        boolean status = response.getBoolean("status");
        String message = response.getString("message");
        log.info("received [" + command + "] from " + host +  ":" + port);

        if (!status) {
            fileSystemManager.cancelFileLoader(pathName);
            log.warning("failed to receive remote file " + pathName);
        } else {
            String content = response.getString("content");
            byte[] decodedBytes = Base64.getDecoder().decode(content);
            ByteBuffer decodedByteBuffer = ByteBuffer.wrap(decodedBytes);

            boolean success = fileSystemManager.writeFile(pathName, decodedByteBuffer, position);
            if (success) {
                fileSystemManager.checkWriteComplete(pathName);
            } else {
                log.warning("failed to write to file " + pathName);
                return;
            }
            log.info(command + " " + message);
        }
    }

    /**
     * Enqueues all the file byte request messages for a given file 'pathName'
     * and its size 'fileSize' into the PeerClient outgoing messages queue. Note
     * that this method does not actually send any file bytes requests itself.
     * @param pathName the path to the file
     * @param md5 the MD5 hash of the file
     * @param fileSize the size of the file
     * @param lastModified the last modified time of the file
     * @param fileSize the size of the file
     */
    private void sendFileBytesRequests(String pathName, String md5, long lastModified, long fileSize) {
        List<String> fileBytesRequests = new ArrayList<>();
        long position = 0;
        long length;

        // Special case: empty file
        if (fileSize == 0) {
            String fileBytesRequest =
                    Messages.getFileBytesRequest(md5, lastModified, fileSize, pathName, 0, 0);
            fileBytesRequests.add(fileBytesRequest);
            this.client.enqueue(fileBytesRequests);
            return;
        }

        // Requests blocks of size 'blockSize' bytes
        for (int i = 0; i < fileSize / blockSize; i++) {
            length = blockSize;
            String fileBytesRequest =
                    Messages.getFileBytesRequest(md5, lastModified, fileSize, pathName, position, length);
            fileBytesRequests.add(fileBytesRequest);
            position += blockSize;
        }
        // Request last block, which may be smaller than 'blockSize'
        if ((length = fileSize % blockSize) > 0) {
            String fileBytesRequest =
                    Messages.getFileBytesRequest(md5, lastModified, fileSize, pathName, position, length);
            fileBytesRequests.add(fileBytesRequest);
        }
        this.client.enqueue(fileBytesRequests);
    }

    private void processDirectoryCreateResponse(Document response) {
        String command = response.getString("command");
        boolean status = response.getBoolean("status");
        String message = response.getString("message");
        log.info("received [" + command + "] from " + host +  ":" + port);
        if (status) {
            log.info(command + " " + message);
        } else {
            log.warning(command + " " + message);
        }
    }

    private void processDirectoryDeleteResponse(Document response) {
        String command = response.getString("command");
        boolean status = response.getBoolean("status");
        String message = response.getString("message");
        log.info("received [" + command + "] from " + host +  ":" + port);
        if (status) {
            log.info(command + " " + message);
        } else {
            log.warning(command + " " + message);
        }
    }

    private void processFileCreateResponse(Document response) {
        String command = response.getString("command");
        boolean status = response.getBoolean("status");
        String message = response.getString("message");
        log.info("received [" + command + "] from " + host +  ":" + port);
        if (status) {
            log.info(command + " " + message);
        } else {
            log.warning(command + " " + message);
        }
    }

    private void processFileDeleteResponse(Document response) {
        String command = response.getString("command");
        boolean status = response.getBoolean("status");
        String message = response.getString("message");
        log.info("received [" + command + "] from " + host +  ":" + port);
        if (status) {
            log.info(command + " " + message);
        } else {
            log.warning(command + " " + message);
        }
    }

    private void processFileModifyResponse(Document response) {
        String command = response.getString("command");
        boolean status = response.getBoolean("status");
        String message = response.getString("message");
        log.info("received [" + command + "] from " + host +  ":" + port);
        if (status) {
            log.info(command + " " + message);
        } else {
            log.warning(command + " " + message);
        }
    }

    private void processInvalidProtocol(Document response) {
        String command = response.getString("command");
        String message = response.getString("message");
        log.warning("received [" + command + "] from " + host + ":" + port);
        log.warning(command + " " + message);
    }

    /**
     * Sends a request/response string to the connected peer.
     * Appends a newline char to the outgoing message.
     * @param message request or response message
     * @throws IOException
     */
    private void send(String message) throws IOException {
        log.info("sending to " + host + ":" + port + " " + message);
        if (mode.equals("tcp")) {
            out.write(message + '\n');
            out.flush();
        } else {
            InetAddress address = InetAddress.getByName(host);
            byte[] sendData = message.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, port);
            socket.send(sendPacket);
        }
    }
}


