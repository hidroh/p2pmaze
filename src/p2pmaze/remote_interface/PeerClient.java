package p2pmaze.remote_interface;

import java.rmi.Remote;
import java.rmi.RemoteException;

import maze.remote_interface.Direction;

public interface PeerClient extends Remote {
	/**
	 * Sends request to join a game
	 * @throws RemoteException if unable to connect to both servers
	 */
	void requestJoin() throws RemoteException;
	void requestExit() throws RemoteException;
	void requestMove(Direction d) throws RemoteException;
}
