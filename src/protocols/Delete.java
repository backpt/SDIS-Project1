package protocols;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;

import main.Peer;

public class Delete implements Runnable {
	
	private String fileID;
	private Peer peer;
	
	public Delete(String file, Peer peer) {
		this.fileID = file;
		this.peer = peer;
	}
	
	@Override
	public void run() {
		File[] chunks = searchChunks(this.fileID);
		
		if(chunks != null) {
			deleteChunks(chunks);
		}
		
		this.peer.saveChunksInfoFile();
		this.peer.saveFilesInfoFile();
	}

	private void deleteChunks(File[] chunks) {
    	for(File file : chunks) {
    		try {
    			String filename = file.getName();
				Files.delete(file.toPath());
				
				//Update memory info
				this.peer.removeChunkInfo(filename, this.peer.getID());
				int size = this.peer.getChunksStoredSize().get(filename);
				this.peer.getChunksStoredSize().remove(filename);
				this.peer.setDiskUsed(this.peer.getDiskUsed() - size);
			} catch (IOException e) {
				System.out.println("Error deleting chunk file.");
			}
    	}
	}

	private File[] searchChunks(String fileID) {
		File chunksDirectory = new File(Peer.PEERS_FOLDER + "/" + Peer.DISK_FOLDER + this.peer.getID() + "/" + Peer.CHUNKS_FOLDER);

    	File[] matches = chunksDirectory.listFiles(new FilenameFilter()
    	{
    	  public boolean accept(File chunksDirectory, String name)
    	  {
    	     return name.endsWith(fileID);
    	  }
    	});
    	
    	return matches;
	}

}
