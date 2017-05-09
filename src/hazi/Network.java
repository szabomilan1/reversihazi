package hazi;

import java.awt.Dialog;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import hazi.GUI;
/**
 * Absztrakt osztály a hálózatot kezelõ osztályok számára.
 * @author Tibi
 *
 */
abstract class Network {
	/**
	 * Elindítja a hálózati interfész mûködését, ami a <code> stop </code> függvény meghívásáig 
	 * folyamatosan próbál csatlakozni a másik félhez.
	 * @param ip A cél IP cím.
	 */
	abstract void start(String ip);
	/**
	 * Lebontja a hálózati interfészt, és megállítja az azt kezelõ objektumot.
	 */
	abstract void stop();	
	
	/**
	 * Blokkolja a konstruktorban megadott ablakot a <code> stop </code> függvény meghívásáig.
	 * A dialógusablak egy címkét és egy gombot tartalmaz, ez utóbbira kattintva bezáródik az alkalmazás.
	 * 
	 * Az osztály célja, hogy a Network-höz tartozó GUI-t leblokkolja a csatlakozás idejére. 
	 * Minden objektum egy saját szálban futtatja a dialógusablak eseménykezelõjét, így a konstruktor meghívója
	 * nem blokkolódik.
	 * @author Tibi
	 *
	 */
	protected class WinBlocker implements Runnable{
		private JDialog d;
		
		/**
		 * Konstruktor. Az objektum létrejötte során egy dialógus ablakot hoz létre, ami futása alatt leblokkolja
		 * a paraméterben megadott GUI objektumot. A dialógusablak tartalmaz egy megszakítás gombot, ami leállítja a
		 * program mûködését.
		 * @param g
		 */
		public WinBlocker(GUI g) {
			
			JPanel pan = new JPanel(new GridLayout(2, 1));
			
			ImageIcon icon = new ImageIcon("loader.gif");
	
			JLabel label = new JLabel("Connecting...",icon,JLabel.CENTER);
			JButton button = new JButton("Terminate");
			button.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					stop();
					System.out.println("Reconnection terminated by the user.");
					System.exit(-1);
				}
			});
			
			pan.add(label);
			pan.add(button);

			d = new JDialog(g.getJ(), "Waiting", Dialog.ModalityType.DOCUMENT_MODAL);
			d.add(pan);
			d.pack();
			d.setLocationRelativeTo(g);
			Thread blocker = new Thread(this);
			blocker.start();			
		}
		public void run() {
			d.setVisible(true);
			System.out.println("Rdy");
		}
		
		/**
		 * Szoftveresen bezára a dialógusablakot.
		 */
		public void stop() {
			d.dispose();
			 d.setVisible(false);
             d.dispatchEvent(new WindowEvent(d, WindowEvent.WINDOW_CLOSING));
		}
	}
	
}
