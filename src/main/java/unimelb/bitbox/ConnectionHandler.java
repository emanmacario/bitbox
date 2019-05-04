package unimelb.bitbox;

import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.HostPort;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Logger;

public class ConnectionHandler implements Runnable {
    private static Logger log = Logger.getLogger(ConnectionHandler.class.getName());

    private int port;
    private String advertisedHost;
    private ServerSocket listeningSocket;
    private PeerConnectionController controller;

    public ConnectionHandler(int port, String advertisedHost) throws IOException, NoSuchAlgorithmException {
        this.port = port;
        this.advertisedHost = advertisedHost;
        this.listeningSocket = new ServerSocket(port);
        this.controller = new PeerConnectionController();
    }

    /**
     * Attempt to establish a new connection between this
     * peer and another peer described by hostname 'host'
     * and port number 'port'
     * @param host the host name of the peer
     * @param port the port number of the peer
     */
    public void connect(String host, int port) {
        try {
            // Create client socket, get input and output buffers
            Socket clientSocket = new Socket(host, port);
            BufferedReader in =
                    new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
            BufferedWriter out =
                    new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8));

            // Attempt to perform a handshake with the peer
            String handshakeRequest = Messages.getHandshakeRequest(host, this.port);
            send(handshakeRequest, out);
            log.info("sending to " + host + ":" + "port " + handshakeRequest);
            String handshakeResponse = in.readLine(); // Blocking receive call
            Document handshakeResponseJSON = Document.parse(handshakeResponse);
            try {
                handleJSONServerMessage(handshakeResponseJSON, clientSocket, out);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            log.warning("while connecting to " + host + ":" + port + " connection refused");
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                // Accept an incoming client connection request (blocking call)
                Socket clientSocket = listeningSocket.accept();
                BufferedReader in =
                        new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
                BufferedWriter out =
                        new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8));

                try {
                    String clientMessage = in.readLine(); // Blocking receive call
                    Document clientMessageJSON = Document.parse(clientMessage);
                    try {
                        handleJSONClientMessage(clientMessageJSON, clientSocket, out);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    }
                } catch (SocketException e) {
                    e.printStackTrace();
                    log.warning("peer socket unexpectedly closed");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (listeningSocket != null) {
                try {
                    listeningSocket.close();
                    log.info("server listening socket closed");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void handleJSONServerMessage(Document json, Socket socket, BufferedWriter out) throws IOException, NoSuchAlgorithmException {
        String command = json.getString("command");
        Document hostPort = (Document) json.get("hostPort");
        Integer port =  (int) hostPort.getLong("port");
        String host = hostPort.getString("host");
        String invalidProtocol;

        switch (command) {
            case "HANDSHAKE_RESPONSE":
                log.info("received command [" + command + "] from " + host + ":" + port);
                // Check message credibility, ensure host and port field are not null
                if (port != null && host != null) {
                    // Proofing if a peer accepted an existing connection
                    if (controller.isPeerConnected(host, port)) {
                        invalidProtocol = Messages.getInvalidProtocol("peer already connected");
                        send(invalidProtocol, out);
                        log.info("sending to " + host + ":" + "port " + invalidProtocol);
                    } else {
                        // Start threads for the outgoing connection
                        controller.addOutgoingConnection(host, port, socket);
                    }
                }
                break;

            case "CONNECTION_REFUSED":
                log.info("received command [" + command + "] from " + host + ":" + port);
                ArrayList<Document> peers = (ArrayList<Document>) json.get("peers");
                for (Document peer : peers) {
                    HostPort hp = new HostPort(peer);
                    port = hp.port;
                    host = hp.host;
                    if (host != null && port != null) {
                        if (!controller.isPeerConnected(host, port)) {
                            connect(host, port);
                        }
                    }
                }
                break;
            default:
                log.warning("received an invalid message from " + host + ":" + port);
                invalidProtocol = Messages.getInvalidProtocol("expected HANDSHAKE_RESPONSE");
                send(invalidProtocol, out);
                log.info("sending to " + host + ":" + port + " " + invalidProtocol);
                break;
        }
    }


    private void handleJSONClientMessage(Document json, Socket clientSocket, BufferedWriter out) throws IOException, NoSuchAlgorithmException {
        String command = json.getString("command");
        Document hostPort = (Document) json.get("hostPort");
        Integer port =  (int) hostPort.getLong("port");
        String host = hostPort.getString("host");

        switch (command) {
            case "HANDSHAKE_REQUEST":
                log.info("received command [" + command + "] from " + host + ":" + port);
                // Check message credibility, ensure host field
                String invalidProtocol;
                if (host != null && port != null) {
                    if (controller.isPeerConnected(host, port)) {
                        invalidProtocol = Messages.getInvalidProtocol("peer already connected");
                        send(invalidProtocol, out);
                        log.info("sending to " + host + ":" + port + " " + invalidProtocol);
                    } else if (!controller.canAcceptIncomingConnection()) {
                        Map<String, Integer> connectedPeers = controller.getConnectedPeers();
                        String connectionRefused = Messages.getConnectionRefused(connectedPeers, "connection limit reached");
                        send(connectionRefused, out);
                        log.info("sending to " + host + ":" + port + " " + connectionRefused);
                    } else {
                        String handShakeResponse = Messages.getHandshakeResponse(advertisedHost, this.port);
                        send(handShakeResponse, out);
                        log.info("sending to " + host + ":" + port + " " + handShakeResponse);
                        controller.addIncomingConnection(host, port, clientSocket);
                    }
                }
                break;

            default:
                invalidProtocol = Messages.getInvalidProtocol("Expected HANDSHAKE_REQUEST");
                send(invalidProtocol, out);
                log.info("sending to " + host + ":" + port + " " + invalidProtocol);
        }
    }

    /**
     * Sends a request/response string to the connected peer.
     * Appends a newline char to the outgoing message.
     * @param message request or response message
     * @throws IOException
     */
    private void send(String message, BufferedWriter out) throws IOException {
        out.write(message + '\n');
        out.flush();
    }
}
