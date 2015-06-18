package tor61;

import java.nio.ByteBuffer;
import java.util.HashMap;

public class Cell {
	public final static byte OPEN = 0x05;
	public final static byte OPENED = 0x06;
	public final static byte OPEN_FAILED = 0x07;
	public final static byte CREATE = 0x01;
	public final static byte CREATED = 0x02;
	public final static byte CREATE_FAILED = 0x08;
	public final static byte DESTROY = 0x04;
	public final static byte RELAY = 0x03;

	public final static byte BEGIN = 0x01;
	public final static byte DATA = 0x02;
	public final static byte END = 0x03;
	public final static byte CONNECTED = 0x04;
	public final static byte EXTEND = 0x06;
	public final static byte EXTENDED = 0x07;
	public final static byte BEGIN_FAILED = 0x0b;
	public final static byte EXTEND_FAILED = 0x0c;
	
	public final static int CELL_SIZE = 512;
	
	static HashMap<String, Byte> CELL_TYPE_MAP = Util.getCellTypeMap();
	static HashMap<String, Byte> RELAY_CMD_MAP = Util.getRelayCmdMap();
	
	// open
	public static byte[] open(short circuitID, int openerID, int openedID){
		ByteBuffer bb = ByteBuffer.allocate(512);
		bb.putShort(circuitID);
		bb.put(OPEN);
		bb.putInt(openerID);
		bb.putInt(openedID);
		return  bb.array();
	}
	
	// opened
	public static byte[] opened(byte[] open){
		ByteBuffer bb = ByteBuffer.wrap(open);
		short circuitID = bb.getShort(0);
		int openerID = bb.getInt(3);
		int openedID = bb.getInt(7);
		
		ByteBuffer result = ByteBuffer.allocate(512);
		result.putShort(circuitID);
		result.put(OPENED);
		result.putInt(openerID);
		result.putInt(openedID);
		return  result.array();
	}
	
	// open failed
	public static byte[] openFailed(byte[] open){
		ByteBuffer bb = ByteBuffer.wrap(open);
		short circuitID = bb.getShort(0);
		int openerID = bb.getInt(3);
		int openedID = bb.getInt(7);
		
		ByteBuffer result = ByteBuffer.allocate(512);
		result.putShort(circuitID);
		result.put(OPEN_FAILED);
		result.putInt(openerID);
		result.putInt(openedID);
		return result.array();
	}
	
	// create
	public static byte[] create(short circuitID){
		ByteBuffer bb = ByteBuffer.allocate(512);
		bb.putShort(circuitID);
		bb.put(CREATE);
		return bb.array();
	}
	
	// created
	public static byte[] created(short circuitID){
		ByteBuffer bb = ByteBuffer.allocate(512);
		bb.putShort(circuitID);
		bb.put(CREATED);
		return bb.array();
	}
	
	// created
	public static byte[] created(byte[] create){
		ByteBuffer bb = ByteBuffer.wrap(create);
		short circuitID = bb.getShort(0);
		
		ByteBuffer result = ByteBuffer.allocate(512);
		result.putShort(circuitID);
		result.put(CREATED);
		return result.array();
	}
	
	// create failed
	public static byte[] createFailed(short circuitID){
		ByteBuffer bb = ByteBuffer.allocate(512);
		bb.putShort(circuitID);
		bb.put(CREATE_FAILED);
		return bb.array();
	}
	
	public static byte[] destroy(short circuitID){
		ByteBuffer bb = ByteBuffer.allocate(512);
		bb.putShort(circuitID);
		bb.put(DESTROY);
		return bb.array();
	}
	
	
	// relay begin
	public static byte[] relayBegin(short circuitID, short streamID, String address){		
		ByteBuffer bb = ByteBuffer.allocate(512);
		bb.putShort(circuitID);
		bb.put(RELAY);
		bb.putShort(streamID);
		bb.putShort((short) 0);
		bb.putInt(0);
		bb.putShort((short) address.length());
		bb.put(BEGIN);
		bb.put((address + "\0").getBytes());
		return  bb.array();
	}
	
	// relay data
	public static byte[] relayData(short circuitID, short streamID, byte[] data){		
		ByteBuffer bb = ByteBuffer.allocate(512);
		bb.putShort(circuitID);
		bb.put(RELAY);
		bb.putShort(streamID);
		bb.putShort((short) 0);
		bb.putInt(0);
		bb.putShort((short) data.length);
		bb.put(DATA);
		bb.put(data);
		
		if (bb.array().length > 512)
			System.err.println("data is too long motherfuck");
		return  bb.array();
	}
	
	// relay end
	public static byte[] relayEnd(short circuitID, short streamID){		
		ByteBuffer bb = ByteBuffer.allocate(512);
		bb.putShort(circuitID);
		bb.put(RELAY);
		bb.putShort(streamID);
		bb.putShort((short) 0);
		bb.putInt(0);
		bb.putShort((short) 0);
		bb.put(END);
		return bb.array();
	}
	
	// relay connected
	public static byte[] relayConnected(short circuitID, short streamID){		
		ByteBuffer bb = ByteBuffer.allocate(512);
		bb.putShort(circuitID);
		bb.put(RELAY);
		bb.putShort(streamID);
		bb.putShort((short) 0);
		bb.putInt(0);
		bb.putShort((short) 0);
		bb.put(CONNECTED);
		return bb.array();
	}
	
	// relay extend
	public static byte[] relayExtend(short circuitID, short streamID, String address, int agentID){		
		String body = address + "\0" + agentID;
		ByteBuffer bb = ByteBuffer.allocate(512);
		bb.putShort(circuitID);
		bb.put(RELAY);
		bb.putShort(streamID);
		bb.putShort((short) 0);
		bb.putInt(0);
		bb.putShort((short) body.length());
		bb.put(EXTEND);
		bb.put(body.getBytes());
		return bb.array();
	}
	
	// relay extended
	public static byte[] relayExtended(short circuitID, short streamID){		
		ByteBuffer bb = ByteBuffer.allocate(512);
		bb.putShort(circuitID);
		bb.put(RELAY);
		bb.putShort(streamID);
		bb.putShort((short) 0);
		bb.putInt(0);
		bb.putShort((short) 0);
		bb.put(EXTENDED);
		return bb.array();
	}
	
	// begin failed
	public static byte[] beginFailed(short circuitID, short streamID){		
		ByteBuffer bb = ByteBuffer.allocate(512);
		bb.putShort(circuitID);
		bb.put(RELAY);
		bb.putShort(streamID);
		bb.putShort((short) 0);
		bb.putInt(0);
		bb.putShort((short) 0);
		bb.put(BEGIN_FAILED);
		return bb.array();
	}
	
	// extend failed
	public static byte[] extendFailed(short circuitID, short streamID){		
		ByteBuffer bb = ByteBuffer.allocate(512);
		bb.putShort(circuitID);
		bb.put(RELAY);
		bb.putShort(streamID);
		bb.putShort((short) 0);
		bb.putInt(0);
		bb.putShort((short) 0);
		bb.put(EXTEND_FAILED);
		return bb.array();
	}
}
