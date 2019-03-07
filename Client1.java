
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.Scanner;


/**
 * 
 * @author ycui9
 *
 */

//ask for different options
public class Client1 extends Thread {
    String peer_ip;
    String RS_ip;
	public Socket rfcClient;
	public Socket rs;
	int sourcePort; //source port of this peer
	int destination_port; 
	
	String peerListFile;
	String indexListFile;
	
	String localhost;
	String peerRequest;
	String response;
	static LinkedList<String> peer_file;
	static LinkedList<ActivePeer> active_peers;
	
	public Client1( int thisPort) {
		this.sourcePort = thisPort;
		rs = new Socket();
		rfcClient = new Socket();
		active_peers = new LinkedList<ActivePeer>();
		peerListFile = sourcePort + "Peer_List_File.txt";
	    indexListFile = sourcePort + "Index_List_File.txt";
	    peer_file = new LinkedList<String>();
	}
	
	public class ActivePeer {
		int cookie;
		String ip;
		int port;
	}


	@Override
	public void run() {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		while(true) {
			System.out.println("Peer choices: ");
			System.out.println("1. Register ");
			System.out.println("2. Leave");
			System.out.println("3. PQuery ");
			System.out.println("4. KeepAlive");
			System.out.println("5. RFCQuery");
			System.out.println("6. GetRFC");
			System.out.println("Your choice is: ");
			try {
				int choice = Integer.parseInt(br.readLine());
				switch(choice) {
				case 1:
					peerRequest = "Register " + this.sourcePort;
					try {
						sendToRS();
						getRSresponse();
						this.rs.close();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					break;
				case 2:
					peerRequest = "Leave " + this.sourcePort;
					try {
						sendToRS();
						getRSresponse();
						this.rs.close();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					System.exit(1);
					break;
				case 3:
					peerRequest = "PQuery " + this.sourcePort; 
					String fileName = "list.txt";
					try {
						sendToRS();
						//getRSresponse();
						if ( !this.response.equals("No") ) {
							getPeerList(peerListFile);
						}	
						this.rs.close();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block 
						e.printStackTrace();
					}
					break;
				case 4:
					peerRequest = "KeepAlive " + this.sourcePort;
					try {
						sendToRS();
						getRSresponse();
						this.rs.close();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					break;
				case 5:
					peerRequest = "RFCQuery " + this.sourcePort;
					File plf = new File(peerListFile);
					String rl = null;
					Scanner psc = new Scanner(plf);

					while(psc.hasNext()) {
						rl = psc.nextLine();
						Scanner lineScan = new Scanner(rl);
						lineScan.useDelimiter(",|\\t|\\n|\\R");
						int peerCookie = Integer.parseInt(lineScan.next());
						String peerIP = lineScan.next();
						peerIP = peerIP.substring(1);
						int peerPort = Integer.parseInt(lineScan.next());
						
						if (peerPort != this.sourcePort) {
							//send the info to the peer
							sendRFCQueryToPeer(peerRequest, peerIP, peerPort);
							getRFCIndexList(this.rfcClient, "requestedIdxPeer1.txt");
						}
					}

					
					this.rfcClient.close();
					break;
				case 6:
					
					File ilf = new File(indexListFile);
					String readline = null;
					Scanner isc = new Scanner(ilf);
					int line_num=0;
					while(line_num < 11 ) {
						readline = isc.nextLine();
						line_num++;
					}
					
					//start to read the index list file
					while(isc.hasNext()) {
						readline = isc.nextLine();
						String[] index_file_info = new String[4];
						Scanner lineScan = new Scanner(readline);
						lineScan.useDelimiter(",|\\t|\\n|\\R");
						int rfc_num = Integer.parseInt(lineScan.next());
						String peerIP = lineScan.next();
						String rfcFile = lineScan.next();
						int peerPort = Integer.parseInt(lineScan.next());
						
						//send the request to peer
						peerRequest = "GetRFC "+ this.sourcePort+ " " +rfcFile;
						sendGetRFCToPeer(peerRequest, peerIP, peerPort);
						
						//download the file
						String copied_Index = "copied_"+rfcFile;						
					    getrfcfile(this.rfcClient, copied_Index);
						
					}

					
					this.rfcClient.close();
					break;
				}
			} catch (NumberFormatException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			};

		}
	
		
	}
	/**
	 * Including Register and Leave message to RS.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void sendToRS() throws IOException, InterruptedException {
		System.out.println("Enter the IP of the RS: ");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		this.RS_ip = br.readLine();
		rs = new Socket(RS_ip, 65243);
		DataOutputStream out = new DataOutputStream(rs.getOutputStream());
		out.writeUTF(peerRequest);

	}
	
	public void getRSresponse() throws IOException, InterruptedException {
		//get the response from the RS
		DataInputStream in = new DataInputStream(rs.getInputStream());
		response = in.readUTF();
        System.out.println(response);

	}
	
	public void getPeerresponse() throws IOException {
		DataInputStream in = new DataInputStream(rfcClient.getInputStream());
		response = in.readUTF();
		System.out.println(response);
	}
	
    public void getPeerList(String fileName) {
    	
    	//if there is a linked list of active peer from RS
		File file = new File(fileName);
		byte[] byteArray = new byte[100000000];
		try {
			InputStream is = rs.getInputStream();
			FileOutputStream fos = new FileOutputStream(file);
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			int readBytes = is.read(byteArray, 0, byteArray.length); //the number of bytes actually read in
			int currentLength = readBytes;
			do {
				readBytes = is.read(byteArray, currentLength, (byteArray.length-currentLength));
				if (readBytes>=0) {
					currentLength += readBytes;
				}
			} while(readBytes > -1);
            bos.write(byteArray, 0, currentLength); //write all the bytes
            bos.flush();
            
            // read the file and get peers
            Scanner sc = new Scanner(file);
            while(sc.hasNextLine()) {
            	String peer = sc.nextLine();
            	Scanner lineScan = new Scanner(peer);
            	lineScan.useDelimiter(",|\\t|\\n|\\R");
            	ActivePeer pe = new ActivePeer();
            	pe.cookie = Integer.parseInt(lineScan.next()); //peer.cookie
            	pe.ip = lineScan.next(); //peer.hostname
            	pe.port = Integer.parseInt(lineScan.next()); //peer.port
            	this.active_peers.add(pe);
            }
            
            System.out.println("Size of the active peer list: "+active_peers.size());
            sc.close();
            
            
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	
	public void sendGetRFCToPeer(String r, String peerIP, int peerPort ) throws IOException {
		System.out.println("Sent the request to "+peerIP + " " + peerPort);		
		rfcClient = new Socket(peerIP, peerPort);
		DataOutputStream out = new DataOutputStream(rfcClient.getOutputStream());
		out.writeUTF(r);
	}
  
	public void sendRFCQueryToPeer(String r, String peerIP, int peerPort) throws UnknownHostException, IOException {
		System.out.println("Sent the request to "+peerIP + " " + peerPort);		
		rfcClient = new Socket(peerIP, peerPort);
		DataOutputStream out = new DataOutputStream(rfcClient.getOutputStream());
		out.writeUTF(r);
	}

	
	//filename: the new created file that got a copy from the requested server
	public void getRFCIndexList(Socket clientSocket, String filename) throws IOException, FileNotFoundException {
		//receive the indexlist file from the other peer
		File file = new File(filename);
		/*
		Scanner sc = new Scanner(file);
		while(sc.hasNext()) {
			String line = sc.nextLine();
			Scanner lineScan = new Scanner(line);
			lineScan.useDelimiter(",|\\t|\\n|\\R");
			int num = Integer.parseInt(lineScan.next());
			String ip = lineScan.next();
			String name = lineScan.next();
			int port = Integer.parseInt(lineScan.next());
			if ( !peer_port.contains(port)) {
				
			}
		}
		*/		
		byte[] bytearray = new byte[1000000];
		FileOutputStream fos = new FileOutputStream(file);
		BufferedOutputStream bos = new BufferedOutputStream(fos);
		InputStream is = clientSocket.getInputStream();
		int readBytes = is.read(bytearray, 0, bytearray.length); //the number of bytes actually read in
		int currentLength = readBytes;
		do {
			readBytes = is.read(bytearray, currentLength, (bytearray.length-currentLength));
			if (readBytes>=0) {
				currentLength += readBytes;
			}
		} while(readBytes > -1);
		bos.write(bytearray, 0, currentLength);
		bos.flush();
		bos.close();
		
		
		//merge RFC index: add the new index to its own index file so that its own server can also read	
		File fileIndex1 = new File(indexListFile);
		if (!fileIndex1.exists() ) {
			fileIndex1.createNewFile();
		}
		File fileIndex2 = new File(filename);
		BufferedReader br1 = new BufferedReader(new FileReader(fileIndex1));
		BufferedReader br2 = new BufferedReader(new FileReader(fileIndex2));
		BufferedWriter bw = new BufferedWriter(new FileWriter(fileIndex1, true));
		
		int count1 = 0, count2 = 0;		
		
		while(br1.readLine()!= null) {
			count1++;
		}
		while(br2.readLine()!= null) {
			count2++;
		}
		br2.close();

		//read again from the start
		br2 = new BufferedReader(new FileReader(fileIndex2));
		//write into its own index file
		String readString = null;
		for(int i = 0; i<count2; i++) {
			readString = br2.readLine();
			//seperate each line to avoid duplicate port message
			Scanner lineScan = new Scanner(readString);
			lineScan.useDelimiter(",|\\t|\\n|\\R");
			int num = Integer.parseInt(lineScan.next());
			String ip = lineScan.next();
			String title = lineScan.next();
			int port = Integer.parseInt(lineScan.next());
			if (!peer_file.contains(title)) {
				peer_file.add(title);
				bw.newLine();
				bw.write(readString);
			}
		}
		bw.close();
		
	}
	
	public void getrfcfile(Socket clientSocket, String filename) throws IOException {
		DataInputStream response_from_peer = new DataInputStream(clientSocket.getInputStream());
		String rfp = response_from_peer.readUTF();

		if (rfp.equals("exist")) {
			File file = new File(filename);
			if (!file.exists()) {
				byte[] filebyte = new byte[1000000000];
				InputStream is = clientSocket.getInputStream();
				FileOutputStream fo = new FileOutputStream(file);
				BufferedOutputStream bos = new BufferedOutputStream(fo);
				int byteread = is .read(filebyte, 0, filebyte.length);
				int currentToken = byteread;
				do {
					byteread = is.read(filebyte, currentToken, (filebyte.length-currentToken));
					if (byteread>=0) {
						currentToken += byteread;
					}
				} while(byteread > -1);
				bos.write(filebyte, 0, currentToken);
				bos.flush();
				bos.close();
				
				System.out.println("This peer has got the file");
			}
			
		}
		if (rfp.equals("Not_exist") ) {
			System.out.println("The requested peer doesn't have this file");
		}
	}
	
	

} 
