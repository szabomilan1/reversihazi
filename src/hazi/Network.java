/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hazi;

import java.awt.Point;

/**
 *
 * @author Predi
 */
abstract class Network {

	protected Logic ctrl;

	Network(Logic c) {
		ctrl = c;
	}

	abstract void connect(String ip);

	abstract void disconnect();

	abstract void send(Point p);
}
