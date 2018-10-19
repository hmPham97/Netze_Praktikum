import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.Properties;
import java.util.Scanner;

public class ServerTH {

	private final static String HMPIC = "https://upload.wikimedia.org/wikipedia/commons/8/8d/Smiley_head_happy.svg";
	private static int port = 8082;
	private static String result;
	static String current = "";
	static Socket socket;
	static ServerSocket target;
	private static Scanner scanner;

	public static void main(String[] args) {
		try {
			
			target = new ServerSocket(port);
			System.out.println("Waiting for Connection");
			socket = target.accept();
			System.out.println("Verbunden");
			result = doRequest();
			BufferedWriter toClient = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			toClient.write("HTTP/1.0 200 OK\r\n");
			toClient.write("Content-length: " + result.length() + "\r\n");
			toClient.write("\r\n");
			toClient.write(result);
			toClient.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String doRequest() {
		System.out.print("Input: ");
		scanner = new Scanner(System.in);
		current = scanner.nextLine();
		String host;
		
		HttpURLConnection con;
		StringBuilder serverResponse = new StringBuilder();
		try {
			if (current.equals("")) {
				String currentDirecotry = "C:\\Users\\Paul\\Documents\\Studium\\Softwareentwicklung 1\\eclipse-workspace-java\\Netzwerke1-Aufgab2\\config.properties";
				FileReader configReader = new FileReader(currentDirecotry);
				Properties properties = new Properties();
				properties.load(configReader);
				host = properties.getProperty("host");
			}
			else {
				host = current;
			}
			URL hostUrl = new URL(host);
			System.out.println(host);
			if (hostUrl.getProtocol().equals("https")) {
				con = (HttpsURLConnection) hostUrl.openConnection();
			} else {
				con = (HttpURLConnection) hostUrl.openConnection();
			}
			con.setRequestMethod("GET");

			try (InputStream input = con.getInputStream();
					BufferedReader fromServer = new BufferedReader(new InputStreamReader(input))) {
				for (String line = fromServer.readLine(); line != null && line.length() > 0; line = fromServer.readLine()) {
					line = leet(line);
					serverResponse.append(line);
				}
				serverResponse = replaceImg(serverResponse);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			}

		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return serverResponse.toString();

	}

	private static StringBuilder replaceImg(StringBuilder serverResponse) {
		String img = "<img";
		int n = 0;
		for (int i = serverResponse.indexOf(img) + 10; i > n; i = serverResponse.indexOf(img, i) + 10) {
			n = i;
			serverResponse.replace(i, serverResponse.indexOf("\"", i), HMPIC);
		}
		return serverResponse;
	}

	private static String leet(String htmlString) {
		String[] replace = { "MMIX ", "Java ", "Computer ", "RISC ", "CISC ", "Debugger ", "Informatik ", "Student ",
				"Studentin ", "Studierende", "Windows ", "Linux ", "Software", "InformatikerInnen", "Informatiker", "Informatikerin"};
		String[][] replacements = new String[replace.length][replace.length];
		for (int i = 0; i < replace.length; i++) {
			replacements[0][i] = replace[i];
			String tmp = replacements[0][i];
			replacements[1][i] = tmp.toLowerCase().replace("mmix", "MMIX(yeah!)").replace("java", "Java(yeah!)").replace("computer", "Computer(yeah!)")
					.replace("risc", "Risc(yeah!)").replace("cisc", "Cisc(yeah!)").replace("debugger", "Debugger(yeah!)").replace("informatik", "Informatik(yeah!)").replace("studentin", "Studentin(yeah!)")
							.replace("studierende", "Studeierende(yeah!)").replace("windows", "Windows(yeah!)").replace("linux", "Linux(yeah!)").replace("software", "Software(yeah!)").replace("informatikerinnen", "InformatikerInnen(yeah!)")
							.replace("Informatiker", "Informatiker(yeah!)").replace("informatikerin", "Informatikerin(yeah!)");
		}

		for (int i = 0; i < replace.length; i++) {
			htmlString = htmlString.replaceAll(replacements[0][i], replacements[1][i]);
		}
		//		String[] htmlWords = htmlString.split(" ");
		//		boolean img = false;
		//		htmlString = "";
		//		for (String word : htmlWords) {
		//			if (img) {
		//				word = HMPIC;
		//				img = false;
		//			}
		//			if (word.equals("<img")) {
		//				img = true;
		//			}
		//			htmlString += " " + word;
		//		}

		// htmlString = htmlWords.toString();


		// htmlString = htmlString.replaceAll("<img src=\"[^]\"]", HMPIC);

		return htmlString;
	}

}
