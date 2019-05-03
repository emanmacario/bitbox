package unimelb.bitbox;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;
import unimelb.bitbox.util.FileSystemObserver;

import java.io.IOException;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Logger;

// A controller class for already established peer-to-peer connections
public class PeerConnectionController implements FileSystemObserver, ConnectionObserver {

    private static Logger log = Logger.getLogger(PeerConnectionController.class.getName());

    private FileSystemManager fileSystemManager;
    private List<PeerConnection> connections;
    private int syncInterval;

    private int maximumIncomingConnections;
    private int currentIncomingConnections;
    private int currentOutgoingConnections;

    public PeerConnectionController() throws NoSuchAlgorithmException, IOException {
        this.fileSystemManager = new FileSystemManager(Configuration.getConfigurationValue("path"),this);
        this.connections = new ArrayList<>();
        this.syncInterval = Integer.parseInt(Configuration.getConfigurationValue("syncInterval"));
        this.maximumIncomingConnections = Integer.parseInt(Configuration.getConfigurationValue("maximumIncommingConnections"));
        this.currentIncomingConnections = 0;
        this.currentOutgoingConnections = 0;
        this.start();
    }

    public void addIncomingConnection(Socket clientSocket) throws IOException, NoSuchAlgorithmException {
        addConnection(clientSocket);
        this.currentIncomingConnections += 1;
    }

    public void addOutgoingConnection(Socket clientSocket) throws IOException, NoSuchAlgorithmException {
        addConnection(clientSocket);
        this.currentOutgoingConnections += 1;
    }

    private void addConnection(Socket clientSocket) throws IOException, NoSuchAlgorithmException {
        PeerConnection connection = new PeerConnection(clientSocket);
        this.connections.add(connection);
    }

    @Override
    public void processFileSystemEvent(FileSystemEvent fileSystemEvent) {
        // Enqueue the file system event in every client thread
        for (PeerConnection pc : this.connections) {
            pc.onNewFileSystemEvent(fileSystemEvent);
        }
    }

    /**
     * Returns whether or not a peer given by
     * host and port is already connected
     * @param host peer host name
     * @param port peer port number
     * @return true or false
     */
    public boolean isPeerConnected(String host, int port) {
        for (PeerConnection pc : connections) {
            if (host.equals(pc.getHost()) && port == pc.getPort()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns whether this peer can accept any more incoming connections.
     * @return true or false
     */
    public boolean canAcceptIncomingConnection() {
        return this.currentIncomingConnections < this.maximumIncomingConnections;
    }

    /**
     * Returns a map of currently connected peers.
     * @return map
     */
    public Map<String, Integer> getConnectedPeers() {
        Map<String, Integer> connectedPeers = new HashMap<>();
        for (PeerConnection pc : this.connections) {
            String host = pc.getHost();
            Integer port = pc.getPort();
            connectedPeers.put(host, port);
        }
        return connectedPeers;
    }

    /**
     * Start running the PeerConnectionController thread
     */
    private void start() {
        log.info("PeerConnectionController Thread started...");
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                relaySyncEvents();
            }
        }, 0,syncInterval * 1000);
    }

    /**
     * Relays file system events generated by the FileSystemManager
     * method generateSyncEvents() to all PeerClient threads,
     * after every 'syncInterval' interval of time (seconds).
     */
    private void relaySyncEvents() {
        List<FileSystemEvent> syncEvents = fileSystemManager.generateSyncEvents();
        log.info("Generating sync events, PeerConnection list size: " + connections.size());
        log.info("Size of syncEvents: " + syncEvents.size());
        for (PeerConnection pc : connections) {
            if (pc != null) {
                pc.onNewSyncEvents(syncEvents);
            }
        }
    }


    /**
     * Updates the current list of peer connections
     * to signify that a peer given by 'host' and 'port'
     * has disconnected from the socket.
     * @param host the peer host name
     * @param port the peer port number
     */
    @Override
    public void disconnect(String host, int port) {

    }
}