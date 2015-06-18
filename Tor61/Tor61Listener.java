package tor61;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

// This is the listener of the Tor61
public class Tor61Listener extends Thread{
	int port;
	int type;
	short streamID;
	
	public Tor61Listener(int port, int type){
		// initiations and data goes here...
		this.port = port;
		this.type = type;
		this.streamID = 0;
	}
	
	@Override
	public void run(){
		//TODO: Question: Where should I close serverSocket?
		try {
			ServerSocket serverSocket = new ServerSocket(port);
			while (true) {
				Socket s = serverSocket.accept();
				if (type == Util.PROXY_SIDE_LISTENER) {
					streamID ++;
					ProxySideBrowserProcessor processor = new ProxySideBrowserProcessor(s);
					processor.start();
					processor.streamID = streamID;
				} else {
					BufferReader receivedSocketBuffer = new BufferReader(s);
					Util.bufferTable.put(s, receivedSocketBuffer.queue);
					receivedSocketBuffer.start();
					
					RouterSideProcessor processor = new RouterSideProcessor(s);
					processor.start();
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Error when creating server socket in proxy side");
			System.out.println(e.getMessage());
		}
	}
}
