package protocols;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
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
		this.filePath = Peer.PEERS_FOLDER + "/" + Peer.DISK_FOLDER + peer.getID() + "/" + Peer.FILES_FOLDER + "/"
				+ file;
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
			}
			;
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

		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		System.out.println("Vou começar o backup" + dateFormat.format(date));

		List<Future<Boolean>> threadResults = new ArrayList<Future<Boolean>>();
		ScheduledExecutorService scheduledPool = Executors.newScheduledThreadPool(100);

		try (FileInputStream fis = new FileInputStream(file); BufferedInputStream bis = new BufferedInputStream(fis)) {
			int size = 0;
			
			while ((size = bis.read(buffer)) > 0) {
				Future<Boolean> result = scheduledPool.submit(new FileChunk(this.fileID, chunkNr, buffer, this.replicationDegree, this.peer));
				threadResults.add(result);

				this.peer.getDesiredReplicationDegrees().put(chunkNr + "_" + this.fileID, this.replicationDegree);
				chunkNr++;
			}
		}

		this.peer.getFilesIdentifiers().put(this.fileName, this.fileID);
		this.peer.getNumberOfChunksPerFile().put(this.fileID, chunkNr);
		this.peer.getFilesBackepUp().put(this.fileID, false);
		
		//Await for chunk threads to finish
		boolean backupDone = false;
		for (Future<Boolean> result : threadResults) {
			try {
				if(!result.get()) {
					backupDone = false;
					scheduledPool.shutdownNow();
					break;
				} else {
					backupDone = true;
				}
			} catch (InterruptedException | ExecutionException e) {
				System.out.println("Chunk thread timed out.");
			}			
		}
		
		if(backupDone) {
			Date date2 = new Date();
			System.out.println("Backup completed. " + dateFormat.format(date2));
		} else {
			Date date2 = new Date();
			System.out.println("Backup was not completed. " + dateFormat.format(date2));
		}
	}

}
