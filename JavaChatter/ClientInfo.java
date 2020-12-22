import java.net.InetAddress;

public class ClientInfo
{
    public String Name;
    public InetAddress Address;
	public int Port;
	private final int ID;
	public int Attempt = 0;

    public ClientInfo(String name, InetAddress address, int port, final int id) 
    {
		Name = name;
		Address = address;
		Port = port;
		ID = id;
	}
    public int getID() 
    {
		return ID;
	}
}
