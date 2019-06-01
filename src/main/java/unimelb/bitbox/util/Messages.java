package unimelb.bitbox.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Iterator;

import org.json.simple.JSONObject;

import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.HostPort;

public class Messages {

	public Messages() {

	}
	
	public static String getInvalidProtocol(String message) {
		Document doc1 = new Document();
		doc1.append("command","INVALID_PROTOCOL");
		doc1.append("messsage",message);
        
        return doc1.toJson();
	}
	
	public static String getHandshakeRequest(String host, int port) {
		Document doc1 = new Document();
        doc1.append("host",host);
        doc1.append("port",port);
        Document doc2 = new Document();
        doc2.append("hostPort",doc1);
        doc2.append("command","HANDSHAKE_REQUEST");
        
        return doc2.toJson();
	}
	
	public static String getHandshakeResponse(String host, int port) {
		Document doc1 = new Document();
        doc1.append("host",host);
        doc1.append("port",port);
        Document doc2 = new Document();
        doc2.append("hostPort",doc1);
        doc2.append("command","HANDSHAKE_RESPONSE");
        
        return doc2.toJson();
	}
	
	public static String getConnectionRefused(List<HostPort> peers, String message) {
		ArrayList<Document> peerList = new ArrayList<>();
		for (HostPort peer : peers) {
		    peerList.add(peer.toDoc());
        }
		Document doc = new Document();
		doc.append("peers", peerList);
        doc.append("command","CONNECTION_REFUSED");
        doc.append("message",message);
        
        return doc.toJson();
	}
	
	public static String getFileCreateRequest(String md5, Long lastModified, Long fileSize, String pathName) {
		Document doc1 = new Document();
        doc1.append("md5",md5);
        doc1.append("lastModified",lastModified);
        doc1.append("fileSize",fileSize);
        
        Document doc2 = new Document();
		doc2.append("fileDescriptor",doc1);
        doc2.append("command","FILE_CREATE_REQUEST");
        doc2.append("pathName",pathName);
        
        return doc2.toJson();
	}
	
	public static String getFileCreateResponse(String md5, Long lastModified, Long fileSize, String pathName, String message, boolean status) {
		Document doc1 = new Document();
        doc1.append("md5",md5);
        doc1.append("lastModified",lastModified);
        doc1.append("fileSize",fileSize);
        
        Document doc2 = new Document();
		doc2.append("fileDescriptor",doc1);
        doc2.append("command","FILE_CREATE_RESPONSE");
        doc2.append("pathName",pathName);
        doc2.append("message", message);
        doc2.append("status", status);
        return doc2.toJson();
	}
	
	public static String getFileBytesRequest(String md5, Long lastModified, Long fileSize, String pathName, long position, long length) {
		Document doc1 = new Document();
        doc1.append("md5",md5);
        doc1.append("lastModified",lastModified);
        doc1.append("fileSize",fileSize);
        
        Document doc2 = new Document();
		doc2.append("fileDescriptor",doc1);
        doc2.append("command","FILE_BYTES_REQUEST");
        doc2.append("pathName",pathName);
        doc2.append("position", position);
        doc2.append("length", length);
        return doc2.toJson();
	}
	
	public static String getFileBytesResponse(String md5, Long lastModified, Long fileSize, String pathName, long position, long length, String content,String message, boolean status) {
		Document doc1 = new Document();
        doc1.append("md5",md5);
        doc1.append("lastModified",lastModified);
        doc1.append("fileSize",fileSize);
        
        Document doc2 = new Document();
		doc2.append("fileDescriptor",doc1);
        doc2.append("command","FILE_BYTES_RESPONSE");
        doc2.append("pathName",pathName);
        doc2.append("position", position);
        doc2.append("length", length);
        doc2.append("message", message);
        doc2.append("content", content);
        doc2.append("status", status);
        return doc2.toJson();
	}
	
	public static String getFileDeleteRequest(String md5, Long lastModified, Long fileSize, String pathName) {
		Document doc1 = new Document();
        doc1.append("md5",md5);
        doc1.append("lastModified",lastModified);
        doc1.append("fileSize",fileSize);
        
        Document doc2 = new Document();
		doc2.append("fileDescriptor",doc1);
        doc2.append("command","FILE_DELETE_REQUEST");
        doc2.append("pathName",pathName);
        
        return doc2.toJson();
	}
	
	public static String getFileDeleteResponse(String md5, Long lastModified, Long fileSize, String pathName,String message, boolean status) {
		Document doc1 = new Document();
        doc1.append("md5",md5);
        doc1.append("lastModified",lastModified);
        doc1.append("fileSize",fileSize);
        
        Document doc2 = new Document();
		doc2.append("fileDescriptor",doc1);
        doc2.append("command","FILE_DELETE_RESPONSE");
        doc2.append("pathName",pathName);
        doc2.append("message", message);
        doc2.append("status", status);

        return doc2.toJson();
	}
	
	public static String getFileModifyRequest(String md5, Long lastModified, Long fileSize, String pathName) {
		Document doc1 = new Document();
        doc1.append("md5",md5);
        doc1.append("lastModified",lastModified);
        doc1.append("fileSize",fileSize);
        
        Document doc2 = new Document();
		doc2.append("fileDescriptor",doc1);
        doc2.append("command","FILE_MODIFY_REQUEST");
        doc2.append("pathName",pathName);
        
        return doc2.toJson();
	}
	
	public static String getFileModifyResponse(String md5, Long lastModified, Long fileSize, String pathName,String message, boolean status) {
		Document doc1 = new Document();
        doc1.append("md5",md5);
        doc1.append("lastModified",lastModified);
        doc1.append("fileSize",fileSize);
        
        Document doc2 = new Document();
		doc2.append("fileDescriptor",doc1);
        doc2.append("command","FILE_MODIFY_RESPONSE");
        doc2.append("pathName",pathName);
        doc2.append("message", message);
        doc2.append("status", status);

        return doc2.toJson();
	}
	
	public static String getDirectoryCreateRequest(String pathName) {
		Document doc1 = new Document();
        doc1.append("command","DIRECTORY_CREATE_REQUEST");
        doc1.append("pathName",pathName);
        
        return doc1.toJson();
	}
	
	public static String getDirectoryCreateResponse(String pathName, String message, boolean status) {
		Document doc1 = new Document();
        doc1.append("command","DIRECTORY_CREATE_RESPONSE");
        doc1.append("pathName",pathName);
        doc1.append("message",message);
        doc1.append("status",status);
        
        return doc1.toJson();
	}
	
	public static String getDirectoryDeleteRequest(String pathName) {
		Document doc1 = new Document();
        doc1.append("command","DIRECTORY_DELETE_REQUEST");
        doc1.append("pathName",pathName);
        
        return doc1.toJson();
	}
	
	public static String getDirectoryDeleteResponse(String pathName, String message, boolean status) {
		Document doc1 = new Document();
        doc1.append("command","DIRECTORY_DELETE_RESPONSE");
        doc1.append("pathName",pathName);
        doc1.append("message",message);
        doc1.append("status",status);
        
        return doc1.toJson();
	}

	public static String getAuthResponse(String EncyptedSecretKey , boolean status) {
		Document doc1 = new Document();
        doc1.append("command","AUTH_RESPONSE");
        doc1.append("AES128",EncyptedSecretKey);
        doc1.append("status",status);
        doc1.append("message","public key found");
       

        return doc1.toJson();
	}

	public static String getAuthResponse(boolean status, String message) {
		Document doc1 = new Document();
        doc1.append("command","AUTH_RESPONSE");
        doc1.append("status",status);
        doc1.append("message",message);
       

        return doc1.toJson();
	}

	public static String getAuthRequest(String identity) {
		Document doc1 = new Document();
        doc1.append("command","AUTH_REQUEST");
        doc1.append("identity",identity);     

        return doc1.toJson();
		
	}

    public static String getListPeersRequest() {
        Document doc1 = new Document();
        doc1.append("command", "LIST_PEERS_REQUEST");
        return doc1.toJson();
    }

    public static String getConnectPeerRequest(String host, int port) {
        Document doc1 = new Document();
        doc1.append("command", "CONNECT_PEER_REQUEST");
        doc1.append("host", host);
        doc1.append("port", port);
        return doc1.toJson();
    }

    public static String getDisconnectPeerRequest(String host, int port) {
        Document doc1 = new Document();
        doc1.append("command", "DISCONNECT_PEER_REQUEST");
        doc1.append("host", host);
        doc1.append("port", port);
        return doc1.toJson();
    }

    public static String getListPeersResponse(List<HostPort> peers) {
        Document doc1 = new Document();
        ArrayList<Document> peerList = new ArrayList<>();
        for (HostPort peer : peers) {
            peerList.add(peer.toDoc());
        }
        doc1.append("command", "LIST_PEERS_RESPONSE");
        doc1.append("peers", peerList);
        return doc1.toJson();
    }

    public static String getConnectPeerResponse(String host, int port, boolean status, String message) {
        Document doc1 = new Document();
        doc1.append("command", "CONNECT_PEER_RESPONSE");
        doc1.append("host", host);
        doc1.append("port", port);
        doc1.append("status", status);
        doc1.append("message", message);
        return doc1.toJson();
    }

    public static String getDisconnectPeerResponse(String host, int port, boolean status, String message) {
        Document doc1 = new Document();
        doc1.append("command", "DISCONNECT_PEER_RESPONSE");
        doc1.append("host", host);
        doc1.append("port", port);
        doc1.append("status", status);
        doc1.append("message", message);
        return doc1.toJson();
    }

    public static String getPayload(String encryptedMessage) {
        Document doc1 = new Document();
        doc1.append("payload", encryptedMessage);
        return doc1.toJson();
    }

	public static String getConnectPeer(String host, int port) {
		Document doc1 = new Document();
        doc1.append("command", "CONNECT_PEER_REQUEST");
        doc1.append("host", host);
        doc1.append("port", port);
        return doc1.toJson();
	}

	public static String getDisconnectPeer(String host, int port) {
		Document doc1 = new Document();
        doc1.append("command", "DISCONNECT_PEER_REQUEST");
        doc1.append("host", host);
        doc1.append("port", port);
        return doc1.toJson();
	}
}