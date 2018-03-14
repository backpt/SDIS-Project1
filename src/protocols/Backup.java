package protocols;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import main.*;

public class Backup {

	private String filePath;
	private String fileID;
	private int replicationDegree;

	private final int CHUNK_MAX_SIZE = 64000;

	public Backup(String file, int replication) throws FileNotFoundException, IOException {
		this.filePath = file;
		this.replicationDegree = replication;

		createIdentifier();
		splitFile();
	}

	private void createIdentifier() {
		// TODO Auto-generated method stub

	}

	private void splitFile() throws FileNotFoundException, IOException {
		byte[] buffer = new byte[CHUNK_MAX_SIZE];
		int chunkNr = 1;

		File file = new File(this.filePath);

		try (FileInputStream fis = new FileInputStream(file); 
				BufferedInputStream bis = new BufferedInputStream(fis)) {

			int size = 0;
			while ((size = bis.read(buffer)) > 0) {
				new Thread(new FileChunk(this.fileID, chunkNr, buffer, size, this.replicationDegree)).start();
				chunkNr++;
			}
		}

	}

}
