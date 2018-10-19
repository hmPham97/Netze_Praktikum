import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class leetServerMain {
	public static final int PORT = 8082;

	public static void main(String[] args) {
		leetServerMain server = new leetServerMain();
		server.startServer();
	}

	private void startServer() {
		leetClient client = new leetClient();

		try (ServerSocket servSock = new ServerSocket(PORT)) {

			System.out.println("Server started, waiting for clients...");

			//while (true) {
				try (Socket s = servSock.accept();
						BufferedReader fromClient = new BufferedReader(new InputStreamReader(s.getInputStream()));
						BufferedWriter toClient = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()))) {
					System.out.println("Got client connection!");
					String serverResponse = client.doRequest();
					toClient.write("HTTP/1.0 200 OK\r\n");
					toClient.write("Content-length: " + serverResponse.length() + "\r\n");
					toClient.write("\r\n");
					System.out.println(serverResponse);
					toClient.write(serverResponse);
					toClient.flush();
				}
			//}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
