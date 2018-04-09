import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.concurrent.BlockingQueue;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.*;

public class PeerSender implements Runnable {
	
	public static Lock aa;
	
	
    static ConcurrentHashMap<String,MSocket> map;  
    static String myself;
    static boolean added=false;
    static String arr[];
    AtomicInteger token ;
    BlockingQueue<MPacket> eventQueue=null;

    static boolean all_acked = false;
    static long timeout = 100;
    
    /** following for piggy-back implementation **/
    	static boolean piggy_token = false;
    	static boolean circle_completed = false;
	    static boolean target_acked = false;
	    static String target_name;
	    static int expected_sn;
	    
	    static MPacket pb_1 = null;
	    static String pb_2 = null;
	    
	    public static void set_pb(MPacket a, String b)
	    {   
	    	pb_1 = a;
	    	pb_2 = b;
	    	piggy_token = true;
	    }
	    public static void unset_pb()
	    {
	    	pb_1 = null;
	    	pb_2 = null;
	    	piggy_token = false;
	    }
    /******************************************/

    
    public static void generate_ack(String ack_to, int SN)
    {

		if(Debug.test) System.out.println("Generating Acknowledgement to " + ack_to +" With SN: "+ SN + " Current Sender: " + myself);
    	MPacket ack_pkt = new MPacket(ack_to, SN, myself);
    	ack_pkt.type = MPacket.PACKET_ACK;
        MSocket tmp=map.get(ack_to);
        assert(tmp!=null);
        
        if(Debug.no_error)
        	tmp.writeObjectNoError(ack_pkt);
        else
        	tmp.writeObject(ack_pkt);

    }
    
	public boolean rcved_all_ack()
	{
		//if(Debug.test) System.out.println("CHECKING IN SENDER");
		all_acked = true;
    	Iterator<Map.Entry<String, Boolean>> s = Mazewar.ack_list.entrySet().iterator();
    	while(s.hasNext())
    	{
    		
    	    Map.Entry<String, Boolean> entry = (Map.Entry<String, Boolean>) s.next();
    	    Boolean acked = (Boolean)entry.getValue();
    	   // if(Debug.test) System.out.println("Entry: " + entry.getKey() + " is acked/not acked: " + entry.getValue());
    	    if(!acked)
    	    {
    	    	all_acked = false;
    	    }
    	}
    	if(all_acked)
    	{
	        if(Debug.test) System.out.println("SENDER RECEIVED ALL ACK");
    		//PeerSender.ALL_ACKED();
    		// re-initial acknowledgement list
    		s = Mazewar.ack_list.entrySet().iterator();
    		while(s.hasNext())
    		{
    			Map.Entry<String, Boolean> entry = (Map.Entry<String, Boolean>) s.next();
        	    entry.setValue(false);
    		}
    		all_acked = false;
    		return true;
    	}
    	else
    		return !all_acked;
	}
	
	public static void target_ack(MPacket rcved_pkt)
	{
		if(Debug.test)
			System.out.println("Target_Ack Funtion: ack_to_sn: " + rcved_pkt.ack_to_sn
									+ " expected_sn: " + expected_sn);

		if (rcved_pkt.sender.equals(target_name)
				&& rcved_pkt.ack_to.equals(myself) 
				&& rcved_pkt.ack_to_sn == expected_sn)
			target_acked = true;
		else
			target_acked = false;
	}

	public static void circle_complete()
	{
//		if(Debug.test) System.out.println("public static void circle_complete()");
		circle_completed = true;
	}
	
    public PeerSender(ConcurrentHashMap<String,MSocket> map,String myself,AtomicInteger token,BlockingQueue eventQueue){
        this.map=map;
        this.myself=myself;
        this.token=token;
        this.eventQueue=eventQueue;
    }


    public void run(){
        try{
            while(true){
            	
                if(piggy_token)
                {	System.out.println("CALLING PIGGY HELPER");
                	piggyback_helper(pb_1, pb_2);
                }
            	
            	
                if(token.get()==-1){
                    continue;//not holding token, do nothing
                }
                    
                else{

                    if(eventQueue.size()!=0){
                        //do have at least one packet to broadcast
                        try{                
                        //Take packet from queue
                            MPacket to_all = (MPacket)eventQueue.take();
                            to_all.type=MPacket.PACKET_NORMAL;
                            //add a sequence number to make sure a client's move won't be out of order
                            to_all.sequenceNumber=token.get();
                            to_all.name=myself;
                            to_all.sender=myself;
                            token.getAndAdd(1);
                            if(Debug.b_cast){
                            					if(Debug.test) System.out.println("broadcast opration " + to_all);
                            					broadcast(to_all);
                            				}
                            if(Debug.p_back)
                            				{
                            					to_all.piggy_owner = myself;
                            					if(Debug.test) System.out.println("***************************************************************");
                            					if(Debug.test) System.out.println("piggy back opration " + to_all);
                            					piggyback(to_all);
                            				}
                         }catch(InterruptedException e){
                            e.printStackTrace();
                            Thread.currentThread().interrupt();    
                        } 
                    }

                    
                    //System.out.println(myself+" hold the token "+" seq: " +token.get()+",then pass to "+findNext());
                    
                    
                    MSocket out = map.get(findNext());
                    MPacket packetToClient = new MPacket();
                    packetToClient.type=MPacket.TOKEN ;
                    packetToClient.sequenceNumber=token.get();
                    out.writeObjectNoError(packetToClient);
                    token.set(-1);
                    Thread.sleep(10);
                    
                }

            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
        
            /*
            if hold the token, call broadcast
            pass token to next person


            else
                queue all the keypress
            */
        
            
    }

    private void broadcast(MPacket packet){
        try{
        	boolean pass_token = true;
        	while(pass_token)
        	{
	            for (String s:map.keySet()){
	            	MSocket tmp=map.get(s);
	            assert(tmp!=null);
	            
	            if(Debug.no_error)
	            	tmp.writeObjectNoError(packet);
	            else
	            	tmp.writeObject(packet);
	            
	            }
	            long start = System.currentTimeMillis();
	            while (System.currentTimeMillis() - start < timeout && pass_token)
	            {
	            	// prepare to pass the token if the sender received all acknowledgement
	            	if(rcved_all_ack())
	            		pass_token = false;

	            }
            }
	        if(Debug.test) System.out.println("Quiting Boardcast & Passing Token");
        }catch(Exception e){
            e.printStackTrace();
        }
        
    }

    private void piggyback(MPacket packet){
        try{

        	expected_sn = packet.sequenceNumber;
        	target_name = findNext();
            MSocket out = map.get(target_name);
        	
            if(Debug.no_error)
		        out.writeObjectNoError(packet);
            else
            	out.writeObject(packet);

		            /***************************************************************/
		            // start timer
		            if(Debug.test) System.out.println("target: "+ target_name 
		            									+" is not ack yet");
		            long start = System.currentTimeMillis();
		            while (!target_acked)
		            {
		            	
		            	// time out reset timer and retransmit
		            	if(System.currentTimeMillis() - start > timeout)
		            	{
		            		start = System.currentTimeMillis();
		            		if(Debug.test) System.out.println("re-transmission" + packet);
		            		
		                    if(Debug.no_error)
		                    	out.writeObjectNoError(packet);
		                    else
		                    	out.writeObject(packet);
		            	}
		            	Thread.sleep(10);
		            }
		            target_acked = false;expected_sn = -1;
		            if(Debug.test) System.out.println("target: "+ target_name 
							+" is acked");
		            /***************************************************************/
		            // pass piggy token to next
                    MPacket piggy_token = new MPacket();
                    piggy_token.type=MPacket.PIGGY_TOKEN;
                    piggy_token.sequenceNumber=expected_sn;
                    piggy_token.sender=myself;
                    piggy_token.piggy_owner = myself;
	                out.writeObjectNoError(piggy_token);
		            /***************************************************************/
	                if(Debug.test) System.out.println("Circle is not complete yet");
                    while(!circle_completed)
		            {
//		            	MPacket rrr = (MPacket)out.readObjectNoError();
//		            	target_ack(rrr);
//		            	if(rrr.piggy_owner.equals(myself))
//		            		circle_complete();
                    	
//                    	if(Debug.test) System.out.print("DEADLOCK - ");
                    	Thread.sleep(10);
		            }
                    circle_completed = false;
                    if(Debug.test) System.out.println("Circle is now completed");
                    if(Debug.test) System.out.println("***************************************************************");
		            /***************************************************************/
	            
                    
	        if(Debug.test) System.out.println("Quiting Piggyback & Passing Token");
	        
        	}catch(Exception e){
        		e.printStackTrace();
        	}
        
    }
    
	public static void piggyback_helper(MPacket packet, String pbowner)
	{
		try{

				expected_sn = packet.sequenceNumber;
		    	target_name = findNext();
		    	MSocket out = map.get(target_name);
		    	MPacket piggy_pkt = packet;
		    	piggy_pkt.sender = myself;
		    	piggy_pkt.piggy_owner = pbowner;
		    	
		        if(Debug.no_error)
			        out.writeObjectNoError(piggy_pkt);
		        else
		        	out.writeObject(piggy_pkt);
			
			            
			            if(Debug.test) System.out.println("Starting Piggyback Helper, "
			            		+ "waiting for: " + target_name + "'s ack");
			            if(Debug.test) System.out.println( "Target: " + target_name + " is not acked yet");
			            // start timer
			            long start = System.currentTimeMillis();
			            target_acked = false;
			            while (!target_acked)
			            {
//			            	MPacket rrr = (MPacket)out.readObjectNoError();
//			            	target_ack(rrr);
//			            	if(Debug.test) System.out.print("HELPER - ");
			            	// time out reset timer and retransmit
			            	if(System.currentTimeMillis() - start > timeout)
			            	{
			            		start = System.currentTimeMillis();
			            		if(Debug.test) System.out.println("re-transmission" + packet);
			            		
			                    if(Debug.no_error)
			                    	out.writeObjectNoError(piggy_pkt);
			                    else
			                    	out.writeObject(piggy_pkt);
			                    
			            	}
			            	Thread.sleep(10);
			            }
			            target_acked = false;expected_sn = -1;
			            if(Debug.test) System.out.println( "Target: " + target_name + " is acked");
			            PeerListener.pb_token = false;
			            if(Debug.test) System.out.println("Passing Piggy Token");
			            // pass piggy token to next

		                MPacket piggy_token = new MPacket();
		                piggy_token.type=MPacket.PIGGY_TOKEN;
		                piggy_token.sequenceNumber=expected_sn;
		                piggy_token.sender=myself;
		                piggy_token.piggy_owner = pbowner;
		                out.writeObjectNoError(piggy_token);

		    	unset_pb();
		        if(Debug.test) System.out.println("Quiting Piggyback Helper");
		        
		    	}catch(Exception e){
		    		e.printStackTrace();
		    	}
    
		
	}
    

    private static String findNext(){
        //find the next user for the token ring
        int i=0;
        if(added==false){   
            arr=new String[map.size()];
            i=0;
            for (String s:map.keySet()){
                arr[i]=s;
                i++;
            }
            Arrays.sort(arr);
            added=true;
        }
    
        for(i=0;i<map.size();i++){
            if(arr[i].equals(myself))
                break;
        }
        if(i+1<map.size())
            return arr[i+1];
        else
            return arr[0];
        
    }
    
    private static String findPrev(){
    	int i = 0;
    	String prev = null;
    	if(arr[0].equals(myself))
    		{
    			i = map.size() - 1;
    			return arr[i];
    		}

    	for (i=1;i<map.size();i++)
    	{
    		prev = arr[i-1];
            if(arr[i].equals(myself))
                break;
    	}
    	return prev;	
    }
}
