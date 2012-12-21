package maze.server;

import java.awt.Point;
import java.io.Serializable;
import java.rmi.RemoteException;

import maze.remote_interface.*;

public class StartResponseImpl implements StartResponse, Serializable {

	private static final long serialVersionUID = 5512423850606353659L;
	int id;
	Point location;
	Maze maze;

	public StartResponseImpl(int assignedID, Point assignedLocation, Maze mazeInfo) {
		id = assignedID;
		location = assignedLocation;
		maze = mazeInfo;
	}

	@Override
	public int getID() throws RemoteException {
		return id;
	}

	@Override
	public Point getInitialLocation() throws RemoteException {
		return location;
	}

	@Override
	public Maze getMaze() throws RemoteException {
		return maze;
	}

	public String toString() {
		return "Join response: " +
				"ID: " + id + "; " +
				"location: (" + 
				(int) location.getX() + ", " + (int) location.getY() + ")";
	}
}
