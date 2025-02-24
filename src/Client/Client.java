package Client;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import javax.swing.JList;
import javax.swing.JOptionPane;

public class Client {
    private Socket socket;
    private String name;
    private PrintWriter writer;
    private BufferedReader reader;
    private JList<String> messages;
    private ReceiveThread receiveThread; // Handles incomming messages

    public Client(String host, int port) {
        try {
            // Attempts to connect to server
            socket = new Socket();
            System.out.println("Trying to connect to " + host + ":" + port + "...");
            socket.connect(new InetSocketAddress(host, port));
            System.out.println("Successfully connected to " + host + ":" + port);

            writer = new PrintWriter(socket.getOutputStream(), true); // Sends data
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream())); // Receives data
            
            // Get user's name
            name = JOptionPane.showInputDialog("Your name: ");
            System.out.println("\nWelcome, " + name + "!");

            writer.println(name + " has joined the chat. Say hi!");
            
            // Start receive thread
            receiveThread = new ReceiveThread(reader, messages);
            receiveThread.start();
            
        } catch (IOException e) {
            System.err.println("Connection error: " + e.getMessage());
            System.exit(1);
        }
    }
}
