package Client;

import javax.swing.SwingUtilities;

public class Main {

    public static void main(String[] args){
        if (args.length < 1) {
            System.out.println("Client <host> [-p port]");
            System.exit(1);
        }

        String host = args[0];
        int port = 1060; // Default

        if (args.length >= 3 && args[1].equals("p")) {
            try {
                port = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number");
                System.exit(1);
            }
        }

        Client client = new Client(host, port);
        SwingUtilities.invokeLater(client::createAndShowGUI);
    }
}