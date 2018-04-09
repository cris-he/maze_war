import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class NServerHandler implements Runnable{
	private MSocket mSocket = null;
	private ConcurrentHashMap<String,Location> map=null;
	private int index;
	private ConcurrentHashMap<String,MSocket> out_map=null;
	public NServerHandler(MSocket mSocket,ConcurrentHashMap<String,Location> map,ConcurrentHashMap<String,MSocket> out_map,int index) {
		this.map=map;
		this.mSocket = mSocket;
		this.out_map=out_map;
		this.index=index;
		System.out.println("Created new Thread to handle new player");
	}

	public void  run() {

		boolean gotByePacket = false;
		try {

				/* stream to read from client */
				//ObjectInputStream fromClient = new ObjectInputStream(socket.getInputStream());
				MPacket packetFromClient;
				
				/* stream to write back to client */
				//ObjectOutputStream toClient = new ObjectOutputStream(socket.getOutputStream());				

				packetFromClient = (MPacket) mSocket.readObjectNoError();//fromClient.readObject();
				/* create a packet to send reply back to client */
				MPacket packetToClient = new MPacket();
				MPacket myself=new MPacket();
			
				if(map.get(packetFromClient.symbol)==null){
					map.put(packetFromClient.symbol,packetFromClient.location);
				    out_map.put(packetFromClient.symbol,mSocket);

					/* send reply back to client */
					packetToClient.symbol=packetFromClient.symbol+ " "+map.get(packetFromClient.symbol).toString()+" registered ";
					System.out.println(packetFromClient.symbol+ map.get(packetFromClient.symbol).toString());
					mSocket.writeObjectNoError(packetToClient);
					//toClient.writeObject(packetToClient);
					myself.symbol=packetFromClient.symbol;
					myself.location=packetFromClient.location;
				}
				else{
					packetToClient.symbol=packetFromClient.symbol+ " already used";
					System.out.println(packetFromClient.symbol+ " already used");
					mSocket.writeObjectNoError(packetToClient);
					//toClient.writeObject(packetToClient);
					return;//exit current thread
				}
				

		
				
				//sync the global hashmap
				System.out.println("syncing "+myself.symbol+"'s local hashmap");
				for(String s:map.keySet()){
					packetToClient = new MPacket();
					packetToClient.symbol=s;
					packetToClient.location=map.get(s);
					mSocket.writeObjectNoError(packetToClient);
					//toClient.writeObject(packetToClient);
				}
				packetToClient = new MPacket();
				if(index==1){
					//first user
					packetToClient.type=MPacket.TOKEN ;
					packetToClient.sequenceNumber=0;
				}
				else
					packetToClient.type=MPacket.PACKET_NS_DONE;	
					mSocket.writeObjectNoError(packetToClient);
					//toClient.writeObject(packetToClient);
			

			/*
			keep the connection
			fromClient.close();
			toClient.close();
			socket.close();
			*/

		} catch (IOException e) {
			if(!gotByePacket)
				e.printStackTrace();
		}
//		catch (ClassNotFoundException e) {
//			if(!gotByePacket)
//				e.printStackTrace();
//		}
	}
}