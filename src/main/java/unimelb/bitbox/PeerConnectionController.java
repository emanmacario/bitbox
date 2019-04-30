package unimelb.bitbox;

import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;
import unimelb.bitbox.util.FileSystemObserver;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.logging.Logger;


public class PeerConnectionController implements FileSystemObserver, Runnable {

    private static Logger log = Logger.getLogger(PeerConnectionController.class.getName());

    private FileSystemManager fileSystemManager;
    private List<PeerConnection> connections;
    private Queue<FileSystemEvent> events;
    private int syncInterval;

    public PeerConnectionController() {
        this.connections = new ArrayList<>();
        this.events = new LinkedList<>();
        this.syncInterval = Integer.parseInt(Configuration.getConfigurationValue("syncInterval"));
    }

    @Override
    public void processFileSystemEvent(FileSystemEvent fileSystemEvent) {
        // Enqueue the file system event in every client thread
        for (PeerConnection pc : connections) {
            pc.onNewFileSystemEvent(fileSystemEvent);
        }
    }

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