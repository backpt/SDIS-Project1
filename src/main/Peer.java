package main;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import protocols.Backup;

public class Peer {
	
	//Peer configurations
	private String protocolVersion;
	private int serverID;
	private InetAddress address;
	private int port;
	private Properties chunksInfo;
	
	//Multicast configurations
	private InetAddress addressMC;
	private InetAddress addressMDB;
	private InetAddress addressMDR;
	private int portMC;
	private int portMDB;
	private int portMDR;
	
	//Global configurations
	public static final String PEERS_FOLDER = "Peers";
	public static final String DISK_FOLDER = "DiskPeer";
	public static final String FILES_FOLDER = "Files";
	public static final String CHUNKS_FOLDER = "Chunks";
	public static final String CHUNKS_INFO = "chunksInfo.properties";
	public static enum multicastChannel {MC, MDB, MDR};
	
	//Data structures
	/**
	 * Maps all the fileIDs with the respective filename - <FileName><FileID>
	 */
	private HashMap<String, String> filesIdentifiers;
	
	/**
	 * Stores the state of the file to backup - <FileID><Backed up or not>
	 */
	private ConcurrentHashMap<String, Boolean> filesBackedUp;
	
	/**
	 * Stores how many chunks a file has - <FileID><Number of Chunks>
	 */
	private ConcurrentHashMap<String, Integer> numberChunksPerFile;
	
	/**
	 * Stores the actual replication degree of each chunk file - <ChunkNr_FileID><Replication Degree>
	 */
	private ConcurrentHashMap<String, Integer> actualReplicationDegrees;
	
	/**
	 * Stores the replication degree of each chunk file - <ChunkNr_FileID><Replication Degree>
	 */
	private ConcurrentHashMap<String, Integer> desiredReplicationDegrees;
	
	/**
	 * Stores who has stored the chunk - <ChunkNr_FileID><List of Peer IDs>
	 */
	private ConcurrentHashMap<String, ArrayList<Integer>> chunksHosts;
	
	
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
		
		initializeAttributes();
		
		//Connect to multicast channels
		new Thread(new MulticastListenner(addressMC, portMC, this)).start();
		new Thread(new MulticastListenner(addressMDB, portMDB, this)).start();
		new Thread(new MulticastListenner(addressMDR, portMDR, this)).start();
		
		//Make peer disk
		String peerDisk = PEERS_FOLDER + "/" + DISK_FOLDER + id;
		String backupFiles = peerDisk + "/" + FILES_FOLDER;
		String chunksFiles = peerDisk + "/" + CHUNKS_FOLDER;
		
		makeDirectory(peerDisk);
		makeDirectory(backupFiles);
		makeDirectory(chunksFiles);
		
		//Client-Peer Communication Test
		if(this.serverID == 1) {
			createBackup("test.pdf", 3);
		} 
	}
	
	private void initializeAttributes() {
		this.chunksInfo = new Properties();
		this.filesIdentifiers = new HashMap<String, String>();
		this.filesBackedUp = new ConcurrentHashMap<String, Boolean>();
		this.numberChunksPerFile = new ConcurrentHashMap<String, Integer>();
		this.actualReplicationDegrees = new ConcurrentHashMap<String, Integer>();
		this.desiredReplicationDegrees = new ConcurrentHashMap<String, Integer>();
		this.chunksHosts = new ConcurrentHashMap<String, ArrayList<Integer>>();
	}

	private void createBackup(String filename, int replication) throws FileNotFoundException, IOException {
		new Backup(filename, replication, this);
	}

	public void sendReplyToMulticast(multicastChannel type, byte [] packet) throws IOException {
		switch(type) {
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
	
	private void makeDirectory(String path) {
		File file = new File(path);
		
		if(file.mkdirs()) {
			System.out.println("Folder "+path+ " created.");
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
	
	public HashMap<String, String> getFilesIdentifiers() {
		return this.filesIdentifiers;
	}
	
	public ConcurrentHashMap<String, Boolean> getFilesBackepUp() {
		return this.filesBackedUp;
	}
	
	public ConcurrentHashMap<String, Integer> getDesiredReplicationDegrees() {
		return this.desiredReplicationDegrees;
	}
	
	public ConcurrentHashMap<String, Integer> getActualReplicationDegrees() {
		return this.actualReplicationDegrees;
	}
	
	public ConcurrentHashMap<String, ArrayList<Integer>> getChunkHosts() {
		return this.chunksHosts;
	}
	
	public Properties getChunksInfoProperties() {
		return this.chunksInfo;
	}
}
