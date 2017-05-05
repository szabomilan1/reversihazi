/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package hazi;

import java.awt.Point;
import java.util.ArrayList;

/**
 *
 * @author Predi
 */
class Logic implements ICommand{

	private GUI gui;
	private Network net = null;
	private ArrayList<Command> commandsSinceLastProc;
	
	Logic() {
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

	void sendClick(Point p) {
		// gui.addPoint(p); //for drawing locally
		if (net == null)
			return;
		net.send(p);
	}

	void clickReceived(Point p) {
		if (gui == null)
			return;
		gui.addPoint(p);
	}

	public void onNewCommand(Command c) {
		// TODO Auto-generated method stub
		
	}
}
