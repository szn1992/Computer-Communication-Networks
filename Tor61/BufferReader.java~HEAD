package tor61;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BufferReader extends Thread{
	Socket socket;				// socket that reads this buffer 
	ConcurrentLinkedQueue<byte[]> queue;	// buffer
	
	public BufferReader(Socket s){
		this.socket = s;
		queue = new ConcurrentLinkedQueue<byte[]>();
		Util.bufferTable.put(socket, queue);
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

