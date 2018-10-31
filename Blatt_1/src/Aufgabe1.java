import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

public class Aufgabe1
{
	private static File log;	
	private static int counter = 1; // aktuelle Zeilenanzahl
	static Scanner scanner;
	static BufferedWriter output;
	static String current = "";
	static String result = "";

	public static void main(String[] args)
	{
		log = new File("/home/network/Logdatei.txt"); // erstellen der Logdatei unter dem angegebenen Pfad
		scanner = new Scanner(System.in);			// neues Scanner Objekt
		try
		{
			output = new BufferedWriter(new FileWriter(log, true));
			inputOutput();
		}
		catch (Exception exception) // Auffangen von Exceptions
		{
			exception.printStackTrace();
		}
	}

	public static void inputOutput()
	{
		System.out.print("Input: "); // wird auf Konsolo angezeigt, damit user weiß wo die Eingabe erfolgen soll
		try
		{
			current = scanner.nextLine();
			if (current.equals("")) // Eingabe soll bei leerem String beendet werden
			{
				numString(current);
				SimpleDateFormat date = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
				output.write(result);
				output.newLine();
				output.close(); // schließe BufferedReader, Eingabe beendet
				System.out.println("Zugriff aufgezeichnet am: [" + date.format(new Date()) + "]!"); // Zugriffszeitpunkt wird auf Konsole ausgegeben
				System.exit(0);
			}
			else
			{
				numString(current);
				output.write(result);
				output.newLine();
				inputOutput();
			}
		}
		catch (Exception exception)
		{
			exception.printStackTrace();
		}
	}

	/**setzt vor den übergebenen String bei Zahlen unter 10 die aktuelle Zeile im Format 01,02,... und bei Zahlen ab 10 im Format 10,11,...
	 * 
	 * @param param übergebener String (eine Zeile)
	 */
	private static void numString(String param)
	{
		String number = "";
		if (counter <= 9) {
			number = "[0" + counter + "]";  // kleiner 10
		} else {
			number = "[" + counter + "]"; // groeßer 10
		}
		counter += 1; // counter wird um eins erhöht damit bei der naechsten uebergebenen Zeile die richtige Zeilennummer angehängt wird
		result = number + param; // Zusammensetzen der Strings (Zeilenanzahl + übergebener String)
	}
}