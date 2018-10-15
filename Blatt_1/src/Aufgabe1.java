import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

public class Aufgabe1
{
    private static File log;
    private static int counter = 1;
    static Scanner scanner;
    static BufferedWriter output;
    static String current = "";
    static String result = "";

    public static void main(String[] args)
    {
        log = new File("/home/network/Logdatei.txt");
        scanner = new Scanner(System.in);
        try
        {
            output = new BufferedWriter(new FileWriter(log, true));
            inputOutput();
        }
        catch (Exception exception)
        {
            exception.printStackTrace();
        }
    }

    public static void inputOutput()
    {
        System.out.print("Input: ");
        try
        {
            current = scanner.nextLine();
            if (current.equals(""))
            {
                numString(current);
                SimpleDateFormat date = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                output.write(result);
                output.newLine();
                output.close();
                System.out.println("Zugriff aufgezeichnet am: [" + date.format(new Date()) + "]!");
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

    private static void numString(String param)
    {
        String number = "";
        if (counter <= 9) {
            number = "[0" + counter + "]";
        } else {
            number = "[" + counter + "]";
        }
        counter += 1;
        result = number + param;
    }
}