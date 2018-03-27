package main;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Properties;

public final class Util {
	
	public static void saveStoredChunksInfo(String senderID, String fileID, int chunkNr, Peer peer) throws IOException {
		String hashmapKey = chunkNr + "_" + fileID;
		
		ArrayList<Integer> chunkHosts = peer.getChunkHosts().get(hashmapKey);
		
		//Check if is the first stored message of the chunk
		if(chunkHosts == null) {
			chunkHosts = new ArrayList<Integer>();
			chunkHosts.add(Integer.parseInt(senderID));
			
			peer.getChunkHosts().put(hashmapKey, chunkHosts);
			peer.getActualReplicationDegrees().put(hashmapKey, chunkHosts.size());
		} else {
			//Check if senderID is already in the list
			if(!chunkHosts.contains(Integer.parseInt(senderID))) {
				chunkHosts.add(Integer.parseInt(senderID));
				peer.getChunkHosts().replace(hashmapKey, chunkHosts);
				peer.getActualReplicationDegrees().replace(hashmapKey, chunkHosts.size());
			}
		}
		
		Util.saveNonVolatileMemory(hashmapKey, chunkHosts.size(), peer);
	}
	
	public static void saveNonVolatileMemory(String key, int actualReplicationDegree, Peer peer) throws IOException {
		int desiredReplicationDegree = -1; 
		if(peer.getDesiredReplicationDegrees().get(key) != null)
			desiredReplicationDegree = peer.getDesiredReplicationDegrees().get(key);
		
		Properties chunksProperties = peer.getChunksInfoProperties();
		OutputStream chunksFile = null;		
		
		chunksProperties.setProperty(key + "_desired", Integer.toString(desiredReplicationDegree));
		chunksProperties.setProperty(key + "_actual", Integer.toString(actualReplicationDegree));

		try {
			chunksFile = new FileOutputStream(Peer.PEERS_FOLDER + "/" + Peer.DISK_FOLDER + peer.getID() + "/" + Peer.CHUNKS_FOLDER + "/chunksInfo.properties");
			
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
