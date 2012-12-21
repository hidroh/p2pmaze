package p2pmaze.remote_interface;

import java.util.Hashtable;

import maze.remote_interface.Maze;
import maze.remote_interface.Player;

public interface GameState {
	Maze getMaze();
	Hashtable<PeerClientListener, Player> getActivePlayers();
	void addPlayer(PeerClientListener client);
	boolean getGameStarted();
	void setGameStarted(boolean gameStarted);
}
