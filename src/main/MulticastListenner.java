package main;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class MulticastListenner implements Runnable {
	private InetAddress address;
	private int port;
	private Peer peer;
	private MulticastSocket socket;
	
	public MulticastListenner(InetAddress address, int port, Peer peer) throws IOException {
		this.address = address;
		this.port = port;
		this.peer = peer;
		
		socket = new MulticastSocket(port);
		socket.joinGroup(address);
	}

	@Override
	public void run() {		
		System.out.println("Vou ficar � escuta no multicast");
		while(true) {
			byte[] requestPacket = new byte[256];
			DatagramPacket packet = new DatagramPacket(requestPacket, requestPacket.length);

			try {
				socket.receive(packet);
			} catch (IOException e) {
				e.printStackTrace();
			}
			String message = new String(packet.getData());

			new Thread(new EventHandler(packet, this.peer)).start();
		}
	}

}
