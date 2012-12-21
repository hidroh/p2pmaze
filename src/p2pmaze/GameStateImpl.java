package p2pmaze;

import java.awt.Point;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Hashtable;
import java.util.Random;

import maze.remote_interface.Maze;
import maze.remote_interface.Player;
import maze.server.MazeImpl;
import maze.server.PlayerImpl;

import p2pmaze.remote_interface.GameState;
import p2pmaze.remote_interface.PeerClientListener;

public class GameStateImpl implements GameState, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1999271184816793357L;
	private MazeImpl maze;
	private int n;
	private int m;
	private Hashtable<PeerClientListener, Player> activePlayers;
	private boolean gameStarted = false;

	public GameStateImpl(int size, int treasures) {
		n = size;
		m = treasures;
		maze = new MazeImpl(n, m);
		activePlayers = new Hashtable<PeerClientListener, Player>();
	}
	
	@Override
	public Maze getMaze() {
		return maze;
	}

	@Override
	public Hashtable<PeerClientListener, Player> getActivePlayers() {
		return activePlayers;
	}

	@Override
	public void addPlayer(PeerClientListener client) {
		// TODO randomize player location
		Point location = new Point();
		location = this.addPlayerLocation();
		PlayerImpl newPlayer = new PlayerImpl(activePlayers.size() + 1, location);
				//new Point(1, 1));
		try {
			maze.getPlayers().add(newPlayer);
		} catch (RemoteException e) {
			// TODO remove RemoteException
		}
		activePlayers.put(client, newPlayer);

	}

	@Override
	public boolean getGameStarted() {
		return gameStarted;
	}
	
	@Override
	public void setGameStarted(boolean gameStarted) {
		this.gameStarted = gameStarted;
	}

	private Point addPlayerLocation() {
		Random rand = new Random();
		int x = rand.nextInt(n);
		int y = rand.nextInt(n);
		Point location = new Point(x,y);
		return location;
	}
}
