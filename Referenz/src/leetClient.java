import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

public class leetClient {

	private final static String HMPIC = "https://upload.wikimedia.org/wikipedia/de/thumb/e/e8/Hochschule_Muenchen_Logo.svg/200px-Hochschule_Muenchen_Logo.svg.png";

	// public static void main(String[] args) {
	// leetClient leetClient = new leetClient();
	// leetClient.doRequest();
	// }

	public String doRequest() {
		HttpURLConnection con;
		StringBuilder serverResponse = new StringBuilder();
		try {
			String currentDirecotry = this.getClass().getClassLoader().getResource("").getPath();
			FileReader configReader = new FileReader(currentDirecotry + "config.properties");
			Properties properties = new Properties();
			properties.load(configReader);
			String host = properties.getProperty("host");
			URL hostUrl = new URL(host);
			if (hostUrl.getProtocol().equals("https")) { // host.contains("https")){
				con = (HttpsURLConnection) hostUrl.openConnection();
			} else {
				con = (HttpURLConnection) hostUrl.openConnection();
			}
			con.setRequestMethod("GET");

			try (InputStream input = con.getInputStream();
					BufferedReader fromServer = new BufferedReader(new InputStreamReader(input))) {
				for (String line = fromServer
						.readLine(); line != null /* && line.length()>0 */; line = fromServer.readLine()) {
					// if(line.charAt(0)== '<') {
					line = leet(line);
					// }
					serverResponse.append(line);
				}
				serverResponse = replaceImg(serverResponse);

			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return serverResponse.toString();

	}

	private StringBuilder replaceImg(StringBuilder serverResponse) {
		String img = "<img";
		int n = 0;
		for (int i = serverResponse.indexOf(img) + 10; i > n; i = serverResponse.indexOf(img, i) + 10) {
			n = i;
			serverResponse.replace(i, serverResponse.indexOf("\"", i), HMPIC);
		}
		return serverResponse;
	}

	private String leet(String htmlString) {
		String[] replace = { "MMIX ", "Java ", "Computer ", "RISC ", "CISC ", "Debugger ", "Informatik ", "Student ",
				"Studentin ", "Studierende", "Windows ", "Linux " };
		String[][] replacements = new String[replace.length][replace.length];
		for (int i = 0; i < replace.length; i++) {
			replacements[0][i] = replace[i];
			String tmp = replacements[0][i];
			replacements[1][i] = tmp.toLowerCase().replace("a", "4").replace("b", "|3").replace("c", "(")
					.replace("d", "|)").replace("e", "3").replace("f", "|=").replace("g", "(").replace("h", "|-|")
					.replace("i", "!").replace("j", "_|").replace("k", "|<").replace("l", "|_").replace("m", "|\\\\/|")
					.replace("n", "|\\|").replace("o", "0").replace("p", "9").replace("q", "(,)").replace("r", "|2")
					.replace("s", "5").replace("t", "7").replace("u", "|_|").replace("v", "\\/").replace("w", "\\/\\/ ")
					.replace("x", ">< ").replace("y", "`/").replace("z", "2");
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
