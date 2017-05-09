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
 * Absztrakt oszt�ly a h�l�zatot kezel� oszt�lyok sz�m�ra.
 * @author Tibi
 *
 */
abstract class Network {
	/**
	 * Elind�tja a h�l�zati interf�sz m�k�d�s�t, ami a <code> stop </code> f�ggv�ny megh�v�s�ig 
	 * folyamatosan pr�b�l csatlakozni a m�sik f�lhez.
	 * @param ip A c�l IP c�m.
	 */
	abstract void start(String ip);
	/**
	 * Lebontja a h�l�zati interf�szt, �s meg�ll�tja az azt kezel� objektumot.
	 */
	abstract void stop();	
	
	/**
	 * Blokkolja a konstruktorban megadott ablakot a <code> stop </code> f�ggv�ny megh�v�s�ig.
	 * A dial�gusablak egy c�mk�t �s egy gombot tartalmaz, ez ut�bbira kattintva bez�r�dik az alkalmaz�s.
	 * 
	 * Az oszt�ly c�lja, hogy a Network-h�z tartoz� GUI-t leblokkolja a csatlakoz�s idej�re. 
	 * Minden objektum egy saj�t sz�lban futtatja a dial�gusablak esem�nykezel�j�t, �gy a konstruktor megh�v�ja
	 * nem blokkol�dik.
	 * @author Tibi
	 *
	 */
	protected class WinBlocker implements Runnable{
		private JDialog d;
		
		/**
		 * Konstruktor. Az objektum l�trej�tte sor�n egy dial�gus ablakot hoz l�tre, ami fut�sa alatt leblokkolja
		 * a param�terben megadott GUI objektumot. A dial�gusablak tartalmaz egy megszak�t�s gombot, ami le�ll�tja a
		 * program m�k�d�s�t.
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
		 * Szoftveresen bez�ra a dial�gusablakot.
		 */
		public void stop() {
			d.dispose();
			 d.setVisible(false);
             d.dispatchEvent(new WindowEvent(d, WindowEvent.WINDOW_CLOSING));
		}
	}
	
}
