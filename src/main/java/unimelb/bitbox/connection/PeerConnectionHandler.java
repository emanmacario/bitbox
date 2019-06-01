package unimelb.bitbox.connection;

import unimelb.bitbox.util.Messages;
import unimelb.bitbox.controller.PeerConnectionController;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.HostPort;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class PeerConnectionHandler implements Runnable, ClientHandler {
    private static Logger log = Logger.getLogger(PeerConnectionHandler.class.getName());
    private static String[] CONNECTION_COMMANDS = {"HANDSHAKE_REQUEST", "HANDSHAKE_RESPONSE", "CONNECTION_REFUSED"};

    private int port;
    private String advertisedHost;
    private String mode;
    private ServerSocket listeningSocket;
    private DatagramSocket listeningSocketUDP;
    private PeerConnectionController controller;

    public PeerConnectionHandler(int port, String advertisedHost, String mode) throws IOException, NoSuchAlgorithmException {
        this.port = port;
        this.advertisedHost = advertisedHost;
        this.mode = mode;
        if (mode.equals("tcp")) {
            listeningSocket = new ServerSocket(port);
        } else {
            listeningSocketUDP = new DatagramSocket(port);
        }
        this.controller = new PeerConnectionController();
    }

    /**
     * Attempt to establish a new connection between this
     * peer and another peer described by hostname 'host'
     * and port number 'port'
     * @param host the host name of the peer
     * @param port the port number of the peer
     */
    public boolean connect(String host, int port) {
        // Check if already connected
        if (controller.isPeerConnected(host, port)) {
            return false;
        }

        // Standard TCP handshaking
        if (mode.equals("tcp")) {
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
                log.info("sending to " + host + ":" + port + " " + handshakeRequest);
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
        // UDP handshaking
        else {
            try {
                // Client socket does not need an IP address and port number
                InetAddress serverAddress = InetAddress.getByName(host);
                String handshakeRequest = Messages.getHandshakeRequest(host, this.port);
                byte[] sendData = handshakeRequest.getBytes();

                // Initialise send packet
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, port);

                // Try sending the packet
                try {
                    listeningSocketUDP.send(sendPacket);
                    log.info("sending to " + host + ":" + "port " + handshakeRequest);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }
        // Signify whether connection attempt was successful
        return controller.isPeerConnected(host, port);
    }

    @Override
    public void run() {
        // Standard TCP implementation
        if (mode.equals("tcp")) {
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
        // Extension UDP implementation
        else {
            try {
                // Set no time limit on blocking receive call
                listeningSocketUDP.setSoTimeout(0);
                while (true) {
                    byte[] receiveData = new byte[65535];
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    listeningSocketUDP.receive(receivePacket);
                    String clientMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
                    Document clientMessageJSON = Document.parse(clientMessage);
                    String command = clientMessageJSON.getString("command");

                    boolean isConnectionCommand = Arrays.asList(CONNECTION_COMMANDS).contains(command);
                    if (isConnectionCommand) {
                        switch (command) {
                            case "HANDSHAKE_REQUEST":
                                try {
                                    handleJSONClientMessage(clientMessageJSON, null, null);
                                } catch (NoSuchAlgorithmException e) {
                                    e.printStackTrace();
                                }
                                break;
                            case "HANDSHAKE_RESPONSE":
                            case "CONNECTION_REFUSED":
                                try {
                                    handleJSONServerMessage(clientMessageJSON, null, null);
                                } catch (NoSuchAlgorithmException e) {
                                    e.printStackTrace();
                                }
                                break;
                            default:
                                break;
                        }
                    } else {
                        controller.processPacket(receivePacket);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (listeningSocketUDP != null) {
                    listeningSocketUDP.close();
                    log.info("server UDP listening socket closed");
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
                        if (mode.equals("tcp")) {
                            send(invalidProtocol, out);
                            log.info("sending to " + host + ":" + "port " + invalidProtocol);
                        } else {
                            send(invalidProtocol, host, port);
                        }
                    } else {
                        // Start threads for the outgoing connection
                        if (mode.equals("tcp")) {
                            controller.addOutgoingConnection(host, port, socket);
                        } else {
                            controller.addOutgoingConnection(host, port, listeningSocketUDP);
                        }
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
            case "INVALID_PROTOCOL":
                log.warning("received command [" + command + "] from " + host + ":" + port);
                break;
            default:
                log.warning("received an invalid message from " + host + ":" + port);
                invalidProtocol = Messages.getInvalidProtocol("expected HANDSHAKE_RESPONSE");
                if (mode.equals("tcp")) {
                    send(invalidProtocol, out);
                    log.info("sending to " + host + ":" + port + " " + invalidProtocol);
                } else {
                    send(invalidProtocol, host, port);
                }
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
                String message;
                if (host != null && port != null) {
                    if (controller.isPeerConnected(host, port)) {
                        message = Messages.getInvalidProtocol("peer already connected");
                    } else if (!controller.canAcceptIncomingConnection()) {
                        List<HostPort> connectedPeers = controller.getConnectedPeers();
                        message = Messages.getConnectionRefused(connectedPeers, "connection limit reached");
                    } else {
                        message = Messages.getHandshakeResponse(advertisedHost, this.port);
                        if (mode.equals("tcp")) {
                            controller.addIncomingConnection(host, port, clientSocket);
                        } else {
                            controller.addIncomingConnection(host, port, listeningSocketUDP);
                        }
                    }
                    if (mode.equals("tcp")) {
                        send(message, out);
                        log.info("sending to " + host + ":" + port + " " + message);
                    } else {
                        send(message, host, port);
                    }
                }
                break;

            default:
                message = Messages.getInvalidProtocol("Expected HANDSHAKE_REQUEST");
                if (mode.equals("tcp")) {
                    send(message, out);
                    log.info("sending to " + host + ":" + port + " " + message);
                } else {
                    send(message, host, port);
                }
                break;
        }
    }

    @Override
    public List<HostPort> listPeers() {
        return controller.getConnectedPeers();
    }

    @Override
    public boolean connectPeer(String host, int port) {
        return connect(host, port);
    }

    @Override
    public boolean disconnectPeer(String host, int port) {
        return controller.disconnectPeer(host, port);
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

    /**
     * Sends a message over UDP without retransmission.
     * @param message message string
     * @param host host name
     * @param port host port number
     */
    private void send(String message, String host, int port) {
        try {
            InetAddress address = InetAddress.getByName(host);
            byte[] sendData = message.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, port);
            log.info("sending to " + host + ":" + port + " " + message);
            listeningSocketUDP.send(sendPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
