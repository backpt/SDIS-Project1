package main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import protocols.Backup;

public class Peer {

	// Peer configurations
	private String protocolVersion;
	private int serverID;
	private InetAddress address;
	private int port;
	private Properties chunksInfo;

	// Multicast configurations
	private InetAddress addressMC;
	private InetAddress addressMDB;
	private InetAddress addressMDR;
	private int portMC;
	private int portMDB;
	private int portMDR;

	// Global configurations
	public static final String PEERS_FOLDER = "Peers";
	public static final String DISK_FOLDER = "DiskPeer";
	public static final String SHARED_FOLDER = "Shared";
	public static final String FILES_FOLDER = "Files";
	public static final String CHUNKS_FOLDER = "Chunks";
	public static final String CHUNKS_INFO = "chunks_info.txt";
	public static final String FILES_INFO = "files_info.txt";

	public static enum multicastChannel {
		MC, MDB, MDR
	};

	// Data structures
	/**
	 * Maps all the fileIDs with the respective filename for each file whose backup
	 * it has initiated - <FileName><FileID>
	 */
	private ConcurrentHashMap<String, String> filesIdentifiers;

	/**
	 * Stores the backup state for each file whose backup it has initiated -
	 * <FileID><true>
	 */
	private ConcurrentHashMap<String, Boolean> backupState;

	/**
	 * Stores how many chunks a file has - <FileID><Number of Chunks>
	 */
	private ConcurrentHashMap<String, Integer> numberChunksPerFile;

	/**
	 * Stores the actual replication degree of each chunk file -
	 * <ChunkNr_FileID><Replication Degree>
	 */
	private ConcurrentHashMap<String, Integer> actualReplicationDegrees;

	/**
	 * Stores the replication degree of each chunk file -
	 * <ChunkNr_FileID><Replication Degree>
	 */
	private ConcurrentHashMap<String, Integer> desiredReplicationDegrees;

	/**
	 * Stores who has stored the chunk - <ChunkNr_FileID><List of Peer IDs>
	 */
	private ConcurrentHashMap<String, CopyOnWriteArrayList<Integer>> chunksHosts;
	
	private ConcurrentHashMap<String, byte[]> restoredChunks;
	
	private ConcurrentHashMap<String, Boolean> waitRestoredChunks;

	public Peer(String protocol, int id, String ap, InetAddress addressMC, int portMC, InetAddress addressMDB,
			int portMDB, InetAddress addressMDR, int portMDR) throws IOException {
		this.protocolVersion = protocol;
		this.serverID = id;
		this.addressMC = addressMC;
		this.addressMDB = addressMDB;
		this.addressMDR = addressMDR;
		this.portMC = portMC;
		this.portMDB = portMDB;
		this.portMDR = portMDR;

		// Make peer disk
		String peerDisk = PEERS_FOLDER + "/" + DISK_FOLDER + id;
		String backupFiles = peerDisk + "/" + FILES_FOLDER;
		String chunksFiles = peerDisk + "/" + CHUNKS_FOLDER;
		String sharedFolder = PEERS_FOLDER + "/" + SHARED_FOLDER;

		makeDirectory(peerDisk);
		makeDirectory(backupFiles);
		makeDirectory(chunksFiles);
		makeDirectory(sharedFolder);

		if (!loadFilesInfo()) {
			initializeFilesAttributes();
		}
		if (!loadChunksInfo()) {
			initializeChunksAttributes();
		}

		// Connect to multicast channels
		new Thread(new MulticastListenner(addressMC, portMC, this)).start();
		new Thread(new MulticastListenner(addressMDB, portMDB, this)).start();
		new Thread(new MulticastListenner(addressMDR, portMDR, this)).start();

		// Client-Peer Communication Test
		if (this.serverID == 1 || this.serverID == 2) {
			createBackup("bigbackup.txt", 3);
			//sendDeleteRequest("test.pdf");
		}
	}

	// Send delete message to MC multicast channel
	public void sendDeleteRequest(String fileName) {
		String fileID = this.filesIdentifiers.get(fileName);

		if (fileID != null) {
			String message = "DELETE " + this.protocolVersion + " " + this.serverID + " " + fileID + " ";
			message = message + EventHandler.CRLF + EventHandler.CRLF;

			try {
				sendReplyToMulticast(Peer.multicastChannel.MC, message.getBytes());
			} catch (IOException e) {
				System.out.println("Error sending delete message to multicast.");
			}

			this.backupState.replace(fileID, false);
		} else {
			System.out.println("Error deleting the file, because it wasn't backed up by me.");
		}
	}

	private void initializeFilesAttributes() {
		this.filesIdentifiers = new ConcurrentHashMap<String, String>();
		this.backupState = new ConcurrentHashMap<String, Boolean>();
		this.numberChunksPerFile = new ConcurrentHashMap<String, Integer>();
	}

	private void initializeChunksAttributes() {
		this.actualReplicationDegrees = new ConcurrentHashMap<String, Integer>();
		this.desiredReplicationDegrees = new ConcurrentHashMap<String, Integer>();
		this.chunksHosts = new ConcurrentHashMap<String, CopyOnWriteArrayList<Integer>>();
	}

	private void createBackup(String filename, int replication) throws FileNotFoundException, IOException {
		new Thread(new Backup(filename, replication, this)).start();
	}

	public void sendReplyToMulticast(multicastChannel type, byte[] packet) throws IOException {
		switch (type) {
		case MC:
			MulticastSocket socketMC = new MulticastSocket(portMC);

			DatagramPacket sendPacketMC = new DatagramPacket(packet, packet.length, addressMC, portMC);
			socketMC.send(sendPacketMC);

			socketMC.close();
			break;

		case MDB:
			MulticastSocket socketMDB = new MulticastSocket(portMDB);

			DatagramPacket sendPacketMDB = new DatagramPacket(packet, packet.length, addressMDB, portMDB);
			socketMDB.send(sendPacketMDB);

			socketMDB.close();
			break;

		case MDR:
			MulticastSocket socketMDR = new MulticastSocket(portMDR);

			DatagramPacket sendPacketMDR = new DatagramPacket(packet, packet.length, addressMDR, portMDR);
			socketMDR.send(sendPacketMDR);

			socketMDR.close();

			break;
		}
	}

	// Method to load the non volatile memory about the backup files
	@SuppressWarnings("unchecked")
	public synchronized boolean loadFilesInfo() {
		try {
			ObjectInputStream serverStream = new ObjectInputStream(new FileInputStream(
					Peer.PEERS_FOLDER + "/" + Peer.DISK_FOLDER + this.serverID + "/" + Peer.FILES_INFO));

			this.filesIdentifiers = (ConcurrentHashMap<String, String>) serverStream.readObject();
			this.backupState = (ConcurrentHashMap<String, Boolean>) serverStream.readObject();
			this.numberChunksPerFile = (ConcurrentHashMap<String, Integer>) serverStream.readObject();

			serverStream.close();
		} catch (IOException | ClassNotFoundException e) {
			System.err.println("Error loading the files info file.");
			return false;
		}

		return true;
	}

	// Method to load the non volatile memory about the chunk files
	@SuppressWarnings("unchecked")
	public synchronized boolean loadChunksInfo() {
		try {
			ObjectInputStream serverStream = new ObjectInputStream(new FileInputStream(
					Peer.PEERS_FOLDER + "/" + Peer.DISK_FOLDER + this.serverID + "/" + Peer.CHUNKS_INFO));

			this.actualReplicationDegrees = (ConcurrentHashMap<String, Integer>) serverStream.readObject();
			this.desiredReplicationDegrees = (ConcurrentHashMap<String, Integer>) serverStream.readObject();
			this.chunksHosts = (ConcurrentHashMap<String, CopyOnWriteArrayList<Integer>>) serverStream.readObject();

			serverStream.close();
		} catch (IOException | ClassNotFoundException e) {
			System.err.println("Error loading the chunks info file.");
			return false;
		}

		return true;
	}

	// Method to save all the runtime data of the server
	public synchronized void saveChunksInfoFile() {
		try {
			ObjectOutputStream serverStream = new ObjectOutputStream(new FileOutputStream(
					Peer.PEERS_FOLDER + "/" + Peer.DISK_FOLDER + this.serverID + "/" + Peer.CHUNKS_INFO));

			serverStream.writeObject(this.actualReplicationDegrees);
			serverStream.writeObject(this.desiredReplicationDegrees);
			serverStream.writeObject(this.chunksHosts);

			serverStream.close();
		} catch (IOException e) {
			System.err.println("Error writing the server info file.");
		}
	}

	// Method to save all the runtime data of the server
	public synchronized void saveFilesInfoFile() {
		try {
			ObjectOutputStream serverStream = new ObjectOutputStream(new FileOutputStream(
					Peer.PEERS_FOLDER + "/" + Peer.DISK_FOLDER + this.serverID + "/" + Peer.FILES_INFO));

			serverStream.writeObject(this.filesIdentifiers);
			serverStream.writeObject(this.backupState);
			serverStream.writeObject(this.numberChunksPerFile);

			serverStream.close();
		} catch (IOException e) {
			System.err.println("Error writing the server info file.");
		}
	}

	public void storeChunkInfo(int senderID, String fileID, int chunkNr) {
		String hashmapKey = chunkNr + "_" + fileID;

		CopyOnWriteArrayList<Integer> chunkHosts = this.chunksHosts.get(hashmapKey);

		// Check if is the first stored message of the chunk
		if (chunkHosts == null) {
			chunkHosts = new CopyOnWriteArrayList<Integer>();
			chunkHosts.add(senderID);

			this.chunksHosts.put(hashmapKey, chunkHosts);
			this.actualReplicationDegrees.put(hashmapKey, chunkHosts.size());
		} else {
			// Check if senderID is already in the list
			if (!chunkHosts.contains(senderID)) {
				chunkHosts.add(senderID);
				this.chunksHosts.replace(hashmapKey, chunkHosts);
				this.actualReplicationDegrees.replace(hashmapKey, chunkHosts.size());
			}
		}
	}

	public void removeChunkInfo(String hashmapKey) {
		CopyOnWriteArrayList<Integer> chunkHosts = this.chunksHosts.get(hashmapKey);

		// Check if is the first stored message of the chunk
		if (chunkHosts != null && chunkHosts.contains(this.serverID)) {
			int index = chunkHosts.indexOf(this.serverID);
			chunkHosts.remove(index);
			this.chunksHosts.replace(hashmapKey, chunkHosts);
			this.actualReplicationDegrees.replace(hashmapKey, chunkHosts.size());
		}
	}

	private void makeDirectory(String path) {
		File file = new File(path);

		if (file.mkdirs()) {
			System.out.println("Folder " + path + " created.");
		}
	}

	public String getProtocolVersion() {
		return this.protocolVersion;
	}

	public int getID() {
		return this.serverID;
	}

	public ConcurrentHashMap<String, Integer> getNumberOfChunksPerFile() {
		return this.numberChunksPerFile;
	}

	public ConcurrentHashMap<String, String> getFilesIdentifiers() {
		return this.filesIdentifiers;
	}

	public ConcurrentHashMap<String, Boolean> getBackupState() {
		return this.backupState;
	}

	public ConcurrentHashMap<String, Integer> getDesiredReplicationDegrees() {
		return this.desiredReplicationDegrees;
	}

	public ConcurrentHashMap<String, Integer> getActualReplicationDegrees() {
		return this.actualReplicationDegrees;
	}

	public ConcurrentHashMap<String, CopyOnWriteArrayList<Integer>> getChunkHosts() {
		return this.chunksHosts;
	}
	
	public ConcurrentHashMap<String, byte[]> getRestoredChunks() {
		return this.restoredChunks;
	}

	public ConcurrentHashMap<String, Boolean> getWaitRestoredChunks() {
		return this.waitRestoredChunks;
	}
}
