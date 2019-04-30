package unimelb.bitbox;

import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.logging.Logger;

public class PeerConnection {
    private static Logger log = Logger.getLogger(PeerConnection.class.getName());

    private Socket clientSocket;
    private String clientHostName;
    private Integer clientPort;
    private BufferedReader in;
    private BufferedWriter out;

    private PeerClient client;
    private PeerServer server;

    public PeerConnection(Socket clientSocket) throws IOException {
        this.clientSocket = clientSocket;
        this.clientHostName = clientSocket.getInetAddress().getHostName();
        this.clientPort = clientSocket.getLocalPort();
        this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
        this.out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"));
        this.start();
        log.info("Peer-to-Peer Connection Thread Running");
    }

    public void onNewFileSystemEvent(FileSystemEvent event) {
        client.enqueue(event);
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
        client.close();
        server.close();
    }

    /**
     * Starts both PeerClient and PeerServer threads
     */
    private void start() {
        Thread clientThread = new Thread(client);
        Thread serverThread = new Thread(server);
        clientThread.start();
        serverThread.start();
    }

    public String getHost() {
        return this.clientHostName;
    }
    public int getPort() {
        return this.clientPort;
    }
}
