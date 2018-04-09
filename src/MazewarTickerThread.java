import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.*;

public class MazewarTickerThread extends Thread  {

	public BlockingQueue<MPacket> eventQueue=null;
	public Map projectileMap = null;  
	public MazewarTickerThread(BlockingQueue event_queue, ConcurrentHashMap projectileMap){
		this.eventQueue = event_queue;
		this.projectileMap = projectileMap;
		
	}
	
	public void run(){
		while(true){
			if(eventQueue == null)
			{System.out.println("wozhenshizuile ");
			}
			try{
				
				 if(!projectileMap.isEmpty()) {	
					 eventQueue.put(new MPacket(getName(), MPacket.ACTION, MPacket.TICK));
					 Thread.sleep(200);}
			
				 
			}
			catch(InterruptedException ie){
                //An exception is caught, do something
                System.out.print("lol");
        }
			
		}
		
	}
	
	
	
	
	
	
	
	
	
}
