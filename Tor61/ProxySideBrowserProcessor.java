package tor61;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ProxySideBrowserProcessor extends Thread{
	Socket browserSocket;
	short streamID;
	byte[] receiveBuffer;
	int bufferSize = 512 - 14;
	
	public ProxySideBrowserProcessor(Socket browserSocket){
		// initiations and data goes here...
		this.browserSocket = browserSocket;
	}
	
	public void run(){
		try {
			
			//=== ======== Extract information from the http request  =============
			InputStream in;
			in = browserSocket.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			int lineCount = 0;
			int port = 80; // default
			String requestType = "";
			String host = "";
			String line = "";
			String request = "";
			String firstLine = "";
			while((line = br.readLine()) != null && !line.equals("")){
				lineCount++;
				String lineWithoutSpace = line.replace(" ", "").toLowerCase();
				if(lineCount == 1) {
					firstLine = line;
					request += line.replace("HTTP/1.1", "HTTP/1.0") + "\r\n";
					System.out.println(">>> " + line);
					requestType = line.split(" ")[0];
					String url = line.split(" ")[1];
					if(url.toLowerCase().startsWith("https//")){
						port = 443;
					}
				}else if(lineWithoutSpace.startsWith("host:")){ // host line
					request += line + "\r\n";
					String[] parts = lineWithoutSpace.substring(5).split(":");
					if(parts.length > 1) {
						port = Integer.valueOf(parts[1]).intValue();
					}else{
						int port_temp = Util.getPortFromURL(firstLine);
						if(port_temp >= 0){
							port = port_temp;
						}
					}
					host = parts[0];
				}else if(lineWithoutSpace.startsWith("connection:")){
					request += line.replace("keep-alive", "close") + "\r\n";
				}else if(lineWithoutSpace.startsWith("proxy-connection:")){
					request += line.replace("keep-alive", "close") + "\r\n";
				}else{
					request += line + "\r\n";
				
				}
			}
			
			if (host == "")	// if host is not found, returns
				return;
			// ====================================================================
			
			
			// ===================== Stream Creation ============================== 
			
			// update stream id table
			
			String host_ip = java.net.InetAddress.getByName(host).getHostName();
			byte[] relayBegin = Cell.relayBegin((short) 1, streamID, host_ip + ":" + port);
			
			Socket adjNodeSocket = Util.broAdjNodeSocket;
			
			Util.bufferTable.get(adjNodeSocket).add(relayBegin);	// add begin cell to the buffer
			
			// receive cell from adjacent node
			ByteBuffer received = ByteBuffer.wrap(Util.readMessageCell(in));
			
			// get cmd type from the received cell
			String cmdType = Util.RELAY_CMD_BYTE_MAP.get(received.get(2));
			
			if (cmdType.equals("CONNECTED")){ // stream created!
				
				// create buffer reader thread which reads buffer and writes to browser
				BufferReader bufferReader = new BufferReader(browserSocket);
				Util.bufferTable.put(browserSocket, bufferReader.queue);
				bufferReader.start();
				
				Util.streamIDtable.put(Pair.of(Pair.of((short) 0, streamID), Util.broAdjNodeSocket), browserSocket);
				
				// send request to web site
				request += "\r\n";
				while(request.length() > bufferSize){
					String data = request.substring(0, bufferSize + 1);
					byte[] relayData = Cell.relayData((short) 1, streamID, data.getBytes());
					Util.bufferTable.get(adjNodeSocket).add(relayData);
					request = request.substring(bufferSize);
				}
				byte[] relayData = Cell.relayData((short) 1, streamID, request.getBytes());
				Util.bufferTable.get(adjNodeSocket).add(relayData);
				
				while(!browserSocket.isClosed()){
					byte[] buffer = new byte[bufferSize];
					int read = 0;
					boolean shouldClose = false;
					while(read < bufferSize){
						int length = in.read(buffer, read, bufferSize - read);
						read += length;
						if(length == -1)
							shouldClose = true;
							break;
					}
					byte[] dataCell = Cell.relayData((short) 1, streamID, buffer);
					Util.bufferTable.get(Util.broAdjNodeSocket).add(dataCell);
					
					if (shouldClose) {
						browserSocket.close();
						// Util.bufferTable.remove(serverSocket);
						byte[] endCell = Cell.relayEnd((short) 1, streamID);
						Util.bufferTable.get(Util.broAdjNodeSocket).add(endCell);
						Util.streamIDtable.remove(Pair.of(Pair.of((short) 1, streamID), browserSocket));
					}
				}
						
			} else if (cmdType.equals("BEGIN FAILED")) {
				System.err.println("********BEGIN FAILED*********");
			} else {
				System.err.println("FATAL ERROR");
			}
			
			// ====================================================================
					
			
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			System.out.println(e1.getMessage());
		}
	}
		
//	public void sendData(BufferedReader br, ConcurrentLinkedQueue<byte[]> targetBuffer){
//		byte[] relayData;
//		String line = "";
//		try {
//			while((line = br.readLine()) != null && !line.equals("")){
//				relayData = Cell.relayData((short) 1, streamID, line.getBytes());
//				targetBuffer.add(relayData);
//			}
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			System.out.println("Error when copying stream");
//			e.printStackTrace();
//		}
//	}
	
	private static int getPortFromURL(String line){
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