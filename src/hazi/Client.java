package hazi;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Kliens h�l�zati interf�sz implement�l�sa.
 * 
 * Megval�s�tja a Network absztrakt oszt�ly f�ggv�nyeit, tov�bb� implement�lja az ICommand interf�szt,
 * �gy ez�ltal parancsok fogad�s�ra �s tov�bbk�ld�s�re is alkalmas.
 * 
 * A kliens p�ld�nyos�t�sa ut�n egy k�l�n sz�lat hoz l�tre, ebben pr�b�lkozik a szerverhez csatlakozni,
 * majd a kapcsolat fel�p�l�se ut�n ez l�tja el a v�rakoz� sz�l szerep�t. A csatlakoz�s alatt leblokkolja
 * a GUI-t, hogy a felhaszn�l� addig ne tudjon semmilyen bevitelt megadni.
 * 
 * M�k�d�se sor�n a socketen kereszt�l fogadott adatfolyamot GameState objektumokk� alak�tja,
 * majd ezeket tov�bb�tja a GUI fel� az IGameState interf�sz felhaszn�l�s�val. 
 * Emellett a GUI fel�l parancsokat fogad (<code>OnNewCommand</code>, amiket a szerver fel� tov�bb�t.
 * @author Tibi
 *
 */
public class Client extends Network implements ICommand{
	
	private Socket socket = null;
	/**
	 * A kapcsolat sz�nd�kos legont�s�t jelz� flag. Ennek igaz �rt�ke eset�n a worker sz�l nem kezd tov�bbi csatlakoz�si
	 * k�s�rletekbe, hanem befejezi m�k�d�s�t.
	 */
	private boolean exit_flag = false;
	private ObjectOutputStream out = null;
	/**
	 * Az exit flag, a socket illetve az output stream p�rhuzamos hozz�f�r�sekt�l val� v�delm�t l�tja el.
	 */
	private ReentrantLock lock = null;
	
	private ObjectInputStream in = null;
	/**
	 * Szerver ip c�me
	 */
	private String ip;
	/**
	 * Kliens oldali GUI.
	 */
	private GUI gui;

	/**
	 * A kliens feladat�t megval�s�t� <code> Runnable </code> objektum.
	 */
	private ListenerWorker worker;
	/**
	 * A worker-t futtat� sz�l.
	 */
	private Thread thread;
	
	/**
	 * Konstruktor
	 * @param gui Kliens oldali GUI.
	 */
	Client(GUI gui)
	{
		//gsInterface = g;
		this.gui = gui;
		gui.setCommand(this);
		worker = new ListenerWorker();
		lock = new ReentrantLock();
	}

	/**
	 * A bels� objektumok felszabad�t�s�t v�gzi el. Ezek: Ki �s bemeneti streamek, socket.
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
			if (socket != null){
				socket.close();
				socket = null;
			}
		} catch (IOException ex){
			System.err.println("Error while closing socket.");
		}
		lock.unlock();
	}
	/** 
	 * Elind�tja a kliens m�k�d�s�t. Els� l�p�sben bez�rja az esetlegesen �ppen fut� kapcsolatokat, majd egy �jat ind�t.
	 */
	public void start(String ip)
	{
		this.ip = ip;
		stop();
		exit_flag=false;
		thread = new Thread(worker);
		thread.start();
	}
	/**
	 * Meg�ll�tja a kliens m�k�d�s�t.
	 * Els� l�p�sk�nt a socket bez�r�s�val megakasztja a worker sz�l m�k�d�s�t, ami ennek k�vetkezt�ben kiv�tel 
	 * dob�s�val kiker�l a v�rakoz� �llapotb�l, �s befejezi a m�k�d�s�t.
	 * A f�ggv�ny visszat�r�se el�tt megv�rja a worker sz�l befejezt�t.
	 */
	public void stop()
	{
		lock.lock();
		try{
			exit_flag = true;
			if(socket != null)
				socket.close();
		} catch(IOException ex){
			System.out.println("Cannot close socket");
		}finally{
			lock.unlock();
		}
		try{
		if(thread!=null)
		{
			thread.join();
			thread=null;
		}
		} catch(InterruptedException ie){
			System.out.println("Join interrupted");
		}

	}
	/**
	 *  A kliens m�k�d�s�t megval�s�t� oszt�ly.
	 *  Futtat�sa sor�n v�gtelen ciklusban el�sz�r megpr�b�l csatlakozni a szerverhez, majd siker eset�n fel�p�ti a 
	 *  kapcsolatot, v�g�l fogadja a bej�v� adatfolyamot, amib�l GameState objektumokat gener�l �s juttat el a GUI fel�.
	 *  Le�ll�s�nak felt�tele az exit_flag true �rt�ke, amit a ciklus bizonyos pontjain ellen�riz.
	 *  A szerverhez hasonl�an a csatlakoz�si k�s�rletek alatt a WinBlocker seg�ts�g�vel leblokkolja a kliens GUI-t.
	 *  
	 * @author Tibi
	 *
	 */
	private class ListenerWorker implements Runnable {
		public void run() {
			while(true){
				boolean f= true;
				Socket s = null;
				ObjectOutputStream o=null;
				// Connecting
				
				lock.lock();
				f = exit_flag;
				lock.unlock();
				if(f) {
					return;
				}
				
				WinBlocker blocker = null;
				// Block main window
				blocker = new WinBlocker(gui);
				while(s == null)
				{

					try{
						lock.lock();
						f = exit_flag;
						lock.unlock();
						if(f) {
							blocker.stop();
							return;
						}
						
						s = new Socket(ip,10007);
						try{
							o = new ObjectOutputStream(s.getOutputStream());
							o.flush();
							in = new ObjectInputStream(s.getInputStream());
						} catch(IOException ex){
							s.close();
							s = null;
						}
					} catch (UnknownHostException he){
						System.out.println("Cannot reach host");
					} catch (IOException ie) {
						System.out.println("Connection error");
					} 
				}
				if(blocker!=null) blocker.stop();
				// share objects
				lock.lock();
				try{
					f = exit_flag;
					if(f){
						o.close();
						in.close();
						s.close();
					} else {
						socket = s;
						out = o;
					}
				}catch (IOException ex){
					System.out.println("Close error");
				} finally{
				lock.unlock();
				}
				if(f) return;
	
				// CONNECTION ESTABLISHED
				System.out.println("Connected to server.");
				// TODO send comman for the logic
				/*
				GameState g = new GameState("Connected");
				gsInterface.onNewGameState(g);
				*/
				try {
					while (true) {
						GameState gs = (GameState) in.readObject();
						if(gs instanceof GameState)
							gui.onNewGameState(gs);
					}
				} catch (Exception ex) {
					ex.printStackTrace();
					System.out.println("Disconnected");
				} finally {
					cleanup();
					// TODO create game state to signal to he gsInterface
					/*
					GameState gs = new GameState("Connection lost");
					gsInterface.onNewGameState(gs);
					*/
				}
			}// while
		} // run
	} //worker
	
	/**
	 * Command objektumok k�ld�s�re szolg�l a szerver fel�.
	 */
	public void onNewCommand(Command c)
	{
		lock.lock();
		try {
			if(out != null){
				out.writeObject(c);
				out.flush();
			}
		} catch (IOException ex) {
			System.out.println("Send error");
		} finally {
			lock.unlock();
		}
		// if command == close, stop client
	}

}
