package hazi;

import java.awt.Dialog;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import hazi.GUI;

abstract class Network {
	abstract void start(String ip);
	abstract void stop();	
	
	/*
	 * Blocks the given window until the function stop() is called.
	 * This class is used for blocking the main GUI window while the Network thread is reconnecting.
	 * 
	 * This class creates an own thread that is running the Dialog window, so the calling thread
	 * is not blocked.
	 */
	protected class WinBlocker implements Runnable{
		private JDialog d;
		
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


			d = new JDialog(g, "Waiting", Dialog.ModalityType.DOCUMENT_MODAL);
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
		
		public void stop() {
			d.dispose();
		}
	}
	
}
