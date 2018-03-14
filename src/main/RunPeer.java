package main;
import java.io.IOException;
import java.net.InetAddress;

public class RunPeer {
	
	public static void main(String[] args) throws IOException {
		//Initialization for now (future: all with arguments)
		int serverID = Integer.parseInt(args[0]);
		
		InetAddress addressMC = InetAddress.getByName("224.0.0.1");
		InetAddress addressMDB = InetAddress.getByName("224.0.0.2");
		InetAddress addressMDR = InetAddress.getByName("224.0.0.3");
		int portMulticast = 4446;

		new Peer(1.0, serverID, "", addressMC, portMulticast, addressMDB, portMulticast, addressMDR, portMulticast);
	}

}