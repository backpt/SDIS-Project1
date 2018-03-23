package main;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class FileChunk implements Runnable {
	
	private String fileID;
	private int number;
	private byte[] content;
	private int replicationDegree;
	private Peer peer;
	
	public FileChunk(String fileID, int chunkNr, byte[] content, int replication, Peer peer) {
		this.fileID = fileID;
		this.number = chunkNr;
		this.content = content;
		this.replicationDegree = replication;
		this.peer = peer;
	}

	@Override
	public void run() {
		byte [] packet = makePutChunkRequest();
		try {
			this.peer.sendReplyToMulticast(Peer.multicastChannel.MDB, packet);
		} catch (IOException e1) {
			System.out.println("Error sending putchunk message");
		}
		
		isBackedUp();
	}

	private void isBackedUp() {
		String hashmapKey = this.number + "_" + this.fileID;
		int actualReplicationDegree = 0;
		
		if(this.peer.getChunkHosts().get(hashmapKey) != null) {
			//actualReplicationDegree = 
		}
		
	}
	
	private byte[] makePutChunkRequest() {
		String message = "PUTCHUNK "+ this.peer.getProtocolVersion() + " " +this.peer.getID() + " " + this.fileID + " " + this.number +
				" " + this.replicationDegree + " ";
		message = message + EventHandler.CRLF + EventHandler.CRLF;
		
		byte [] header = message.getBytes();
		byte[] packet = new byte[header.length + this.content.length];
		System.arraycopy(header, 0, packet, 0, header.length);
		System.arraycopy(this.content, 0, packet, header.length, this.content.length);
		
		return packet;
	}

}
