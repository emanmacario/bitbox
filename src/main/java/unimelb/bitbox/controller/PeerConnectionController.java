package unimelb.bitbox.controller;

import unimelb.bitbox.Peer;
import unimelb.bitbox.connection.ConnectionObserver;
import unimelb.bitbox.util.*;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Logger;

// A controller class for already established peer-to-peer connections
public class PeerConnectionController implements FileSystemObserver, ConnectionObserver {

    private static Logger log = Logger.getLogger(PeerConnectionController.class.getName());

    private FileSystemManager fileSystemManager;
    private List<Peer> connections;
    private List<HostPort> incomingConnections;
    private int syncInterval;
    private int maximumIncomingConnections;
    private int currentIncomingConnections;
    private String mode;

    public PeerConnectionController() throws NoSuchAlgorithmException, IOException {
        this.fileSystemManager = new FileSystemManager(Configuration.getConfigurationValue("path"),this);
        this.connections = new ArrayList<>();
        this.incomingConnections = new ArrayList<>();
        this.syncInterval = Integer.parseInt(Configuration.getConfigurationValue("syncInterval"));
        this.maximumIncomingConnections = Integer.parseInt(Configuration.getConfigurationValue("maximumIncommingConnections"));
        this.mode = Configuration.getConfigurationValue("mode");
        this.currentIncomingConnections = 0;
        this.start();
    }

    public void processPacket(DatagramPacket packet) {
        // Extract information from the packet
        String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
        InetAddress address = packet.getAddress();
        String host = address.getHostName();
        int port = packet.getPort();

        // Delegate processing to the peer the packet belongs to
        for (Peer peer : connections) {
            String peerHost = peer.getHost();
            int peerPort = peer.getPort();
            if (host.equals(peerHost) && (port == peerPort)) {
                try {
                    peer.processMessage(Document.parse(message));
                } catch (NoSuchAlgorithmException | IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void addIncomingConnection(String host, int port, Socket socket) throws IOException, NoSuchAlgorithmException {
        addConnection(host, port, socket);
        this.incomingConnections.add(new HostPort(host, port));
        this.currentIncomingConnections += 1;
    }

    public void addIncomingConnection(String host, int port, DatagramSocket socket) throws IOException, NoSuchAlgorithmException {
        addConnection(host, port, socket);
        this.incomingConnections.add(new HostPort(host, port));
        this.currentIncomingConnections += 1;
    }

    public void addOutgoingConnection(String host, int port, Socket socket) throws IOException, NoSuchAlgorithmException {
        addConnection(host, port, socket);
    }

    public void addOutgoingConnection(String host, int port, DatagramSocket socket) throws IOException, NoSuchAlgorithmException {
        addConnection(host, port, socket);
    }

    private void addConnection(String host, int port, Socket socket) throws IOException, NoSuchAlgorithmException {
        Peer peer = new Peer(host, port, socket, this);
        this.connections.add(peer);
    }

    private void addConnection(String host, int port, DatagramSocket socket) throws IOException, NoSuchAlgorithmException {
        Peer peer = new Peer(host, port, socket, this);
        this.connections.add(peer);
    }


    @Override
    public void processFileSystemEvent(FileSystemEvent fileSystemEvent) {
        // Enqueue the file system event in every client thread
        for (Peer pc : this.connections) {
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
        for (Peer pc : connections) {
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
        for (Peer pc : this.connections) {
            String host = pc.getHost();
            Integer port = pc.getPort();
            connectedPeers.put(host, port);
        }
        return connectedPeers;
    }

    public boolean disconnectPeer(String host, int port) {
        for (Peer peer : connections) {
            if (peer.getHost().equals(host) && (peer.getPort() == port)) {
                peer.disconnect();
                return true;
            }
        }
        return false;
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
        }, 0, syncInterval * 1000);
    }

    /**
     * Relays file system events generated by the FileSystemManager
     * method generateSyncEvents() to all PeerClient threads,
     * after every 'syncInterval' interval of time (seconds).
     */
    private void relaySyncEvents() {
        List<FileSystemEvent> syncEvents = fileSystemManager.generateSyncEvents();
        log.info("generating sync events: " + syncEvents.size() + " events created");
        for (Peer pc : connections) {
            if (pc != null) {
                pc.onNewSyncEvents(syncEvents);
            }
        }
    }


    /**
     * Updates the current list of peer connections
     * to signify that a peer given by 'host' and 'port'
     * has disconnected from the peer socket.
     * @param host the peer host name
     * @param port the peer port number
     */
    @Override
    public void disconnect(String host, int port) {
        // Update current peers connections list
        Peer disconnect = null;
        for (Peer pc : connections) {
            if (host.equals(pc.getHost()) && port == pc.getPort()) {
                disconnect = pc;
            }
        }
        connections.remove(disconnect);
        log.info("disconnected from " + host + ":" + port);

        // Update maximum incoming connections if-and-only-if the
        // connection for the peer that disconnected was incoming
        HostPort incomingDisconnect = null;
        for (HostPort hp : incomingConnections) {
            if (host.equals(hp.host) && port == hp.port) {
                incomingDisconnect = hp;
                currentIncomingConnections -= 1;
            }
        }
        incomingConnections.remove(incomingDisconnect);
    }
}