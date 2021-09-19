import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Scanner;
import java.util.stream.Collectors;

public class FTPClient {

    private BufferedReader streamFromServer;
    private BufferedWriter streamToServer;
    private static final int FTP_PORT = 21;
    private String server;
    private Socket dataConnection;

    public static void main(String[] args) {
        FTPClient client = new FTPClient();
        client.start("inet.cs.fiu.edu");

    }

    public void start(String serverVal) {
    	this.server = serverVal;
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
                else if(command.equals("pwd")) {
                	sendCommand("PWD");
                	System.out.println(receiveResponseLine());
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
                /*else {
                    System.out.println(command + " is not a valid command.");
                }*/
                else {
                	sendCommand(command);
                	System.out.println(receiveResponseLine());
                }

            }
        }

    }

    private void list() {
    	/*sendCommand("PASV");
    	String response = receiveResponseLine();
    	System.out.println(response);
    	
    	int port = getPort(response);
    	
    	/*
    	Socket sock = new Socket();
        InetSocketAddress address = new InetSocketAddress(server, port);
        BufferedReader fromServ;
        BufferedWriter toServ;*/
        try {
            /*sock.connect(address);
            System.out.println("Connected to " + server + " for a data connection");
            fromServ = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            toServ = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
            */
        	BufferedReader fromServ = dataConnection("LIST");
        	
            sendCommand("LIST");
            System.out.println(receiveResponseLine());
            
            System.out.println(fromServ.lines().collect(Collectors.joining(System.lineSeparator())));  
            
            System.out.println(receiveResponseLine());
            
        }
        catch (Exception e) {
        	System.out.println(e);
            System.out.println("Error: Could not connect to server");
            System.exit(1);
        }
    }
    
    private BufferedReader dataConnection(String command) throws IOException {
    	sendCommand("PASV");
    	String response = receiveResponseLine();
    	System.out.println(response);
    	
    	int port = getPort(response);
    	
    	
    	Socket sock = new Socket();
        InetSocketAddress address = new InetSocketAddress(server, port);
        BufferedReader fromServ;
        
        sock.connect(address);
        fromServ = new BufferedReader(new InputStreamReader(sock.getInputStream()));
        
        return fromServ;
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
    	String file = command.substring(command.indexOf(" "));
    	command = "STOR"+file;
    	
    	try {
    		BufferedReader fromServ = dataConnection(command);
    		
    		sendCommand(command);
    		System.out.println(receiveResponseLine());
    		System.out.println("here1");
    		//System.out.println(fromServ.lines().collect(Collectors.joining(System.lineSeparator())));
    		System.out.println(fromServ.readLine());
    		System.out.println("here2");
    		System.out.println(receiveResponseLine());
    	}
    	catch(Exception e) {
    		System.out.println(e);
    		System.exit(1);
    	}
    }

    
    
    
    private void get(String command) {
    	String file = command.substring(command.indexOf(" "));
    	command = "RETR"+file;
    	System.out.println("here");
    	try {
    		BufferedReader fromServ = dataConnection(command);
    		
    		
    		sendCommand(command);
    		System.out.println(receiveResponseLine());
    		
    		System.out.println(fromServ.lines().collect(Collectors.joining(System.lineSeparator())));
    		
    		
    		System.out.println(receiveResponseLine());
    		
    	}
    	catch(Exception e) {
    		System.out.println(e);
    		System.exit(1);
    	}
    }

    private void delete(String command) {
        if (command.trim().length() <8) {
            System.out.println("Error: Must specify a file or directory to delete.");
            return;
        }
        
        String file = command.substring( 7);
        
        sendCommand("DELE " + file);
        
        System.out.println(receiveResponseLine());
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
        catch (Exception e) {
            // print error
        }

        return response;
    }
    
    private int getPort(String response) {
    	int index1 = response.indexOf("(");
    	int index2 = response.indexOf(")");
    	response = response.substring(index1,index2);
    	//System.out.println(response);
    	
    	int arr[] = new int[6];
    	int startIndex = 0;
    	int prevIndex = 0;
    	
    	for(int i=0; i<response.length(); i++) {
    		if(response.charAt(i) == ',') {
    			arr[startIndex] = Integer.parseInt(response.substring(prevIndex+1,i));
    			prevIndex = i;
    			startIndex++;
    		}
    		if(startIndex == 5) {
    			arr[5] = Integer.parseInt(response.substring(prevIndex+1,response.length()));
    			break;
    		}
    	}
    	/*
    	for(int i=0;i<arr.length;i++) {
    		System.out.println(arr[i]);
    	}
    	*/
    	int port = (arr[4] * 256) + arr[5];
    	//System.out.println(port);
    	return port;
    }
    
}