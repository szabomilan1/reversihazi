package hazi;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JOptionPane;

/**
 * Szerver hálózati interfész implementálása.
 * 
 * Megvalósítja a Network absztrakt osztály függvényeit, továbbá implementálja az IGameState interfészt,
 * így ezáltal játék állapotok fogadására és továbbküldésére is alkalmas.
 * 
 * A szerver példányosítása után egy külön szálat hoz létre, amiben elõször szerver socketet nyit 
 * a 10007-es porton, majd várakozni kezd a kliens csatlakozására. Ennek megtörténte után felépíti
 * a kapcsolatot, majd fogadja a beérkezõ adatfolyamot, amit Command objektumokká alakítva a Logic felé továbbít
 * az ICommand interfészen keresztül. A játékállapotok (<code> GameState </code>) kliens felé küldésére az IGameState
 * interfész OnNewGameState függvényével biztosít lehetõséget. 
 * A kapcsolat megszakadása esetén újból kliens csatlakozásra vár a szerver socketen keresztül. A várakozás alatt
 * a játéktáblát megvalósító GUI objektumot letiltja a <code> Network WinBlocker</code> alosztálya segítségével.
 * @author Tibi
 *
 */
public class Server extends Network implements IGameState{
	
	private ServerSocket serverSocket = null;
	private Socket clientSocket = null;
	/**
	 * A kapcsolat szándékos lebontását jelzõ flag. Ennek igaz értéke esetén a worker szál nem kezd további várakozásba,
	 * hanem befejezi mûködését.
	 */
	private boolean exit_flag;
	private ObjectOutputStream out = null;
	/**
	 * Az exit flag, output stream, szerver és kliens socket párhuzamos hozzáférésektõl való védelmét látja el.
	 */
	private ReentrantLock lock = null;
	
	private ObjectInputStream in = null;
	
	private Logic commandInterface;
	private GUI gui;
	/**
	 * A szerver mûködését megvalósító <code> Runnable </code> objektum.
	 */
	private ListenerWorker worker;
	/**
	 * A worker-t futtató szál.
	 */
	private Thread thread;
	
	/**
	 * Konstruktor
	 * @param ci A szerver oldali Logic.
	 * @param g A szerver oldali GUI. (A csatlakozás közbeni blokkolás miatt szükséges.)
	 */
	Server(Logic ci, GUI g)
	{
		worker = new ListenerWorker();
		commandInterface = ci;
		this.gui = g;
		lock = new ReentrantLock();
	}
	/**
	 * A kliens socket, ki és bemeneti adatfolyamok bezárására szolgál.
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
	  * Elindítja a szerever mûködését. Elsõ lépésben bezárja az esetleg meglévõ kapcsolatot, majd egy
	  * új worker szálat készít, ami ellátja a kliensekre várakozást, illetve az adatfogadást.
	  */
	@Override
	void start(String ip) {
		stop();
		exit_flag=false;
		thread = new Thread(worker);
		thread.start();
	}
	
	/**
	 * Megállítja a futó szerver mûködését.
	 * Elõször bezárja a kliens és szerver socketet, ami által a worker szál kivétel dobásával
	 * kikerül az esetleges várakozó állapotból. Végül megvárja a worker szál befejezõdését, majd visszatér.
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
	 * Worker szál, ami a szerver mûködését biztosítja. Feladata a szerver socket létrehozása, kliensekre várakozás
	 * kapcsolat felépítése, majd a bejövõ adatok fogadása.
	 * A kapcsolat megszakadása esetén újból kliensre várakozó állapotba kerül.
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
         * Játékállapot küldését valósítja meg a kliens felé.
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
