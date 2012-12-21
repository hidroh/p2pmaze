package maze.server;

import java.awt.Point;
import java.io.Serializable;
import java.rmi.RemoteException;

import maze.remote_interface.Maze;
import maze.remote_interface.EndResponse;

public class EndResponseImpl implements EndResponse, Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3280415750215559515L;
	int treasures;
	Point location;
	MazeImpl maze;

	public EndResponseImpl(int treasures, Point location, MazeImpl maze) {
		this.treasures = treasures;
		this.location = location;
		this.maze = maze;
	}

	@Override
	public Point getLocation() throws RemoteException {
		return location;
	}

	@Override
	public Maze getMaze() throws RemoteException {
		return maze;
	}

	@Override
	public int getTreasureCount() throws RemoteException {
		return treasures;
	}

	public String toString() {
		return "Final result: " + "location: (" + (int) location.getX() + ", "
				+ (int) location.getY() + "); " + "treasures collected: " + treasures;
	}
}
