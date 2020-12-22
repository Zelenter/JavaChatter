import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.jar.Attributes.Name;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultCaret;

public class Client
{
	private static final long serialVersionUID = 1L;
		
	private DatagramSocket Socket;
	
	private String Name;
	private String Address;
	private int Port;
	private InetAddress IP;
	private Thread SenderThread;
	private int ID = -1;

	public Client(String name, String address, int port)
	{
		Name = name;
		Address = address;
		Port = port;
	}
	public String GetName()
	{
		return Name;
	}
	public String GetAddress()
	{
		return Address;
	}
	public int GetPort()
	{
		return Port;
	}
	public boolean OpenConnection(String address)
	{
		try
		{
			Socket = new DatagramSocket();
			IP = InetAddress.getByName(address);
		}
		catch (UnknownHostException e)
		{
			e.printStackTrace();
			return false;
		}
		catch (SocketException e)
		{
			e.printStackTrace();
			return false;
		}

		return true;
	}
	public String RecievePackage()
	{
		int dataSize = 1024;
		byte[] data = new byte[dataSize];
		DatagramPacket packet = new DatagramPacket(data, data.length);

		try 
		{
			Socket.receive(packet);
		}
		catch (SocketException e) 
		{
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		String message = new String(packet.getData());
		return message;
	}
	public void SendPackage(final byte[] data)
	{
		SenderThread = new Thread("SenderThread")
		{
			public void run()
			{
				DatagramPacket Packet = new DatagramPacket(data, data.length, IP, Port);
				try 
				{
					Socket.send(Packet);
				}
				catch (IOException e) 
				{
					e.printStackTrace();
				}
			}			
		};
		SenderThread.start();
	}
	public void CloseConnection()
	{
		new Thread()
		{
			public void run()
			{
				synchronized (Socket)
				{
					Socket.close();
				}
			}
		}.start();
	}
	public void SetID(int id)
	{
		ID = id;
	}
	public int GetID()
	{
		return ID;
	}
}
