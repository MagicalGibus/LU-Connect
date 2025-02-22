import java.util.Scanner;

public class ExitHandler extends Thread {
    private Server server;

    public ExitHandler(Server server) {
        this.server = server; // Reference to server object
    }

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in); // Scanner object that reads from standard input
        while (true) {
            String input = scanner.nextLine();

            // If input is q in the server console then it will close the server
            if ("q".equals(input)) {
                System.out.println("Closing all connections...");
                for (ClientHandler client : server.getClients()) {
                    client.closeSocket();
                }
                System.out.println("Shutting down the server...");
                server.shutdown();
                System.exit(0);
            }
        }
    }
}
