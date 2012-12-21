package p2pmaze;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import maze.helper.Config;
import maze.helper.Logger;
import maze.remote_interface.Direction;
import maze.remote_interface.EndResponse;
import maze.remote_interface.MoveResponse;
import maze.remote_interface.Player;
import maze.remote_interface.StartResponse;
import maze.server.StartResponseImpl;
import p2pmaze.helper.P2PConfig;
import p2pmaze.remote_interface.GameState;
import p2pmaze.remote_interface.PeerClient;
import p2pmaze.remote_interface.PeerClientListener;
import p2pmaze.remote_interface.PeerServer;

public class GamePlayer extends UnicastRemoteObject implements PeerClient, PeerClientListener, PeerServer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7852961025310444190L;
	String primaryServerHost;
	String backupServerHost;
	PeerServer primaryServerStub;
	PeerServer backupServerStub;
	PeerClientListener lastRequested;
	
	boolean gameInitiated;
	ScheduledFuture<?> endGameTimer;
	GameState gameState;

	protected GamePlayer() throws RemoteException {
		super();
		// TODO Auto-generated constructor stub
	}
	
	public static void main(String[] args) {
		try {
			while (true) { // repeating until process is terminated
				GamePlayer player = new GamePlayer();
				boolean connected = player.connectToServer();
				if (!connected) {
					player.setAsServer(true);
					player.createGame(4, 5);
//					if (args.length == 2) {
//						int size = Integer.parseInt(args[0]);
//						int treasures = Integer.parseInt(args[1]);
//						player.createGame(size, treasures);
//					} else {
//						Logger.logError("Unable to start: Insufficient arguments.");
//					}
				}
				
				player.playGame();
			}
		} catch (RemoteException e) {
			Logger.logError("Unable to communicate with registry");
		}

	}
	
	private void playGame() {
		// TODO Full implementation
		System.out.println("=== Play game ===");
		Scanner in = new Scanner(System.in);
		int i = in.nextInt();
		try {
			if (i == 1) {
				requestJoin();
			}
		} catch (RemoteException e) {
			Logger.logError("Unable to join. No servers available.");
		}
	}

	/**
	 * Sets (local) self as primary/backup server
	 * @param isPrimary indicates if server role is primary
	 */
	private void setAsServer(boolean isPrimary) {
		Logger.logMessage("Set self as " + (isPrimary ? "primary" : "backup"));
		
		String registryName = null;
		if (isPrimary) {
			registryName = P2PConfig.PRIMARY_SERVER_NAME;
		} else {
			registryName = P2PConfig.BACKUP_SERVER_NAME;
		}
		Registry registry = null;
		try {
			registry = LocateRegistry.getRegistry();
			registry.bind(registryName, this);
			setServerStub(this, isPrimary);
		} catch (Exception e) {
			e.printStackTrace();
//			try {
//				registry.unbind(registryName);
//				registry.bind(registryName, this);
//			} catch (Exception e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			}
		}

	}
	
	/**
	 * Tries to connect to primary then backup server
	 * @return true if able to connect to primary or backup server, false otherwise
	 * @throws RemoteException if unable to communicate with registry
	 */
	private boolean connectToServer() throws RemoteException {
		boolean primaryConnected = connectToServer(true);
		if (!primaryConnected) {
			boolean backupConnected = connectToServer(false);
			if (!backupConnected) {
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Connects and sets primary/backup server stub
	 * @param isPrimary indicates if connection is for primary server
	 * @return true if successful, false otherwise
	 * @throws RemoteException if unable to communicate with registry
	 */
	private boolean connectToServer(boolean isPrimary) throws RemoteException {
		String host = isPrimary ? primaryServerHost : backupServerHost;
		String registryName = isPrimary? P2PConfig.PRIMARY_SERVER_NAME : P2PConfig.BACKUP_SERVER_NAME;
		try {
			Registry registry = LocateRegistry.getRegistry(host);
			PeerServer stub = (PeerServer) registry.lookup(registryName);
			if (stub != null) {
				setServerStub(stub, isPrimary);
				return true;
			} else {
				return false;
			}
		} catch (NotBoundException e) {
			return false;
		} 
	}
	
	private void createGame(int size, int treasures) {
		gameState = new GameStateImpl(size, treasures);
	}
	
	private void setServerStub(PeerServer newServer, boolean isPrimary) {
		if (isPrimary) {
			primaryServerStub = newServer;
		} else {
			backupServerStub = newServer;
		}
	}
	
	@Override
	public synchronized void joinGame(PeerClientListener client) throws RemoteException {
		Logger.logMessage("Server: join requested");
		
		lastRequested = client;
		validateRequest();
		
		boolean joinResult = false;
		if (gameState.getGameStarted()) {
			joinResult = false;
		} else {
			gameState.addPlayer(client);
			sync();

			if (!gameInitiated) {
				gameInitiated = true;
				Runnable task = new Runnable() {
					public void run() {
						syncStartGame();
					}
				};
				ScheduledExecutorService worker = Executors
						.newSingleThreadScheduledExecutor();
				worker.schedule(task, Config.JOIN_GAME_DELAY, TimeUnit.SECONDS);
			}

			joinResult = true;
		}
		
		syncResponseJoin(client, joinResult);
	}
	
	/**
	 * Synchronizes join response from primary and backup servers
	 * @param client client to response
	 * @param joinResult response message
	 */
	private void syncResponseJoin(PeerClientListener client, boolean joinResult) {
		boolean backupCompleted = false;
		while (!backupCompleted) {
			try {
				backupServerStub.responseJoin(client, joinResult);
				backupCompleted = true;
			} catch (RemoteException e) {
				boolean assigned = assignBackupServer();
				if (!assigned) {
					break;			
				}
			}
		}

		try {
			responseJoin(client, joinResult);
		} catch (RemoteException e) {
			// do nothing
		}
	}

	/**
	 * Synchronizes start game response from primary and backup servers
	 */
	private void syncStartGame() {
		boolean backupCompleted = false;
		while (!backupCompleted) {
			try {
				backupServerStub.startGame();
				backupCompleted = true;
			} catch (RemoteException e) {
				boolean assigned = assignBackupServer();
				if (!assigned) {
					break;			
				}
			}
		}

		try {
			startGame();
		} catch (RemoteException e) {
			// Do nothing
		}
	}

	/**
	 * Checks if request is being sent to backup server.
	 * If that is the case then swap servers and assign new backup server.
	 */
	private void validateRequest() {
		if (isBackup()) {
			setAsServer(true);
			String myHost = null;
			try {
				myHost = getHost();
			} catch (RemoteException e) {
				// do nothing
			}
			broadcastServerChanged(myHost, true);
			assignBackupServer();
		}
	}

//	private boolean isPrimary() {
//		return primaryServerStub == this;
//	}
	
	private boolean isBackup() {
		return backupServerStub == this;
	}

	@Override
	public boolean exitGame(PeerClientListener client) throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean move(PeerClientListener client, Direction d) throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void sync() throws RemoteException {
		Logger.logMessage("Server: sync");
		
		try {
			backupServerStub.setGameState(gameState);
		} catch (RemoteException e) {
			assignBackupServer();
		}
	}

	/**
	 * Assigns a player as backup server, last requested player has priority
	 * @return true if assigned, false otherwise
	 */
	private boolean assignBackupServer() {
		PeerClientListener newServer = lastRequested;
		Enumeration<PeerClientListener> playerEnumerator = gameState.getActivePlayers().keys(); 
		boolean assigned = false;
		String assignedHost = null;
		while (!assigned) {
			try {
				newServer.assignedAsBackupServer(gameState);
				assignedHost = newServer.getHost();
				backupServerHost = assignedHost;
				connectToServer(false);
				assigned = true;
			} catch (RemoteException e1) {
				
				if (playerEnumerator.hasMoreElements()) {
					newServer = playerEnumerator.nextElement();
				} else {
					newServer = null;
				}
			}			
		}
		
		if (assignedHost != null) {
			broadcastServerChanged(assignedHost, false);
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * Broadcasts primary/backup server change to all players
	 * @param host new server host
	 * @param isPrimary indicates if new server update is primary
	 */
	private void broadcastServerChanged(String host, boolean isPrimary) {
		Hashtable<PeerClientListener, Player> activePlayers = gameState.getActivePlayers();
		Enumeration<PeerClientListener> playerEnumerator = activePlayers.keys(); 
		while (playerEnumerator.hasMoreElements()) {
			PeerClientListener listener = playerEnumerator.nextElement();
			try {
				listener.serverChanged(host, isPrimary);
			} catch (RemoteException e) {
				activePlayers.remove(listener);
			}
		}
	}

	@Override
	public void serverChanged(String host, boolean isPrimary) throws RemoteException {
		Logger.logMessage("Client: " + (isPrimary ? "primary" : "backup") + " server changed.");
		
		if (isPrimary) {
			primaryServerHost = host;
			connectToServer(isPrimary);
		} else {
			backupServerHost = host;
		}
	}

	@Override
	public void gameStarted(StartResponse response) throws RemoteException {
		// TODO Auto-generated method stub
		
		Logger.logMessage("Client: Game started.");
	}

	@Override
	public void gameEnded(EndResponse response) throws RemoteException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void moved(MoveResponse response) throws RemoteException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void requestJoin() throws RemoteException {
		Logger.logMessage("Client: request to join");
		
		try {
			primaryServerStub.joinGame(this);
		} catch (RemoteException e) {
			backupServerStub.joinGame(this);
		}	
	}

	@Override
	public void requestExit() throws RemoteException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void requestMove(Direction d) throws RemoteException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void startGame() throws RemoteException {
		Logger.logMessage("Server: start game.");
		
		Hashtable<PeerClientListener, Player> activePlayers = gameState.getActivePlayers();
		
		if (activePlayers.size() > 0) {
			gameState.setGameStarted(true);
			for (Enumeration<PeerClientListener> e = activePlayers.keys(); e
					.hasMoreElements();) {
				PeerClientListener listener = e.nextElement();
				Player p = activePlayers.get(listener);
				try {
					StartResponse response = new StartResponseImpl(p.getID(),
							p.getLocation(), gameState.getMaze());
					listener.gameStarted(response);
				} catch (RemoteException re) {
					activePlayers.remove(listener);
				}
			}

			startGameTimeout();
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
		// TODO Auto-generated method stub
		
	}

	@Override
	public void joined(boolean isSuccessful) throws RemoteException {
		// TODO Auto-generated method stub
		
		Logger.logMessage("Client: Joined " + (isSuccessful ? "successfully." : "failed."));
	}

	@Override
	public void responseJoin(PeerClientListener client, boolean joinResult) throws RemoteException {
		Logger.logMessage("Server: response to join");
		
		try {
			client.joined(joinResult);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void setGameState(GameState state) throws RemoteException {
		Logger.logMessage("Game state updated to latest");
		gameState = state;
	}

	@Override
	public void assignedAsBackupServer(GameState state) throws RemoteException {
		Logger.logMessage("Client: Assigned as backup server");
		setGameState(state);
		setAsServer(false);
	}

	@Override
	public String getHost() throws RemoteException {
		String hostName = null;
		try {
			InetAddress addr = InetAddress.getLocalHost();
			hostName = addr.getHostAddress();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return hostName;
	}
}
