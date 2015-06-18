package tor61;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;


public class Tor61 {

	static int PROXY_PORT;
	static int ROUTER_PORT;
	static String NAME = "Tor61Router-";
	static int INSTANCE_NUM;
	static String GROUP;
	static String PREFIX = "Tor61";
	//static HashMap<String, Byte> CELL_TYPE_MAP; 	// map cell command to byte version
	//static HashMap<String, Byte> RELAY_CMD_MAP;		// map relay cell command to byte version
	static HashMap<Byte, String> CELL_TYPE_BYTE_MAP = Util.getByteCellTypeMap();
	static HashMap<Byte, String> RELAY_CMD_BYTE_MAP = Util.getByteRelayCmdMap();
	
	public static void main(String[] args) throws NumberFormatException, UnknownHostException, IOException {
		if (args.length != 3) {
	    	System.err.println("Usage: java Tor61 <group number> <instance number> <HTTP Proxy port>");
	        System.exit(1);
	   }
		
		// get info from arguments
		GROUP = args[0];
		INSTANCE_NUM  = Integer.valueOf(args[1]);
		PROXY_PORT = Integer.valueOf(args[2]);
		ROUTER_PORT = PROXY_PORT ++; // TODO: Find a better way to set port.
		
		String instanceNum_s = Util.padding(INSTANCE_NUM, 4);
		
		int data = (Integer.valueOf(GROUP) << 16) | INSTANCE_NUM;	// DATA OR agent id
		Util.openerID = data;
		
		// register this router at registration service
		Util.register("" + ROUTER_PORT, NAME + GROUP + "-" + instanceNum_s, "" + data);
		
		// fetch information of routers
		ArrayList<String> routersAll = Util.fetch(NAME + GROUP);
		int routersNum = routersAll.size();
		
		//ArrayList<String> routersSelected = new ArrayList<String>();
		Random rand = new Random();
		
		// ======================== circuit creation =======================
		
		String[] info = routersAll.get(rand.nextInt(routersNum)).split(" "); //randomly pick one and extract info
		String ip = info[0];
		int port = Integer.valueOf(info[1]);
		int registrationData = Integer.valueOf(info[2]);
		System.out.println("Check point1, first node information: " + ip + " " + port);
		
		// Here, I try to connect to the first node without checking if there is a 
		// TCP connection, fine when create circuit for the first time, but need to 
		// change when circuit creation need to be done multiple times
		Socket s = new Socket(ip, port);
		Util.broAdjNodeSocket = s;
		byte[] openCell = Cell.open((short)0, data, registrationData);
		s.getOutputStream().write(openCell); // send the open cell
		
		InputStream in = s.getInputStream();
		ByteBuffer received = ByteBuffer.wrap(Util.readMessageCell(in));
		if(CELL_TYPE_BYTE_MAP.get(received.get(2)).equals("OPENED")){ // first node successfully opened
			// create a circuit
			short circuitID = 1; // hard code the first circuit id to be 1
			byte[] createCell = Cell.create(circuitID);
			s.getOutputStream().write(createCell);
			received = ByteBuffer.wrap(Util.readMessageCell(in));
			if(CELL_TYPE_BYTE_MAP.get(received.get(2)).equals("CREATED")){ // circuit to first node successfully created
				int count = 0;
				while(count < 2){
					String[] infoNext = routersAll.get(rand.nextInt(routersNum)).split(" ");
					String ipNext = infoNext[0];
					int portNext = Integer.valueOf(infoNext[1]);
					int registrationDataNext = Integer.valueOf(infoNext[2]);
					String address = ipNext + ":" + portNext;
					byte[] extendCell = Cell.relayExtend((short) 1, (short) 0, address, registrationDataNext);
					s.getOutputStream().write(extendCell);
					received = ByteBuffer.wrap(Util.readMessageCell(in));
					if(CELL_TYPE_BYTE_MAP.get(received.get(2)).equals("EXTENDED")){
						count++;
					}
				}			
			}else{
				// do something
			}
		}else{
			// do something?
			s.close();
			// and loop back again
		}
		
		Tor61Listener browserListener = new Tor61Listener(PROXY_PORT, Util.PROXY_SIDE_LISTENER);
		browserListener.start();
		Tor61Listener routerListener = new Tor61Listener(ROUTER_PORT, Util.ROUTER_SIDE_LISTENER);
		routerListener.start();
	   
    }	
}
