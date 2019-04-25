package unimelb.bitbox;

import java.util.ArrayList;
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
	
	public static String getConnectionRefused(Map<String,Integer> peers, String message) {
		ArrayList<Document> peerList = new ArrayList<Document>();
		for (Map.Entry<String, Integer> entry : peers.entrySet()) {
            String host = entry.getKey();
		    Integer port = entry.getValue();
		    peerList.add(new HostPort(host, port).toDoc());
		}

		System.out.println("Size of peerlist:" + peerList.size());
	    
		Document doc = new Document();
		doc.append("peers", peerList);
        doc.append("command","CONNECTION_REFUSED");
        doc.append("message",message);
        
        return doc.toJson();
	}
	
	public static String getFileCreateRequest(String md5, Long lastModified, Long fileSize, String pathName) {
		Document doc1 = new Document();
        doc1.append("md5",md5);
        doc1.append("lastModified",lastModified.toString());
        doc1.append("fileSize",fileSize.toString());
        
        Document doc2 = new Document();
		doc2.append("fileDescriptor",doc1);
        doc2.append("command","FILE_CREATE_REQUEST");
        doc2.append("pathName",pathName);
        
        return doc2.toJson();
	}
	
	public static String getFileCreateResponse(String md5, Long lastModified, Long fileSize, String pathName, String message, boolean status) {
		Document doc1 = new Document();
        doc1.append("md5",md5);
        doc1.append("lastModified",lastModified.toString());
        doc1.append("fileSize",fileSize.toString());
        
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
        doc1.append("lastModified",lastModified.toString());
        doc1.append("fileSize",fileSize.toString());
        
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
        doc1.append("lastModified",lastModified.toString());
        doc1.append("fileSize",fileSize.toString());
        
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
        doc1.append("lastModified",lastModified.toString());
        doc1.append("fileSize",fileSize.toString());
        
        Document doc2 = new Document();
		doc2.append("fileDescriptor",doc1);
        doc2.append("command","FILE_DELETE_REQUEST");
        doc2.append("pathName",pathName);
        
        return doc2.toJson();
	}
	
	public static String getFileDeleteResponse(String md5, Long lastModified, Long fileSize, String pathName,String message, boolean status) {
		Document doc1 = new Document();
        doc1.append("md5",md5);
        doc1.append("lastModified",lastModified.toString());
        doc1.append("fileSize",fileSize.toString());
        
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
        doc1.append("lastModified",lastModified.toString());
        doc1.append("fileSize",fileSize.toString());
        
        Document doc2 = new Document();
		doc2.append("fileDescriptor",doc1);
        doc2.append("command","FILE_MODIFY_REQUEST");
        doc2.append("pathName",pathName);
        
        return doc2.toJson();
	}
	
	public static String getFileModifyResponse(String md5, Long lastModified, Long fileSize, String pathName,String message, boolean status) {
		Document doc1 = new Document();
        doc1.append("md5",md5);
        doc1.append("lastModified",lastModified.toString());
        doc1.append("fileSize",fileSize.toString());
        
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
	
}