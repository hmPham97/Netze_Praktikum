import javax.xml.crypto.Data;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.zip.CRC32;

public class FileSender {

	enum State {
		SEND_PACKET, WAIT_FOR_ACK, DONE;
	}

	enum Doing {
		SENDING_TO_RECEIVER, RECEIVED_ACK_FROM_RECEIVER, FOUND_NOTHING_LEFT;
	}

	public int manipulated;
	int chanceTwoTimes = 0;
	int chanceChange = 0;
	int chanceDelete = 50;
	String twoTimes = "twoTimes";
	String change = "change";
	String delete = "delete";
	String normal = "normal";
	ArrayList<String> arr = new ArrayList<String>();

	private int endPacketSize = 1400;
	private DatagramSocket socket;
	private File f;
	private InetAddress adr;
	private final int port = 9001;
	private final int portSendingToReceiver = 9002;
	private int sequenceNumber = 0;

	// SENDER NECESSARY
	private byte[] buf;
	private int length = 1388;
	private int offset = 0;
	private DatagramPacket packet;
    private DatagramPacket savepacket;
	// RECEIVED FROM RECEIVER
	private DatagramPacket fromServer;
	private byte[] fromServerBuf = new byte[1];

	private State currentState;
	private Transition[][] transitions;

	private int receivedFromServer;
	private byte[] sendFileByte;


	/**Start the Automat for Sender. Starts automat in SEND_PACKET state. Also start up socket for Sender.
	 * @param file    The file which will be used.
	 * @param address The destination where the file will be send to.
	 */
	public FileSender(String file, String address) {
		try {
			String name = file.substring(file.lastIndexOf("\\") + 1);
			initArr();
			currentState = State.SEND_PACKET;
			transitions = new Transition[State.values().length][Doing.values().length];
			transitions[State.SEND_PACKET.ordinal()][Doing.SENDING_TO_RECEIVER.ordinal()] = new Send_To_Wait();
			transitions[State.WAIT_FOR_ACK.ordinal()][Doing.RECEIVED_ACK_FROM_RECEIVER.ordinal()] = new Wait_To_Send();
			transitions[State.SEND_PACKET.ordinal()][Doing.FOUND_NOTHING_LEFT.ordinal()] = new Send_To_Close();
			f = new File(file);
			Path path = Paths.get(f.getAbsolutePath());
			buf = Files.readAllBytes(path);
			adr = InetAddress.getByName(address);
            socket = new DatagramSocket(port);
            sendFileName(name.getBytes());
            send(offset);
		} catch (IOException e) {
			System.err.println("Socket Exception or invalid file was given");
		}
	}

	private void sendFileName(byte[] name) {
	    try {
	        CRC32 check = new CRC32();
	        check.reset();
	        check.update(name);
	        long v = check.getValue();
	        ByteBuffer f = ByteBuffer.allocate(8);
	        f.putLong(v);
	        byte[] whole = new byte[f.array().length + name.length];
	        System.arraycopy(f.array(), 0, whole, 0, f.array().length);
	        System.arraycopy(name, 0, whole, f.array().length, name.length);
            DatagramPacket containsName = new DatagramPacket(whole, whole.length, adr, portSendingToReceiver);
            DatagramPacket gotACK = new DatagramPacket(new byte[10], 10);
            socket.send(containsName);
            socket.receive(gotACK);
            System.out.println(gotACK.getData()[0]);
            if(gotACK.getData()[0] == 0) {
                sendFileName(name);
            }
        } catch (IOException e) {
            System.err.println("Couldn't send the file name");
        }
    }

	/**Sends the packet. Puts current state to WAIT_FOR_ACK;
	 * @param offset
	 */
	private void send(int offset) {
		try {
			if (currentState == State.SEND_PACKET) {
				socket.setSoTimeout(10000);
				sendFileByte = createPacket();
				packet = new DatagramPacket(sendFileByte , sendFileByte.length , adr , portSendingToReceiver);
                savePacket(packet);
                if (offset >= buf.length) {
					//process(Doing.FOUND_NOTHING_LEFT);
					socket.close();
				}
                System.out.println("in SEND_PACKET " + getPacket().getData()[4] + "    " + getPacket().getData()[5] + "    " + getPacket().getData()[6] +"    " + getPacket().getData()[7]);
                decideSend(packet);
				sequenceNumber++;
				process(Doing.SENDING_TO_RECEIVER);
				waitForACK();
			} else if (currentState == State.WAIT_FOR_ACK) {
                System.out.println(getPacket().getData()[manipulated]);
                decideSend(getPacket());
                System.out.println(getPacket().getData()[4] + "    " + getPacket().getData()[5] + "    " + getPacket().getData()[6] +"    " + getPacket().getData()[7]);
				waitForACK();
			}
		} catch (SocketException e) {
			System.err.println("No receiver connected with sender");
		} catch (IOException | TimeoutException e) {
			System.err.println("bye");
			process(Doing.FOUND_NOTHING_LEFT);
			socket.close();
		}
	}
	
	/**holt sich aus dem array arr der groesse 100 einen zufaelligen string. je nach string wird das paket manipuliert und versendet,
	 * nicht versendet, versendet oder doppelt versendet
	 * @param packet2
	 * @throws IOException
	 */
	private void decideSend(DatagramPacket packet2) throws IOException {
		int rnd = new Random().nextInt(100);
		String current = arr.get(rnd);
		if (current.equalsIgnoreCase(normal)) {
            System.out.println(normal);
			socket.send(packet2);
        }
		else if (current.equalsIgnoreCase(twoTimes)) {
            System.out.println("two times");
			socket.send(packet2);
			socket.send(packet2);
		}
		else if (current.equalsIgnoreCase(change)) {
            System.out.println("change");
			byte[] wrongFileByte = manipulate(sendFileByte);
            System.out.println(manipulated);
            System.out.println(sendFileByte[manipulated]);
			DatagramPacket wrongPacket = new DatagramPacket(wrongFileByte , wrongFileByte.length , adr , portSendingToReceiver);
            System.out.println(wrongFileByte[manipulated]);
			socket.send(wrongPacket);
        } else if (current.equalsIgnoreCase(delete)){
            System.out.println("delete");
        }
	}

	/**manipuliert einzelne bits von jedem Byte (12-1400) des Byte Arrays. Byte 0-11 enthaelt die Sequenznummer und Checksumme.
	 * Diese sollen nicht manipuliert werden.
	 * @param sendFileByte2
	 * @return
	 */
	private byte[] manipulate(byte[] sendFileByte2) {
		byte[] result = Arrays.copyOfRange(sendFileByte2, 0, sendFileByte2.length);
        System.out.println(result.length);
        int rnd = new Random().nextInt(endPacketSize);
        byte i = result[rnd];
        if (i != 0) {
            result[rnd] = 0;
        } else {
            result[rnd] = 1;
        }
        manipulatedBit(rnd);
		return result;
	}

	/**Sender wartet auf eine Antwort des Receivers
	 */
	private void waitForACK() {
		if (currentState == State.WAIT_FOR_ACK) {
			try {
				socket.setSoTimeout(1000);
				fromServer = new DatagramPacket(fromServerBuf, fromServerBuf.length);
				socket.receive(fromServer);
				receivedFromServer = fromServer.getData()[0];
				if(receivedFromServer == 1) {
                    offset = offset + length;
					process(Doing.RECEIVED_ACK_FROM_RECEIVER);
					send(offset);
				} else {
                    send(offset);
				}
			} catch (SocketTimeoutException e) {
			    System.err.println("timedout");
                send(offset);
			}
			catch (IOException e) {
				System.out.println("Receive failed");
                send(offset);
			}
		}
	}

	/**erzeugt das Paket das versendet wird. Byte 0-7 = Checksumme. Byte 8-11 = Sequenznummer. Byte 12-1400 = Daten.
	 * @return
	 */
	private byte[] createPacket() throws TimeoutException{
		byte byteArray[];
        if ((buf.length - offset) >= 1388) {
			byteArray = Arrays.copyOfRange(buf, offset, offset + length);
		}
		else if (offset >= buf.length){
			throw new TimeoutException();
		} else {
            byteArray = Arrays.copyOfRange(buf, offset, buf.length);
        }
		byteArray = appendSequenceNumber(byteArray);
		byteArray = appendChecksum(byteArray);
		return byteArray;
	}
	
	/**an das byte Array data wird vorne die Sequenznummer angehaengt (4 Byte).
	 * @param data
	 * @return
	 */
	private byte[] appendSequenceNumber(byte[] data) {
		ByteBuffer buffer = ByteBuffer.allocate(4);
		buffer.putInt(sequenceNumber);
		byte[] packetSequence = buffer.array();
		data = concat(packetSequence,data);
		return data;
	}

	/**an das byte Array data wird vorne die Checksumme angehaengt (4 Byte).
	 * @param data
	 * @return
	 */
	private byte[] appendChecksum(byte[] data) {
		CRC32 crc = new CRC32();
		crc.update(data);
		ByteBuffer buff = ByteBuffer.allocate(8);
		buff.putLong(crc.getValue());
		byte[] checksum = buff.array();
		data = concat(checksum,data);
		return data; 
	}

	/**data1 wird vor data2 gehaengt (in einem neuen byte array result).
	 * @param data1
	 * @param data2
	 * @return
	 */
	private byte[] concat(byte[] data1, byte[] data2) {
		int data1Len = data1.length;
		int data2Len = data2.length;
		byte[] result = new byte[data1Len + data2Len];
		System.arraycopy(data1, 0, result, 0, data1Len);
		System.arraycopy(data2, 0, result, data1Len, data2Len);
		return result;
	}

	
	/**wird benoetigt um einstellige wahrcheinlichkeiten von 4 ereignissen zu erzeugen.
	 */
	private void initArr() {
		for (int i = 0; i < chanceTwoTimes; i++) {
			arr.add(twoTimes);
		}
		for (int j = 0; j < chanceChange; j++) {
			arr.add(change);
		}
		for (int k = 0; k < chanceDelete; k++) {
			arr.add(delete);
		}
		for (int z = 0; z < 100 - chanceTwoTimes - chanceChange - chanceDelete; z++) {
			arr.add(normal);
		}
		Collections.shuffle(arr);
	}

    private void process(Doing input) {
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

	private void manipulatedBit(int manipulatedBit) {
	    manipulated = manipulatedBit;
    }
	private void savePacket(DatagramPacket packet1) {
	    this.savepacket = packet1;
    }

    private DatagramPacket getPacket() {
	    return savepacket;
    }
}