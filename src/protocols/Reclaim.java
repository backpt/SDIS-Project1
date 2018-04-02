package protocols;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import main.EventHandler;
import main.Peer;

public class Reclaim implements Runnable {
	
	private long spaceReclaim;
	private Peer peer;
	private ArrayList<String> chunksDeleted;
	private long diskUsed;
	private String chunksPath;
	
	public Reclaim(long kbytes, Peer peer) {
		this.spaceReclaim = kbytes * 1000;
		this.peer = peer;
		this.diskUsed = peer.getDiskUsed();
		this.chunksPath = Peer.PEERS_FOLDER + "/" + Peer.DISK_FOLDER + this.peer.getID() + "/" + Peer.CHUNKS_FOLDER;
		this.chunksDeleted = new ArrayList<String>();
	}

	@Override
	public void run() {
		getChunksToRemove();
		sendRemoveMessages();
		
		System.out.println("Reclaim finished. Disk usage updated to: " +this.peer.getDiskUsed());
	}

	private void getChunksToRemove() {
		while(this.diskUsed > spaceReclaim) {			
			File chunksFolder = new File(this.chunksPath);
			File [] chunksList = chunksFolder.listFiles();
			
			//No more chunks to delete
			if(chunksList == null) {
				System.out.println("No more chunks to erase.");
				return;
			}
			
			//Remove the first chunk of the list
			File chunkToDelete = chunksList[0];
			
			//Update run-time memory
			String chunkName = chunkToDelete.getName();
			chunksDeleted.add(chunkName);
			this.peer.removeChunkInfo(chunkName, this.peer.getID());
			this.peer.getChunksStoredSize().remove(chunkName);
			this.diskUsed = this.diskUsed - chunkToDelete.length();	
			
			//Delete file from disk
			chunkToDelete.delete();		
		}
		
		this.peer.setDiskUsed(this.diskUsed);
		this.peer.setDiskMaxSpace(this.spaceReclaim);
		this.peer.saveChunksInfoFile();
		this.peer.saveFilesInfoFile();
	}
	
	private void sendRemoveMessages() {
		for(String key : chunksDeleted) {
			byte [] packet = makeRemoveMessage(key);
			try {
				this.peer.sendReplyToMulticast(Peer.multicastChannel.MC, packet);
			} catch (IOException e) {
				System.out.println("Error sending removed message.");
			}
		}
	}

	private byte[] makeRemoveMessage(String key) {
		String [] fileInfo = key.split("_");
		
		String message = "REMOVED" + " " + this.peer.getProtocolVersion() + " " + this.peer.getID() + " " + fileInfo[1] 
				+ " " + fileInfo[0] + " ";
		message = message + EventHandler.CRLF + EventHandler.CRLF;
		
		return message.getBytes();
	}

}
