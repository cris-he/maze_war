import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class NServer{
	public static void main(String[] args) throws Exception{
		MServerSocket mServerSocket = null;
		ConcurrentHashMap<String,Location> map=new ConcurrentHashMap<String,Location>();
        ConcurrentHashMap<String,MSocket> out_map=new ConcurrentHashMap<String,MSocket >();
        int index=1; 

		boolean listening=true;
        try {
        	if(args.length == 1) {
        		mServerSocket = new MServerSocket(Integer.parseInt(args[0]));
        	} else {
        		System.err.println("ERROR: Invalid arguments!");
        		System.exit(-1);
        	}
        } catch (IOException e) {
            System.err.println("ERROR: Could not listen on port!");
            System.exit(-1);
        }
         // one thread only
        while (listening) {
            MSocket mSocket=mServerSocket.accept();
            Thread r=new Thread(new NServerHandler(mSocket,map,out_map,index));
            index++;
            r.start();  
        }
	}
}