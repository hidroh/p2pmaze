/**
 * Maze coordinate system:
 * | y (North)		(N, N)
 * |
 * |(1, 1)		
 * |_________________ x (West)
 */
package maze.server;

import java.awt.Point;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Vector;
import java.util.Random;

import maze.remote_interface.*;

public class MazeImpl implements Maze, Serializable {
	private static final long serialVersionUID = -6546792729685701562L;
	int n;
	int m;
	Vector<Point> treasures;
	Vector<Player> players;

	public MazeImpl(int N, int M) {
		n = N;
		m = M;
		players = new Vector<Player>();
		treasures = new Vector<Point>();
		// TODO randomize treasure location
		int count = 0;
		while (count < m) {
			Random rand = new Random();
			int x = rand.nextInt(n);
			int y = rand.nextInt(n);
			Point location = new Point(x,y);
			treasures.add(location);
			count++;
		}
	}

	@Override
	public int getN() throws RemoteException {
		return n;
	}

	@Override
	public int getM() throws RemoteException {
		return m;
	}

	@Override
	public Vector<Point> getTreasureLocations() throws RemoteException {
		return treasures;
	}

	@Override
	public Vector<Player> getPlayers() throws RemoteException {
		return players;
	}

	public Player getPlayerById(int id) throws RemoteException {
		for (Player player : players) {
			if (player.getID() == id) {
				return player;
			}
		}
		return null;
	}
	
	public void setPlayerLocationById(int id, Direction d) throws RemoteException {
		for (Player player : players) {
			PlayerImpl playerImpl = (PlayerImpl) player;
			if (playerImpl.getID() == id) {
				playerImpl.setLocation(d);
			}
		}
	}
	
	public String toString() {
		return "Maze info: " +
				"Size [" + n + " x " + n + "]; \n" + 
				"Treasures: " + treasures.size() + "; \n" + 
				"Treasures location: " + getTreasuresLocation() + "\n" +
				"Players: " + players.size() + "\n" +
				"Players location: \n" + getPlayersInfo() + "\n";
	}
	
	public int collectTreasure(Point location) throws RemoteException {
		int i = 0;
		int collected = 0;
		while (i < treasures.size()) {
			if (treasures.elementAt(i).equals(location)) {
				treasures.removeElementAt(i);
				collected = collected + 1;
			} else {
				i = i + 1;
			}
		}
		return collected;
	}
	
	public void addTreasuresForPlayer(int id, int treasuresCount) throws RemoteException{
		for (Player player : players) {
			PlayerImpl playerImpl = (PlayerImpl) player;
			if (playerImpl.getID() == id) {
				playerImpl.addTreasure(treasuresCount);
			}
		}
	}
	
	private String getTreasuresLocation() {
		int size = treasures.size();
		String location = new String();
		for (int i = 0; i < size; i++) {
			int x = treasures.elementAt(i).x;
			int y = treasures.elementAt(i).y;
			location += "(" + x + "," + y + ")";
		}
		return location;
	}
	
	private String getPlayersInfo() {
		int size = players.size();
		String location = new String();
		for (int i = 0; i < size; i++) {
			location += players.elementAt(i).toString();
		}
		return location;
	}
}