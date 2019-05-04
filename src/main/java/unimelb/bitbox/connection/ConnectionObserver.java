package unimelb.bitbox.connection;

public interface ConnectionObserver {
    void disconnect(String host, int port);
}
