import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.zip.CRC32;

public class FileSender {

	enum State {
		SEND_PACKET, WAIT_FOR_ACK, DONE;
	}

	enum Doing {
		SENDING_TO_RECEIVER, RECEIVED_ACK_FROM_RECEIVER, FOUND_NOTHING_LEFT;
	}

	private DatagramSocket socket;
	private File f;
	private InetAddress adr;
	private final int port = 8000;
	private int sequenceNumber = 0;

	// SENDER NECESSARY
	private byte[] buf;
	private int length = 1388;
	private int offset;
	private DatagramPacket packet;

	// RECEIVED FROM RECEIVER
	private DatagramPacket fromServer;
	private byte[] fromServerBuf = new byte[1];

	private State currentState;
	private Transition[][] transitions;

	private int receivedFromServer;
	private byte[] sendFileByte;


	/**
	 * Start the Automat for Sender. Starts automat in SEND_PACKET state. Also start up socket for Sender.
	 *
	 * @param file    The file which will be used.
	 * @param address The destination where the file will be send to.
	 */
	public FileSender(String file, String address) {
		try {
			currentState = State.SEND_PACKET;
			transitions = new Transition[State.values().length][Doing.values().length];
			transitions[State.SEND_PACKET.ordinal()][Doing.SENDING_TO_RECEIVER.ordinal()] = new Send_To_Wait();
			transitions[State.WAIT_FOR_ACK.ordinal()][Doing.RECEIVED_ACK_FROM_RECEIVER.ordinal()] = new Wait_To_Send();
			transitions[State.SEND_PACKET.ordinal()][Doing.FOUND_NOTHING_LEFT.ordinal()] = new Send_To_Close();
			socket = new DatagramSocket(port);
			f = new File(file);
			Path path = Paths.get(f.getAbsolutePath());
			buf = Files.readAllBytes(path);
			adr = InetAddress.getByName(address);
		} catch (IOException e) {
			System.err.println("Socket Exception");
		}
	}

	/**
	 * Sends the packet. Puts current state to WAIT_FOR_ACK;
	 *
	 * @param offset
	 */
	public void send(int offset) {
		try {
			socket.setSoTimeout(30000);
			if (currentState == State.SEND_PACKET) {
				socket.setSoTimeout(10000);
				sendFileByte = createPacket();
				packet = new DatagramPacket(sendFileByte , sendFileByte.length , adr , port);
				if (offset >= getFileLength()) {
					process(Doing.FOUND_NOTHING_LEFT);
					//beende Programm
				}
				socket.send(packet);
				process(Doing.SENDING_TO_RECEIVER);
				waitForACK();
			} else if (currentState == State.WAIT_FOR_ACK) {
				socket.send(getPacket());
				waitForACK();
			}
		} catch (SocketException e) {
			System.err.println("No receiver connected with sender");
		} catch (IOException e) {
			System.err.println("bye");
			process(Doing.FOUND_NOTHING_LEFT);
		}
	}

	public void waitForACK() {
		if (currentState == State.WAIT_FOR_ACK) {
			try {
				socket.setSoTimeout(15000);
				fromServer = new DatagramPacket(fromServerBuf, fromServerBuf.length);
				socket.receive(fromServer);
				receivedFromServer = fromServer.getData()[0];
				if(receivedFromServer == 1) {
					offset = setOffset();
					process(Doing.RECEIVED_ACK_FROM_RECEIVER);
				} else {
					send(getOffset());
				}
			} catch (SocketTimeoutException e) {
				send(getOffset());
			}
			catch (IOException e) {
				System.out.println("Receive failed");
				send(getOffset());
			}
		}
	}



	private byte[] createPacket() {
		if ((getFileLength() - offset) >= 1388) {
			byte byteArray[] = Arrays.copyOfRange(buf, offset, offset + length);
			byteArray = appendSequenceNumber(byteArray);
			byteArray = appendChecksum(byteArray);
			return byteArray;
		}
		else {
			byte byteArray[] = Arrays.copyOfRange(buf, offset, getFileLength() - offset);
			byteArray = appendSequenceNumber(byteArray);
			byteArray = appendChecksum(byteArray);
			return byteArray;
		}
	}

	private byte[] appendChecksum(byte[] data) {
		CRC32 crc = new CRC32();
		crc.update(data);

		byte[] checksum = longToBytes(crc.getValue());

		data = concat(checksum,data);

		return data; 
	}

	private byte[] concat(byte[] checksum, byte[] data) {

		int checksumLen = checksum.length;
		int dataLen = data.length;
		byte[] result = new byte[checksumLen + dataLen];
		System.arraycopy(checksum, 0, result, 0, checksumLen);
		System.arraycopy(data, 0, result, checksumLen, dataLen);
		return result;
	}

	private byte[] longToBytes(long l) {
		byte b[] = new byte[8];

		ByteBuffer buf = ByteBuffer.wrap(b);
		buf.putLong(l);
		return b;

	}

	private byte[] appendSequenceNumber(byte[] data) {
		ByteBuffer buffer = ByteBuffer.allocate(4);
		buffer.putInt(sequenceNumber);
		byte[] packetSequence = buffer.array();

		data = concat(packetSequence,data);

		sequenceNumber++;

		return data;
	}

	private int getOffset() {
		return offset;
	}

	private int setOffset() {
		return offset + length;
	}

	public int getLength() {
		return length;
	}

	public int getFileLength() {
		return buf.length;
	}

	private DatagramPacket getPacket() {
		return packet;
	}

	public void process(Doing input) {
		System.out.println("INFO Received " + input + " in state " + currentState);
		Transition trans = transitions[currentState.ordinal()][input.ordinal()];
		if (trans != null) {
			currentState = trans.execute(input);
		}
		System.out.println("current state: " + currentState);
	}

	abstract class Transition {
		abstract public State execute(Doing input);
	}

	class Send_To_Wait extends Transition {
		@Override
		public State execute(Doing input) {
			System.out.println("SENDING_TO_RECEIVER");
			return State.WAIT_FOR_ACK;
		}
	}

	class Wait_To_Send extends Transition {
		@Override
		public State execute(Doing input) {
			System.out.println("Got an ACK. Sending packet again");
			return State.SEND_PACKET;
		}
	}

	class Send_To_Close extends Transition {
		@Override
		public State execute(Doing input) {
			System.out.println("No more packet available. Done with process");
			return State.DONE;
		}
	}
}