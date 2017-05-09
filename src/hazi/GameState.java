package hazi;

import java.io.Serializable;

public class GameState implements Serializable{

	public int time;
	public char turn;		// w: white, b: black, e: end
	public char[][] table;	// e: empty, b: black, w: white, p: white possible
							// p: possible black, v: possible white
	public int[] points;	//	0: white, 1: black
	
	private static final long serialVersionUID = 1L;

	public GameState() {
		points = new int[]{0,0};
		time = 0;
		turn = 'e';
		table = new char[8][8];
		for(int i = 0; i < 8; i++){
			for(int j = 0; j < 8; j++){
				table[i][j] = 'e';
			}
		}
	}
	public GameState(GameState gs) {
		points = new int[2];
		points[0] = gs.points[0];
		points[1] = gs.points[1];
		time = gs.time;
		turn = gs.turn;
		table = new char[8][8];
		for(int i = 0; i < 8; i++){
			for(int j = 0; j < 8; j++){
				table[i][j] = gs.table[i][j];
			}
		}
	}
	
	public void copy(GameState gs){
		points[0] = gs.points[0];
		points[1] = gs.points[1];
		time = gs.time;
		turn = gs.turn;
		for(int i = 0; i < 8; i++){
			for(int j = 0; j < 8; j++){
				table[i][j] = gs.table[i][j];
			}
		}
	}

}
