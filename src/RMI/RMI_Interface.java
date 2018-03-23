package RMI;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;


public interface RMI_Interface extends Remote
{
    
	public static void main(String[] args)
	{
		try
		{
			LocateRegistry.createRegistry(1099);
		}
		catch (RemoteException e)
		{
			e.printStackTrace();
		}
		
		while(true)
		{
			
		}
	}

} 