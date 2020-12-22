import java.io.IOException;
import java.net.SocketException;
import java.util.Scanner;
import java.net.DatagramSocket;
import java.net.DatagramPacket;

public class ServerMain
{
    static Scanner scanner = new Scanner(System.in);

    private int Port;
    private Server server;

    public ServerMain(int port)
    {
        Port = port;
		server = new Server(port);
    }
    public static void main(String[] args) 
    {
        int port = 8192;
        new ServerMain(port);
    }
}
