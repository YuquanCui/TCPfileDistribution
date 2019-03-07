
import java.io.BufferedInputStream;
import java.io. BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Scanner;


/**
 * 
 * @author ycui9
 *
 */
public class Server1 extends Thread {
	
	
	public class RFCIndex {
		   public int RFC_num;
		   public String title;
		   public String hostname;  
		   public int TTL;
		   public int sourcePort;
		   public RFCIndex() {		   
			   this.TTL = 7200;
		   }
	}
	
	public LinkedList<RFCIndex> RFC_index;
	int sourcePort;
	String localhost;
	String request;
	String response;
	String peer_ip;
	String requested_file;
	
	String peerListFile;
	String indexListFile;
	
	ServerSocket server;
	Socket clientSocket;
	
	public Server1( int sourcePort ) throws IOException {
		this.sourcePort = sourcePort;
		server = new ServerSocket(this.sourcePort);	
		
		RFC_index = new LinkedList<RFCIndex>();	
		peerListFile = sourcePort + "Peer_List_File.txt";
		indexListFile = sourcePort + "Index_List_File.txt";

	}


	@Override
	public void run() {
		try {
			while(true) {
				System.out.println("About to accept a peer...");
				clientSocket = server.accept();
				
				this.peer_ip = clientSocket.getInetAddress().toString();
				System.out.println("New connection Established with " + peer_ip);
				
				getRequest(clientSocket);
				
				String[] mini = request.split(" "); 
				//TODO print to check the mini make sure the results are correct
				for (int i=0; i<mini.length; i++) {
					System.out.println(mini[i]);
				}
				
				//read the request from the requesting peer
				if (mini[0].equals("RFCQuery")) {
					sendRFCindex(clientSocket);
					//clientSocket.close();
				}
				if (mini[0].equals("GetRFC")) {
					//where if port numer?
					String fn = mini[2];
					sendDocument(clientSocket, fn); //how to know the requested file name
					//clientSocket.close();
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void getRequest(Socket clientSocket) {
		try {
			DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
			request = dis.readUTF();
			System.out.println(request);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void sendRFCindex(Socket rfcClient) throws IOException {
		//convert its own RFC index file to index linkedlist
		
		File indexfile = new File(indexListFile); //the index file only for this peer
		BufferedReader br = new BufferedReader(new FileReader(indexfile));
		
		String readline = null;
		Scanner sc = new Scanner(indexfile);
		while(sc.hasNextLine()) {
			readline = sc.nextLine();
			//System.out.println(readline);
			Scanner lineScan = new Scanner(readline);
			lineScan.useDelimiter(",|\\t|\\n|\\R");
			RFCIndex rfci = new RFCIndex();
			rfci.RFC_num = Integer.parseInt(lineScan.next());
			System.out.println(rfci.RFC_num);
			rfci.hostname = lineScan.next();
			System.out.println(rfci.hostname);
			rfci.title = lineScan.next();
			System.out.println(rfci.title);
			rfci.sourcePort = Integer.parseInt(lineScan.next());
			System.out.println(rfci.sourcePort);


			
			this.RFC_index.add(rfci); //add to linked list
			
		}
		//close the file buffer
		sc.close();
		
			
		//send the rfcindex file to the requesting peer
		String filename = indexListFile;
		File file = new File(filename);
		FileInputStream fin = new FileInputStream(file);
		byte[] fileArray = new byte [(int) file.length()];
		BufferedInputStream bis = new BufferedInputStream(fin);
		bis.read(fileArray, 0, fileArray.length ); //??
		OutputStream os = rfcClient.getOutputStream();
		os.write(fileArray, 0, fileArray.length);
		os.flush();
		os.close();
		System.out.println("RFC Index file was sent!"); 
	}
	
	public void sendDocument(Socket rfcClient, String file) throws IOException {
		//send the index.txt file to the requesting peer
		File indexFile = new File(file);
		
		if(indexFile.exists()) {
			
			//first send message response to the peer
			response = "exist";
			DataOutputStream dos = new DataOutputStream(rfcClient.getOutputStream());
			dos.writeUTF(response);
			dos.flush();
			
			// Then send the index file to the peer
			FileInputStream fis = new FileInputStream(indexFile);
			byte[] fileArray = new byte [(int) indexFile.length()];	
			System.out.println(fileArray.length);
			BufferedInputStream bis = new BufferedInputStream(fis);
			bis.read(fileArray, 0, fileArray.length ); 
			OutputStream os = rfcClient.getOutputStream();
			os.write(fileArray, 0, fileArray.length);
			os.flush();
			os.close();
			System.out.println("Sent the file");
			
		} else {
			response = "Not_exist";
			DataOutputStream dos = new DataOutputStream(rfcClient.getOutputStream());
			dos.writeUTF(response);
		}
	}

}
