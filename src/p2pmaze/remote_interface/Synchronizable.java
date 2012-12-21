package p2pmaze.remote_interface;

import java.rmi.RemoteException;

public interface Synchronizable {
	void responseJoin(PeerClientListener client, boolean joinResult) throws RemoteException;
	void startGame() throws RemoteException;
	void setGameState(GameState state) throws RemoteException;
	
	/**
	 * Sync game state with backup server
	 * @throws RemoteException if unable to communicate with backup server
	 */
	void sync() throws RemoteException;
}
