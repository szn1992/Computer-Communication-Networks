package tor61;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

public class Util {
	
	static int PROXY_SIDE_LISTENER = 0;
	static int ROUTER_SIDE_LISTENER = 1;
	static int openerID;
	static HashMap<Byte, String> CELL_TYPE_BYTE_MAP = getByteCellTypeMap();
	static HashMap<Byte, String> RELAY_CMD_BYTE_MAP = getByteRelayCmdMap();
	
	static Socket broAdjNodeSocket;
	
	static ConcurrentMap<Pair<Socket, Short>,Pair<Socket,Short>> routingTable = new ConcurrentHashMap<Pair<Socket, Short>,Pair<Socket,Short>>();
	static ConcurrentMap<Socket, ConcurrentLinkedQueue<byte[]>> bufferTable = new ConcurrentHashMap<Socket, ConcurrentLinkedQueue<byte[]>>();
	static ConcurrentMap<Pair<Pair<Short, Short>, Socket>, Socket> streamIDtable = new ConcurrentHashMap<Pair<Pair<Short, Short>, Socket>, Socket>();// circuitID, StreamID,incomingSocket,outgoingSocket
	static ConcurrentMap<Integer, Socket> agentIDTable = new ConcurrentHashMap<Integer, Socket>();
	
	public static boolean containCircuitID(short id, Socket socket){
		if(routingTable.containsKey(Pair.of(socket, id))){
			return true;
		}
		if(routingTable.containsValue(Pair.of(socket, id))){
			return true;
		}
		
		return false;
	}
	
  public static ArrayList<String> fetch(String name) {
	  List<String> list = new ArrayList<String>();
	  try {
          ProcessBuilder pb = new ProcessBuilder("python","fetch.py", name);
          //pb.inheritIO();
          Process p = pb.start();
          // read output, line by line
          BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
          String line;
          while ((line = in.readLine()) != null ) {
            System.out.println("Read: " + line);
            list.add(line);
          }
          //p.waitFor();
      }
      catch(Exception e) {
          e.printStackTrace();
      }
      return (ArrayList<String>) list;
  }
  
  public static void register(String port, String name, String data) {
     try {
         ProcessBuilder pb = new ProcessBuilder("python","registration_client.py", port, name, data);
         pb.inheritIO();
         Process p = pb.start();
     }
     catch(Exception e) {
         e.printStackTrace();
     }
  }
  
  public static String padding(int num, int length){
	  String num_s = Integer.toString(num);
	  for(int i = 0; i < length - num_s.length(); i++){
		  num_s = "0" + num_s;
	  }
	  return num_s;
  }
  
  public static HashMap<String, Byte> getCellTypeMap() {
	  HashMap<String, Byte> map = new HashMap<String, Byte>();
	  map.put("OPEN", (byte) 0x05);
	  map.put("OPENED", (byte) 0x06);
	  map.put("OPEN FAILED", (byte) 0x07);
	  map.put("CREATE", (byte) 0x01);
	  map.put("CREATED", (byte) 0x02);
	  map.put("CREATE FAILED", (byte) 0x08);
	  map.put("DESTROY", (byte) 0x04);
	  map.put("RELAY", (byte) 0x03);
	  
	  return map;
  }
  
  public static HashMap<String, Byte> getRelayCmdMap() {
	  HashMap<String, Byte> map = new HashMap<String, Byte>();
	  map.put("BEGIN", (byte) 0x01);
	  map.put("DATA", (byte) 0x02);
	  map.put("END", (byte) 0x03);
	  map.put("CONNECTED", (byte) 0x04);
	  map.put("EXTEND", (byte) 0x06);
	  map.put("EXTENDED", (byte) 0x07);
	  map.put("BEGIN_FAILED", (byte) 0x0b);
	  map.put("EXTEND_FAILED", (byte) 0x0c);
	  
	  return map;
  }
  
  public static HashMap<Byte, String> getByteCellTypeMap() {
	  HashMap<Byte, String> map = new HashMap<Byte, String>();
	  map.put((byte) 0x05, "OPEN");
	  map.put((byte) 0x06, "OPENED");
	  map.put((byte) 0x07, "OPEN_FAILED");
	  map.put((byte) 0x01, "CREATE");
	  map.put((byte) 0x02, "CREATED");
	  map.put((byte) 0x08, "CREATE_FAILED");
	  map.put((byte) 0x04, "DESTROY");
	  map.put((byte) 0x03, "RELAY");
	  
	  return map;
  }
  
  public static HashMap<Byte, String> getByteRelayCmdMap() {
	  HashMap<Byte, String> map = new HashMap<Byte, String>();
	  map.put((byte) 0x01, "BEGIN");
	  map.put((byte) 0x02, "DATA");
	  map.put((byte) 0x03, "END");
	  map.put((byte) 0x04, "CONNECTED");
	  map.put((byte) 0x06, "EXTEND");
	  map.put((byte) 0x07, "EXTENDED");
	  map.put((byte) 0x0b, "BEGIN_FAILED");
	  map.put((byte) 0x0c, "EXTEND_FAILED");
	  
	  return map;
  }
  
  // read message from the input stream
  // TODO: add timeout?
  public static byte[] readMessageCell(InputStream in){
	  byte[] buffer = new byte[512];
	  int read = 0;
	  try {
		  while(read < 512){
			  int length = in.read(buffer, read, 512 - read);
			  if(length == -1)
				  break;
			  read += length;
		  }
	  } catch (IOException e) {
		  System.out.println("Error when reading message from the input stream.");
		  e.printStackTrace();
	  }
	  return buffer;
  }
  
  public static int getPortFromURL(String line){
		String[] parts = line.split(" ");
		if(parts.length >= 2){
			String[] URLParts = parts[1].split(":");
			if(URLParts.length >= 2){
				try{
					int port = Integer.valueOf(URLParts[URLParts.length-1]).intValue();
					return port;
				}catch(NumberFormatException e){
					return -1;
				}
			}
		}
		return -1;			
	}
}
