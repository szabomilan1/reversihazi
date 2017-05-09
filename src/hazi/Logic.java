package hazi;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

class Logic implements ICommand {

	private GUI gui;
	private Network net = null;
	private Server s;
	private ArrayList<Command> commandsSinceLastProc;
	private GameState gs;
	private int timeSec;
	private char turnLast;
	private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
	private ScheduledFuture<?> future = null;
	private boolean noMoreMoves;
	private GameState gspos;

	private static final int[] rowOffset = { -1, -1, -1, 0, 0, 1, 1, 1 };
	private static final int[] colOffset = { -1, 0, 1, -1, 1, -1, 0, 1 };

	Logic() {
		gs = new GameState();
		timeSec = 0;
		turnLast = 'w';
		commandsSinceLastProc = new ArrayList<Command>();
		noMoreMoves = false;
		s = null;
		gspos = new GameState();
	}

	void setGUI(GUI g) {
		gui = g;
	}

	void startServer() {
		if (net != null)
			net.stop();
		net = new Server(this, gui);
		net.start("localhost");
	}

	void startClient(String s) {
		if (net != null)
			net.stop();
		net = new Client(gui);
		net.start(s);
	}

	public boolean validMove(int field) {

		int col = field % 8;
		int row = field / 8;

		if (gs.table[row][col] != 'e')
			return false;

		char oppPiece = (gs.turn == 'b') ? 'w' : 'b';

		boolean valid = false;
		boolean endWhile = false;
		for (int i = 0; i < 8 && valid == false; ++i) {
			int currentRow = row + rowOffset[i];
			int currentCol = col + colOffset[i];
			endWhile = false;
			boolean oppPieceBetween = false;
			while (currentRow >= 0 && currentRow < 8 && currentCol >= 0 && currentCol < 8 && endWhile == false) {

				if (gs.table[currentRow][currentCol] == oppPiece)
					oppPieceBetween = true;
				else if ((gs.table[currentRow][currentCol] == gs.turn) && oppPieceBetween == true) {
					valid = true;
				} else
					endWhile = true;

				currentRow += rowOffset[i];
				currentCol += colOffset[i];
			}
		}

		return valid;
	}

	public void setPiece(int field) {
		int col = field % 8;
		int row = field / 8;
		boolean endWhile = false;
		gs.table[row][col] = gs.turn;

		for (int i = 0; i < 8; ++i) {
			endWhile = false;
			int currentRow = row + rowOffset[i];
			int currentCol = col + colOffset[i];
			boolean oppPieceBetween = false;
			while (currentRow >= 0 && currentRow < 8 && currentCol >= 0 && currentCol < 8 && endWhile == false) {
				if (gs.table[currentRow][currentCol] != 'e') {
					if (gs.table[currentRow][currentCol] != gs.turn)
						oppPieceBetween = true;
					else
						endWhile = true;
					if ((gs.table[currentRow][currentCol] == gs.turn) && oppPieceBetween == true) {
						int setPieceRow = row + rowOffset[i];
						int setPieceCol = col + colOffset[i];
						while (setPieceRow != currentRow || setPieceCol != currentCol) {
							gs.table[setPieceRow][setPieceCol] = gs.turn;
							setPieceRow += rowOffset[i];
							setPieceCol += colOffset[i];
						}

						endWhile = true;
					}

					currentRow += rowOffset[i];
					currentCol += colOffset[i];
				} else
					endWhile = true;
			}
		}
	}

	public void startLogic() {
		this.gui.setCommand(this);
		s = (Server) net;

		Runnable periodicTask = new Runnable() {
			public void run() {
				createGameState();
			}
		};

		if (future == null || future.isCancelled())
			future = executor.scheduleAtFixedRate(periodicTask, 0, 200, TimeUnit.MILLISECONDS);
		gui.onNewGameState(gs);
		s.onNewGameState(gs);
		turnLast = (gs.turn == 'w') ? 'b' : 'w';
		createGameState();
	}

	public void newGame() {
		noMoreMoves = false;
		timeSec = 0;
		for (int i = 0; i < 8; i++) {
			for (int j = 0; j < 8; j++) {
				gs.table[i][j] = 'e';
			}
		}
		gs.table[3][3] = 'w';
		gs.table[4][4] = 'w';
		gs.table[3][4] = 'b';
		gs.table[4][3] = 'b';
		gs.time = 0;
		gs.turn = 'w';
		turnLast = 'b';
		commandsSinceLastProc.clear();
		gui.onNewGameState(gs);
		s.onNewGameState(gs);

	}

	public void stopGame() {
		gs.turn = 'e';
		calculatePoints();
		gui.onNewGameState(gs);
		s.onNewGameState(gs);
	}

	public void calculatePoints() {
		gs.points[0] = 0;
		gs.points[1] = 0;
		for (int i = 0; i < 8; i++) {
			for (int j = 0; j < 8; j++) {
				if (gs.table[i][j] == 'w')
					gs.points[0]++;
				if (gs.table[i][j] == 'b')
					gs.points[1]++;
			}
		}
	}

	public void onNewCommand(Command c) {
		commandsSinceLastProc.add(c);
	}

	public void createGameState() {
		if (gs.turn != 'e') {
			timeSec++;
			if (timeSec >= 25) {
				gs.time++;
				timeSec = 0;
				if (gs.turn == 'w') {
					gui.onNewGameState(gspos);
					s.onNewGameState(gs);
				} else {
					gui.onNewGameState(gs);
					s.onNewGameState(gspos);
				}
			}
			boolean validMove = true;
			if (turnLast != gs.turn) {
				validMove = false;
				gspos.copy(gs);
				for (int i = 0; i < 8; i++) {
					for (int j = 0; j < 8; j++) {
						if (validMove(i * 8 + j)) {
							gspos.table[i][j] = 'p';
							validMove = true;
							noMoreMoves = false;
						}
					}
				}
				if (gspos.turn == 'w') {
					gui.onNewGameState(gspos);
				} else {
					s.onNewGameState(gspos);
				}
				turnLast = gs.turn;
			}

			if (validMove == false) {
				if (noMoreMoves == true) {
					stopGame();
				}
				else{
					gs.turn = (gs.turn == 'w') ? 'b' : 'w';
					noMoreMoves = true;
					s.onNewGameState(gs);
					gui.onNewGameState(gs);
				}
			}

		}

		if (commandsSinceLastProc.isEmpty())
			return;

		Command c;
		c = commandsSinceLastProc.get(0);
		if (c.playerNewGame == true) {
			newGame();
		}

		while (!commandsSinceLastProc.isEmpty()) {
			c = commandsSinceLastProc.remove(0);
			int field = c.clickedField;
			if (field != -1 && gs.turn != 'e') {
				if (validMove(field)) {
					setPiece(field);
					gs.turn = (gs.turn == 'w') ? 'b' : 'w';
					calculatePoints();
					s.onNewGameState(gs);
					gui.onNewGameState(gs);
				}
			}
		}
		

	}

}
