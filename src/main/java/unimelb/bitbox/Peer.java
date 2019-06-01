package unimelb.bitbox;

import unimelb.bitbox.connection.ClientConnectionHandler;
import unimelb.bitbox.connection.ClientHandler;
import unimelb.bitbox.connection.PeerConnectionHandler;
import unimelb.bitbox.connection.ConnectionObserver;
import unimelb.bitbox.protocols.PeerClient;
import unimelb.bitbox.protocols.PeerServer;
import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;

import java.io.*;
import java.net.DatagramSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.logging.Logger;

public class Peer {
    private static Logger log = Logger.getLogger(Peer.class.getName());

    private String peerHost;
    private Integer peerPort;
    private Socket socket;
    private PeerClient client;
    private PeerServer server;
    private ConnectionObserver observer;

    // TCP Peer constructor
    public Peer(String host, int port, Socket socket, ConnectionObserver observer)
            throws IOException, NoSuchAlgorithmException {
        log.info("Connection to " + host + ":" + port + " established");
        this.observer = observer;
        this.peerHost = host;
        this.peerPort = port;
        this.socket = socket;
        this.client = new PeerClient(host, port, socket);
        this.server = new PeerServer(this.client, host, port, socket, observer);
        this.start();
    }

    // UDP Peer constructor
    public Peer(String host, int port, DatagramSocket socket, ConnectionObserver observer)
            throws IOException, NoSuchAlgorithmException {
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

    public void processMessage(Document message) throws NoSuchAlgorithmException, IOException {
        server.handleIncomingClientMessage(message);

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

    public void disconnect() {
        String mode = Configuration.getConfigurationValue("mode");
        if (mode.equals("tcp")) {
            try {
                this.socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            client.close();
            observer.disconnect(peerHost, peerPort);
        }
    }

    public String getHost() {
        return this.peerHost;
    }

    public int getPort() {
        return this.peerPort;
    }


    public static void main( String[] args ) throws IOException, NumberFormatException, NoSuchAlgorithmException, InterruptedException
    {
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tc] %2$s %4$s: %5$s%n");
        log.info("BitBox Peer starting...");
        Configuration.getConfiguration();

        // Read configuration file values
        String advertisedName = Configuration.getConfigurationValue("advertisedName");
        String mode = Configuration.getConfigurationValue("mode");
        log.info("Mode selected: " + mode);
        int serverPort;
        if (mode.equals("udp")) {
            serverPort = Integer.parseInt(Configuration.getConfigurationValue("udpPort"));
        } else {
            serverPort = Integer.parseInt(Configuration.getConfigurationValue("port"));
        }

        // Start main I/O connection handler thread
        PeerConnectionHandler peerConnectionHandler = new PeerConnectionHandler(serverPort, advertisedName, mode);

        Thread peerConnectionHandlerThread = new Thread(peerConnectionHandler);
        if (mode.equals("tcp")) {
            peerConnectionHandlerThread.start();
        }

        // Attempt to connect to peers listed in the configuration file
        String[] peersArray = Configuration.getConfigurationValue("peers").split("\\s*,\\s*");
        for (String peer: peersArray) {
            String[] peerHostPort = peer.split(":");
            String host = peerHostPort[0];
            int port = Integer.parseInt(peerHostPort[1]);
            log.info("attempting to connect to " + host + ":" + port);
            peerConnectionHandler.connect(host,port);
        }

        if (mode.equals("udp")) {
            peerConnectionHandlerThread.start();
        }

        ClientConnectionHandler clientConnectionHandler;
        try {
            clientConnectionHandler = new ClientConnectionHandler(peerConnectionHandler);
            Thread clientConnectionHandlerThread = new Thread(clientConnectionHandler);
            clientConnectionHandlerThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}