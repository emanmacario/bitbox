package unimelb.bitbox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.json.simple.JSONObject;

import unimelb.bitbox.util.Document;

public class Messages {
	public Messages() {
		
        
	}
	
	public String getInvalidProtocol(String message) {
		Document doc1 = new Document();
		doc1.append("command","INVALID_PROTOCOL");
		doc1.append("messsage",message);
        
        return doc1.toJson();
	}
	
	public String getHandshakeRequest(String host, int port) {
		Document doc1 = new Document();
        doc1.append("host",host);
        doc1.append("port",port);
        Document doc2 = new Document();
        doc2.append("hostPort",doc1);
        doc2.append("command","HANDSHAKE_REQUEST");
        
        return doc2.toJson();
	}
	
	public String getHandshakeResponse(String host, Long port) {
		Document doc1 = new Document();
        doc1.append("host",host);
        doc1.append("port",port);
        Document doc2 = new Document();
        doc2.append("hostPort",doc1);
        doc2.append("command","HANDSHAKE_RESPONSE");
        
        return doc2.toJson();
	}
	
	public String getConnectionRefused(HashMap<String,Long> peers, String message) {
		ArrayList<Document> docs = new ArrayList<Document>();
		Document doc1 = new Document();
		
		for (HashMap.Entry<String, Long> entry : peers.entrySet()) {
		    Long port = entry.getValue();
		    String host = entry.getKey();
		    doc1.append("host",host);
	        doc1.append("port",port);
	        docs.add(doc1);
		}
	    
		Document doc2 = new Document();
		doc2.append("peers",docs);
        
        doc2.append("command","CONNECTION_REFUSED");
        doc2.append("message",message);
        
        return doc2.toJson();
	}
	
	public String getFileCreateRequest(String md5, Long lastModified, Long fileSize, String pathName) {
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
	
	public String getFileCreateResponse(String md5, Long lastModified, Long fileSize, String pathName, String message, boolean status) {
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
	
	public String getFileBytesRequest(String md5, Long lastModified, Long fileSize, String pathName, long position, long length) {
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
	
	public String getFileBytesResponse(String md5, Long lastModified, Long fileSize, String pathName, long position, long length, String content,String message, boolean status) {
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
	
	public String getFileDeleteRequest(String md5, Long lastModified, Long fileSize, String pathName) {
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
	
	public String getFileDeleteResponse(String md5, Long lastModified, Long fileSize, String pathName,String message, boolean status) {
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
	
	public String getFileModifyRequest(String md5, Long lastModified, Long fileSize, String pathName) {
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
	
	public String getFileModifyResponse(String md5, Long lastModified, Long fileSize, String pathName,String message, boolean status) {
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
	
	public String getDirectoryCreateRequest(String pathName) {
		Document doc1 = new Document();
        doc1.append("command","DIRECTORY_CREATE_REQUEST");
        doc1.append("pathName",pathName);
        
        return doc1.toJson();
	}
	
	public String getDirectoryCreateResponse(String pathName, String message, boolean status) {
		Document doc1 = new Document();
        doc1.append("command","DIRECTORY_CREATE_RESPONSE");
        doc1.append("pathName",pathName);
        doc1.append("message",message);
        doc1.append("status",status);
        
        return doc1.toJson();
	}
	
	public String getDirectoryDeleteRequest(String pathName) {
		Document doc1 = new Document();
        doc1.append("command","DIRECTORY_DELETE_REQUEST");
        doc1.append("pathName",pathName);
        
        return doc1.toJson();
	}
	
	public String getDirectoryDeleteResponse(String pathName, String message, boolean status) {
		Document doc1 = new Document();
        doc1.append("command","DIRECTORY_DELETE_RESPONSE");
        doc1.append("pathName",pathName);
        doc1.append("message",message);
        doc1.append("status",status);
        
        return doc1.toJson();
	}
	
}