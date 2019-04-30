package unimelb.bitbox;

import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;

import java.util.List;

public class PeerConnection {

    private PeerClient client;
    private PeerServer server;

    public PeerConnection() {

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
}
