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

    private final static String NEWPICTURE = " src=\"https://upload.wikimedia.org/wikipedia/commons/8/8d/Smiley_head_happy.svg\"";
    private static int port = 8082;
    static String current = "";
    static Socket socket;
    static ServerSocket target;
    private static Scanner scanner;

    public static void main(String[] args) {
        open();
    }

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

    public static String doRequest() {
        System.out.print("Input: ");
        scanner = new Scanner(System.in);
        current = scanner.nextLine();
        String host;

        HttpURLConnection con;
        StringBuilder serverResponse = new StringBuilder();
        try {
            if (current.equals("")) {
                String currentDirecotry = "C:\\Users\\Paul\\eclipse-workspace-Java\\Netzwerke Aufgabe 2\\config.properties";
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
            System.out.println(hostUrl.getProtocol());
            if (hostUrl.getProtocol().equals("https")) {
                con = (HttpsURLConnection) hostUrl.openConnection();
            } else {
                con = (HttpURLConnection) hostUrl.openConnection();
            }
            con.setRequestMethod("GET");
            con.addRequestProperty("Host", host);
            con.addRequestProperty("Connection", "keep-alive");
            con.setInstanceFollowRedirects(true);
            HttpURLConnection.setFollowRedirects(true);
            InputStream input = con.getInputStream();
            BufferedReader fromServer = new BufferedReader(new InputStreamReader(input));
            String line = "";
            BufferedWriter toClient = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            while ((line = fromServer.readLine())  != null) {
                line = changeYeah(line);
                serverResponse.append(line);
            }
            serverResponse = replaceImg(serverResponse);
            line = serverResponse.toString();
            toClient.write("HTTP/1.0 200 OK\r\n");
            toClient.write("Content-length: " + line.length() + "\r\n");
            toClient.write("\r\n");
            toClient.write(line);
            toClient.flush();
            con.disconnect();
            socket.close();
            target.close();
            open();
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return serverResponse.toString();
    }

    private static StringBuilder replaceImg(StringBuilder serverResponse) {
        String ImgBlock;
        int end;
        int start = serverResponse.indexOf("<img");
        int ImgBlockStart;
        int ImgBlockEnde;
        while (start != -1){
            end = serverResponse.indexOf(">", start);
            ImgBlock = serverResponse.substring(start, end);
            ImgBlockStart = ImgBlock.indexOf("src=\"", 0);
            ImgBlockEnde = ImgBlock.indexOf("\"", ImgBlockStart);
            ImgBlock = ImgBlock.substring(0, ImgBlockStart - 1) + NEWPICTURE + ImgBlock.substring(ImgBlockEnde + 1, ImgBlock.length());
            serverResponse.replace(start, start + ImgBlockEnde, ImgBlock);
            start = serverResponse.indexOf("<img", start + ImgBlockEnde);
        }
        return serverResponse;
    }

    private static String changeYeah(String htmlString) {
        String[] replace = { "MMIX ", "Java ", "Computer ", "RISC ", "CISC ", "Debugger ", "Informatik ", "Student ",
                "Studentin ", "Studierende", "Windows ", "Linux ", "Software", "InformatikerInnen", "Informatiker", "Informatikerin"};
        for (int i = 0; i < replace.length; i++) {
            htmlString = htmlString.replaceAll("(?i)" + replace[i], replace[i] + "(yeah!)");
        }
        return htmlString;
    }
}