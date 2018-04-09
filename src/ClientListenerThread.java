import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Hashtable;
import java.util.*;

public class ClientListenerThread implements Runnable {

    private MSocket mSocket  =  null;
    private Hashtable<String, Client> clientTable = null;

    public ClientListenerThread( MSocket mSocket,
                                Hashtable<String, Client> clientTable){
        this.mSocket = mSocket;
        this.clientTable = clientTable;
        if(Debug.debug) System.out.println("Instatiating ClientListenerThread");
    }

    public void run() {
        MPacket received = null;
        Client client = null;
        if(Debug.debug) System.out.println("Starting ClientListenerThread");
        /**********************My code here******************************
                1. use a min heap to keep track of all the received command
                2. peek the root command of the heap when new cmd arrives, 
                   if it's consistent with the expected seq. number, pop it,
                   and then execute
                3. if not, just wait for the next command   
        *****************************************************************/
        int seq=0;
        PriorityQueue<MPacket> queue = new PriorityQueue<MPacket>(20,new Comparator<MPacket>() {
            @Override
            public int compare(MPacket p1, MPacket p2) {
                return p1.sequenceNumber-p2.sequenceNumber;
            }
        });
    
        while(true){
            try{
                received = (MPacket) mSocket.readObject();
                System.out.println("Received " + received);
                queue.add(received);
                while(!queue.isEmpty()){
                    if(queue.peek().sequenceNumber==seq){
                        System.out.println("ececuting seq.: "+seq);
                        received=queue.poll();
                        client = clientTable.get(received.name);
                        if(received.event == MPacket.UP){
                            client.forward();
                            seq++;
                        }else if(received.event == MPacket.DOWN){
                            client.backup();
                            seq++;
                        }else if(received.event == MPacket.LEFT){
                            client.turnLeft();
                            seq++;
                        }else if(received.event == MPacket.RIGHT){
                            client.turnRight();
                            seq++;
                        }else if(received.event == MPacket.FIRE){
                            client.fire();
                            seq++;
                        }
                        else if(received.event == MPacket.TICK){
                        	client.tick();
                        	seq++;
                        	continue;
                        	
                        }else{
                        
                            throw new UnsupportedOperationException();
                        }    
                    }
                    else
                        break;
                }

              
            }catch(IOException e){
                Thread.currentThread().interrupt();    
            }catch(ClassNotFoundException e){
                e.printStackTrace();
            }            
        }
    }
}
