package unimelb.bitbox;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;
import unimelb.bitbox.util.FileSystemObserver;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Logger;

// A controller class for already established peer-to-peer connections
public class PeerConnectionController implements FileSystemObserver, Runnable {

    private static Logger log = Logger.getLogger(PeerConnectionController.class.getName());

    private FileSystemManager fileSystemManager;
    private List<PeerConnection> connections;
    private Queue<FileSystemEvent> events;
    private int syncInterval;

    private int maximumIncomingConnections;
    private int currentIncomingConnections;
    private int currentOutgoingConnections;

    public PeerConnectionController() throws NoSuchAlgorithmException, IOException {
        this.fileSystemManager = new FileSystemManager(Configuration.getConfigurationValue("path"),this);
        this.connections = new ArrayList<>();
        this.events = new LinkedList<>();
        this.syncInterval = Integer.parseInt(Configuration.getConfigurationValue("syncInterval"));
        this.maximumIncomingConnections = Integer.parseInt(Configuration.getConfigurationValue("maximumIncomingConnections"));
        this.currentIncomingConnections = 0;
        this.currentOutgoingConnections = 0;
    }

    public void addIncomingConnection(PeerConnection connection) {
        this.connections.add(connection);
        this.currentIncomingConnections += 1;
    }

    public void addOutgoingConnection(PeerConnection connection) {
        this.connections.add(connection);
        this.currentOutgoingConnections += 1;
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
     * Returns whether this peer can accept more incoming connections
     * @return
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

    // TODO: Need to start running this thread somewhere
    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(syncInterval * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            List<FileSystemEvent> syncEvents = fileSystemManager.generateSyncEvents();
            for (PeerConnection pc : connections) {
                pc.onNewSyncEvents(syncEvents);
            }
        }
    }
}