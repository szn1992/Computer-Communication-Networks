package tor61;

import java.nio.ByteBuffer;

public class Testing {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		byte[] b = Cell.create((short) 1);
		ByteBuffer c = ByteBuffer.wrap(b);
		System.out.print(c.getShort(0));
		System.out.print(c.get(2));
		
		String s = "hahalalalaladfasd";
		System.out.println(s.length());
		byte[] bytes = s.getBytes();
		System.out.println(bytes.length);
		
	}
}
