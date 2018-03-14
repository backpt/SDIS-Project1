package main;

public class FileChunk implements Runnable {
	
	private String fileID;
	private int number;
	private byte[] content;
	private int size;
	private int replicationDegree;
	
	public FileChunk(String fileID, int chunkNr, byte[] content, int size, int replication) {
		this.fileID = fileID;
		this.number = chunkNr;
		this.content = content;
		this.size = size;
		this.replicationDegree = replication;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}

}
