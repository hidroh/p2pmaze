package maze.server;

import java.awt.Point;
import java.io.Serializable;
import java.rmi.RemoteException;

import maze.remote_interface.*;

public class PlayerImpl implements Player, Serializable {

	private static final long serialVersionUID = -48744781048580100L;

	int id;
	Point location;
	int treasureCount;

	public PlayerImpl(int playerID, Point initialLocation) {
		id = playerID;
		location = initialLocation;
		treasureCount = 0;
	}

	@Override
	public int getID() throws RemoteException {
		return id;
	}

	@Override
	public Point getLocation() throws RemoteException {
		return location;
	}

	public void setLocation(Direction d) {
		switch (d) {
		case South:
			location.setLocation(location.getX(), location.getY() - 1);
			break;
		case North:
			location.setLocation(location.getX(), location.getY() + 1);
			break;
		case West:
			location.setLocation(location.getX() - 1, location.getY());
			break;
		case East:
			location.setLocation(location.getX() + 1, location.getY());
			break;
		default:
			break;
		}
	}

	@Override
	public int getTreasureCount() throws RemoteException {
		return treasureCount;
	}

	public void addTreasure(int count) {
		treasureCount = treasureCount + count;
	}

	public String toString() {
		return "Player info: " +
				"ID: " + id + ";" +
				"Location: (" + 
				(int) location.getX() + ", " + (int) location.getY() + "); " +
				"Treasures: " + treasureCount + "\n";
	}
}
