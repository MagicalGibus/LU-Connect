package Server;

import Encryption.EncryptionTool;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

public class ExitHandler extends Thread {
    private Server server;
    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("[HH:mm:ss]");
    
    public ExitHandler(Server server) {
        this.server = server; // Reference to server object
    }
    
    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in); // Scanner object that reads from standard input
        String input;
        
        System.out.println("Type 'exit' to shut down the server");
        
        while (true) {
            input = scanner.nextLine();
            if ("exit".equalsIgnoreCase(input)) {
                System.out.println("Shutting down server...");
                
                // Close all client connections
                List<ClientHandler> clients = server.getClients();
                for (ClientHandler client : clients) {
                    try {
                        // Adds timestamp to shutdown message
                        String timestamp = LocalDateTime.now().format(timeFormatter);
                        client.send(EncryptionTool.encrypt(timestamp + " Server: The server is shutting down. Goodbye!"));
                        client.closeSocket();
                    } catch (Exception e) {
                        System.out.println("Error notifying client of shutdown: " + e.getMessage());
                    }
                }
                
                // Shut down the server
                server.shutdown();
                break;
            }
        }
        
        scanner.close();
    }
}