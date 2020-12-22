import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultCaret;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

public class ClientWindow extends JFrame implements Runnable
{
    private static final long serialVersionUID = 1L;

	private JPanel contentPane;
	private JTextField txtMessage;
	private JTextArea txtHistory;
	private JMenuBar MenuBar;
	private JMenu MenuOptions;
	private JMenuItem MenuOptionsOnlineUsers;

	private UserInfo Users;

	private Thread RunThread; 
	private Thread ListenThread;
	private Client client;
	
	private boolean Running = false;

    public ClientWindow(String name, String address, int port)
    {
        setTitle("Client");
        client = new Client(name, address, port);

        boolean Connected = client.OpenConnection(address);

        if (!Connected) 
        {
			System.err.println("Connection failed!");
			Console("Connection failed!");
        }
        else
        {
            CreateWindow();
        }
        Console(name + " attempting a connection to: " + address + " with port: " + port);

		String connectionInfo = "/c/" + name + "/e/";
		client.SendPackage(connectionInfo.getBytes());

		Users = new UserInfo();
		Running = true;
		RunThread = new Thread(this, "RunThread");
		RunThread.start();
    }
    private void CreateWindow()
    {
        try 
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} 
		catch (Exception e1) 
		{
			e1.printStackTrace();
		}
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);	
		setSize(880, 550);
		setLocationRelativeTo(null);

		MenuBar = new JMenuBar();
		setJMenuBar(MenuBar);

		MenuOptions = new JMenu("Options");
		MenuBar.add(MenuOptions);

		MenuOptionsOnlineUsers = new JMenuItem("Online Users");
		MenuOptionsOnlineUsers.addActionListener(new ActionListener() 
		{
			public void actionPerformed(ActionEvent e) 
			{
				Users.setVisible(true);
			}
		});
		MenuOptions.add(MenuOptionsOnlineUsers);

		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		
		GridBagLayout gbl_contentPane = new GridBagLayout();
		gbl_contentPane.columnWidths = new int[] { 28, 815, 30, 7 };
		gbl_contentPane.rowHeights = new int[] { 25, 485, 40 };
		contentPane.setLayout(gbl_contentPane);
		
		txtHistory = new JTextArea();
		txtHistory.setEditable(false);
		
		JScrollPane historyScroll = new JScrollPane(txtHistory);

		GridBagConstraints ScrollConstrains = new GridBagConstraints();
		ScrollConstrains.fill = GridBagConstraints.BOTH;
		ScrollConstrains.gridx = 0;
		ScrollConstrains.gridy = 0;
		ScrollConstrains.gridwidth = 3;
		ScrollConstrains.gridheight = 2;
		ScrollConstrains.weightx = 1;
		ScrollConstrains.weighty = 1;
		ScrollConstrains.insets = new Insets(0, 5, 0, 0);
		contentPane.add(historyScroll, ScrollConstrains);
		
		txtMessage = new JTextField();
		txtMessage.addKeyListener(new KeyAdapter() 
		{
			public void keyPressed(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_ENTER) 
				{
					Send(txtMessage.getText(), true);
				}
			}
		});
		GridBagConstraints gbc_txtMessage = new GridBagConstraints();
		gbc_txtMessage.insets = new Insets(0, 0, 0, 5);
		gbc_txtMessage.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtMessage.gridx = 0;
		gbc_txtMessage.gridy = 2;
		gbc_txtMessage.gridwidth = 2;
		gbc_txtMessage.weightx = 1;
		gbc_txtMessage.weighty = 0;

		contentPane.add(txtMessage, gbc_txtMessage);
		txtMessage.setColumns(10);
		
		JButton btnSend = new JButton("Send");
		btnSend.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e) 
			{
				Send(txtMessage.getText(), true);
			}
		});

		GridBagConstraints gbc_btnSend = new GridBagConstraints();
		gbc_btnSend.insets = new Insets(0, 0, 0, 5);
		gbc_btnSend.gridx = 2;
		gbc_btnSend.gridy = 2;
		gbc_btnSend.weightx = 0;
		gbc_btnSend.weighty = 0;
		contentPane.add(btnSend, gbc_btnSend);
		
		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				String Disconnect = "/d/" + client.GetID() + "/e/";
				Send(Disconnect, false);
				Running = false;
				client.CloseConnection();
			}
		});

		setVisible(true);
		txtMessage.requestFocusInWindow();
	}
	public void Listen()
	{
		ListenThread = new Thread("ListenThread")
		{
			public void run()
			{
				while (Running)
				{
					String message = client.RecievePackage();

					if (message.startsWith("/c/"))
					{
						client.SetID(Integer.parseInt(message.split("/c/|/e/")[1]));
						Console("Succesfully connected to server with ID: " + client.GetID());
					}
					else if (message.startsWith("/m/"))
					{
						String TextMessage = message.substring(3);
						TextMessage = TextMessage.split("/e/")[0];
						Console(TextMessage);
					}
					else if (message.startsWith("/i/"))
					{
						String TextMessage = "/i/" + client.GetID() + "/e/";
						Send(TextMessage, false);
					}
					else if (message.startsWith("/u/"))
					{
						String[] u = message.split("/u/|/n/|/e/");
						Users.Update(Arrays.copyOfRange(u, 1, u.length - 1));
					}
				}
			}
		};
		ListenThread.start();
	}
	public void run()
	{
		Listen();
	}
    public void Send(String message, boolean isText)
	{
		if	(message.equals(""))
		{
			return; 
		}
		else
		{
			if	(isText)
			{
				message = client.GetName() + ": " + message;
				message = "/m/" + message + "/e/";
				txtMessage.setText("");
			}

			client.SendPackage(message.getBytes());
		}
	}
    public void Console(String message)
	{
		txtHistory.append(message + "\n");
		txtHistory.setCaretPosition(txtHistory.getDocument().getLength());
	}
}
