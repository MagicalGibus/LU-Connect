package Server;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class Server extends Thread {
    private List<ClientHandler> clients = new ArrayList<ClientHandler>(); // Tracks all connected clients
    private String host;
    private int port;
    private ServerSocket serverSocket;

    public Server(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public void run(){
        try {
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(host, port)); // Binds socket to specified host and port

            System.out.println("Listening at " + serverSocket.getLocalSocketAddress());

            while (!serverSocket.isClosed()){
                Socket clientSocket = serverSocket.accept(); // Waits for clients to connect
                System.out.println("Accepting a new connection from " + clientSocket.getRemoteSocketAddress() + " to " + clientSocket.getLocalSocketAddress());

                ClientHandler clientHandler = new ClientHandler(clientSocket, this); // Creates an instance of ClientHandler for each connected client, starts in new thread
                clientHandler.start();

                clients.add(clientHandler); // Adds client to list of connected clients
                System.out.println("Ready to receive message from " + clientSocket.getRemoteSocketAddress());
            }
        } catch(IOException e) {
            if (!serverSocket.isClosed()) {
                System.out.println("Server error: " + e.getMessage());
                }
        }
    }

    // Sends message to all connected clients
    public void broadcast(String message, SocketAddress source) {
        for (ClientHandler client : clients) {
            client.send(message);
        }
    }
    

    // Removes a client handler from the list when it disconnects
    public void removeClient(ClientHandler client) {
        clients.remove(client);
    }

    // Getter method to return list of all connected clients
    public List<ClientHandler> getClients() {
        return clients;
    }

    // Shuts down server
    public void shutdown() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.out.println("Error closing server socket: " + e.getMessage());
        }
    }
}
