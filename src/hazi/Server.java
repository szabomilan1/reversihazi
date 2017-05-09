package hazi;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JOptionPane;

/**
 * Szerver h�l�zati interf�sz implement�l�sa.
 * 
 * Megval�s�tja a Network absztrakt oszt�ly f�ggv�nyeit, tov�bb� implement�lja az IGameState interf�szt,
 * �gy ez�ltal j�t�k �llapotok fogad�s�ra �s tov�bbk�ld�s�re is alkalmas.
 * 
 * A szerver p�ld�nyos�t�sa ut�n egy k�l�n sz�lat hoz l�tre, amiben el�sz�r szerver socketet nyit 
 * a 10007-es porton, majd v�rakozni kezd a kliens csatlakoz�s�ra. Ennek megt�rt�nte ut�n fel�p�ti
 * a kapcsolatot, majd fogadja a be�rkez� adatfolyamot, amit Command objektumokk� alak�tva a Logic fel� tov�bb�t
 * az ICommand interf�szen kereszt�l. A j�t�k�llapotok (<code> GameState </code>) kliens fel� k�ld�s�re az IGameState
 * interf�sz OnNewGameState f�ggv�ny�vel biztos�t lehet�s�get. 
 * A kapcsolat megszakad�sa eset�n �jb�l kliens csatlakoz�sra v�r a szerver socketen kereszt�l. A v�rakoz�s alatt
 * a j�t�kt�bl�t megval�s�t� GUI objektumot letiltja a <code> Network WinBlocker</code> aloszt�lya seg�ts�g�vel.
 * @author Tibi
 *
 */
public class Server extends Network implements IGameState{
	
	private ServerSocket serverSocket = null;
	private Socket clientSocket = null;
	/**
	 * A kapcsolat sz�nd�kos lebont�s�t jelz� flag. Ennek igaz �rt�ke eset�n a worker sz�l nem kezd tov�bbi v�rakoz�sba,
	 * hanem befejezi m�k�d�s�t.
	 */
	private boolean exit_flag;
	private ObjectOutputStream out = null;
	/**
	 * Az exit flag, output stream, szerver �s kliens socket p�rhuzamos hozz�f�r�sekt�l val� v�delm�t l�tja el.
	 */
	private ReentrantLock lock = null;
	
	private ObjectInputStream in = null;
	
	private Logic commandInterface;
	private GUI gui;
	/**
	 * A szerver m�k�d�s�t megval�s�t� <code> Runnable </code> objektum.
	 */
	private ListenerWorker worker;
	/**
	 * A worker-t futtat� sz�l.
	 */
	private Thread thread;
	
	/**
	 * Konstruktor
	 * @param ci A szerver oldali Logic.
	 * @param g A szerver oldali GUI. (A csatlakoz�s k�zbeni blokkol�s miatt sz�ks�ges.)
	 */
	Server(Logic ci, GUI g)
	{
		worker = new ListenerWorker();
		commandInterface = ci;
		this.gui = g;
		lock = new ReentrantLock();
	}
	/**
	 * A kliens socket, ki �s bemeneti adatfolyamok bez�r�s�ra szolg�l.
	 */
	 private void cleanup()
	{
		 lock.lock();
			try{
				if (out != null){
					out.close();
					out = null;
				}
			} catch (IOException ex){
				System.err.println("Error while closing out.");
			}
			try{
				if (in != null){
					in.close();
					in = null;
				}
			} catch (IOException ex){
				System.err.println("Error while closing in.");
			}
			try{
				if (clientSocket != null){
					clientSocket.close();
					clientSocket = null;
				}
			} catch (IOException ex){
				System.err.println("Error while closing socket.");
			}
			lock.unlock();
	}
	
	 /**
	  * Elind�tja a szerever m�k�d�s�t. Els� l�p�sben bez�rja az esetleg megl�v� kapcsolatot, majd egy
	  * �j worker sz�lat k�sz�t, ami ell�tja a kliensekre v�rakoz�st, illetve az adatfogad�st.
	  */
	@Override
	void start(String ip) {
		stop();
		exit_flag=false;
		thread = new Thread(worker);
		thread.start();
	}
	
	/**
	 * Meg�ll�tja a fut� szerver m�k�d�s�t.
	 * El�sz�r bez�rja a kliens �s szerver socketet, ami �ltal a worker sz�l kiv�tel dob�s�val
	 * kiker�l az esetleges v�rakoz� �llapotb�l. V�g�l megv�rja a worker sz�l befejez�d�s�t, majd visszat�r.
	 */
	@Override
	void stop() {
		lock.lock();
		exit_flag=true;
		try{
			if(serverSocket != null){
				serverSocket.close();
				serverSocket = null;
			}
		} catch (Exception ex){
			System.out.println("Cannot close server socket");
		}
		try{
			if(clientSocket != null){
				clientSocket.close();
				clientSocket = null;
			}
		} catch (Exception ex) {
			System.out.println("Cannot close client socket");
		}
		lock.unlock();
		
		
		try {
			if(thread!=null)
				thread.join();
		} catch (InterruptedException e) {
			System.out.println("Cannot stop the worker thread.");
		}
	}
	
	/**
	 * Worker sz�l, ami a szerver m�k�d�s�t biztos�tja. Feladata a szerver socket l�trehoz�sa, kliensekre v�rakoz�s
	 * kapcsolat fel�p�t�se, majd a bej�v� adatok fogad�sa.
	 * A kapcsolat megszakad�sa eset�n �jb�l kliensre v�rakoz� �llapotba ker�l.
	 * @author Tibi
	 *
	 */
        private class ListenerWorker implements Runnable {
		public void run() {
			// Create server socket
			ServerSocket ss = null;
			while(ss==null)
				try{
					ss =  new ServerSocket(10007);
				}catch(IOException ex){
					System.out.println("Failed to create server socket");
					JOptionPane.showMessageDialog(gui, "Cannot create server socket on port 10007.","Fatal error",JOptionPane.ERROR_MESSAGE);
					System.exit(-1);
				}
			lock.lock();
			serverSocket = ss;
			lock.unlock();

			// Start listening
			while(true)
			{
				boolean f = true;
				Socket cs=null;
				ObjectOutputStream os=null;
				// Waiting for clients
				while(cs==null)
				{

					lock.lock();
					f = exit_flag;
					lock.unlock();
					if(f==true) return;
					
					WinBlocker blocker = null;
					try{
						// Block main window
						blocker = new WinBlocker(gui);
						cs = serverSocket.accept();
						// Release blocking
						blocker.stop();
						try{
							os = new ObjectOutputStream(cs.getOutputStream());
							in = new ObjectInputStream(cs.getInputStream());
							os.flush();
						} catch(IOException ex){
							cs.close();
							cs = null;
						}
					} catch (Exception ex) {
						if(blocker != null) blocker.stop();
					}
				}

				// share objects
				lock.lock();
				try{
					f = exit_flag;
					if(f){
						os.close();
						in.close();
						cs.close();
					} else {
						clientSocket = cs;
						out = os;
					}
				}catch (IOException ex){
					System.out.println("Close error");
				} finally{
				lock.unlock();
				}
				if(f) return;
				
				// CONNECTION ESTABLISHED
				System.out.println("Client connected.");
				commandInterface.startLogic();
				// COMMUNICATING	
				try {
					while (true) {
						Command c = (Command) in.readObject();
						if(c instanceof Command)
							commandInterface.onNewCommand(c);
					}
				} catch (Exception ex) {
					System.out.println("Disconnected");
				} finally {
					cleanup();
				}
			} // while
		}//run
	}// worker
	
        /**
         * J�t�k�llapot k�ld�s�t val�s�tja meg a kliens fel�.
         */
	public void onNewGameState(GameState gs)
	{
		lock.lock();
		try {
			if(out != null)
			{
				out.reset();
				out.writeObject(gs);
				out.flush();
			}
		} catch (IOException ex) {
			System.err.println("Send error.");
		} finally {
			lock.unlock();
		}
	}
}
