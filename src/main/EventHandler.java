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
			if (header.length != 6) {
				System.out.println("[" + header[0] + "]" + "Header message is invalid.");
				return;
			}
			
			//Check if the chunk is from a file that I requested the backup
			if(this.peer.getFilesBackepUp().containsKey(header[4])) {
				return;
			}
			
			//Save desired replication degree of chunk file
			this.peer.getDesiredReplicationDegrees().put(header[4] + "_" + header[3], Integer.parseInt(header[5]));

			new Thread(new FileChunk(header[3], Integer.parseInt(header[4]), this.body, Integer.parseInt(header[5]), this.peer)).start();

			break;

		case "STORED":			
			if (header.length != 5) {
				System.out.println("[" + header[0] + "]" + "Header message is invalid.");
				return;
			}
			
			try {
				saveStoredChunksInfo(header[2], header[3], header[4]);
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

	private void saveStoredChunksInfo(String senderID, String fileID, String chunkNr) throws IOException {
		String hashmapKey = chunkNr + "_" + fileID;
		
		ArrayList<Integer> chunkHosts = this.peer.getChunkHosts().get(hashmapKey);
		
		//Check if is the first stored message of the chunk
		if(chunkHosts == null) {
			chunkHosts = new ArrayList<Integer>();
			chunkHosts.add(Integer.parseInt(senderID));
			
			this.peer.getChunkHosts().put(hashmapKey, chunkHosts);
			this.peer.getStoredReplies().put(hashmapKey, 1);
		} else {
			//Check if senderID is already in the list
			if(!chunkHosts.contains(senderID)) {
				chunkHosts.add(Integer.parseInt(senderID));
				this.peer.getChunkHosts().put(hashmapKey, chunkHosts);
			}
			
			this.peer.getStoredReplies().put(hashmapKey, chunkHosts.size());
		}
		
		saveNonVolatileMemory(hashmapKey, chunkHosts.size());
	}

	private void saveNonVolatileMemory(String key, int actualReplicationDegree) throws IOException {
		int desiredReplicationDegree = this.peer.getDesiredReplicationDegrees().get(key);
		Properties chunksProperties = this.peer.getChunksInfoProperties();
		OutputStream chunksFile = null;		
		
		chunksProperties.setProperty(key + "_desired", Integer.toString(desiredReplicationDegree));
		chunksProperties.setProperty(key + "_actual", Integer.toString(actualReplicationDegree));

		try {
			chunksFile = new FileOutputStream(Peer.PEERS_FOLDER + "/" + Peer.DISK_FOLDER + this.peer.getID() + "/" + Peer.CHUNKS_FOLDER + "/chunksInfo.properties");
			
			chunksProperties.store(chunksFile, null);
		} catch (IOException io) {
			System.out.println("Erro");
		} finally {
			if (chunksFile != null) {
				try {
					chunksFile.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}
	}
}
