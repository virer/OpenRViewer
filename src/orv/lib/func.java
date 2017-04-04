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
package orv.lib;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.xbill.DNS.ARecord;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

public class func {
	public static final String version = "0.01";
	private static JFrame frmOpt;
	
	/**
	 * TODO Detect geographical location and use selected server
	 */
	public String openRCloudServer = "AA";
	
	/**
	 * Get openRCloudServer IP based on his name
	 * @throws TextParseException 
	 */
	public static String getOpenRCloudServerIP(String name) throws TextParseException {		
		Record[] records = new Lookup(name+".srv.openrviewer.org", Type.A).run();
		ARecord aR = (ARecord) records[0];
		return aR.rdataToString();
	}
	
	/**
	 * Generate ID of the system based on his MAC address
	 * @param mac
	 * @return
	 */
	public String genId() {
		String id = getMac().replaceAll("-",""); 
		return (openRCloudServer+id).replaceAll("\\s+","");
	}
	
	/**
	 * Get NIC MAC Address
	 * @return
	 */
	public static String getMac() {
		byte[] mac;
		InetAddress ip;
		try {
			ip = InetAddress.getLocalHost();
			// System.out.println("Current IP address : " + ip.getHostAddress());
			NetworkInterface network = NetworkInterface.getByInetAddress(ip);
			mac = network.getHardwareAddress();
		
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < mac.length; i++) {
				sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
			}
		
			return sb.toString();
		} catch (UnknownHostException e) {
			System.out.println(e.getMessage());
		} catch (SocketException e) {
			System.out.println(e.getMessage());
		}
		return "00-00-00-00-00-00";
	}
	
	/**
	 * Fake HTTP header used to pass basic proxies
	 * @param serverHostname
	 * @param serverId
	 * @param manager
	 * @param version
	 * @return
	 */
	public static String genHttpHeader(String serverHostname, String serverId, boolean manager) {
		String clientType = (manager ? "MANAGER" : "RCUSER"); 
		String header="GET /openrviewer/ HTTP/1.1\n"
				+"Host: "+serverHostname+"\n"
				+"User-Agent: OpenRViewer/"+version+"\n"
				+"Accept: application/octet-stream\n"
				+"Keep-Alive: 9999\n"
				+"Connection: keep-alive\n"
				+"Cookie: "+clientType+"="+serverId+"\n"
				+"Pragma: no-cache"
				+"Cache-Control: no-cache\n\n";
		return header;
	}
	
	public Socket createSSLSocket(String hostname, int port) {
		/**
		 * Load truststore (CAcert) that is stored inside the jar file
		 */
		InputStream trustStore =  this.getClass().getResourceAsStream("/truststore.ks");
	    try {
	    	InetAddress addr = InetAddress.getByName(hostname);
	    	
	    	KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
	    	keyStore.load(trustStore, "openrviewer".toCharArray()); 
	    	
	    	TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
	    	trustManagerFactory.init(keyStore);
	    	
	    	TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
	    	SSLContext sc = SSLContext.getInstance("SSL");
	    	sc.init(null, trustManagers, new java.security.SecureRandom());
	    	SSLContext.setDefault(sc);
	    	SSLSocketFactory sslSocketFactory = sc.getSocketFactory();
	    	
		    SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(addr.getHostAddress(), port);
		    sslSocket.setUseClientMode(true);
		    sslSocket.setEnabledCipherSuites(sslSocket.getSupportedCipherSuites());
		    return (Socket) sslSocket;
		    
	    } catch (NoSuchAlgorithmException | CertificateException | IOException | KeyStoreException | KeyManagementException e) {
	    	System.out.println(e.getMessage());
		} finally { 
	    	try {
				trustStore.close();
			} catch (IOException e) {
				System.out.println(e.getMessage());
			} 
	    }
		return null;
	}
	
	public static String randomNum(int length) {
		String result = ""; 
		for(int i=0;i<length;i++) {
			result = result+((int) (Math.random()*10));
		}
		return result;
	}
	
	public static int[] bytesToIntegers(byte[] input) {
		int intArr[] = new int[input.length / 4];
		   int offset = 0;
		   for(int i = 0; i < intArr.length; i++) {
		      intArr[i] = (input[3 + offset] & 0xFF) | ((input[2 + offset] & 0xFF) << 8) |
		                  ((input[1 + offset] & 0xFF) << 16) | ((input[0 + offset] & 0xFF) << 24);  
		   offset += 4;
		   }
		   return intArr;
	}
	
	public static byte[] integersToBytes(int[] data) {
		
		ByteBuffer byteBuffer = ByteBuffer.allocate(data.length * 4);
		for(int a: data) {
			byteBuffer.putInt(a);
		}
		return byteBuffer.array();
	}
	
	public static int byteArrayToLeInt(byte[] b) {
		return ByteBuffer.wrap(b).order(ByteOrder.BIG_ENDIAN).getInt();
	}

	public static byte[] leIntToByteArray(int i) {
		return ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(i).array();
	}
	
	public static void displayErrorMsg(String msg) {
		if (frmOpt == null) {
            frmOpt = new JFrame();
        }
		frmOpt.setVisible(true);
        frmOpt.setLocation(600, 500);
        frmOpt.setAlwaysOnTop(true);
		JOptionPane.showMessageDialog(frmOpt, msg, msg, JOptionPane.PLAIN_MESSAGE);
		frmOpt.dispose();
	}
	
}
