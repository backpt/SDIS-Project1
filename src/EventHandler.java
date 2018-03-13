import java.net.DatagramPacket;

import protocols.*;


public class EventHandler implements Runnable {
	private String service;
	private DatagramPacket packet;
	private Peer peer;

	public EventHandler(DatagramPacket packet, Peer peer) {
		String message = new String(packet.getData());
		setService(message);
		this.peer = peer;
	}
	
	public void setService(String message) {
		String [] messageSplitted = message.split("");
		
		this.service = messageSplitted[0];
	}

	@Override
	public void run() {
		switch(this.service) {
		case "BACKUP":

			break;
			
		case "PUTCHUNK":
			break;
			
		case "STORED":
			break;
			
		case "GETCHUNK":
			break;
			
		case "CHUNK":
			break;
			
		case "DELETE":
			break;
			
		case "REMOVED":
			break;
		}
	}
}
