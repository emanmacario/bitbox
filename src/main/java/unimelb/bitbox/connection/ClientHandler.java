package unimelb.bitbox.connection;

import java.util.Map;

public interface ClientHandler {
    Map<String, Integer> listPeers();
    boolean connectPeer(String host, int port);
    boolean disconnectPeer(String host, int port);
}
