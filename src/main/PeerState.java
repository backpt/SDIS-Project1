package main;

public class PeerState {
	
	private Peer peer;
	private String state;
	
	public PeerState(Peer peer) {
		this.peer = peer;
		this.state = "";
		
		getInfoStateBackup();
		getInfoStateChunks();
		getInfoStateDisk();
	}
	
	private void getInfoStateBackup() {
		this.state += "\n--- Files whose backup has initiated ---";
		
		if(this.peer.getFilesIdentifiers().size() == 0) {
			this.state += "\nNo backup has been started.\n";
		} else {
			this.peer.getFilesIdentifiers().forEach( (filename, fileid) -> {
				this.state += "\n- File Path: "+ Peer.PEERS_FOLDER + "/" + Peer.SHARED_FOLDER + "/" + filename;
				this.state += "\n- File ID: "+ fileid + " - Desired Replication Degree: " + this.peer.getDesiredReplicationDegrees().get("0_"+fileid);
				
				if(this.peer.getBackupState().get(fileid) == false) {
					this.state += "\nThis file started a backup, but this backup was later deleted.";
				} else {
					this.state += "\nChunks of the file:";
					
					this.peer.getActualReplicationDegrees().forEach( (key, perseived) -> {
						if(key.endsWith(fileid)) {
							String [] info = key.split("_");
							this.state += "\nChunkNr: "+info[0] + " - Perceived Replication Degree: "+perseived;
						}
					});
				}
				
				this.state += "\n";
			});
		}
	}
	
	private void getInfoStateChunks() {
		this.state += "\n--- Stored Chunks ---";
		
		if(this.peer.getChunksStoredSize().size() == 0) {
			this.state += "\nNo chunks stored.";
		} else {
			this.peer.getChunksStoredSize().forEach( (key, size) -> {
				this.peer.getActualReplicationDegrees().forEach( (key2, perseived) -> {
					if(key2.equals(key)) {
						this.state += "\n- Chunk ID: "+ key + "\n- Size: "+ size/1000 + " KBytes" + " - Perceived Replication Degree: "+perseived;
					}
				});
			});
		}
		
		this.state += "\n";
	}
	
	private void getInfoStateDisk() {
		this.state += "\n--- Disk Info ---";
		this.state += "\n- Disk Space: "+this.peer.getDiskSpace() / 1000 + " KBytes";
		this.state += "\n- Disk Used: "+this.peer.getDiskUsed() / 1000 + " KBytes";
		
		this.state += "\n";
	}
	
	public String getState() {
		return this.state;
	}
}
