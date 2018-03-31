package main;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.io.IOException;
import java.net.InetAddress;

public class RunPeer {
	
	public static void main(String[] args) throws IOException {
		//Initialization for now (future: all with arguments)
		int serverID = Integer.parseInt(args[0]);
		//int serverID = 1;
		
		InetAddress addressMC = InetAddress.getByName("224.0.0.2");
		InetAddress addressMDB = InetAddress.getByName("224.0.0.3");
		InetAddress addressMDR = InetAddress.getByName("224.0.0.4");
		int portMulticast = 4446;

		String rmiAddress = "rmi"+serverID; 
		
		Peer peer = new Peer("1.0", serverID, "", addressMC, portMulticast, addressMDB, portMulticast, addressMDR, portMulticast);
		
		// Start RMI
			       
		try
		{
		    IRMI rmi = (IRMI) UnicastRemoteObject.exportObject((Remote) peer, 0);
		    Registry registry = LocateRegistry.getRegistry();
		    registry.rebind(rmiAddress, rmi);
		}
		catch (RemoteException e)
		{
		    e.printStackTrace();
		}
	
	}


}