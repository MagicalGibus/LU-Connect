import java.net.*;
import java.io.*;

public class ClientHandler extends Thread {
    private Socket clientSocket;
    private SocketAddress socketAddress;
    private Server server;
    private PrintWriter writer;

    public ClientHandler(Socket clientSocket, Server server) {
        this.clientSocket = clientSocket;
        this.socketAddress = clientSocket.getRemoteSocketAddress();
        this.server = server; // The main server
    }

    @Override
    public void run() {
        try {

            // Reads text from client
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            writer = new PrintWriter(clientSocket.getOutputStream(), true); // Sends text to client

            // Continuously reads messages from client
            String message;
            while ((message = reader.readLine()) != null) {
                System.out.println(socketAddress + " says " + message);
                server.broadcast(message, socketAddress);
            }

            // Prints info when client disconnects
            System.out.println(socketAddress + " has closed the connection");
            clientSocket.close();
            server.removeClient(this);
        } catch (IOException e) {
            System.out.println("Error handling client: " + e.getMessage());
            try {
                clientSocket.close();
            } catch (IOException ex) {
                // Either ignore silently or log the error
                System.out.println("Error closing socket: " + ex.getMessage());
            }
            server.removeClient(this);
        }
    }

    // Send message to this client
    public void send(String message) {
        if (writer != null) {
            writer.println(message);
        }
    }

    // Getter for address
    public SocketAddress getSocketAddress() {
        return socketAddress;
    }

    // CLoses the client socket
    public void closeSocket() {
        try {
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            System.out.println("Error closing socket: " + e.getMessage());
        }
    }
}
