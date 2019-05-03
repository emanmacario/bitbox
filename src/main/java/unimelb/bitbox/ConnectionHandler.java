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
            String handshakeResponse = in.readLine(); // Blocking receive call
            Document handshakeResponseJSON = Document.parse(handshakeResponse);
            try {
                handleJSONServerMessage(handshakeResponseJSON, clientSocket, out);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }

            log.info("Incoming response: " + handshakeResponse);

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
                    log.info("Incoming request: " + clientMessage);
                    Document clientMessageJSON = Document.parse(clientMessage);

                    try {
                        handleJSONClientMessage(clientMessageJSON, clientSocket, in, out);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    }
                } catch (SocketException e) {
                    e.printStackTrace();
                    log.warning("Client socket closed");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if (listeningSocket != null) {
                try {
                    log.info("Server listening socket closed");
                    listeningSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private void handleJSONServerMessage(Document json, Socket socket, BufferedWriter out) throws IOException, NoSuchAlgorithmException {
        String command = json.getString("command");
        String invalidProtocol;

        switch (command) {
            case "HANDSHAKE_RESPONSE":
                Document hostPort = (Document) json.get("hostPort");
                Integer port =  (int) hostPort.getLong("port");
                String host = hostPort.getString("host");

                // Check message credibility, ensure host and port field are not null
                if (port != null && host != null) {
                    // Proofing if a peer accepted an existing connection
                    if (controller.isPeerConnected(host, port)) {
                        invalidProtocol = Messages.getInvalidProtocol("Peer already connected!");
                        send(invalidProtocol, out);
                    } else {
                        // Start threads for the outgoing connection
                        controller.addOutgoingConnection(socket);
                    }
                } else {
                    invalidProtocol = Messages.getInvalidProtocol("message must contain hostPort field");
                    send(invalidProtocol, out);
                }
                break;

            case "CONNECTION_REFUSED":
                ArrayList<Document> peers = (ArrayList<Document>) json.get("peers");
                for (Document peer : peers) {
                    HostPort hp = new HostPort(peer);
                    port = hp.port;
                    host = hp.host;
                    if (host != null && port != null) {
                        if (!controller.isPeerConnected(host, port)) {
                            connect(host, port);
                        }
                    } else {
                        invalidProtocol = Messages.getInvalidProtocol("message must contain hostPort field");
                        send(invalidProtocol, out);
                    }
                }
                break;
            default:
                invalidProtocol = Messages.getInvalidProtocol("Expected HANDSHAKE_RESPONSE");
                send(invalidProtocol, out);
                break;
        }
    }


    private void handleJSONClientMessage(Document json, Socket clientSocket, BufferedReader in,BufferedWriter out) throws IOException, NoSuchAlgorithmException {
        String command = json.getString("command");
        Document hostPort = (Document) json.get("hostPort");
        Integer port =  (int) hostPort.getLong("port");
        String host = hostPort.getString("host");

        switch (command) {
            case "HANDSHAKE_REQUEST":
                // Check message credibility, ensure host field
                String invalidProtocol;
                if (host != null && port != null) {
                    if (controller.isPeerConnected(host, port)) {
                        invalidProtocol = Messages.getInvalidProtocol("Peer already connected!");
                        log.info("Connection Refused between port: "+ this.port + " @ host: "+ this.advertisedHost +" and port: " + port +" @ host: " + host + invalidProtocol);
                        send(invalidProtocol, out);
                    } else if (!controller.canAcceptIncomingConnection()) {
                        Map<String, Integer> connectedPeers = controller.getConnectedPeers();
                        String connectionRefused = Messages.getConnectionRefused(connectedPeers, "connection limit reached");
                        log.info("Connection Refused between port: "+ this.port + " @ host: "+ this.advertisedHost +" and port: " + port +" @ host: " + host + connectionRefused);
                        send(connectionRefused, out);
                    } else {
                        log.info("Connection established between port: "+ this.port + " @ host: "+ this.advertisedHost +" and port: " + port +" @ host: " + host );
                        String handShakeResponse = Messages.getHandshakeResponse(advertisedHost, this.port);
                        send(handShakeResponse, out);
                        controller.addIncomingConnection(clientSocket);
                    }
                } else {
                    invalidProtocol = Messages.getInvalidProtocol("message must contain hostPort field");
                    log.info("Invalid message between port: "+ this.port + " @ host: "+ this.advertisedHost +" and port: " + port +" @ host: " + host + invalidProtocol);
                    send(invalidProtocol, out);
                }
                break;

            default:
                invalidProtocol = Messages.getInvalidProtocol("Expected HANDSHAKE_REQUEST");
                log.info("Invalid message between port: "+ this.port + " @ host: "+ this.advertisedHost +" and port: " + port +" @ host: " + host + invalidProtocol);
                send(invalidProtocol, out);
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
        log.info("Sending: " + message);
    }
}
