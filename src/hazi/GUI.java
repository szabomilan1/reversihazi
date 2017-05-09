package hazi;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
//import java.util.Random; //teszteléshez
import javax.swing.*;
import java.awt.*;

public class GUI extends JPanel
		implements IGameState, MouseListener/* , MouseMotionListener */ {
 static final long serialVersionUID = 1L;
	private int array[] = null;
	private ICommand command = null;
	private final int NONE = 0;
	private final int BLACK = 1;
	private final int WHITE = 2;
	private final int MAYBE = 3;
	private int nextplayer = NONE; // fekete 1; feher 2;
	private int winner = NONE;
	private JFrame j;
	
	void setCommand(ICommand c) {
		command = c;
	}

	public GUI(Logic c) {
		this.addMouseListener(this);
		// this.addMouseMotionListener(this);
		array = new int[64];
		// Random r= new Random(); //teszteléshez
		// for(int i=0; i<64; i++){
		// array[i]=r.nextInt(4);
		// }
		j = new JFrame("Reversi Game");
		j.setSize(950, 750);
		j.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		j.setLayout(null);

		JMenuBar menuBar = new JMenuBar();

		JMenu menu = new JMenu("Start");

		JMenuItem menuItem = new JMenuItem("Create Server");
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				c.startServer();
			}
		});
		menu.add(menuItem);

		menuItem = new JMenuItem("Connect To Server");
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String s = (String) JOptionPane.showInputDialog(j, "Server IP:", "Connecting...",
						JOptionPane.PLAIN_MESSAGE, null, null, "localhost");
				c.startClient(s);
			}
		});
		menu.add(menuItem);

		menuBar.add(menu);

		menuItem = new JMenuItem("New game");
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Command com = new Command(); // direkt megszakítás
				com.playerNewGame = true;
				if (command != null) {
					command.onNewCommand(com);
					winner = NONE;
				}
			}
		});

		menuBar.add(menuItem);
		menuItem = new JMenuItem("Exit");
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.exit(0); // direkt megszakítás
			}
		});
		menuBar.add(menuItem);

		j.setJMenuBar(menuBar);

		j.setContentPane(this);
		j.setVisible(true);
	}

	public JFrame getJ() {
		return j;
	}

	@Override
	public void paintComponent(Graphics g) {

		g.setColor(Color.lightGray);
		g.fillRect(0, 0, 950, 750);
		g.setColor(Color.black);
		g.setFont(new Font("Times New Roman", Font.BOLD, 24));
		g.setColor(Color.green);
		g.fillRect(50, 50, 560, 560);
		g.setColor(Color.BLACK);
		g.drawLine(50, 50, 50, 610);
		g.drawLine(50, 50, 610, 50);
		g.drawLine(50, 610, 610, 610);
		g.drawLine(610, 50, 610, 610);

		for (int i = 0; i <= 7; i++) {
			g.drawLine(50, i * 70 + 50, 610, i * 70 + 50);
			g.drawLine(i * 70 + 50, 50, i * 70 + 50, 610);
		}

		int blacks = 0;
		int whites = 0;

		for (int i = 0; i < 64; i++) {
			int sor = i / 8;
			int oszlop = i % 8;
			if (array[i] == NONE) {
				g.setColor(Color.green);
			} else if (array[i] == BLACK) {
				g.setColor(Color.black);
				blacks++;
			} else if (array[i] == WHITE) {
				g.setColor(Color.white);
				whites++;
			}
			// g.fillOval(55+70*oszlop, 70*sor+55, 60, 60);
			if (array[i] == MAYBE) {
				g.setColor(Color.cyan);
				g.fillOval(80 + 70 * oszlop, 70 * sor + 80, 10, 10);
			} else
				g.fillOval(55 + 70 * oszlop, 70 * sor + 55, 60, 60);
		}
		g.setColor(Color.black);

		g.drawString(String.format("Black: %d", blacks), 700, 400);
		g.setColor(Color.white);
		g.drawString(String.format("White: %d", whites), 700, 300);

		g.setColor(Color.black);
		if (winner == NONE) {
			if (nextplayer == BLACK) {
				g.drawString("Next Player: Black", 50, 650);
			}
			g.setColor(Color.white);
			if (nextplayer == WHITE) {
				g.drawString("Next Player: White", 50, 650);
			}
			if (nextplayer == NONE) {
				g.drawString("Let's Play!", 50, 650);
			}
		} else {
			if (winner == WHITE) {
				g.drawString("Congratulations, the white player won!!!", 50, 650);
			}
			g.setColor(Color.black);
			if (winner == BLACK) {
				g.drawString("Congratulations, the black player won!!!", 50, 650);
			}
			if (winner == MAYBE) {
				g.drawString("It's a tie!", 50, 650);
			}
		}

	}

	@Override
	public void onNewGameState(GameState gs) {
		for (int i = 0; i < 8; i++) {
			for (int y = 0; y < 8; y++) {
				if (gs.table[i][y] == 'e') {
					array[i * 8 + y] = NONE;
				} else if (gs.table[i][y] == 'w') {
					array[i * 8 + y] = WHITE;
				} else if (gs.table[i][y] == 'b') {
					array[i * 8 + y] = BLACK;
				} else if (gs.table[i][y] == 'p') {
					array[i * 8 + y] = MAYBE;
				}
			}
		}
		if (gs.turn == 'w') {
			nextplayer = WHITE;
			winner = NONE;
		} else if (gs.turn == 'b') {
			nextplayer = BLACK;
			winner = NONE;
		} else if (gs.turn == 'e') {
			if (gs.points[0] < gs.points[1])
				winner = BLACK;
			if (gs.points[0] > gs.points[1])
				winner = WHITE;
			if (gs.points[0] == gs.points[1]){
				if(gs.points[0] > 0)
					winner = MAYBE;
				else{
					winner = NONE;
					nextplayer = NONE;
				}
					
				
			}
		}
		repaint();
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
		int y = e.getY();
		int x = e.getX();

		int oszlop = (x - 55) / 70;
		if (oszlop >= 8)
			return;
		int sor = (y - 55) / 70;
		if (oszlop >= 8)
			return;
		int index = sor * 8 + oszlop;

		Command c = new Command();
		c.clickedField = index;
		if (command != null) {
			command.onNewCommand(c);
		}
	}

	/*
	 * @Override public void mouseDragged(MouseEvent e) { // TODO Auto-generated
	 * method stub
	 * 
	 * }
	 * 
	 * @Override public void mouseMoved(MouseEvent e) { // TODO Auto-generated
	 * method stub int y=e.getY(); int x=e.getX(); int index; int
	 * oszlop=(x-55)/70; int sor=(y-55)/70; index=sor*8+oszlop; if(szabad==false
	 * && array[index]==NONE){ array[index]=MAYBE; repaint();
	 * 
	 * } }
	 */
}