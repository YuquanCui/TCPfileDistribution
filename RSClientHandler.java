package RegisterServer;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

/**
 * 
 * @author ycui9
 *
 */
// Peer and RS actions...
public class RSClientHandler extends Thread {	
	
	static public class Peer {

	   public int cookie;
	   public String hostname; 
	   public int port; //source port
	   public int TTL; 
	   public boolean isActive;
	   public String lastTime;
	   public int num_registed; //register times
	   
	   public Peer() {
		   num_registed = 0;	
		   hostname = null;
		   isActive = false;
	   }	   
	}
	
    Socket socket;
    String peerRequest;
    String RSresponse;
    static LinkedList<Peer> peers = new LinkedList<Peer>();;
    LinkedList<Peer> activePeers = new LinkedList<Peer>();
    static int cookie = 0; //cookie for peers
    
	public RSClientHandler( Socket peerConnection ) {

		socket = peerConnection;
		peerRequest = "";
		RSresponse = "";
	}




	@Override
	public void run() {
		boolean isExist = false; //if this peer actually exist
		int index = 0;

		Peer newP = new Peer();
		DateFormat df = new SimpleDateFormat("dd/MM/YY HH:MM:SS");
		Date currentDate = new Date();
		
		try {
			DataInputStream in = new DataInputStream(socket.getInputStream());
			peerRequest = in.readUTF(); //read in the message from the peer
			//System.out.println("This peer request message: " + peerRequest);
			//split the whole message based on the protocol format, whether it's 
			//getting rfc index or rfc file?
			
			
			//protocol format: 
			// method port 			
			String[] mini = peerRequest.split(" "); 
			// print to check the mini make sure the results are correct
			for (int i=0; i<mini.length; i++) {
				System.out.println(mini[i]);
			}
			//System.out.println(socket.getPort());
			// check if the peer registered before.
			if (peers.size()>0) {
				for (int i=0; i<peers.size(); i++) {
					if (peers.get(i).port == Integer.parseInt(mini[1]) ) {
						System.out.println("This peer has already registered");
						isExist = true;
						index = i;
						newP = peers.get(i);						
						break;						
					}
				}
			} else {
				isExist = false;
			}
			
			
			if (mini[0].equals("Register")) {
				if(isExist) {
					newP.port = Integer.parseInt(mini[1]);
					newP.isActive = true;
					long startTime = System.currentTimeMillis();
					newP.TTL = 7200;
					//newP.TTL = 7200 - (int) ((System.currentTimeMillis() - startTime)/1000);
					newP.lastTime = df.format(currentDate);
					newP.num_registed++;
					//System.out.println(peers.get(i).lastTime);

				}
				
				if (!isExist) {
					++this.cookie;
					System.out.println("Cookie: "+this.cookie);
					newP.hostname = socket.getInetAddress().toString();
					newP.port = Integer.parseInt(mini[1]);
					newP.cookie = this.cookie;
					newP.isActive = true;
					newP.TTL = 7200;
					newP.num_registed++;
					System.out.println("Registered num: "+newP.num_registed);

					newP.lastTime = df.format(currentDate);
					System.out.println(newP.lastTime);
					peers.add(newP);
					System.out.println("Peer status: "+newP.isActive);
					System.out.println("registered successfully");
					System.out.println("Size of peer list: " + peers.size());
				}
				
			    RSresponse = newP.cookie+ " registered successfully";
			    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
			    out.writeUTF(RSresponse);			    
	
			    
			} else if (mini[0].equals("Leave")) {
				if(isExist) {
					newP.isActive = false;
					newP.TTL = 0; //??
					RSresponse = newP.cookie+ " LEFT successfully";			
				    
				}
				DataOutputStream out = new DataOutputStream(socket.getOutputStream());
			    out.writeUTF(RSresponse);
			    socket.close();
			    System.out.println("Left successfully");
				
				
			} else if (mini[0].equals("PQuery")) {
				// TODO Auto-generated catch block
				
				//add active peers into a new linkedlist
				for (int i=0; i<peers.size(); i++) {
					if (peers.get(i).isActive) {
						activePeers.add(peers.get(i));
					}
				}
				System.out.println(activePeers.size());
				
				//if no active peers then output a message
				if (activePeers.size() < 1) {
					RSresponse = "No";
					DataOutputStream out = new DataOutputStream(socket.getOutputStream());
				    out.writeUTF(RSresponse);
				}
				
				//if has active peers
				File file = new File("peerListFile.txt");
				BufferedWriter out = new BufferedWriter(new FileWriter(file, false)); //whether write from the start or the end???
				
				//DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
				//RSresponse = "Active peers will be sent as a file";
				//dos.writeUTF(RSresponse);
				
				
				for( int j=0; j<activePeers.size(); j++) {
					//write out the cookie, ip address and the port number of that peer
					out.write(activePeers.get(j).cookie + "\t" + activePeers.get(j).hostname + "\t"+ activePeers.get(j).port);
					out.newLine();
				}
				//close the file buffer
				out.close(); 				
				//send the file to the requesting peer
				byte[] fileArray = new byte [(int) file.length()];
				FileInputStream fis = new FileInputStream(file);
				BufferedInputStream bis = new BufferedInputStream(fis);
				bis.read(fileArray, 0, fileArray.length);
				OutputStream os = socket.getOutputStream();
				os.write(fileArray, 0, fileArray.length);							
				socket.close(); // close the tcp connection 
				System.out.println("RS has sent the active peerlist file to the requesting peer...");
				
			} else if (mini[0].equals("KeepAlive")) {				
				if(isExist) {				
					newP.isActive = true;
					newP.TTL = 7200; //??
					RSresponse = newP.cookie+ " KeepAlive successfully";			
				    
				}
				DataOutputStream out = new DataOutputStream(socket.getOutputStream());
			    out.writeUTF(RSresponse);
			    socket.close();
				
			}
			
			
		} catch (IOException e) {
			RSresponse = "Cannot read peer's request";
			try {
				DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
				dos.writeUTF(RSresponse);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			e.printStackTrace();
		} 
		
	}

}
