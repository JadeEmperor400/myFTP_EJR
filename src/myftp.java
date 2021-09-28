import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class myftp {

    private BufferedReader streamFromServer;
    private BufferedWriter streamToServer;
    private static final int FTP_PORT = 21;
    private String server;

    public static void main(String[] args) {
        myftp client = new myftp();
        client.start(args[0]);
    }

    public void start(String serverVal) {
        this.server = serverVal;
        Socket s = new Socket();
        InetSocketAddress addr = new InetSocketAddress(server, FTP_PORT);
        try {
            s.connect(addr);
            streamFromServer = new BufferedReader(new InputStreamReader(s.getInputStream()));
            streamToServer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
        } catch (Exception e) {
            System.out.println("Error: Could not connect to server");
            System.exit(1);
        }

        System.out.println("Connected to " + server);

        // welcome message from server
        System.out.println(receiveResponseLine());

        // send user name
        System.out.print("FTP Username: ");
        Scanner scan = new Scanner(System.in);
        String user = scan.nextLine();
        sendCommand("USER " + user);
        System.out.println(receiveResponseLine());

        // send password
        System.out.print("Password: ");
        user = scan.nextLine();
        sendCommand("PASS " + user);
        String response = receiveResponseLine();
        System.out.println(response);

        // 230 means login successful
        if (!response.startsWith("230")) {
            System.out.println("Login failed.");
            System.exit(1);
        }

        String menuFormat = "%-23s%s\n";
        System.out.printf(menuFormat, "ls", "List the files in the current directory on the remote server.");
        System.out.printf(menuFormat, "cd remote-dir", "Change the current directory to \"remote-dir\" on the remote server.");
        System.out.printf(menuFormat, "put local-file", "Upload the file \"local-file\" from the local machine to the remote server.");
        System.out.printf(menuFormat, "get remote-file", "Download the file \"remote-file\" from the remote server to the local machine.");
        System.out.printf(menuFormat, "delete remote-file", "Delete the file \"remote-file\" from the remote server.");
        System.out.printf(menuFormat, "quit", "Quit the FTP client.");

        String command;
        boolean loop = true;
        scan = new Scanner(System.in);

        while (loop) {
            System.out.println("\nPlease enter a command:");
            command = scan.nextLine();
            System.out.println("");

            if (command.equals("ls")) {
                list();
            } else if (command.equals("pwd")) {
                sendCommand("PWD");
                System.out.println(receiveResponseLine());
            } else if (command.startsWith("cd")) {
                cd(command);
            } else if (command.startsWith("put")) {
                put(command);
            } else if (command.startsWith("get")) {
                get(command);
            } else if (command.startsWith("delete")) {
                delete(command);
            } else if (command.equals("quit")) {
                disconnect();
                loop = false;
            } else {
                System.out.println(command + " is not a valid command.");
            }
        }
    }


    private void list() {
        try {
            Socket sock = passiveDataSocket();
            BufferedReader fromServ = new BufferedReader(new InputStreamReader(sock.getInputStream()));

            sendCommand("LIST");

            Stream linesStream = fromServ.lines();

            System.out.println(receiveResponseLine());
            String data = (String)linesStream.collect(Collectors.joining(System.lineSeparator()));
            System.out.println(data);

            System.out.println(receiveResponseLine());


            System.out.println(data.getBytes().length + " bytes received");

        } catch (Exception e) {
            System.out.println(e);
            System.out.println("Error: Could not connect to server");
            System.exit(1);
        }
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


    private void delete(String command) {
        if (command.trim().length() < 8) {
            System.out.println("Error: Must specify a file or directory to delete.");
            return;
        }

        String file = command.substring(7);

        sendCommand("DELE " + file);

        System.out.println(receiveResponseLine());
    }

    private void disconnect() {
        sendCommand("QUIT");
        System.out.println(receiveResponseLine());
    }


    private void put(String command) {
        String filepath = command.substring(command.indexOf(" ") + 1);
        command = "STOR " + filepath;
        long bytesTransferred;

        File f = new File(filepath);

        if (!f.exists()) {
            System.out.println(filepath + ": File not found");
            return;
        }

        try {
            Socket sock = passiveDataSocket();

            sendCommand(command);
            System.out.println(receiveResponseLine());

            bytesTransferred = transferFile(sock, filepath, "put");

            System.out.println(receiveResponseLine());
            System.out.println("Transferred " + bytesTransferred + " bytes.");
        } catch (Exception e) {
            System.out.println(e);
            System.exit(1);
        }
    }


    private void get(String command) {
        String filepath = command.substring(command.indexOf(" ") + 1);
        command = "RETR " + filepath;
        long bytesTransferred;

        try {
            Socket sock = passiveDataSocket();

            sendCommand(command);
            String reply = receiveResponseLine();
            System.out.println(reply);

            if (!reply.startsWith("150")) {
                sock.close();
                return;
            }

            bytesTransferred = transferFile(sock, filepath, "get");

            System.out.println(receiveResponseLine());
            System.out.println("Transferred " + bytesTransferred + " bytes.");

        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
            System.exit(1);
        }
    }


    //Depending on the type (get or put) this will either send the contents of a file to the server stream or
    //will retrieve the contents of a file from the server stream and save them to a file
    private long transferFile(Socket sock, String filepath, String type) throws IOException {
        File file = new File(filepath);
        BufferedInputStream input;
        BufferedOutputStream output;
        byte[] buffer = new byte[256];
        long bytesTransferred = 0;
        int numRead;

        if (type.equals("get")) {
            input = new BufferedInputStream(sock.getInputStream());
            output = new BufferedOutputStream(new FileOutputStream(file));

            //buffer = new byte[bufferSize(reply)];
        }
        // "put" command
        else {
            input = new BufferedInputStream(new FileInputStream(file));
            output = new BufferedOutputStream(sock.getOutputStream());

            //buffer = new byte[(int) file.length()];
        }


        while ((numRead = input.read(buffer)) >= 0) {
            bytesTransferred += numRead;
            output.write(buffer, 0, numRead);
        }

        if (type.equals("put")) {
            output.flush();
        }

        input.close();
        output.close();
        sock.close();

        return bytesTransferred;
    }


    //creates a new socket for data connection
    private Socket passiveDataSocket() throws IOException {
        sendCommand("PASV");
        String response = receiveResponseLine();
        System.out.println(response);

        int port = getPort(response);


        Socket sock = new Socket();
        InetSocketAddress address = new InetSocketAddress(server, port);

        sock.connect(address);

        return sock;
    }


    //parses the reply from server in order to retrieve the size of the buffer
    private int bufferSize(String reply) {
        int size = 0;
        String str = "";

        for (int i = reply.length() - 1; i >= 0; i--) {
            if (reply.charAt(i) == '(') {
                str = reply.substring(i + 1);
                break;
            }
        }
        int endIndex = 0;

        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == ' ') {
                endIndex = i;
                break;
            }
        }

        str = str.substring(0, endIndex);
        size = Integer.parseInt(str);

        return size;
    }


    // send command over control socket
    private void sendCommand(String command) {

        try {
            streamToServer.write(command + "\r\n");
            streamToServer.flush();
        } catch (IOException e) {
            // print error
        }
    }

    // reads responses from control socket
    private String receiveResponseLine() {
        String response = null;

        try {
            response = streamFromServer.readLine();
        } catch (Exception e) {
            System.out.println(e);
        }

        return response;
    }

    //parses the reply from PASV in order to get the correct port
    private int getPort(String response) {
        int index1 = response.indexOf("(");
        int index2 = response.indexOf(")");
        response = response.substring(index1, index2);

        int arr[] = new int[6];
        int startIndex = 0;
        int prevIndex = 0;

        for (int i = 0; i < response.length(); i++) {
            if (response.charAt(i) == ',') {
                arr[startIndex] = Integer.parseInt(response.substring(prevIndex + 1, i));
                prevIndex = i;
                startIndex++;
            }
            if (startIndex == 5) {
                arr[5] = Integer.parseInt(response.substring(prevIndex + 1, response.length()));
                break;
            }
        }

        int port = (arr[4] * 256) + arr[5];

        return port;
    }
}