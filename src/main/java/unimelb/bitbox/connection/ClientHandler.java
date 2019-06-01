package unimelb.bitbox.connection;

import unimelb.bitbox.util.HostPort;

import java.util.List;

public interface ClientHandler {
    List<HostPort> listPeers();
    boolean connectPeer(String host, int port);
    boolean disconnectPeer(String host, int port);
}
