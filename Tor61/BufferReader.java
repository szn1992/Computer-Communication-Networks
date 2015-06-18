package tor61;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BufferReader extends Thread{
	Socket socket;
	ConcurrentLinkedQueue<byte[]> queue = new ConcurrentLinkedQueue<byte[]>();
	
	public BufferReader(Socket s){
		this.socket = s;
	}
	
	@Override
	public void run(){
		while(!socket.isClosed()){
			byte[] message = queue.poll();
			if(message != null){
				try {
					socket.getOutputStream().write(message);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					System.out.println("Error when trying to send message in buffer");
					e.printStackTrace();
				}
			}
		}
	}
}
