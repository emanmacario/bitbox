package unimelb.bitbox;

import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;

import java.io.*;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.logging.Logger;

public class PeerConnection {
    private static Logger log = Logger.getLogger(PeerConnection.class.getName());

    private String peerHost;
    private Integer peerPort;
    private PeerClient client;
    private PeerServer server;

    public PeerConnection(String host, int port, Socket socket, ConnectionObserver observer) throws IOException, NoSuchAlgorithmException {
        log.info("Connection to " + host + ":" + port + " established");
        this.peerHost = host;
        this.peerPort = port;
        this.client = new PeerClient(host, port, socket);
        this.server = new PeerServer(this.client, host, port, socket, observer);
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
     * Starts both PeerClient and PeerServer threads
     */
    private void start() {
        Thread clientThread = new Thread(client);
        Thread serverThread = new Thread(server);
        clientThread.start();
        serverThread.start();
        log.info("PeerClient thread for " + peerHost + ":" + peerPort + " started");
        log.info("PeerServer thread for " + peerHost + ":" + peerPort + " started");
    }

    public String getHost() {
        return this.peerHost;
    }

    public int getPort() {
        return this.peerPort;
    }
}