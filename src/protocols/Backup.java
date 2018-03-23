package protocols;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.io.FileNotFoundException;
import java.io.IOException;
import main.*;

public class Backup {
	private String fileID;
	private String fileName;
	private String filePath;
	private int replicationDegree;
	private Peer peer;

	private final int CHUNK_MAX_SIZE = 64000;

	public Backup(String file, int replication, Peer peer) throws FileNotFoundException, IOException {
		this.filePath = Peer.PEERS_FOLDER + "/" + Peer.DISK_FOLDER + peer.getID() + "/" + Peer.FILES_FOLDER + "/" + file;
		this.replicationDegree = replication;
		this.peer = peer;
		this.fileName = file;

		createIdentifier();
		splitFile();
	}

	private void createIdentifier() {
		try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            FileInputStream fis = new FileInputStream(this.filePath);
           
            byte[] dataBytes = new byte[8192];
 
            int nread = 0;
            while ((nread = fis.read(dataBytes)) != -1) {
              md.update(dataBytes, 0, nread);
            };
            byte[] mdbytes = md.digest();
            fis.close();
 
 
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < mdbytes.length; i++) {
              sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
            }
           
            this.fileID = sb.toString(); 
        } catch (Exception e) {
            e.printStackTrace();
        }
	}

	private void splitFile() throws FileNotFoundException, IOException {
		byte[] buffer = new byte[CHUNK_MAX_SIZE];
		int chunkNr = 0;

		File file = new File(this.filePath);

		try (FileInputStream fis = new FileInputStream(file); 
				BufferedInputStream bis = new BufferedInputStream(fis)) {

			int size = 0;
			while ((size = bis.read(buffer)) > 0) {
				new Thread(new FileChunk(this.fileID, chunkNr, buffer, this.replicationDegree, this.peer)).start();
				
				//byte [] packet = makePutChunkRequest(this.fileID, chunkNr, buffer, size, this.replicationDegree, this.peer);
				//this.peer.sendReplyToMulticast(Peer.multicastChannel.MDB, packet);
				//
				this.peer.getDesiredReplicationDegrees().put(chunkNr + "_" + this.fileID, this.replicationDegree);
				chunkNr++;
			}
		}
		
		this.peer.getFilesIdentifiers().put(this.fileName, this.fileID);
		this.peer.getNumberOfChunksPerFile().put(this.fileID, chunkNr);
		this.peer.getFilesBackepUp().put(this.fileID, false);
	}

}
