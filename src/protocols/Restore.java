package protocols;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import main.EventHandler;
import main.FileIdentifier;
import main.Peer;

public class Restore implements Callable<Boolean> {

	private String fileID;
	private String filePath;
	private Peer peer;
	private final int CHUNK_MAX_SIZE = 64000;
	private int actualChunk;
	private HashMap<Integer, byte[]> fileChunks;

	public Restore(String filename, Peer peer) {
		this.fileChunks = new HashMap<Integer, byte[]>();
		this.filePath = Peer.PEERS_FOLDER + "/" + Peer.SHARED_FOLDER + "/" + filename;
		this.fileID = new FileIdentifier(this.filePath).toString();
	}

	@Override
	public Boolean call() {
		ScheduledExecutorService scheduledPool = Executors.newScheduledThreadPool(1);

		boolean restored = false;
		int timeTask = 1;
		this.actualChunk = 0;
		int attempts = 0;

		while (restored == false) {

			// After 3 attempts the restore protocol stops
			if (attempts == 3) {
				System.out.println("File restore finished without success.");
				return false;
			}

			// It only sends the getchunk message one time
			if (attempts == 0) {
				byte[] packet = makeGetChunkMessage(this.fileID, this.actualChunk);

				try {
					this.peer.sendReplyToMulticast(Peer.multicastChannel.MC, packet);
					this.peer.getWaitRestoredChunks().put(this.actualChunk + "_" + this.fileID, true);
				} catch (IOException e) {
					System.out.println("Error sending getchunk message");
				}
			}

			Future<Boolean> future = scheduledPool.schedule(isRestored, timeTask, TimeUnit.SECONDS);

			boolean result = false;

			try {
				result = future.get();
			} catch (InterruptedException | ExecutionException e) {
			}

			// If the chunk restored has not yet arrived, the time interval doubles to check
			// again
			if (!result) {
				timeTask = timeTask * 2;
				attempts++;
			} else {
				// Check if it was the last chunk
				byte[] chunk = this.peer.getRestoredChunks().get(this.actualChunk + "_" + this.fileID);
				this.fileChunks.put(this.actualChunk, chunk);

				if (chunk.length < CHUNK_MAX_SIZE) {
					restored = true;
				} else {
					this.actualChunk++;
					attempts = 0;
				}
			}
		}

		if (restored) {
			restoreFile();
		}

		return true;
	}

	private void restoreFile() {
		File file = new File(this.filePath);

		try {
			FileOutputStream outputStream = new FileOutputStream(file);
			
			this.fileChunks.forEach( (key, value) -> {
				try {
					outputStream.write(value);
				} catch (IOException e) {
					System.out.println("Error writing the chunk");
				}
			});

			outputStream.close();
		} catch (IOException e) {
			System.out.println("Error saving the restored chunks");
		}
	}

	Callable<Boolean> isRestored = () -> {
		String hashmapKey = this.actualChunk + "_" + this.fileID;
		boolean restoredDone = false;

		if (this.peer.getRestoredChunks().get(hashmapKey) != null) {
			restoredDone = true;
		}

		return restoredDone;
	};

	private byte[] makeGetChunkMessage(String fileID, int chunkNr) {
		String message = "GETCHUNK " + this.peer.getProtocolVersion() + " " + this.peer.getID() + " " + fileID + " "
				+ chunkNr + " ";
		message = message + EventHandler.CRLF + EventHandler.CRLF;

		return message.getBytes();
	}

}
