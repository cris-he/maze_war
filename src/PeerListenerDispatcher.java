import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


public class PeerListenerDispatcher extends Thread{
	private MServerSocket mServerSocket;
	private ConcurrentHashMap<String,MSocket> map;
	private String myself;
	private Location mylocation;
	AtomicInteger token ;
	ConcurrentHashMap<String, Client> clientTable = null;
	AtomicInteger seq ;
	
	public PeerListenerDispatcher(MServerSocket mServerSocket,ConcurrentHashMap<String,MSocket> map,String myself,Location mylocation,AtomicInteger token, ConcurrentHashMap<String, Client> clientTable, AtomicInteger seq ){
		this.mServerSocket=mServerSocket;
		this.map=map;
		this.myself=myself;
		this.mylocation=mylocation;
		this.token=token;
		this.clientTable=clientTable;
		this.seq=seq;
	}


	public void run(){
		try{
			while(true){
            MSocket mSocket= mServerSocket.accept();
            new Thread(new PeerListener(mSocket,map,myself,mylocation,token,clientTable,seq)).start();
       		}
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}
	
}