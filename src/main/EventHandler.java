package main;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

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
			System.out.println("putchunk");
			if (header.length != 6) {
				System.out.println("[" + header[0] + "]" + "Header message is invalid.");
				return;
			}
			
			//Check if the chunk is from a file that I requested the backup
			if(this.peer.getFilesBackepUp().containsKey(header[4])) {
				return;
			}
			
			//Save desired replication degree of chunk file
			String hashmapKey = header[4] + "_" + header[3];
			this.peer.getDesiredReplicationDegrees().put(hashmapKey, Integer.parseInt(header[5]));
			
			//Check if I already stored this chunk
			ArrayList<Integer> chunkHosts = peer.getChunkHosts().get(hashmapKey);
			
			if(chunkHosts != null && chunkHosts.contains(this.peer.getID())) {
				return;
			}

			try {
				storeChunk(header[3], header[4], this.body);
			} catch (IOException e1) {
				System.out.println("Error storing chunk");
			}

			break;

		case "STORED":			
			if (header.length != 5) {
				System.out.println("[" + header[0] + "]" + "Header message is invalid.");
				return;
			}
			
			try {
				Util.saveStoredChunksInfo(header[2], header[3], Integer.parseInt(header[4]), this.peer);
			} catch (FileNotFoundException e) {
				System.out.println("Erro");
			} catch (IOException e) {
				System.out.println("Erro");
			}			
			
			break;

		case "GETCHUNK":
			break;

		case "CHUNK":
			break;

		case "DELETE":
			break;

		case "REMOVED":
			break;
		}
	}
	
	private void storeChunk(String fileID, String chunkNr, byte[] content) throws IOException {
		String filePath = Peer.PEERS_FOLDER + "/" + Peer.DISK_FOLDER + peer.getID() + "/" + Peer.CHUNKS_FOLDER + 
				"/" + chunkNr + "_" + fileID;
		
		try (FileOutputStream fos = new FileOutputStream(filePath)) {
			fos.write(content);
		}
		
		//Save chunks info in memory
		Util.saveStoredChunksInfo(new Integer(peer.getID()).toString(), fileID, Integer.parseInt(chunkNr), this.peer);
		
		//Send message STORED
		byte [] packet = makeStoreChunkReply(fileID, chunkNr);
		this.peer.sendReplyToMulticast(Peer.multicastChannel.MC, packet);
	}
	
	private byte[] makeStoreChunkReply(String fileID, String chunkNr) {
		String message = "STORED "+ this.peer.getProtocolVersion() + " " + this.peer.getID() + " " + fileID 
				+ " " + chunkNr + " ";
		message = message + EventHandler.CRLF + EventHandler.CRLF;
		
		return message.getBytes();
	}
}
