package protocols;

public class Backup {
	
	private String filePath;
	private int replicationDegree;
	
	public Backup(String file, int replication) {
		this.filePath = file;
		this.replicationDegree = replication;
		
		createIdentifier();
		splitFile();
	}

	private void createIdentifier() {
		// TODO Auto-generated method stub
		
	}

	private void splitFile() {
		
		
	}

}
