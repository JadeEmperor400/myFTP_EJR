import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Scanner;

public class FTPClient {

    private BufferedReader streamFromServer;
    private BufferedWriter streamToServer;
    private static final int FTP_PORT = 21;

    public static void main(String[] args) {
        FTPClient client = new FTPClient();
        client.start("inet.cs.fiu.edu");

    }

    public void start(String server) {
        Socket s = new Socket();
        InetSocketAddress addr = new InetSocketAddress(server, FTP_PORT);
        try {
            s.connect(addr);
            streamFromServer = new BufferedReader(new InputStreamReader(s.getInputStream()));
            streamToServer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
        }
        catch (Exception e) {
            System.out.println("Error: Could not connect to server");
            System.exit(1);
        }

        System.out.println("Connected to " + server);

        // welcome message from server
        System.out.println(receiveResponseLine());

        // send user name
        sendCommand("USER demo");
        System.out.println(receiveResponseLine());

        // send password
        sendCommand("PASS demopass");
        String response = receiveResponseLine();
        System.out.println(response);

        // 230 means login successful
        if (response.startsWith("230")) {
            boolean loop = true;
            Scanner scan = new Scanner(System.in);
            String command = null;

            while (loop) {
                System.out.println("Please enter a command, only \"quit\" supported so far.");
                command = scan.next();

                if (command.equalsIgnoreCase("quit")) {
                    loop = false;
                }
                else {
                    System.out.println("Error: " + command + " not supported!");
                }

            }
        }

    }

    // send command over control socket
    private void sendCommand(String command) {

        try {
            streamToServer.write(command + "\r\n");
            streamToServer.flush();
        }
        catch (IOException e) {
            // print error
        }
    }

    // reads responses from control socket
    private String receiveResponseLine() {
        String response = null;

        try {
            response = streamFromServer.readLine();
        }
        catch (IOException e) {
            // print error
        }

        return response;
    }
}
