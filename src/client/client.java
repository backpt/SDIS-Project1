package client;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import protocols.Backup;
import RMI.RMI_Interface;

public class client {
    public static void main(String args[]) {
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
        try {
            String backupname = "Backup";
            Registry registry = LocateRegistry.getRegistry(args[0]);
            Backup backup = (Backup) registry.lookup(backupname);
        } catch (Exception e) {
            System.err.println("Backup exception:");
            e.printStackTrace();
        }
    }    
}