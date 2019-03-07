package RegisterServer;



import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


/**
 * 
 * @author ycui9
 *
 */
public class RS {

    

	public static void main(String[] args) throws IOException {
		int i = 1;
		ServerSocket server = new ServerSocket(65243);
		Socket connection = new Socket();
        while(true) {
        	System.out.println("Register server is waiting to connect a peer...");
        	connection = server.accept();
        	//System.out.println(server.getLocalPort());
        	System.out.println("Peer " + i + " is registering "+ connection.getInetAddress().toString());
        	new RSClientHandler(connection).start();
        	i++;
        }
	}

}
