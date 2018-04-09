import java.io.Serializable;
public class Location implements Serializable {
	public String  host;
	public Integer port;
	
	/* constructor */
	public Location(String host, Integer port) {
		this.host = host;
		this.port = port;
	}
	
	/* printable output */
	public String toString() {
		return " HOST: " + host + " PORT: " + port; 
	}
	
}