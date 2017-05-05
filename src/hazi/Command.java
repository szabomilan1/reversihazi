package hazi;

import java.io.Serializable;

public class Command implements Serializable{
	
	public int clickedField;
	public boolean playerNewGame;
	
	private static final long serialVersionUID = 1L;
	
	Command()
	{
		clickedField = -1;
		playerNewGame = false;
	}
	
}
