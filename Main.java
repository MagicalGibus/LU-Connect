public class Main {

    public static void main(String [] args) {
        String host = "localhost";
        int port = 1060;

        Server server = new Server(host, port);
        server.start();
    }
}