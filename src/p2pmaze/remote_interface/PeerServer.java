package p2pmaze.remote_interface;

import java.rmi.Remote;
import java.rmi.RemoteException;

import maze.remote_interface.Direction;

public interface PeerServer extends Remote, Synchronizable {
	void joinGame(PeerClientListener client) throws RemoteException;
	boolean exitGame(PeerClientListener client) throws RemoteException;
	boolean move(PeerClientListener client, Direction d) throws RemoteException;
}
