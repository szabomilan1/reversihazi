package hazi;

import java.io.Serializable;

public class GameState implements Serializable{

	public int time;
	public char turn;		// w: white, b: black
	public char[][] table;	// e: empty, b: black, w: white, p: white possible
							// p: possible black, v: possible white 
	
	private static final long serialVersionUID = 1L;

	public GameState() {
		time = 0;
		turn = 'w';
		table = new char[8][8];
		for(int i = 0; i < 8; i++){
			for(int j: table[i]){
				table[i][j] = 'e';
			}
		}
	}

}
