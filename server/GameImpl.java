package maze.server;

import java.awt.Point;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.Random;

import maze.helper.Config;
import maze.helper.Logger;
import maze.remote_interface.*;

public class GameImpl implements Game {

	MazeImpl maze;
	int n;
	int m;
	Hashtable<GameListener, Player> activePlayers;
	boolean gameStarted;
	boolean gameInitiated;
	ScheduledFuture<?> endGameTimer;

	private GameImpl(int size, int treasures) {
		n = size;
		m = treasures;
		activePlayers = new Hashtable<GameListener, Player>();
		maze = new MazeImpl(size, treasures);
	}

	public static void main(String[] args) {
		Game stub = null;
		Registry registry = null;

		if (args.length == 2) {
			try {
				int size = Integer.parseInt(args[0]);
				int treasures = Integer.parseInt(args[1]);
				GameImpl game = new GameImpl(size, treasures);
				stub = (Game) UnicastRemoteObject.exportObject(game, 0);
				registry = LocateRegistry.getRegistry();
				registry.bind("MazeGame", stub);
				Logger.logMessage("Server ready.\n" + game);
			} catch (Exception e) {
				try {
					// Logger.logError("Server exception: \n" + e.toString(),
					// e);
					Logger.logMessage("Retrying...");
					registry.unbind("MazeGame");
					registry.bind("MazeGame", stub);
					Logger.logMessage("Server ready.");
				} catch (Exception ee) {
					Logger.logError("Server exception: \n" + ee.toString(), ee);
				}
			}
		} else {
			Logger.logError("Unable to start: Insufficient arguments.");
		}
	}

	@Override
	public synchronized boolean joinGame(GameListener listener)
			throws RemoteException {
		if (gameStarted) {
			return false;
		} else {
			Player newPlayer = addPlayer();
			activePlayers.put(listener, newPlayer);
			Logger.logMessage("New player joined.\n" + newPlayer);
		}

		if (!gameInitiated) {
			gameInitiated = true;
			Runnable task = new Runnable() {
				public void run() {
					startGame();
				}
			};
			ScheduledExecutorService worker = Executors
					.newSingleThreadScheduledExecutor();
			worker.schedule(task, Config.JOIN_GAME_DELAY, TimeUnit.SECONDS);
		}

		return true;
	}

	@Override
	public synchronized boolean exitGame(GameListener listener)
			throws RemoteException {
		if (gameStarted && activePlayers.containsKey(listener)) {
			Player player = activePlayers.get(listener);
			maze.getPlayers().remove(player);
			activePlayers.remove(listener);
			Logger.logMessage("Player #" + player.getID() + " exited.\n" + this);
			if (activePlayers.size() == 0 || maze.getTreasureLocations().size() == 0) {
				endGame();
			}
			return true;
		} else {
			return false;
		}
	}

	@Override
	public synchronized MoveResponse move(int id, Direction d)
			throws RemoteException {
		String log = "Move requested from player #" + id + "\n";
		Player player = (Player) maze.getPlayerById(id);
		int treasureCollected = 0;
		boolean moveValid = validateMove(player.getLocation(), d);
		if (moveValid) {
			//player.setLocation(d);
			maze.setPlayerLocationById(id, d);
			treasureCollected = maze.collectTreasure(player.getLocation());
			//player.addTreasure(treasureCollected);
			maze.addTreasuresForPlayer(id,treasureCollected);
		}
		log += "Move " + (moveValid ? "accepted" : "rejected")
				+ " for player #" + id + "\n" + this;
		Logger.logMessage(log);
		boolean isLastMove = maze.getTreasureLocations().size() == 0;
		return new MoveResponseImpl(moveValid, isLastMove, treasureCollected,
				player.getLocation(), maze);
	}

	public String toString() {
		return "Game info:\n" + "- " + maze + "\n" + "- Active player: "
				+ activePlayers.size();
	}

	private void startGame() {
		
		String log = "Registered players: " + activePlayers.size()
				+ ". Starting game...\n";
		if (activePlayers.size() > 0) {
			gameStarted = true;
			for (Enumeration<GameListener> e = activePlayers.keys(); e
					.hasMoreElements();) {
				GameListener listener = e.nextElement();
				Player p = activePlayers.get(listener);
				try {
					StartResponse response = new StartResponseImpl(p.getID(),
							p.getLocation(), maze);
					listener.gameStarted(response);
					log += "Player #" + p.getID() + " is up.\n";
				} catch (RemoteException re) {
					activePlayers.remove(listener);
				}
			}

			startGameTimeout();

			log += "Game started.\n" + this;
			Logger.logMessage(log);
		} else {
			endGame();
		}
	}

	private void startGameTimeout() {
		Runnable task = new Runnable() {
			public void run() {
				Logger.logMessage("Game timeout.");
				endGame();
			}
		};
		ScheduledExecutorService worker = Executors
				.newSingleThreadScheduledExecutor();
		endGameTimer = worker.schedule(task, Config.GAME_TIMEOUT,
				TimeUnit.SECONDS);
	}

	private void endGame() {
		if (endGameTimer != null) {
			endGameTimer.cancel(false);
		}

		gameStarted = false;
		gameInitiated = false;
		for (Enumeration<GameListener> e = activePlayers.keys(); e
				.hasMoreElements();) {
			GameListener listener = e.nextElement();
			Player p = activePlayers.get(listener);
			try {
				EndResponse response = new EndResponseImpl(
						p.getTreasureCount(), p.getLocation(), maze);
				listener.gameEnded(response);
				activePlayers.remove(listener);
			} catch (RemoteException re) {
				activePlayers.remove(listener);
			}finally{
				maze = new MazeImpl(n,m);
			}
		}

		Logger.logMessage("Game ended.");
	}
	
	private Point addPlayerLocation() {
		Random rand = new Random();
		int x = rand.nextInt(n);
		int y = rand.nextInt(n);
		Point location = new Point(x,y);
		return location;
	}

	private Player addPlayer() throws RemoteException {
		// TODO randomize player location
		Point location = new Point();
		location = this.addPlayerLocation();
		PlayerImpl newPlayer = new PlayerImpl(activePlayers.size() + 1, location);
				//new Point(1, 1));
		maze.getPlayers().add(newPlayer);
		return newPlayer;
	}

	private boolean validateMove(Point location, Direction d) {
		switch (d) {
		case South:
			return location.getY()-1 >= 0; 	//go down
		case North:
			return location.getY()+1 < n;	//go up
		case West:
			return location.getX()-1 >= 0;	//go right
		case East:
			return location.getX()+1 < n;	//go left
		default:
			return true;
		}
	}

}
