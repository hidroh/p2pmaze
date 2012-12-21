package p2pmaze.remote_interface;

import java.rmi.Remote;
import java.rmi.RemoteException;

import maze.remote_interface.EndResponse;
import maze.remote_interface.MoveResponse;
import maze.remote_interface.StartResponse;

public interface PeerClientListener extends Remote {
	void assignedAsBackupServer(GameState state) throws RemoteException;
	void serverChanged(String host, boolean isPrimary) throws RemoteException;
	void gameStarted(StartResponse response) throws RemoteException;
	void gameEnded(EndResponse response) throws RemoteException;
	void moved(MoveResponse response) throws RemoteException;
	void joined(boolean isSuccessful) throws RemoteException;
	String getHost() throws RemoteException;
}
