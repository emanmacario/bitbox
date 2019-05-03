package unimelb.bitbox;

public interface ConnectionObserver {
    void disconnect(String host, int port);
}
