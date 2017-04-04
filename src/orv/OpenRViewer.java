/*
OpenRViewer a java based application using RFB protocol(VNC)
Copyright (C) 2017 Sebastien CAPS

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

/**
 * File based on a demo(RFBService) application by Igor Delac that show how to implement RFB service and protocol to
 * allow Swing / GUI components to be displayed remotely, usually on VNC viewers.
 * Source: https://github.com/idelac3/RFBService
 * 
 * @author OpenRViewer s.caps
 */

package orv;

import java.awt.AWTException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;

import org.xbill.DNS.*;

import orv.RCuser.OpenRClass;
import orv.RCuser.RobotKeyboard;
import orv.RCuser.RobotMouse;
import orv.RCuser.RobotScreen;
import orv.lib.func;
import orv.swing.MainWindow;

public class OpenRViewer implements Runnable {	
	/**
	 * List of open VNC sessions. 
	 */
	public static List<OpenRClass> rfbClientList = new ArrayList<OpenRClass>();
	
	public static Socket clientSocket;
	private static BufferedInputStream in;
	private static BufferedOutputStream out;
	
	
	public static String serverId = "";
	public static String password = "";
	public static MainWindow MW = null;
	public static String routerVersion = "";
	
	private static boolean localdevelmode = false;
	
	private static func lib;
	
	/**
	 * Look for Nimbus look. Better than default Metal look.
	 */
	private static void changeLookAndFeel() throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {
    	LookAndFeelInfo[] installedInfos = UIManager.getInstalledLookAndFeels();
    	for (LookAndFeelInfo info : installedInfos) {
    		if (info.getName().equalsIgnoreCase("Nimbus")) {
    			UIManager.setLookAndFeel(info.getClassName());
    			break;
    		}
    	}
	}
	
	/**
	 * Initialize static Robot objects for screen, keyboard and mouse. 
	 */
	private static void initRobot() throws AWTException {	
		RobotScreen.robo = new orv.RCuser.RobotScreen();
		RobotKeyboard.robo = new orv.RCuser.RobotKeyboard();
		RobotMouse.robo = new orv.RCuser.RobotMouse();
		RobotScreen.robo.run();
		RobotMouse.robo.run();
	}
	
	/**
	 * Server selection
	 */
	private static String selectedServerIP() {
		Record[] records;
		try {
			// TODO add zone checking before: like europe, us-east, us-west, asia, ...
			records = new Lookup("srvlist.openrviewer.org", Type.TXT).run();
			TXTRecord txt;
			String aR;
			String[] srvlist;
			for (int i = 0; i < records.length; i++) {
				txt = (TXTRecord) records[i];
				srvlist = txt.rdataToString().replaceAll("\"", "").split(",");
				for(String srv: srvlist) {
						aR = func.getOpenRCloudServerIP(srv);
						// TODO ping the full selection of servers to get the server with the minimum latency
						lib.openRCloudServer = srv;
						return aR;
				}
			}
			
		} catch (TextParseException e) {
			System.out.println(e.getMessage());
		}
		return "localhost";
	} 
	
	private static boolean connect(String routerHostname){
		try {
			lib = new func();
			clientSocket = lib.createSSLSocket(routerHostname, 443);
	        if(null == clientSocket || !clientSocket.isConnected()) {
	        	return false;
	        }
		
	        in = new BufferedInputStream(clientSocket.getInputStream());
	        out = new BufferedOutputStream(clientSocket.getOutputStream());

	        System.out.println("Connected, sending our ID: "+serverId);
			String httpHeader = func.genHttpHeader(routerHostname, serverId, false);
			out.write(httpHeader.getBytes());
			out.flush();
			
			/*
			 * Read success of registering
			 */
			byte a[] = new byte[1];
		
			// mark, read one byte, rewind  this is the only way to get in.available() value on a SSL socket
			in.mark(1);
			in.read(a);
			in.reset();
			
			a = new byte[in.available()];
			in.read(a);
			String result = new String(a);
			if(result.indexOf("HTTP/1.1 200 OK") == -1 ) {
				System.out.println("Not registered => disconnecting");
				clientSocket.close();
				return false;
			} else {
				routerVersion = result.substring(result.indexOf("ORV-Version: ")+13, result.indexOf("Cache-Control")).trim();
				if(!routerVersion.equals(func.version)) {
					String msg = "Router version("+routerVersion+") differ from Viewer version("+func.version+") check for update";
					System.out.println(msg);
					func.displayErrorMsg(msg);
					msg=null;
				}
				return true;
			}
		} catch (IOException e) {
			System.out.println(e.getMessage());
			return false;
		} 
	}
	
	/**
	 * authentification of the manager
	 * @return
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 */
	private static boolean authentification() throws IOException, NoSuchAlgorithmException {
		boolean authenticated = false;
		int i = 0;
		String salt_str  = "";
		byte[] auth       = new byte[2];
		byte[] hash_len   = new byte[4];
		byte[] hash_data  = new byte[1];
		byte[] hash_expected = new byte[1024];
		byte[] wrong_pw   = new byte[]{ (byte) 0x02, (byte) 0x04 };
		byte[] auth_failed= new byte[]{ (byte) 0x04, (byte) 0x04 };

		/**
		 * -> 0x02, 0x01 : manager ask for salt
		 * -> 0x02, 0x02 : RCuser send salt to manager
		 * -> 0x02, 0x03 : manager send hash of salt and password
		 * -> 0x02, 0x04 : RCuser reply to manager wrong password
		 * -> 0x03, 0x01 : RCuser reply to manager password accepted -> start RFB protocol
		 */
		int len = 0;
		int wrong_password_count = 0;
		while(authenticated == false && !clientSocket.isClosed()) {
			System.out.println("Authentication: waiting for manager data...");
			in.read(auth);
			System.out.println("Authentication: bytes from manager received.");
			
			if( (byte) 0x02 == auth[0] && (byte) 0x03 == auth[1]) {
				// Receiving hash from manager
				in.read(hash_len);
				len = ByteBuffer.wrap(hash_len).order(ByteOrder.BIG_ENDIAN).getInt();
				hash_data = new byte[len];
				in.read(hash_data);		
				if( hash_expected != null &&  MessageDigest.isEqual(hash_data, hash_expected) ) {
						authenticated = true;
						break;	
				} else {
						// Wrong password !
						hash_expected = null;
						if(wrong_password_count > 6) {
							 out.write(auth_failed);
							 out.flush();
							 break; 
						}
						wrong_password_count++;
						System.out.println("Authentication: Wrong password !");
			    		try {
			    			Thread.sleep(5000); // Pause for 5 seconds
			    		} catch (InterruptedException e) {
			    			System.out.println(e.getMessage());
			    		}
			    		out.write(wrong_pw);
			    		out.flush();
				}
			} else if((byte) 0x02 == auth[0] && (byte) 0x01 == auth[1]) {
				// manager ask for salt => Sending salt random bytes
				salt_str = func.randomNum(10);
				byte[] salt_len = ByteBuffer.allocate(4).putInt(salt_str.getBytes().length).array();
				byte to_send[] = new byte[6+salt_str.getBytes().length];
								
				to_send[0] = (byte) 0x02;
				to_send[1] = (byte) 0x02;
				to_send[2] = salt_len[0];
				to_send[3] = salt_len[1];
				to_send[4] = salt_len[2];
				to_send[5] = salt_len[3];
								
				for(i=6; i < to_send.length ;i++) {
					to_send[i] = salt_str.getBytes()[i-6];
				}
				out.write(to_send);
				out.flush();
				String salt_and_pwd = salt_str + "" + password;
								
				MessageDigest digest;
				digest = MessageDigest.getInstance("SHA-512");
				hash_expected = digest.digest(salt_and_pwd.getBytes(StandardCharsets.UTF_8));
					System.out.println("Authentication: salt sent");
			} else {
				try {
		    		Thread.sleep(1200); //Pause for 1.2 seconds
		    	} catch (InterruptedException e) {
		    		System.out.println(e.getMessage());
		    	}
			}
		}
		// Auth ^
		if(!authenticated) {
			// Error during authentication
			System.out.println("Authentication error! exiting...");
			clientSocket.close();
			return false;
		} else if(authenticated) {
			System.out.println("Authentication successful starting RFB...");
			// Sending auth ok bytes to server (then server repeat it to manager)
			byte ok_bytes[] = new byte[] { (byte) 0x03, (byte) 0x01 };
			out.write(ok_bytes);
			out.flush();
			return true;
		}
		return false;
	}
	
	private static void connectAndAUth(String serverHostname) {
		while(true) {
			/*
			 * Connection to the selected server
			 */
			System.out.println("Connecting to " + serverHostname + " on port " + 443);
			for(int y=0; y<7; y++) {
				if(connect(serverHostname)) {
					if(null != MW) { MW.changeStatus("Status: ready."); }
					break;
				} else {
					try {
						Thread.sleep(10000); // Wait 10 seconds then retry
					} catch (InterruptedException e) {
						System.out.println(e.getMessage());
					}
				}
			}
		
			try {
				boolean auth =  authentification();
				if(auth == true) {
					if(null != MW) { MW.changeStatus("Status: manager authenticated, starting remote session"); }
					
					// Minimise main window
					MainWindow.frmOpenrviewer.toBack();
				
					// Start RFB protocol
					OpenRClass rfbService = new OpenRClass(clientSocket);
					rfbService.RFB();
					
					// Restore window
					MainWindow.frmOpenrviewer.toFront();
				} else {
					try {
						clientSocket.close();
					} catch(IOException e) {
						System.out.println(e.getMessage());
					}
				}
			} catch(IOException e) {
				System.out.println(e.getMessage());
			} catch (NoSuchAlgorithmException e) {
				System.out.println(e.getMessage());
			}
		}
	}
	
	/**
	 * Main application entry.
	 * 
	 * @param args
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws UnsupportedLookAndFeelException
	 * @throws IOException
	 * @throws AWTException
	 */
	public static void main(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException, AWTException {
		lib = new orv.lib.func();
		serverId = lib.genId().toUpperCase();
		password = func.randomNum(6);
		/*
    	 * Select the server to use and get the RCuser serverID based on it 
    	 */
    	String serverHostname = selectedServerIP();
		
    	changeLookAndFeel();
		initRobot();
    	
		/*
    	 * Start Swing GUI in separate thread.
    	 */
    	SwingUtilities.invokeLater(new Runnable() {			
			@Override
			public void run() {
				MW = new MainWindow();
				MainWindow.frmOpenrviewer.setVisible(true);
				MW.serverId = serverId;
				MW.password = password;
				MW.setPassword();
				MW.formatServerId();
				MW.localdevelmode = localdevelmode;
				MW.changeStatus("Status: NOT ready.");
			}
		});
    	
		if(args.length > 0 && args[0].equals("-devel")) localdevelmode  = true;
    	if(localdevelmode) serverHostname = "192.168.100.121"; 
    	
    	connectAndAUth(serverHostname);
    	stop();
	}

	public static void stop() {
		try {
			clientSocket.close();
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}

	@Override
	public void run() {
		try {
			main(null);
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException | AWTException e) {
			System.out.println(e.getMessage());
		}
	}
}
