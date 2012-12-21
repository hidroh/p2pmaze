package maze.server;

import java.awt.Point;
import java.io.Serializable;
import java.rmi.RemoteException;

import maze.remote_interface.*;

public class MoveResponseImpl implements MoveResponse, Serializable {

	private static final long serialVersionUID = 5422149233945696585L;
	int treasures;
	Point location;
	MazeImpl maze;
	boolean moveValid;
	boolean isLastMove;
	
	public MoveResponseImpl(boolean moveValid, boolean isLastMove, int treasures, Point location, 
			MazeImpl maze) {
		this.moveValid = moveValid;
		this.isLastMove = isLastMove;
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
	public int getTreasureCollected() throws RemoteException {
		return treasures;
	}

	public String toString() {
		if (moveValid) {
			return "Move accepted: " +
					"new location: (" + 
					(int) location.getX() + ", " + (int) location.getY() + "); " + 
					"treasures collected: " + treasures;
		} else {
			return "Move rejected.";
		}
	}

	@Override
	public boolean getIsLastMove() throws RemoteException {
		return isLastMove;
	}
}
