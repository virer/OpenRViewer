/** 
 *  OpenRViewer a java base application using RFB protocol(VNC)
    Copyright (C) 2017 Sebastien CAPS

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
     
   */
package orv.swing;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JTextField;

import org.xbill.DNS.TextParseException;

import com.glavsoft.viewer.Viewer;

import orv.lib.func;

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Font;
import java.awt.Color;
import java.awt.SystemColor;
import javax.swing.JLabel;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;

public class MainWindow {
	private final String License = "OpenRViewer version "+func.version+", Copyright (C) 2017 Sebastien CAPS under GNU GPLv2";
	public String serverHostname = "localhost";
	public int serverPortNumber = 443;
	public static JFrame frmOpenrviewer;
	public String serverId = "123456789";
	public static String targetServerID = "123456789";
	public String password = "abcdef";
	public JTextField id_textField;
	private JTextField txtPassword;
	public JTextField clientTextField;
	public boolean localdevelmode = false;
	private JLabel lblStatus;
	
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MainWindow.frmOpenrviewer.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public MainWindow() {
		initialize();
	}

	/**
	 * Format serverId to display it with some space interval
	 */
	public void formatServerId() {
		String parts1 = serverId.substring(0, 3);
		String parts2 = serverId.substring(3, 6);
		String parts3 = serverId.substring(6, 9);
		String parts4 = serverId.substring(9, serverId.length());
		id_textField.setText(parts1 + " " + parts2 + " " + parts3 + " " + parts4 );
	}

	/**
	 * Format serverId to display it with some space interval
	 */
	public void setPassword() {
		String parts1 = password.substring(0, 2);
		String parts2 = password.substring(2, 4);
		String parts3 = password.substring(4, 6);
		txtPassword.setText(parts1 + " " + parts2 + " " + parts3 );
	}
	
	/**
	 * Change Status
	 */
	public void changeStatus(String status) {
		lblStatus.setText(status);
	}
	
	private void startManagerMode() {
		targetServerID = clientTextField.getText().toString().replaceAll("\\s+", "").toUpperCase().trim();
		/*
		 * Get the first two letter of the targetId that identify the OpenRCloudServer name  
		 */
		String srvName = targetServerID.substring(0, 2);
		try {
			String srvIp = func.getOpenRCloudServerIP(srvName);
			if(localdevelmode) srvIp = "192.168.100.121"; 
			String[] params = new String[]{ "-host="+srvIp,"-port=443","-showConnectionDialog=No","-targetServerId="+targetServerID,"-LocalPointer=on" };
			Viewer.StartViewer(params);	
		} catch (TextParseException e) {
			func.displayErrorMsg("Error cannot get the OpenRCloudServer IP address, please check your internet connection \n"+e.getMessage());
		}
	}
	
	/**
	 * Initialize the contents of the frame.
	 * @wbp.parser.entryPoint
	 */
	@SuppressWarnings("static-access")
	private void initialize() {
		frmOpenrviewer = new JFrame();
		frmOpenrviewer.setTitle("OpenRViewer");
		frmOpenrviewer.setBounds(100, 100, 600, 440);
		frmOpenrviewer.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		try {
			frmOpenrviewer.setIconImage(ImageIO.read(this.getClass().getResourceAsStream("/OpenRViewer.png")));
		} catch (IOException e1) {
			System.out.println(e1);
		}
		
		JMenuBar menuBar = new JMenuBar();
		frmOpenrviewer.setJMenuBar(menuBar);
		
		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);
		
		JMenuItem mntmOptions = new JMenuItem("Options");
		mnFile.add(mntmOptions);
		
		JMenuItem mntmAbout = new JMenuItem("About");
		mntmAbout.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(frmOpenrviewer, License);
			}
		});
		mnFile.add(mntmAbout);
		
		JMenuItem mntmExit = new JMenuItem("Exit");
		mntmExit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//openRViewer.stop();
				System.exit(0);
			}
		});
		mnFile.add(mntmExit);
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{0, 44, 0, 55, 55, 96, 96, 57, 39, 44, 57, 45, 96, 96, 0};
		gridBagLayout.rowHeights = new int[]{37, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
		gridBagLayout.columnWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		frmOpenrviewer.getContentPane().setLayout(gridBagLayout);
		
		JLabel lblAllowRemoteControl = new JLabel("Allow remote control");
		lblAllowRemoteControl.setFont(new Font("Monospaced", Font.BOLD, 14));
		GridBagConstraints gbc_lblAllowRemoteControl = new GridBagConstraints();
		gbc_lblAllowRemoteControl.gridwidth = 4;
		gbc_lblAllowRemoteControl.insets = new Insets(0, 0, 5, 5);
		gbc_lblAllowRemoteControl.gridx = 3;
		gbc_lblAllowRemoteControl.gridy = 1;
		frmOpenrviewer.getContentPane().add(lblAllowRemoteControl, gbc_lblAllowRemoteControl);
		
		JLabel lblYourOpenrviewerId = new JLabel("Your OpenRViewer ID :");
		lblYourOpenrviewerId.setFont(new Font("Monospaced", Font.BOLD, 11));
		GridBagConstraints gbc_lblYourOpenrviewerId = new GridBagConstraints();
		gbc_lblYourOpenrviewerId.anchor = GridBagConstraints.WEST;
		gbc_lblYourOpenrviewerId.gridwidth = 7;
		gbc_lblYourOpenrviewerId.insets = new Insets(0, 0, 5, 5);
		gbc_lblYourOpenrviewerId.gridx = 3;
		gbc_lblYourOpenrviewerId.gridy = 3;
		frmOpenrviewer.getContentPane().add(lblYourOpenrviewerId, gbc_lblYourOpenrviewerId);
		
		id_textField = new JTextField();
		id_textField.setText("12345678901234");
		id_textField.setEditable(false);
		id_textField.setBackground(SystemColor.control);
		id_textField.setForeground(Color.BLUE);
		id_textField.setFont(new Font("Monospaced", Font.BOLD, 18));
		GridBagConstraints gbc_id_textField = new GridBagConstraints();
		gbc_id_textField.gridwidth = 4;
		gbc_id_textField.insets = new Insets(0, 0, 5, 5);
		gbc_id_textField.fill = GridBagConstraints.HORIZONTAL;
		gbc_id_textField.gridx = 3;
		gbc_id_textField.gridy = 4;
		frmOpenrviewer.getContentPane().add(id_textField, gbc_id_textField);
		id_textField.setColumns(10);
		
		JLabel lblYourOpenrviewerPassword = new JLabel("Your OpenRViewer password :");
		lblYourOpenrviewerPassword.setFont(new Font("Monospaced", Font.BOLD, 11));
		GridBagConstraints gbc_lblYourOpenrviewerPassword = new GridBagConstraints();
		gbc_lblYourOpenrviewerPassword.gridwidth = 4;
		gbc_lblYourOpenrviewerPassword.anchor = GridBagConstraints.WEST;
		gbc_lblYourOpenrviewerPassword.insets = new Insets(0, 0, 5, 5);
		gbc_lblYourOpenrviewerPassword.gridx = 3;
		gbc_lblYourOpenrviewerPassword.gridy = 6;
		frmOpenrviewer.getContentPane().add(lblYourOpenrviewerPassword, gbc_lblYourOpenrviewerPassword);
		
		txtPassword = new JTextField();
		txtPassword.setText("abcdef");
		txtPassword.setForeground(Color.BLUE);
		txtPassword.setFont(new Font("Monospaced", Font.BOLD, 18));
		txtPassword.setEditable(false);
		txtPassword.setColumns(10);
		txtPassword.setBackground(SystemColor.menu);
		GridBagConstraints gbc_txtPassword = new GridBagConstraints();
		gbc_txtPassword.gridwidth = 4;
		gbc_txtPassword.insets = new Insets(0, 0, 5, 5);
		gbc_txtPassword.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtPassword.gridx = 3;
		gbc_txtPassword.gridy = 7;
		frmOpenrviewer.getContentPane().add(txtPassword, gbc_txtPassword);
		
		JLabel lblRemoteId = new JLabel("Remote ID :");
		lblRemoteId.setFont(new Font("Monospaced", Font.BOLD, 11));
		GridBagConstraints gbc_lblRemoteId = new GridBagConstraints();
		gbc_lblRemoteId.insets = new Insets(0, 0, 5, 5);
		gbc_lblRemoteId.gridx = 10;
		gbc_lblRemoteId.gridy = 11;
		frmOpenrviewer.getContentPane().add(lblRemoteId, gbc_lblRemoteId);
		
		clientTextField = new JTextField();
		clientTextField.setForeground(Color.BLUE);
		clientTextField.setFont(new Font("Monospaced", Font.BOLD, 18));
		clientTextField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
			    if (e.getKeyCode()==KeyEvent.VK_ENTER){
			    	startManagerMode();
			    }
			}
		});
		GridBagConstraints gbc_clientTextField = new GridBagConstraints();
		gbc_clientTextField.gridwidth = 3;
		gbc_clientTextField.insets = new Insets(0, 0, 5, 5);
		gbc_clientTextField.fill = GridBagConstraints.HORIZONTAL;
		gbc_clientTextField.gridx = 9;
		gbc_clientTextField.gridy = 12;
		frmOpenrviewer.getContentPane().add(clientTextField, gbc_clientTextField);
		clientTextField.setColumns(10);
		
		JButton btnNewButton = new JButton("Connect");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				startManagerMode();
					}
				});
		GridBagConstraints gbc_btnNewButton = new GridBagConstraints();
		gbc_btnNewButton.insets = new Insets(0, 0, 5, 5);
		gbc_btnNewButton.gridx = 11;
		gbc_btnNewButton.gridy = 13;
		frmOpenrviewer.getContentPane().add(btnNewButton, gbc_btnNewButton);
		
		lblStatus = new JLabel("Status: Initializing...");
		GridBagConstraints gbc_lblStatus = new GridBagConstraints();
		gbc_lblStatus.anchor = GridBagConstraints.SOUTHWEST;
		gbc_lblStatus.gridwidth = 8;
		gbc_lblStatus.insets = new Insets(0, 0, 0, 5);
		gbc_lblStatus.gridx = 3;
		gbc_lblStatus.gridy = 21;
		frmOpenrviewer.getContentPane().add(lblStatus, gbc_lblStatus);
		
		frmOpenrviewer.setVisible(true);
		
	}
	
}
