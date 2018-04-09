import java.io.Serializable;

public class MPacket implements Serializable {

    /*The following are the type of events*/
    public static final int PACKET_INTRO = 0;
    public static final int PACKET_NS_DONE =1;
    public static final int PACKET_NORMAL =2;
    public static final int PACKET_ACK = 3;
    
    public static final int PIGGY_TOKEN = 4;
    
    public String symbol;
    public String sender;
    public Location location;
    
    public String ack_to;
    public int ack_to_sn;

    public String piggy_owner;
    
    public static final int HELLO = 100;
    public static final int ACTION = 200;
    public static final int TOKEN = 1000;

    /*The following are the specific action 
    for each type*/
    /*Initial Hello*/
    public static final int HELLO_INIT = 101;
    /*Response to Hello*/
    public static final int HELLO_RESP = 102;

    /*Action*/
    public static final int UP = 201;
    public static final int DOWN = 202;
    public static final int LEFT = 203;
    public static final int RIGHT = 204;
    public static final int FIRE = 205;
    public static final int TICK = 206;
    public static final int ACK = 207;
    
    //These fields characterize the event  
    public int type;
    public int event; 

    //The name determines the client that initiated the event
    public String name;
    
    //The sequence number of the event
    public int sequenceNumber;

    //These are used to initialize the board
    public int mazeSeed;
    public int mazeHeight;
    public int mazeWidth; 
    public Player[] players;

    public MPacket(){
        
    } 
    public MPacket(int type, int event){
        this.type = type;
        this.event = event;
    }
    
    public MPacket(String name, int type, int event){
        this.name = name;
        this.type = type;
        this.event = event;
    }
    
    public MPacket(String ack_to, int SN, String sender)
    {
    	this.ack_to = ack_to;
    	this.ack_to_sn = SN;
    	this.sequenceNumber = SN;
    	this.sender = sender;
    	this.name = sender;
    }
    
    public String toString(){
        String typeStr;
        String eventStr;
        
        switch(type){
            case 100:
                typeStr = "HELLO";
                break;
            case 200:
                typeStr = "ACTION";
                break;
            case 2:
                typeStr = "NORMAL";
            case 3:
                typeStr = "ACK";
                break;
            default:
                typeStr = "ERROR";
                break;        
        }
        switch(event){
            case 101:
                eventStr = "HELLO_INIT";
                break;
            case 102:
                eventStr = "HELLO_RESP";
                break;
            case 201:
                eventStr = "UP";
                break;
            case 202:
                eventStr = "DOWN";
                break;
            case 203:
                eventStr = "LEFT";
                break;
            case 204:
                eventStr = "RIGHT";
                break;
            case 205:
                eventStr = "FIRE";
                break;
             
            case 206: 
            	eventStr = "TICK";
            	break; 
            	
            case 207: 
            	eventStr = "ACK";
            	break; 
            	
            default:
                eventStr = "ERROR";
                break;        
        }
        //MPACKET(NAME: name, <typestr: eventStr>, SEQNUM: sequenceNumber)
        String retString = String.format("MPACKET(NAME: %s, <%s: %s>, SEQNUM: %s)", name, 
            typeStr, eventStr, sequenceNumber);
        return retString;
    }

}
