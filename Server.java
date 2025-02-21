import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class Server extends Thread {
    private String host;
    private int port;
    private List<Socket> connections;

    public Server(String host, int port){
        this.host = host;
        this.port = port;
        this.connections = new ArrayList<Socket>();
    }
}