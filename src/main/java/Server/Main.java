package Server;

public class Main {

    public static void main(String[] args) {
        String host = "localhost"; // Default
        int port = 1060; // Default

        // Parse command-line arguments
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-h") || args[i].equals("--host")) {
                if (i + 1 < args.length) {
                    host = args[i + 1];
                    i++; 
                }
            } else if (args[i].equals("-p") || args[i].equals("--port")) {
                if (i + 1 < args.length) {
                    try {
                        port = Integer.parseInt(args[i + 1]);
                        i++; 
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid port number");
                        System.exit(1);
                    }
                }
            }
        }

        System.out.println("Starting server on " + host + ":" + port);
        Server server = new Server(host, port);
        server.start();
    }
}