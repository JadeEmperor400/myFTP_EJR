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
            String menuFormat = "%-23s%s\n";

            while (loop) {
                System.out.println("\nPlease enter a command:");
                System.out.printf(menuFormat ,"ls", "List the files in the current directory on the remote server.");
                System.out.printf(menuFormat, "cd remote-dir", "Change the current directory to \"remote-dir\" on the remote server.");
                System.out.printf(menuFormat, "put local-file", "Upload the file \"local-file\" from the local machine to the remote server.");
                System.out.printf(menuFormat, "get remote-file", "Download the file \"remote-file\" from the remote server to the local machine.");
                System.out.printf(menuFormat, "delete remote-file", "Delete the file \"remote-file\" from the remote server.");
                System.out.printf(menuFormat, "quit", "Quit the FTP client.");
                command = scan.nextLine();

                if (command.equals("ls")) {
                    list();
                }
                else if (command.startsWith("cd")) {
                    cd(command);
                }
                else if (command.startsWith("put")) {
                    put(command);
                }
                else if (command.startsWith("get")) {
                    get(command);
                }
                else if (command.startsWith("delete")) {
                    delete(command);
                }
                else if (command.equals("quit")) {
                    disconnect();
                    loop = false;
                }
                else {
                    System.out.println(command + " is not a valid command.");
                }

            }
        }

    }

    private void list() {

    }

    private void cd(String command) {

        // make sure a remote directory was provided
        if (command.trim().length() < 4) {
            System.out.println("Error: Must specify remote-dir to switch to for cd command.");
            return;
        }

        String directory = command.substring(3);

        sendCommand("CWD " + directory);
        System.out.println(receiveResponseLine());
    }

    private void put(String command) {

    }

    private void get(String command) {

    }

    private void delete(String command) {

    }

    private void disconnect() {

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
