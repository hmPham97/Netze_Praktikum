import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

public class Aufgabe2 {

    public static void main(String[] args) {

        try (BufferedWriter wr = new BufferedWriter(new OutputStreamWriter())) {
            HttpURLConnection uconn = new
            String link = wr.nextLine();
            URL url = new URL(link);
            String host = url.getHost();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }
}
