import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class PeerListener extends Thread{
	MSocket mSocket;
	ConcurrentHashMap<String,MSocket> map;  
	Location peer_location;
	String peer_name;
	String myself;
	Location mylocation;
	AtomicInteger token ;
	ConcurrentHashMap<String, Client> clientTable = null;
	AtomicInteger seq ;
	
	static MPacket pb_temp = null;
	static String current_pb_owner = null;
	public static boolean pb_token = false;
	public static boolean pb_helper_done = false;
	public PeerListener(MSocket mSocket,ConcurrentHashMap<String,MSocket> map,String myself,Location mylocation,AtomicInteger token, ConcurrentHashMap<String, Client>clientTable, AtomicInteger seq){
		//server as sever
		this.mSocket=mSocket;
		this.map=map;
		this.myself=myself;
		this.mylocation=mylocation;
		this.token=token;
		this.clientTable=clientTable;
		this.seq=seq;
	}


	public PeerListener(ConcurrentHashMap<String,MSocket> map,String peer_name,Location peer_location,String myself,Location mylocation,AtomicInteger token, ConcurrentHashMap<String, Client> clientTable,AtomicInteger seq){
		//learn from name service
		try{
			mSocket=new MSocket(peer_location.host,peer_location.port);
		}
		catch(Exception e){
			e.printStackTrace();
		}
		this.map=map;
		this.peer_name=peer_name;
		this.peer_location=peer_location;
		this.myself=myself;
		this.mylocation=mylocation;
		this.token=token;
		this.clientTable=clientTable;
		this.seq=seq;
	}
	
	public MPacket read_ack()
	{				MPacket packetFromPeer = null;
		try {
				packetFromPeer = (MPacket) mSocket.readObjectNoError();
			} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
			}
		return packetFromPeer;
	}
	
	public void run(){
		try{

			assert(mSocket!=null);
			MPacket received = null;
	        Client client = null;
	        if(Debug.test) System.out.println("Starting ClientListenerThread");
	        /**********************My code here******************************
	                1. use a min heap to keep track of all the received command
	                2. peek the root command of the heap when new cmd arrives, 
	                   if it's consistent with the expected seq. number, pop it,
	                   and then execute
	                3. if not, just wait for the next command   
	        *****************************************************************/
	        PriorityQueue<MPacket> queue = new PriorityQueue<MPacket>(20,new Comparator<MPacket>() {
	            @Override
	            public int compare(MPacket p1, MPacket p2) {
	                return p1.sequenceNumber-p2.sequenceNumber;
	            }
	        });


			//ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
			if(peer_name!=null&&peer_location!=null){
				//learn directly from naming service, introduce my self
				map.put(peer_name,mSocket);
				MPacket packetToPeer = new MPacket();  
		        packetToPeer.location=mylocation;
		        packetToPeer.sender =myself;
		        packetToPeer.type=MPacket.PACKET_INTRO;
		        mSocket.writeObjectNoError(packetToPeer);
		        //out.writeObject(packetToPeer);
		        //out.flush();
		      	System.out.println("connection to "+peer_name+peer_location.toString()+" added");	           
			}

			//ObjectInputStream in= new ObjectInputStream(socket.getInputStream());
		
	
						
			while(true){

				MPacket packetFromPeer;// = null;
				
					        if(Debug.no_error)
					        	packetFromPeer = (MPacket) mSocket.readObjectNoError();//in.readObject();
					        else
					        	packetFromPeer = (MPacket) mSocket.readObject();
		        
                if(packetFromPeer.type!=MPacket.TOKEN)
                	System.out.println("RCV From :" + packetFromPeer.sender + " -- Type: " + packetFromPeer.type +" -- Rcv SN: "+ packetFromPeer.sequenceNumber +" -- Cur SN: "+ seq);
                
                
                if(packetFromPeer.type==MPacket.PACKET_INTRO){
                	//learn by other client's broadcast
                	if(map.get(packetFromPeer.sender)==null){
                		map.put(packetFromPeer.sender,mSocket);
                		System.out.println("connection to "+packetFromPeer.sender+packetFromPeer.location.toString()+" added");
                	}	
                }

                else if(packetFromPeer.type==MPacket.TOKEN){
                	token.set(packetFromPeer.sequenceNumber);
                	continue;//doing nothing
                }

                else if(packetFromPeer.type==MPacket.PACKET_NORMAL){
                	received = packetFromPeer;
	                System.out.println("Type Normal SN:  " + received.sequenceNumber +" Current seq="+seq);
	                PeerSender.generate_ack(received.sender,received.sequenceNumber);
	                // AT-MOST-ONCE MSG DELIVERY
	                if(received.sequenceNumber >= seq.get())
	                	queue.add(received);
//	                if(received.sequenceNumber<seq.get())
//	                {
//                    	assert(received.sender!=null);
//                    	PeerSender.generate_ack(received.sender,received.sequenceNumber);
//	                }
	                while(!queue.isEmpty()){
	                	System.out.println("I PEEK THE SN: "+queue.peek().sequenceNumber +" Seq.get(): "+ seq.get());
	                    if(queue.peek().sequenceNumber==seq.get()){

	                    	assert(packetFromPeer.sender!=null);
	                    	pb_temp = packetFromPeer;
	                    	current_pb_owner = packetFromPeer.piggy_owner;
	               //     	PeerSender.generate_ack(packetFromPeer.sender,packetFromPeer.sequenceNumber);
	               //     	if(pb_token) PeerSender.piggyback_helper(pb_temp);
	                        System.out.println("ececuting seq.: "+seq);
	                        seq.addAndGet(1);
	                        received=queue.poll();
	                        client = clientTable.get(received.name);
	                        if(received.event == MPacket.UP){
	                            client.forward();
	                        }else if(received.event == MPacket.DOWN){
	                            client.backup();
	                        }else if(received.event == MPacket.LEFT){
	                            client.turnLeft();
	                        }else if(received.event == MPacket.RIGHT){
	                            client.turnRight();
	                        }else if(received.event == MPacket.FIRE){
	                            client.fire();
	                        }
	                        else if(received.event == MPacket.TICK){
	                        	client.tick();
	                        }else{
	                            throw new UnsupportedOperationException();
	                        }    
	                    }
	                    else
	                        break;
	                }
                }
                else if (packetFromPeer.type==MPacket.PACKET_ACK)
                {
                	// broadcast enabled
                	if(Debug.b_cast)
                	{
                		if(packetFromPeer.ack_to.equals(myself))
                		Mazewar.ack_list.put(packetFromPeer.sender, true);
                	}
                	
                	
                	// piggy back enabled
                	if (Debug.p_back)
                		PeerSender.target_ack(packetFromPeer);
                }
                else if (packetFromPeer.type==MPacket.PIGGY_TOKEN)
                {
                	if (Debug.p_back)
                		 System.out.println("received piggy token, current pb owner: " +packetFromPeer.piggy_owner);
                	
                	
                	if(packetFromPeer.piggy_owner.equals(myself))
                		PeerSender.circle_complete();
                	else
                		PeerSender.set_pb(pb_temp,current_pb_owner);
                		//PeerSender.piggyback_helper(pb_temp,current_pb_owner);
                }

                else{

                }
                
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
	}
}