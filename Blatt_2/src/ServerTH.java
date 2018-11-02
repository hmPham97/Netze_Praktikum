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

	private final static String NEWPICTURE = " src=\"https://upload.wikimedia.org/wikipedia/commons/8/8d/Smiley_head_happy.svg\""; // Link zum neuen Bild
	private static int port = 8082;
	static String current = "";
	static Socket socket;
	static ServerSocket target;
	private static Scanner scanner;

	public static void main(String[] args) {
		open();
	}

	/**
	 * macht erneuten Input moeglich durch rekursiven Aufruf
	 */
	private static void open() {
		try {
			target = new ServerSocket(port);
			System.out.println("Waiting for Connection");
			socket = target.accept();
			System.out.println("Verbunden");
			doRequest();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @return http body der entsprechenden Website als String
	 */
	public static String doRequest() {
		System.out.print("Input: ");
		scanner = new Scanner(System.in);
		current = scanner.nextLine();
		String host;

		HttpURLConnection con;
		StringBuilder serverResponse = new StringBuilder();
		try {
			if (current.equals("")) { // gibt es keine Eingabe und der Anwender drueckt nur Enter wird der Host aus der Konfig File verwendet
				String currentDirecotry = "C:\\Users\\Paul\\eclipse-workspace-Java\\Netzwerke Aufgabe 2\\config.properties"; //Pfad in dem die Konfig File hinterlegt sein muss, ggf aendern
				FileReader configReader = new FileReader(currentDirecotry);
				Properties properties = new Properties();
				properties.load(configReader);
				host = properties.getProperty("host");
			}
			else {
				host = current; // host muss im richtigen format angegeben werden, z.B https://www.hm.edu
			}
			URL hostUrl = new URL(host); // neue Url
			System.out.println(host);
			System.out.println(hostUrl.getProtocol());
			if (hostUrl.getProtocol().equals("https")) { // pruefe ob url https oder http enthaelt
				con = (HttpsURLConnection) hostUrl.openConnection();  // oeffne https connection
			} else {
				con = (HttpURLConnection) hostUrl.openConnection(); // oeffne http connection
			}
			con.setRequestMethod("GET"); // GET Request an Server senden
			con.addRequestProperty("Host", host);
			con.addRequestProperty("Connection", "keep-alive");
			con.setInstanceFollowRedirects(true);
			HttpURLConnection.setFollowRedirects(true);
			InputStream input = con.getInputStream();
			BufferedReader fromServer = new BufferedReader(new InputStreamReader(input));
			String line = "";
			BufferedWriter toClient = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			while ((line = fromServer.readLine())  != null) {
				line = replaceLocal(line, host);
				line = changeYeah(line); // tausche Text
				serverResponse.append(line); 
				System.out.println(line);
			}
			serverResponse = replaceImg(serverResponse); // tausche Bilder
			line = serverResponse.toString();
			toClient.write("HTTP/1.0 200 OK\r\n"); // GET Response an Server
			toClient.write("Content-length: " + line.length() + "\r\n");
			toClient.write("\r\n");
			toClient.write(line);
			toClient.flush(); // schlieﬂe BufferedReader
			con.disconnect(); // schlieﬂe InputStream
			socket.close(); // schlieﬂe Socket
			target.close(); // schlieﬂe ServerSocket
			open(); 
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return replaceUmlaut(serverResponse.toString());
	}

	/**Methode die nach img Blocks sucht und in diesem Block den Link sucht und diesen austauscht
	 * 
	 * @param serverResponse http body der angeforderten Website als Stringbuilder
	 * @return ausgbesserten http body (links aller bilder getauscht)
	 */
	private static StringBuilder replaceImg(StringBuilder serverResponse) {
		String ImgBlock;
		int end;
		int start = serverResponse.indexOf("<img"); // erster img Block
		int ImgBlockStart;
		int ImgBlockEnde;
		while (start != -1){
			end = serverResponse.indexOf(">", start); // suche Ende des img Blocks
			ImgBlock = serverResponse.substring(start, end); // speichere img Block
			ImgBlockStart = ImgBlock.indexOf("src=\"", 0); // suche Anfang des Links in img Block 
			ImgBlockEnde = ImgBlock.indexOf("\"", ImgBlockStart); // suche Ende des Links in img Block 
			ImgBlock = ImgBlock.substring(0, ImgBlockStart - 1) + NEWPICTURE + ImgBlock.substring(ImgBlockEnde + 1, ImgBlock.length()); // speichere zu ersetzenden Link
			serverResponse.replace(start, start + ImgBlockEnde, ImgBlock); // ersetzte Link durch neuen
			start = serverResponse.indexOf("<img", start + ImgBlockEnde); // naechster img Block oder -1 wenn es keinen mehr gibt = Abbruchbedingung fue while Schleife
		}
		return serverResponse;
	}

	/**
	 * 
	 * @param htmlString uebergebener String
	 * @return String in dem die Schlagworte im String Array replace geaendert wurden
	 */
	private static String changeYeah(String htmlString) {
		String[] replace = { "MMIX ", "Java ", "Computer ", "RISC ", "CISC ", "Debugger ", "Informatik ", "Student ",
				"Studentin ", "Studierende", "Windows ", "Linux ", "Software", "InformatikerInnen", "Informatiker", "Informatikerin"}; // Schlagworte
		for (int i = 0; i < replace.length; i++) { // geht jedes einzelne Schlagwort durch und ersetzt es wenn vorhanden durch Schlagwort + (yeah!)
			htmlString = htmlString.replaceAll(/*"(?i)" +*/ replace[i], replace[i] + "(yeah!)");
		}
		return htmlString;
	}

	/**
	 *
	 * @param htmlString uebergebener String
	 * @param link Linkaddresse die zu href hinzugef¸gt wird, falls kein link angegeben ist
	 * @return neuer htmlString
	 */
	private static String replaceLocal(String htmlString, String link) {
		htmlString = htmlString.replaceAll("href=\"/" , "href=\"" + link + "/");
		if(!htmlString.contains("http")) {
			htmlString = htmlString.replaceAll("href=\"", "href=\"" + link + "/");
		}
		return  htmlString;
	}
	
	private static String replaceUmlaut(String input) {

	     String output = input.replace("¸", "ue")
	                          .replace("ˆ", "oe")
	                          .replace("‰", "ae")
	                          .replace("ﬂ", "ss");

	     output = output.replace("‹(?=[a-z‰ˆ¸ﬂ ])", "Ue")
	                    .replace("÷(?=[a-z‰ˆ¸ﬂ ])", "Oe")
	                    .replace("ƒ(?=[a-z‰ˆ¸ﬂ ])", "Ae");
	     
	     output = output.replace("‹", "UE")
	                    .replace("÷", "OE")
	                    .replace("ƒ", "AE");

	     return output;
	}
	
	
}