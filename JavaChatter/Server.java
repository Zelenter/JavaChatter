import java.io.IOException;
import java.security.SecureRandom;
import java.net.SocketException;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import jdk.jshell.spi.ExecutionControl.UserException;
import java.util.Scanner;

public class Server implements Runnable
{
    private List<ClientInfo> Clients = new ArrayList<ClientInfo>();
    private List<Integer> ClientResponse = new ArrayList<Integer>();
    
    private DatagramSocket Socket;
    private int Port;
    private boolean Running = false;
    private Thread RunThread; 
    private Thread ManagerThread;
    private Thread ReceiverThread;
    private Thread SenderThread;

    private final int MaxAttempts = 5;

    private boolean Raw = false;

    public Server(int port)
    {
        Port = port;

        try 
        {
            Socket = new DatagramSocket(port);
        } 
        catch (SocketException e) 
        {
            e.printStackTrace();
            return;
        }

        RunThread = new Thread(this, "ServerRunThread");
        RunThread.start();
    }
    private void ReceiveData()
    {
        ReceiverThread = new Thread("ServerReceiverThread")
        {
            public void run()
            {
                while (Running)
                {
                    int dataSize = 1024;
                    byte[] data = new byte[dataSize];
                    
                    DatagramPacket packet = new DatagramPacket(data, data.length);
                    
                    try 
                    {
                        Socket.receive(packet);
                    } 
                    catch (SocketException e) 
                    {}
                    catch (IOException e) 
                    {
                        e.printStackTrace();
                    }

                    ProcessData(packet);
                }
            }
        };
        ReceiverThread.start();
    }
    private void ManageClients()
    {
        ManagerThread = new Thread("ServerManagerThread")
        {
            public void run()
            {
                while (Running)
                {
                    SendToAllClients("/i/server");
                    SendStatus();

                    try
                    {
						Thread.sleep(2000);
                    } 
                    catch (InterruptedException e) 
                    {
						e.printStackTrace();
					}

                    for (int i = 0; i < Clients.size(); i++) 
                    {
                        ClientInfo c = Clients.get(i);

                        if (!ClientResponse.contains(c.getID())) 
                        {
                            if (c.Attempt >= MaxAttempts) 
                            {
                                Disconnect(c.getID(), false);
                            }
                            else 
                            {
                                c.Attempt++;
                            }
                        }
                        else 
                        {
                            ClientResponse.remove(new Integer(c.getID()));
							c.Attempt = 0;
						}
                    }
                }
            }
        };
        ManagerThread.start();
    }
    private void SendToAllClients(String message)
    {
        if (message.startsWith("/m/")) 
        {
			String TextMessage = message.substring(3);
			TextMessage = TextMessage.split("/e/")[0];
			System.out.println(TextMessage);
		}

        for (int i = 0; i < Clients.size(); i++) 
        {
			ClientInfo client = Clients.get(i);
			SendToClient(message.getBytes(), client.Address, client.Port);
		}
    }
    private void SendStatus()
    {
        if (Clients.size() <= 0)
        {
            return;
        }
        else
        {
            String Users = "/u/";

            for (int i = 0; i < Clients.size() - 1; i++)
            {
                Users += Clients.get(i).Name + "/n/";
            }
            Users += Clients.get(Clients.size() - 1).Name + "/e/";
            SendToAllClients(Users);
        }
    }
    private void SendToClient(final byte[] data, final InetAddress address, final int port)
    {
        SenderThread = new Thread("ServerSenderThread") 
        {
            public void run() 
            {
                DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
                try 
                {
					Socket.send(packet);
                }
                catch (IOException e) 
                {
					e.printStackTrace();
				}
			}
        };
        SenderThread.start();
    }
    private void SendMessage(String message, final InetAddress address, final int port)
    {
        message += "/e/";
        SendToClient(message.getBytes(), address, port);
    }
    private void ProcessData(DatagramPacket packet) 
    {
        String packetString = new String(packet.getData());

        if (Raw)
        {
            System.out.println(packetString);
        }
        if (packetString.startsWith("/c/")) 
        {
            int id = UniqueID.getID();

            String ClientName = packetString.split("/c/|/e/")[1];
            System.out.println(ClientName + " [ID:" + id + "] connected to server");

            Clients.add(new ClientInfo(ClientName, packet.getAddress(), packet.getPort(), id));
            String ID = "/c/" + id;
            SendMessage(ID, packet.getAddress(), packet.getPort());
        }
        else if (packetString.startsWith("/m/"))
        {
            SendToAllClients(packetString);
        }
        else if (packetString.startsWith("/d/"))
        {
            String DisconnectClientID = packetString.split("/d/|/e/")[1];
            Disconnect(Integer.parseInt(DisconnectClientID), true);
        }
        else if (packetString.startsWith("/i/"))
        {
            ClientResponse.add(Integer.parseInt(packetString.split("/i/|/e/")[1]));
        }
        else 
        {
			System.out.println(packetString);
		}
    }
    private void Quit()
    {
        for (int i = 0; i < Clients.size(); i++) 
        {
			Disconnect(Clients.get(i).getID(), true);
        }

		Running = false;
		Socket.close();
    }
    private void PrintHelp()
    {
        System.out.println("List of available commands:");
		System.out.println("=========================================");
		System.out.println("/Raw - Enables raw mode.");
		System.out.println("/Clients - Shows all connected clients.");
		System.out.println("/Kick [Users ID or Username] - kicks a user.");
		System.out.println("/Help - Shows this help message.");
        System.out.println("/Quit - Shuts down the server.");
        System.out.println("=========================================");
    }
    private void Disconnect(int id, boolean status)
    {
        ClientInfo DisconnectClient = null;
        String StatusMessage = "";
        boolean Removed = false;

        for (int i = 0; i < Clients.size(); i++)
        {
            if (Clients.get(i).getID() == id)
            {
                DisconnectClient = Clients.get(i);
                Clients.remove(i);
                Removed = true;
                break;
            }
        }

        if (Removed)
        {
            if (status)
            {
                StatusMessage = "Client: " + DisconnectClient.Name + " [" + DisconnectClient.getID() + "] {" + DisconnectClient.Address.toString() + ":" + DisconnectClient.Port + "} disconnected";
            }
            else
            {
                StatusMessage = "Client: " + DisconnectClient.Name + " [" + DisconnectClient.getID() + "] {" + DisconnectClient.Address.toString() + ":" + DisconnectClient.Port + "} timed out";
            }
            System.out.println(StatusMessage);
        }
        else if (!Removed)
        {
            return;
        }
    }
    public void run()
    {
        Running = true;
        System.out.println("Server launched on port " + Port);

        ManageClients();
        ReceiveData();

        Scanner scanner = new Scanner(System.in);
        while (Running)
        {
            String Data = scanner.nextLine();
            
            if (!Data.startsWith("/")) 
            {
				SendToAllClients("/m/Server: " + Data + "/e/");
				continue;
            }
            else if (Data.startsWith("/"))
            {
                Data = Data.substring(1);
            
                if (Data.equals("Raw")) 
                {
                    if (Raw)
                    {
                        System.out.println("Raw mode off.");
                    }
                    else
                    {
                        System.out.println("Raw mode on.");
                    }
                    Raw = !Raw;
                }
                else if (Data.equals("Clients")) 
                {
                    System.out.println("Clients:");
                    System.out.println("========");
    
                    for (int i = 0; i < Clients.size(); i++) 
                    {
                        ClientInfo c = Clients.get(i);
                        System.out.println(c.Name + "(" + c.getID() + "): " + c.Address.toString() + ":" + c.Port);
                    }
    
                    System.out.println("========");
                }
                else if (Data.startsWith("Kick")) 
                {
                    String ClientIndex = Data.split(" ")[1];
                    int ClientID = -1;
                    boolean IsNumber = true;

                    try 
                    {
                        ClientID = Integer.parseInt(ClientIndex);
                    }
                    catch (NumberFormatException e)
                    {
                        IsNumber = false;
                    }

                    if (IsNumber)
                    {
                        boolean ClientExist = false;

                        for (int i = 0; i < Clients.size() && !ClientExist; i++)
                        {
                            if (Clients.get(i).getID() == ClientID)
                            {
                                ClientExist = true;
                            }
                        }

                        if (ClientExist)
                        {
                            Disconnect(ClientID, true);
                        }
                        else
                        {
                            System.out.println("Client with ID: " + ClientID + " doesn't exist");
                        }
                    }
                    else
                    {
                        boolean ClientExist = false;

                        for (int i = 0; i < Clients.size() && !ClientExist; i++) 
                        {
                            ClientInfo c = Clients.get(i);

                            if (ClientIndex.equals(c.Name)) 
                            {
                                ClientExist = true;
                                Disconnect(c.getID(), true);
                            }
                        }
                    }
                }
                else if (Data.equals("Quit"))
                {
                    Quit();
                }
                else if (Data.equals("Help"))
                {
                    PrintHelp();
                }
                else
                {
                    System.out.println("Unknown command.");
                    PrintHelp();
                }
            }
        }
        scanner.close();
    }
}
