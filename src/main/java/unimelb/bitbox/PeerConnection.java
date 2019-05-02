package unimelb.bitbox;

import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.logging.Logger;

public class PeerConnection {
    private static Logger log = Logger.getLogger(PeerConnection.class.getName());

    private String clientHostName;
    private Integer clientPort;
    private PeerClient client;
    private PeerServer server;

    public PeerConnection(Socket clientSocket) throws IOException, NoSuchAlgorithmException {
        this.clientHostName = clientSocket.getInetAddress().getHostName();
        this.clientPort = clientSocket.getLocalPort();
        BufferedReader in
                = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
        BufferedWriter out
                = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8));
        this.client = new PeerClient(out);
        this.server = new PeerServer(in, out);
        this.start();
    }

    public void onNewFileSystemEvent(FileSystemEvent event) {
        this.client.enqueue(event);
    }

    public void onNewSyncEvents(List<FileSystemEvent> syncEvents) {
        for (FileSystemEvent event : syncEvents) {
            client.enqueue(event);
        }
    }

    /**
     * Stops PeerClient and PeerServer threads
     * for this established connection.
     */
    public void close() {
        this.client.close();
        this.server.close();
    }

    /**
     * Starts both PeerClient and PeerServer threads
     */
    private void start() {
        Thread clientThread = new Thread(client);
        Thread serverThread = new Thread(server);
        clientThread.start();
        serverThread.start();
        log.info("PeerClient Thread running");
        log.info("PeerServer Thread running");
    }

    public String getHost() {
        return this.clientHostName;
    }

    public int getPort() {
        return this.clientPort;
    }
}