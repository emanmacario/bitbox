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
import java.util.Map;
import java.util.logging.Logger;

public class ConnectionHandler implements Runnable {
    private static Logger log = Logger.getLogger(ConnectionHandler.class.getName());

    private int port;
    private String advertisedHost;
    private ServerSocket listeningSocket;
    private DatagramSocket listeningSocketUDP;
    private PeerConnectionController controller;
    private String mode;

    public ConnectionHandler(int port, String advertisedHost, String mode) throws IOException, NoSuchAlgorithmException {
        this.port = port;
        this.advertisedHost = advertisedHost;
        this.listeningSocket = new ServerSocket(port);
        this.listeningSocketUDP = new DatagramSocket(port);
        this.mode = mode;
        this.controller = new PeerConnectionController();
    }

    /**
     * Attempt to establish a new connection between this
     * peer and another peer described by hostname 'host'
     * and port number 'port'
     * @param host the host name of the peer
     * @param port the port number of the peer
     */
    public void connect(String host, int port, String mode) {
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
                log.info("sending to " + host + ":" + "port " + handshakeRequest);
                String handshakeResponse = in.readLine(); // Blocking receive call
                Document handshakeResponseJSON = Document.parse(handshakeResponse);
                try {
                    handleJSONServerMessage(handshakeResponseJSON, clientSocket, out, "tcp", null);
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                log.warning("while connecting to " + host + ":" + port + " connection refused");
            }
        }
        // UDP doesn't use handshake
        else if (mode.equals("udp")) {
            try {
                // Create client socket, get input and output buffers
                DatagramSocket clientSocketUDP = new DatagramSocket(port);
                // TO DO change 8192 to blockSize
                byte[] sendHandshake = new byte[8192];
                byte[] receiveData = new byte[8192];

                InetAddress IPAddress = InetAddress.getByName(host);

            } catch (IOException e) {
                log.warning("while connecting to " + host + ":" + port + " connection refused");
            }
        }
    }
    @Override
    public void run() {
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
                            handleJSONClientMessage(clientMessageJSON, clientSocket, out, "tcp", null);
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
        } else if (mode.equals("udp")) {
            try {
                while (true) {
                    // Accept an incoming client connection request (blocking call)
                    byte[] buffer = new byte[8192];
                    //DatagramSocket clientSocketUDP = new DatagramSocket(port);

                    DatagramPacket in = new DatagramPacket(buffer, buffer.length);
                    listeningSocketUDP.receive(in);

                    byte[] data = in.getData();
                    String clientMessage = new String(data, 0, data.length);
                    Document clientMessageJSON = Document.parse(clientMessage);
                    try {
                        handleJSONClientMessage(clientMessageJSON, null, null, "udp", listeningSocketUDP);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    }
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // TO DO UDP implementation
    private void handleJSONServerMessage(Document json, Socket socket, BufferedWriter out, String mode, DatagramSocket socketUDP) throws IOException, NoSuchAlgorithmException {
        String command = json.getString("command");
        Document hostPort = (Document) json.get("hostPort");
        Integer port;
        if (mode.equals("udp")){
            port = (int) hostPort.getLong("udpPort");
        }  else {
            port = (int) hostPort.getLong("port");
        }
        String host = hostPort.getString("host");
        String invalidProtocol;
        byte[] sendBytes = new byte[1024];
        InetAddress IPAddress = InetAddress.getByName(host);

        switch (command) {
            case "HANDSHAKE_RESPONSE":
                System.out.print("handshake response");
                log.info("received command [" + command + "] from " + host + ":" + port);
                // Check message credibility, ensure host and port field are not null
                if (port != null && host != null) {
                    // Proofing if a peer accepted an existing connection
                    if (controller.isPeerConnected(host, port)) {
                        invalidProtocol = Messages.getInvalidProtocol("peer already connected");
                        if (mode.equals("tcp")) {send(invalidProtocol, out); };
                        log.info("sending to " + host + ":" + "port " + invalidProtocol);
                    } else {
                        // Start threads for the outgoing connection
                        controller.addOutgoingConnection(host, port, socket, mode, socketUDP);
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
                            connect(host, port, mode);
                        }
                    }
                }
                break;
            default:
                log.warning("received an invalid message from " + host + ":" + port);
                invalidProtocol = Messages.getInvalidProtocol("expected HANDSHAKE_RESPONSE");
                if (mode.equals("tcp")) {send(invalidProtocol, out); };
                log.info("sending to " + host + ":" + port + " " + invalidProtocol);
                break;
        }
    }

    //TO DO UDP implementation
    private void handleJSONClientMessage(Document json, Socket clientSocket, BufferedWriter out, String mode, DatagramSocket clientSocketUDP) throws IOException, NoSuchAlgorithmException {
        String command = json.getString("command");
        Document hostPort = (Document) json.get("hostPort");
        Integer port;
        if (mode.equals("udp")){
            port = (int) hostPort.getLong("udpPort");
        }  else {
            port = (int) hostPort.getLong("port");
        }
        String host = hostPort.getString("host");

        byte[] sendBytes = new byte[1024];
        InetAddress IPAddress = InetAddress.getByName(host);

        switch (command) {
            case "HANDSHAKE_REQUEST":
                System.out.print("handshake request");
                log.info("received command [" + command + "] from " + host + ":" + port);
                // Check message credibility, ensure host field
                String invalidProtocol;
                if (host != null && port != null) {
                    if (controller.isPeerConnected(host, port)) {
                        invalidProtocol = Messages.getInvalidProtocol("peer already connected");
                        if (mode.equals("tcp")) { send(invalidProtocol, out); };
                        log.info("sending to " + host + ":" + port + " " + invalidProtocol);
                    } else if (!controller.canAcceptIncomingConnection()) {
                        Map<String, Integer> connectedPeers = controller.getConnectedPeers();
                        String connectionRefused = Messages.getConnectionRefused(connectedPeers, "connection limit reached");
                        if (mode.equals("tcp")) { send(connectionRefused, out); };
                            log.info("sending to " + host + ":" + port + " " + connectionRefused);
                    } else {
                        String handShakeResponse = Messages.getHandshakeResponse(advertisedHost, this.port);
                        send(handShakeResponse, out);
                        if (mode.equals("tcp")) { send(handShakeResponse, out); };

                        log.info("sending to " + host + ":" + port + " " + handShakeResponse);
                        controller.addIncomingConnection(host, port, clientSocket, mode, clientSocketUDP);
                    }
                }
                break;

            default:
                invalidProtocol = Messages.getInvalidProtocol("Expected HANDSHAKE_REQUEST");
                if (mode.equals("tcp")) { send(invalidProtocol, out); };
                if (mode.equals("udp")) {
                    sendBytes = invalidProtocol.getBytes();
                    DatagramPacket sendPacket = new DatagramPacket(sendBytes, sendBytes.length, IPAddress, port);
                    clientSocketUDP.send(sendPacket);
                }
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
