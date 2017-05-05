/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package hazi;

import java.awt.Point;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Predi
 */
class Logic implements ICommand{

	private GUI gui;
	private Network net = null;
	private ArrayList<Command> commandsSinceLastProc;
	private GameState gs;
	private int timeSec;
	private char turnLast;
	
	private static final int[] rowOffset = {-1, -1, -1,  0,  0,  1,  1,  1};
	private static final int[] colOffset = {-1,  0,  1, -1,  1, -1,  0,  1};
	
	Logic() {
		gs = new GameState();
		timeSec = 0;
		turnLast = 'w';
		ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

		Runnable periodicTask = new Runnable() {
		    public void run() {
		        createGameState();
		        
		    }
		};

		executor.scheduleAtFixedRate(periodicTask, 0, 40, TimeUnit.MILLISECONDS);
	}

	void setGUI(GUI g) {
		gui = g;
	}

	void startServer() {
		if (net != null)
			net.disconnect();
		net = new SerialServer(this);
		net.connect("localhost");
	} 

	void startClient() {
		if (net != null)
			net.disconnect();
		net = new SerialClient(this);
		net.connect("localhost");
	}
	
	public boolean validMove(int field) {
		// check whether this square is empty
		
		int col = field % 8;
		int row = field / 8;
		
		if (gs.table[row][col] != 'e')
			return false;
		
		char oppPiece = (gs.turn == 'b') ? 'w' : 'b';
		
		boolean valid = false;
		boolean endWhile = false;
		for (int i = 0; i < 8 && valid == false; ++i) {
			int curRow = row + rowOffset[i];
			int curCol = col + colOffset[i];
			endWhile = false;
			boolean oppPieceBetween = false;
			while (curRow >=0 && curRow < 8 && curCol >= 0 && curCol < 8 && endWhile == false) {
				
				if (gs.table[curRow][curCol] == oppPiece)
					oppPieceBetween = true;
				else if ((gs.table[curRow][curCol] == gs.turn) && oppPieceBetween)
				{
					valid = true;
				}
				else
					endWhile = true;
				
				curRow += rowOffset[i];
				curCol += colOffset[i];
			}
		}
		
		return valid;
	}

	public void newGame(){
		for(int i=0; i<8; i++){
			for(int j=0; j<8; j++){
				gs.table[i][j] = 'e';
			}
		}
		gs.table[4][4] = 'w';
		gs.table[5][5] = 'w';
		gs.table[4][5] = 'b';
		gs.table[5][4] = 'b';
		gs.time = 0;
		gs.turn = 'w';
	}
	
	public void onNewCommand(Command c) {		
		commandsSinceLastProc.add(c);
	}
	
	public void createGameState(){

		if(commandsSinceLastProc.isEmpty())
			return;
		boolean validMove = false;
		Command c;
		if(turnLast != gs.turn){
			for(int i=0; i<8; i++){
				for(int j=0; j<8; j++){
					if(validMove(i*8+j)){
						gs.table[i][j] = (gs.turn == 'w')? 'v' : 'p';
						validMove = true;
					}
				}
			}
			turnLast = gs.turn;
		}
		
		if(validMove == false){
			c = new Command();
			gs.turn = (gs.turn == 'w')? 'b' : 'w';
			commandsSinceLastProc.add(c);
		}
		else{
			c = commandsSinceLastProc.remove(0);
			while(!commandsSinceLastProc.isEmpty()){
				c = commandsSinceLastProc.remove(0);
				int field = c.clickedField;
				if(field != -1){
					if(validMove(field)){
						gs.table[field/8][field%8] = gs.turn;
						gs.turn = (gs.turn == 'w')? 'b' : 'w';
					}
				}
			}
		}
	}
	

}
