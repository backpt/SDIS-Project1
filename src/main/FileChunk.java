package main;

import java.io.FileOutputStream;
import java.io.IOException;

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
		try {
			storeChunk();
		} catch (IOException e) {
			System.out.println("Error storing a chunk");
		}
	}

	private void storeChunk() throws IOException {
		String filePath = Peer.PEERS_FOLDER + "/" + Peer.DISK_FOLDER + peer.getID() + "/" + Peer.CHUNKS_FOLDER + 
				"/" + this.number + "_" + this.fileID;
		
		try (FileOutputStream fos = new FileOutputStream(filePath)) {
			fos.write(this.content);
		}
		
		byte [] packet = makeStoreChunkReply();
		this.peer.sendReplyToMulticast(Peer.multicastChannel.MC, packet);
	}
	
	private byte[] makeStoreChunkReply() {
		String message = "STORED "+ this.peer.getProtocolVersion() + " " + this.peer.getID() + " " + this.fileID 
				+ " " + this.number + " ";
		message = message + EventHandler.CRLF + EventHandler.CRLF;
		
		return message.getBytes();
	}

}
