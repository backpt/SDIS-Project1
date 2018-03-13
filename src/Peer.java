import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class Peer {
	
	//Peer configurations
	private double protocolVersion;
	private int serverID;
	private InetAddress address;
	private int port;
	
	//Multicast configurations
	private InetAddress addressMC;
	private InetAddress addressMDB;
	private InetAddress addressMDR;
	private int portMC;
	private int portMDB;
	private int portMDR;
	
	public enum multicastChannel {MC, MDB, MDR};
	
	public Peer(double protocol, int id, String ap, InetAddress addressMC, int portMC, InetAddress addressMDB, 
			int portMDB, InetAddress addressMDR, int portMDR) throws IOException {
		this.protocolVersion = protocol;
		this.serverID = id;
		this.addressMC = addressMC;
		this.addressMDB = addressMDB;
		this.addressMDR = addressMDR;
		this.portMC = portMC;
		this.portMDB = portMDB;
		this.portMDR = portMDR;
		
		new Thread(new MulticastListenner(addressMC, portMC, this)).start();
		new Thread(new MulticastListenner(addressMDB, portMDB, this)).start();
		new Thread(new MulticastListenner(addressMDR, portMDR, this)).start();
	}
	
	private void sendReplyToMulticast(multicastChannel type, byte [] packet) throws IOException {
		switch(type) {
		case MC:
			MulticastSocket socket = new MulticastSocket(portMC);
			
			DatagramPacket sendPacket = new DatagramPacket(packet, packet.length, addressMC, portMC);
			socket.send(sendPacket);
			
			socket.close();
			break;
			
		case MDB:
			break;
			
		case MDR:
			break;
			
		}
	}

}
