package tor61;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.ByteBuffer;

public class RouterSideProcessor extends Thread{
	public final int CELL_SIZE = 512;
	Socket receivedSocket;
	
	public RouterSideProcessor(Socket s){
		// initiations and data goes here...
		this.receivedSocket = s;
	}
	
	public void run(){
		try {
			InputStream in = receivedSocket.getInputStream();
			
			// ================= 此处应有 while 包裹 0v0 =============
			while(true){
			byte[] message = Util.readMessageCell(in);
			if (message.length == 0)
				break;
			
			System.out.println("Readed message: " + message.toString());
			ByteBuffer messageBuffer = ByteBuffer.wrap(message);
			short circuitID = messageBuffer.getShort(0);
			Pair<Socket, Short> incoming = Pair.of(receivedSocket, circuitID);
			byte type = messageBuffer.get(2);
			String messageType = Util.CELL_TYPE_BYTE_MAP.get(type);
			System.out.println("Get Cell: " + messageType);
			// ====================== open =========================
			if(messageType == "OPEN"){
				int agentID = messageBuffer.getInt(3);
				byte[] openedCell = Cell.opened(message);
				Util.agentIDTable.put(agentID, receivedSocket);
				Util.bufferTable.get(receivedSocket).add(openedCell);
			// ====================== opened =======================
			}else if (messageType == "OPENED"){
				// send create cell to the node
				if(Util.routingTable.containsKey(incoming)){
					byte[] createCell = Cell.create(circuitID);
					Util.bufferTable.get(receivedSocket).add(createCell);
				}else{
					System.out.println("I don't think this should happen");
				}
			} else if (messageType == "OPEN_FAILED"){
				byte[] extendFailedCell = Cell.extendFailed(circuitID, (short) 0);
				forwardMessage(incoming, extendFailedCell);
				Util.routingTable.remove(incoming);
				
			// ==================== create =========================
			}else if(messageType == "CREATE"){

				byte[] createdCell = Cell.created(message);
				Util.bufferTable.get(receivedSocket).add(createdCell);
			// ==================== created =========================
			}else if (messageType == "CREATED"){
				if(Util.routingTable.containsKey(incoming)){
					byte[] extendedCell = Cell.relayExtended((short)0, (short)0);
					forwardMessage(incoming, extendedCell);
					Util.routingTable.put(Util.routingTable.get(incoming),incoming);
				}else{
					System.out.println("I don't think this should happen");
				}
			// ==================== create failed =========================
			}else if (messageType == "CREATE_FAILED"){
				byte[] extendFailedCell = Cell.extendFailed(circuitID, (short) 0);
				forwardMessage(incoming, extendFailedCell);
				Util.routingTable.remove(incoming);
			// ==================== destroy =========================
			}else if (messageType == "DESTROY"){
				Socket outgoingSocket = Util.routingTable.get(incoming).getFirst();
				short outgoingCircuitID = Util.routingTable.get(incoming).getSecond();
				byte[] outgoingDestroy = Cell.destroy(outgoingCircuitID);
				Util.bufferTable.get(outgoingSocket).add(outgoingDestroy);
				Util.routingTable.remove(incoming);
			}else if(messageType == "RELAY"){
				short streamID = messageBuffer.getShort(3);
				byte relay = messageBuffer.get(14);
				int length = messageBuffer.getShort(11);
				String relayType = Util.RELAY_CMD_BYTE_MAP.get(relay);
				System.out.println("The relay type is " + relayType);
				// ==================== relay begin =========================
				if(relayType == "BEGIN"){
					byte[] address = new byte[length];
					messageBuffer.get(address, 14, length);
					String address_s = address.toString();
					if(Util.routingTable.containsKey(incoming)){
						System.out.println("In relay begin, not the last cell");
						forwardMessage(incoming, message);
					}else{
						System.out.println("In relay begin, the last cell, create connection");
						// ====create connection to server====
						String ip = address_s.split(":")[0];
						int port = Integer.valueOf(address_s.split(":")[1]);
						Socket serverSocket = new Socket(ip, port);
						ProxySideServerProcessor p = new ProxySideServerProcessor(serverSocket, receivedSocket);
						Util.streamIDtable.put(Pair.of(Pair.of(circuitID, streamID),receivedSocket), serverSocket);
						p.circuitID = circuitID;
						p.streamID = streamID;
						p.start();
						// ===================================					
					}
				// ==================== relay data =========================
				}else if (relayType == "DATA"){
					if(Util.routingTable.containsKey(incoming)){
						forwardMessage(incoming, message);
					}else{
						Socket outgoingSocket = Util.streamIDtable.get(Pair.of(Pair.of(circuitID, streamID), receivedSocket));
						byte[] data = new byte[length];
						messageBuffer.get(data, 14, length);
						Util.bufferTable.get(outgoingSocket).add(data);
					}
				// ==================== end =========================
				}else if (relayType == "END"){
					if(Util.routingTable.containsKey(incoming)){
						forwardMessage(incoming, message);
					}else{
						Util.streamIDtable.get(Pair.of(Pair.of(circuitID, streamID),receivedSocket)).close(); // close the socket?
						Util.streamIDtable.remove(Pair.of(Pair.of(circuitID, streamID),receivedSocket));
					}		
				// ==================== connected =========================
				}else if (relayType == "CONNECTED"){
					if(Util.routingTable.containsKey(incoming)){
						forwardMessage(incoming, message);
					}else{
						System.out.println("I don't think it is possible to get a conncected here 0 0 we  will see");
					}
				// ==================== relay-extend =========================
				}else if (relayType == "EXTEND"){
					if(Util.routingTable.containsKey(incoming)){
						forwardMessage(incoming, message);
					}else{
						// ======extend to the next node====
						byte[] data = new byte[length];
						messageBuffer.get(data, 14, length);
						String address = data.toString().split("\0")[0];
						int agentID = Integer.valueOf(data.toString().split("\0")[1]);
						if(Util.agentIDTable.containsKey(agentID)){
							Socket target = Util.agentIDTable.get(agentID);
							byte[] createCell = Cell.create(circuitID);
							Util.bufferTable.get(target).add(createCell);
							short targetCid = (short) (Math.random() * Short.MAX_VALUE);
							while(targetCid == 0 || Util.containCircuitID(targetCid, target)){
								targetCid = (short) (Math.random() * Short.MAX_VALUE);
							}
							Util.routingTable.put(Pair.of(target, targetCid), Pair.of(receivedSocket, circuitID));
						}else{ // no TCP connection between this node and next node
							// open TCP connection, send open cell, send create cell
							String ip = address.split(" ")[0];
							int port = Integer.valueOf(address.split(" ")[1]);
							Socket socket = new Socket(ip,port);
							
							BufferReader bf = new BufferReader(socket);						
							Util.bufferTable.put(socket, bf.queue);
							bf.start();				
							RouterSideProcessor processor = new RouterSideProcessor(socket);
							processor.start();
							
							short newCid = (short) (Math.random() * Short.MAX_VALUE);
							while(newCid == 0 || Util.containCircuitID(newCid, socket)){
								newCid = (short) (Math.random() * Short.MAX_VALUE);
							}
							
							byte[] openCell = Cell.open(newCid, Util.openerID, agentID);
							bf.queue.add(openCell);	
							Util.routingTable.put(Pair.of(socket, newCid), Pair.of(receivedSocket, circuitID));
						}
						// =================================
					}
				// ==================== relay-extended =========================
				}else if (relayType == "EXTENDED"){
					if(Util.routingTable.containsKey(incoming)){
						forwardMessage(incoming, message);
					}else{
						System.out.println("I don't think it is possible to get a extended here 0 0 we  will see");
					}
				// ==================== relay-begin-failed =========================
				}else if (relayType == "BEGIN_FAILED"){
					System.out.println("BEGIN_FAILED");
				// ==================== relay-extend-failed =========================
				}else if (relayType == "EXTEND_FAILED"){
					System.out.println("BEGIN_FAILED");
				}else{
					System.out.println("relay type unknow, you must made some mistakes!");
				}
			}
			}
			// ==================================================
		   
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private void forwardMessage(Pair<Socket, Short> incoming, byte[] message){
		System.out.println("Forwarding...");
		Socket socket = Util.routingTable.get(incoming).getFirst();
		short circuitID = Util.routingTable.get(incoming).getSecond();
		
		byte[] newMessage = message.clone();
		ByteBuffer newMessageBuf = ByteBuffer.wrap(newMessage);
		newMessageBuf.putShort(0, circuitID);
		
		Util.bufferTable.get(socket).add(newMessageBuf.array());
	}
}
