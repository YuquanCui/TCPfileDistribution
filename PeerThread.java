

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;

//Start the RFC server and client within a peer
public class PeerThread extends Thread {

	
	@Override
	public void run() {
		 BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
	      System.out.println("THIS PEER listening port: ");
        int peer_source_port;
		try {
			peer_source_port = Integer.parseInt(br.readLine());
			new Server1( peer_source_port ).start();
			new Client1(peer_source_port).start();
		} catch (NumberFormatException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}


	}

}
