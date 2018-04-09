/*
Copyright (C) 2004 Geoffrey Alan Washburn
   
This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.
   
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
   
You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
USA.
*/
  
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JOptionPane;

import java.awt.EventQueue;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.BorderFactory;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The entry point and glue code for the game.  It also contains some helpful
 * global utility methods.
 * @author Geoffrey Washburn &lt;<a href="mailto:geoffw@cis.upenn.edu">geoffw@cis.upenn.edu</a>&gt;
 * @version $Id: Mazewar.java 371 2004-02-10 21:55:32Z geoffw $
 */

public class Mazewar extends JFrame {

        private static final int MAX_CLIENTS=3;
        private static AtomicInteger token = new AtomicInteger(-1);

        /**
         * The default width of the {@link Maze}.
         */
        private final int mazeWidth = 20;

        /**
         * The default height of the {@link Maze}.
         */
        private final int mazeHeight = 10;

        /**
         * The default random seed for the {@link Maze}.
         * All implementations of the same protocol must use 
         * the same seed value, or your mazes will be different.
         */
        private final int mazeSeed = 0;
        private Random randomGen = new Random(mazeSeed); 
        /**
         * The {@link Maze} that the game uses.
         */
        private Maze maze = null;

        /**
         * The Mazewar instance itself. 
         */
        private Mazewar mazewar = null;
        private MSocket mSocket = null;
        private ObjectOutputStream out = null;
        private ObjectInputStream in = null;

        /**
         * The {@link GUIClient} for the game.
         */
        private GUIClient guiClient = null;
        
        public  static ConcurrentHashMap projectileMap = new ConcurrentHashMap();
        /**
         * A map of {@link Client} clients to client name.
         */
        public static  ConcurrentHashMap<String, Client> clientTable = new  ConcurrentHashMap<String,Client>();

        /**
         * A map of {@link MPacket Acknowledgment} from all peers.
         */
        public static  ConcurrentHashMap<String, Boolean> ack_list = new  ConcurrentHashMap<String,Boolean>();
        
        /**
         * A queue of events.
         */
        private  static BlockingQueue eventQueue = null;
        
        /**
         * The panel that displays the {@link Maze}.
         */
        private OverheadMazePanel overheadPanel = null;

        /**
         * The table the displays the scores.
         */
        private JTable scoreTable = null;
        
        /** 
         * Create the textpane statically so that we can 
         * write to it globally using
         * the static consolePrint methods  
         */
        private static final JTextPane console = new JTextPane();
      
        /** 
         * Write a message to the console followed by a newline.
         * @param msg The {@link String} to print.
         */ 
        public static synchronized void consolePrintLn(String msg) {
                console.setText(console.getText()+msg+"\n");
        }
        
        /** 
         * Write a message to the console.
         * @param msg The {@link String} to print.
         */ 
        public static synchronized void consolePrint(String msg) {
                console.setText(console.getText()+msg);
        }
        
        /** 
         * Clear the console. 
         */
        public static synchronized void clearConsole() {
           console.setText("");
        }
        
        /**
         * Static method for performing cleanup before exiting the game.
         */
        public static void quit() {
                // Put any network clean-up code you might have here.
                // (inform other implementations on the network that you have 
                //  left, etc.)
                

                System.exit(0);
        }
       
        /** 
         * The place where all the pieces are put together. 
         */
        public Mazewar(ConcurrentHashMap<String,MSocket> map,String name) throws IOException,
                                                ClassNotFoundException {
                super("ECE419 Mazewar");
                consolePrintLn("ECE419 Mazewar started!");
                // Create the maze
                 
            
                maze = new MazeImpl(new Point(mazeWidth, mazeHeight), mazeSeed, projectileMap );
                
                assert(maze != null);
                
                // Have the ScoreTableModel listen to the maze to find
                // out how to adjust scores.
                ScoreTableModel scoreModel = new ScoreTableModel();
                assert(scoreModel != null);
                maze.addMazeListener(scoreModel);
                
                // Throw up a dialog to get the GUIClient name.
                if((name == null) || (name.length() == 0)) {
                  Mazewar.quit();
                }
                
             
                //Initialize queue of events
                eventQueue = new LinkedBlockingQueue<MPacket>();
                //Initialize hash table of clients to client name 
               // clientTable = new Hashtable<String, Client>(); 
                
                //Create the GUIClient and connect it to the KeyListener queue
                RemoteClient remoteClient = null;

                int i=0;
                String user_list[]=new String[map.size()];
                for (String s:map.keySet()){
                        user_list[i]=s;
                        i++;
                }
                Arrays.sort(user_list);
                
                // initial acknowledgment list and initialize to client
                for(String s: user_list)
                {
                	ack_list.put(s, false);
                }
        
                for(String s: user_list){  
                        if(s.equals(name)){
                        	if(Debug.test)System.out.println("Adding guiClient: " + s);
                                guiClient = new GUIClient(name, eventQueue);
                                Point point =new Point(randomGen.nextInt(mazeWidth),randomGen.nextInt(mazeHeight));
                                maze.addClientAt(guiClient, point, Player.North);
                                this.addKeyListener(guiClient);
                                clientTable.put(s, guiClient);
                        }else{
                        	if(Debug.test)System.out.println("Adding remoteClient: " + s);
                               remoteClient = new RemoteClient(s);
                                Point point =new Point(randomGen.nextInt(mazeWidth),randomGen.nextInt(mazeHeight));
                                maze.addClientAt(remoteClient, point, Player.North);
                                clientTable.put(s, remoteClient);
                        }
                }
                
                // Use braces to force constructors not to be called at the beginning of the
                // constructor.
                /*
                {
                        maze.addClient(new RobotClient("Norby"));
                        maze.addClient(new RobotClient("Robbie"));
                        maze.addClient(new RobotClient("Clango"));
                        maze.addClient(new RobotClient("Marvin"));
                }
                */

                
                // Create the panel that will display the maze.
                overheadPanel = new OverheadMazePanel(maze, guiClient);
                assert(overheadPanel != null);
                maze.addMazeListener(overheadPanel);
                
                // Don't allow editing the console from the GUI
                console.setEditable(false);
                console.setFocusable(false);
                console.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder()));
               
                // Allow the console to scroll by putting it in a scrollpane
                JScrollPane consoleScrollPane = new JScrollPane(console);
                assert(consoleScrollPane != null);
                consoleScrollPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Console"));
                
                // Create the score table
                scoreTable = new JTable(scoreModel);
                assert(scoreTable != null);
                scoreTable.setFocusable(false);
                scoreTable.setRowSelectionAllowed(false);

                // Allow the score table to scroll too.
                JScrollPane scoreScrollPane = new JScrollPane(scoreTable);
                assert(scoreScrollPane != null);
                scoreScrollPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Scores"));
                
                // Create the layout manager
                GridBagLayout layout = new GridBagLayout();
                GridBagConstraints c = new GridBagConstraints();
                getContentPane().setLayout(layout);
                
                // Define the constraints on the components.
                c.fill = GridBagConstraints.BOTH;
                c.weightx = 1.0;
                c.weighty = 3.0;
                c.gridwidth = GridBagConstraints.REMAINDER;
                layout.setConstraints(overheadPanel, c);
                c.gridwidth = GridBagConstraints.RELATIVE;
                c.weightx = 2.0;
                c.weighty = 1.0;
                layout.setConstraints(consoleScrollPane, c);
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.weightx = 1.0;
                layout.setConstraints(scoreScrollPane, c);
                                
                // Add the components
                getContentPane().add(overheadPanel);
                getContentPane().add(consoleScrollPane);
                getContentPane().add(scoreScrollPane);
                
                // Pack everything neatly.
                pack();

                // Let the magic begin.
                setVisible(true);
                overheadPanel.repaint();
                this.requestFocusInWindow();
        }

        /*
        *Starts the ClientSenderThread, which is 
         responsible for sending events
         and the ClientListenerThread which is responsible for 
         listening for events
        */
        private void startThreads(ConcurrentHashMap<String,MSocket> out_map,String name){
                //Start a new listener thread for keyboard input 
                //new Thread(new ClientListenerKeyBoard( eventQueue)).start();
                //Start a new listener thread 
                //new Thread(new ClientListenerThread(mSocket, clientTable)).start();   
                new Thread(new PeerSender(out_map,name,token,eventQueue)).start();    
        }

        
        /**
         * Entry point for the game.  
         * @param args Command-line arguments.
         */
        public static void main(String args[]) throws IOException,
                                        ClassNotFoundException{

            MServerSocket mServerSocket = null;
            MSocket mSocket_lookup=null;
            boolean listening = true;
            //ObjectOutputStream out_lookup=null;
            //ObjectInputStream in_lookup=null;
            ConcurrentHashMap<String,MSocket> out_map=null;
            try {
                if(args.length == 4) {
                	mSocket_lookup= new MSocket(args[0],Integer.parseInt(args[1]));
                    //out_lookup = new ObjectOutputStream(socket_lookup.getOutputStream());
                    //in_lookup = new ObjectInputStream(socket_lookup.getInputStream());
                	mServerSocket = new MServerSocket(Integer.parseInt(args[2]));//servers as server
                    AtomicInteger seq = new AtomicInteger(0);
                   
                    MPacket packetToServer = new MPacket();
                    Location mylocation=new  Location(InetAddress.getLocalHost().getHostAddress(),Integer.parseInt(args[2]));
                    packetToServer.location=mylocation;
                    packetToServer.symbol =args[3];
                    mSocket_lookup.writeObjectNoError(packetToServer);
                    //out_lookup.writeObject(packetToServer);

                    /* print server reply */
                    MPacket packetFromServer;
                    packetFromServer = (MPacket) mSocket_lookup.readObjectNoError();//in_lookup.readObject();
                    System.out.println(packetFromServer.symbol);
                    if(packetFromServer.symbol.indexOf("used")!=-1)
                        return;
                    //the name is already used, choose another one
                    out_map=new ConcurrentHashMap<String,MSocket>();  
                    new Thread(new PeerListenerDispatcher(mServerSocket,out_map,args[3],mylocation,token,clientTable,seq)).start();//listen to the connection from other peers
                   /***************************************
                   syncing hashmap from server
                    ***************************************/
                   int count=0;
                   while(true){
                        packetFromServer = (MPacket) mSocket_lookup.readObjectNoError();// in_lookup.readObject();
                        if(packetFromServer.type==MPacket.PACKET_NS_DONE){
                            System.out.println("syncing done: Know "+count+" peers");
                            break;
                        }    
                        if(packetFromServer.type==MPacket.TOKEN){
                            System.out.println("syncing done: Know "+count+" peers");
                            token.set(packetFromServer.sequenceNumber);
                            break;
                        }  
                        if(out_map.get(packetFromServer.symbol)==null){
                            //System.out.println("trying to connect "+packetFromServer.symbol+" "+packetFromServer.location.toString());
                            new Thread(new PeerListener(out_map,packetFromServer.symbol,packetFromServer.location,args[3],mylocation,token,clientTable,seq)) .start();
                            count++;
                        }
                    
                    }

                   
                } else {
                    System.err.println("ERROR: Invalid arguments!");
                    System.exit(-1);
                }
            } catch (Exception e) {
                System.err.println("ERROR: Could not listen on port!");
                System.exit(-1);
            }
            /************************************************************
            each client is a server to all the other clients, and it's a 
            client to all the other clients and naming service as well as the
            naming server
            *************************************************************/

             /* Create the GUI */
            while(out_map.size()<MAX_CLIENTS){
                try{
                     Thread.sleep(10);
                }
               catch(Exception e){
                    e.printStackTrace();
               }
             }
           
             Mazewar mazewar = new Mazewar(out_map, args[3]);
             mazewar.startThreads(out_map,args[3]);
             
             //new MazewarTickerThread(eventQueue, projectileMap).start();
             

        }
}
