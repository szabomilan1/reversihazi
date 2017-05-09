package hazi;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Kliens hálózati interfész implementálása.
 * 
 * Megvalósítja a Network absztrakt osztály függvényeit, továbbá implementálja az ICommand interfészt,
 * így ezáltal parancsok fogadására és továbbküldésére is alkalmas.
 * 
 * A kliens példányosítása után egy külön szálat hoz létre, ebben próbálkozik a szerverhez csatlakozni,
 * majd a kapcsolat felépülése után ez látja el a várakozó szál szerepét. A csatlakozás alatt leblokkolja
 * a GUI-t, hogy a felhasználó addig ne tudjon semmilyen bevitelt megadni.
 * 
 * Mûködése során a socketen keresztül fogadott adatfolyamot GameState objektumokká alakítja,
 * majd ezeket továbbítja a GUI felé az IGameState interfész felhasználásával. 
 * Emellett a GUI felõl parancsokat fogad (<code>OnNewCommand</code>, amiket a szerver felé továbbít.
 * @author Tibi
 *
 */
public class Client extends Network implements ICommand{
	
	private Socket socket = null;
	/**
	 * A kapcsolat szándékos legontását jelzõ flag. Ennek igaz értéke esetén a worker szál nem kezd további csatlakozási
	 * kísérletekbe, hanem befejezi mûködését.
	 */
	private boolean exit_flag = false;
	private ObjectOutputStream out = null;
	/**
	 * Az exit flag, a socket illetve az output stream párhuzamos hozzáférésektõl való védelmét látja el.
	 */
	private ReentrantLock lock = null;
	
	private ObjectInputStream in = null;
	/**
	 * Szerver ip címe
	 */
	private String ip;
	/**
	 * Kliens oldali GUI.
	 */
	private GUI gui;

	/**
	 * A kliens feladatát megvalósító <code> Runnable </code> objektum.
	 */
	private ListenerWorker worker;
	/**
	 * A worker-t futtató szál.
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
	 * A belsõ objektumok felszabadítását végzi el. Ezek: Ki és bemeneti streamek, socket.
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
	 * Elindítja a kliens mûködését. Elsõ lépésben bezárja az esetlegesen éppen futó kapcsolatokat, majd egy újat indít.
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
	 * Megállítja a kliens mûködését.
	 * Elsõ lépésként a socket bezárásával megakasztja a worker szál mûködését, ami ennek következtében kivétel 
	 * dobásával kikerül a várakozó állapotból, és befejezi a mûködését.
	 * A függvény visszatérése elõtt megvárja a worker szál befejeztét.
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
	 *  A kliens mûködését megvalósító osztály.
	 *  Futtatása során végtelen ciklusban elõször megpróbál csatlakozni a szerverhez, majd siker esetén felépíti a 
	 *  kapcsolatot, végül fogadja a bejövõ adatfolyamot, amibõl GameState objektumokat generál és juttat el a GUI felé.
	 *  Leállásának feltétele az exit_flag true értéke, amit a ciklus bizonyos pontjain ellenõriz.
	 *  A szerverhez hasonlóan a csatlakozási kísérletek alatt a WinBlocker segítségével leblokkolja a kliens GUI-t.
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
	 * Command objektumok küldésére szolgál a szerver felé.
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
