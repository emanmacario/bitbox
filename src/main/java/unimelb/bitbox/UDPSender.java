package unimelb.bitbox;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.Map;

public class UDPSender implements Runnable {

    private DatagramSocket socket;
    private DatagramSocket connect;

    public UDPSender(DatagramSocket socket, DatagramSocket connect) {
        this.socket = socket;
        this.connect = connect;
    }

    public void send(String message) {
    }


    @Override
    public void run() {
        DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
        this.socket.connect(connect.getLocalAddress(), connect.getPort());
        try {
            System.out.println("Receiving");
            this.socket.receive(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public static void main(String[] args) throws SocketException {

        long minimum = Math.min(8192, 20000);
        System.out.println("Minimum: " + minimum);

        String[] CONNECTION_COMMANDS = {"HANDSHAKE_REQUEST", "HANDSHAKE_RESPONSE", "CONNECTION_REFUSED"};
        String command = "HANDSHAKE_REQUEST";
        boolean isConnectionCommand = Arrays.asList(CONNECTION_COMMANDS).contains(command); // Arrays.stream(CONNECTION_COMMANDS).anyMatch(command :: equals);
        System.out.println("isConnectionCommand: " + isConnectionCommand);

        try {
            InetAddress ip1 = InetAddress.getByName("localhost");
            InetAddress ip2 = InetAddress.getByName("localhost");
            System.out.println("Hostname 1: " + ip1.getHostName());
            System.out.println("Hostname 2: " + ip2.getHostName());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        /*
        DatagramSocket s1 = new DatagramSocket(8111);
        DatagramSocket s2 = new DatagramSocket(8112);
        DatagramSocket s3 = new DatagramSocket(8113);
        //s2.connect(s1.getLocalAddress(), 8111);

        UDPSender sender1 = new UDPSender(s1 ,s2);
        UDPSender sender2 = new UDPSender(s1, s3);


        Thread c1 = new Thread(sender1);
        Thread c2 = new Thread(sender2);
        c1.start();
        c2.start();

         */



    }
}
