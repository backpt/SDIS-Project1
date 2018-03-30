package main;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import protocols.Backup;
import protocols.Delete;

public class EventHandler implements Runnable {
	private Peer peer;
	private String[] header;
	private byte[] body;

	public static final String CRLF = "\r\n";
	public static final byte CR = 0xD;
	public static final byte LF = 0xA;

	public EventHandler(DatagramPacket packet, Peer peer) {
		this.peer = peer;
		splitMessage(packet);
	}

	public void splitMessage(DatagramPacket packet) {
		ByteArrayInputStream input = new ByteArrayInputStream(packet.getData());

		byte character = 0;
		String allHeader = "";

		// Read char by char
		while (character != CR && character != -1) {
			character = (byte) input.read();
			allHeader += (char) character;
		}

		// After CR comes LF to end the header message
		character = (byte) input.read();
		if (character != LF) {
			System.out.println("ERROR: Message with wrong header");
			return;
		} else {
			allHeader += (char) character;
		}

		// Length + 2 because of the extra CRLF
		this.body = Arrays.copyOfRange(packet.getData(), allHeader.length() + 2, packet.getData().length);
		this.header = allHeader.trim().split(" ");
	}

	@Override
	public void run() {		
		// Check if it was me that sent the message - Ignore
		if (Integer.parseInt(header[2]) == this.peer.getID()) {
			return;
		}

		// Compare protocol version of Peer and version of message
		if (!header[1].equals(this.peer.getProtocolVersion())) {
			return;
		}

		switch (header[0]) {

		case "PUTCHUNK":	
			if (header.length != 6) {
				System.out.println("[" + header[0] + "]" + "Header message is invalid.");
				return;
			}
			
			//Save desired replication degree of chunk file
			String hashmapKey = header[4] + "_" + header[3];
			this.peer.getDesiredReplicationDegrees().put(hashmapKey, Integer.parseInt(header[5]));
			
			//Check if I already stored this chunk
			CopyOnWriteArrayList<Integer> chunkHosts = peer.getChunkHosts().get(hashmapKey);
			
			if(chunkHosts != null && chunkHosts.contains(this.peer.getID())) {
				return;
			}
			
			Random random = new Random();
	        int waitTime = random.nextInt(400);
	        
	        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
			
			executor.schedule(storeChunk, waitTime, TimeUnit.MILLISECONDS);

			break;

		case "STORED":						
			if (header.length != 5) {
				System.out.println("[" + header[0] + "]" + "Header message is invalid.");
				return;
			}
			
			//Save chunks information
			this.peer.storeChunkInfo(Integer.parseInt(this.header[2]), this.header[3], Integer.parseInt(this.header[4]));
			this.peer.saveChunksInfoFile();
			
			break;

		case "GETCHUNK":
			break;

		case "CHUNK":
			break;

		case "DELETE":
			if (header.length != 4) {
				System.out.println("[" + header[0] + "]" + "Header message is invalid.");
				return;
			}
			
			new Thread(new Delete(header[3], this.peer)).start();
			
			break;

		case "REMOVED":
			break;
		}
	}
	
	private byte[] makeStoreChunkReply(String fileID, String chunkNr) {
		String message = "STORED "+ this.peer.getProtocolVersion() + " " + this.peer.getID() + " " + fileID 
				+ " " + chunkNr + " ";
		message = message + EventHandler.CRLF + EventHandler.CRLF;
		
		return message.getBytes();
	}

	Runnable storeChunk = () -> {
    	String filePath = Peer.PEERS_FOLDER + "/" + Peer.DISK_FOLDER + peer.getID() + "/" + Peer.CHUNKS_FOLDER + 
				"/" + this.header[4] + "_" + this.header[3];
		
		try (FileOutputStream fos = new FileOutputStream(filePath)) {
			fos.write(this.body);
		} catch (IOException e) {
			System.out.println("Error saving chunk file");
		}
		
		//Save chunks information
		this.peer.storeChunkInfo(this.peer.getID(), this.header[3], Integer.parseInt(this.header[4]));
		this.peer.saveChunksInfoFile();
		
		//Send message STORED
		byte [] packet = makeStoreChunkReply(this.header[3], this.header[4]);
		try {
			this.peer.sendReplyToMulticast(Peer.multicastChannel.MC, packet);
		} catch (IOException e) {
			System.out.println("Error sending message to multicast");
		}
	};
}
