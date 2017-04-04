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
package orv.RCuser;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import orv.OpenRViewer;
import orv.lib.encoders;
import orv.swing.MainWindow;

public class OpenRClass {
	/**
	 * RFB version string. This string helps vnc viewers to determine RFB server
	 * capabilities and protocol format.<BR>
	 * It is set to version 3.3 mainly because of {@link #SECURITY_TYPE} record.
	 */
	private final byte[] RFB_VER = "RFB 003.003\n".getBytes();
	
	/**
	 * Simple authentication where we don't request any password from user to enter during
	 * session establishment.
	 */
	private final byte[] SECURITY_TYPE = {0x00, 0x00, 0x00, 0x01};
	
	/**
	 * Client socket instance provided in constructor of this class.
	 */
	private Socket clientSocket;
	
	/**
	 * Input stream instance provided in constructor of this class.
	 */
	private BufferedInputStream in;
	
	/**
	 * Output stream instance provided in constructor of this class.
	 */
	private BufferedOutputStream out;
	
	/**
	 * Pixel encoding. How many bits per pixel are used. This is set by client
	 * in {@link #readSetPixelFormat()} method.<BR>
	 * Possible values are: <I>8</I> and <I>32</I><BR>
	 * Other values are not valid.
	 */
	public byte bits_per_pixel = 0;
	
	/**
	 * Desktop color depth. Currently unused.
	 */
	private byte depth = 0;
	
	/**
	 * Big endian flag. Currently unused.
	 */
	private byte big_endian = 0;
	
	/**
	 * True color flag. Currently unused.
	 */
	private byte true_color = 0;
	
	/**
	 * Red color maximum value. Currently unused.
	 */
	private int red_maximum = 0;
	
	/**
	 * Green color maximum value. Currently unused.
	 */
	private int green_maximum = 0;
	
	/**
	 * Blue color maximum value. Currently unused.
	 */
	private int blue_maximum = 0;
	
	/**
	 * Bit position where red color value begin. Currently unused.
	 */
	private byte red_shift = 0;
	
	/**
	 * Bit position where green color value begin. Currently unused.
	 */
	private byte green_shift = 0;
	
	/**
	 * Bit position where blue color value begin. Currently unused.
	 */
	private byte blue_shift = 0;
	
	/**
	 * Color mapping of RGB colors into 8-bit colors.
	 */
	private ColorMap8bit colorMap;
	
	/**
	 * A list of supported encodings that are reported by vnc viewer.
	 * See {@link #readSetEncoding()} method.
	 */
	private List<Integer> supportedEncoding;
	
	/**
	 * TODO change this value by a supported encoding value by both peer (now force using Zlib=6)
	 */
	int usedEncodingType = 6;

	/**
	 * Screen dimension which is initially sent to client. See:<BR>
	 * {@link #sendServerInit(int, int, String)} method.
	 */
	public int screenWidth, screenHeight;
	
	/**
	 * A flag that is set to <I>true</I> when client sends frame buffer update
	 * request with incremental flag.<BR>
	 * This flag is then used by Swing components and listeners to determine if
	 * screen update should be sent to VNC viewer(s).
	 */	
	public boolean incrementalFrameBufferUpdate;
	
	public String serverId = "";
	
	/**
	 * Instance of RFB service.
	 * 
	 * @param clientSocket client socket when a new connection is accepted
	 * 
	 * @throws IOException
	 */
	public OpenRClass(Socket clientSocket) throws IOException {
		
		this.clientSocket = clientSocket;
		
		/*
		 * Create input and output streams. They will be used to write and read bytes.
		 */
		this.in = new BufferedInputStream(clientSocket.getInputStream());
		this.out = new BufferedOutputStream(clientSocket.getOutputStream());
		
		/*
		 * Initially this flag should be false.
		 */
		incrementalFrameBufferUpdate = false;
		
		/*
		 * Instance of 8-bit color map in case that VNC viewer request 8-bit pixel encoding.
		 */
		colorMap = new ColorMap8bit();
		
		/*
		 * Supported encodings, array list of integers.
		 */
		supportedEncoding = new ArrayList<Integer>();
		
	}

	/**
	 * Read 2 bytes from socket and return integer value.
	 * 
	 * @return integer
	 * @throws IOException
	 */
	private int readU16int() throws IOException {
		
		byte[] buffer = new byte[2];
		
		int offset = 0, left = buffer.length;
		while (offset < buffer.length) {
			int numOfBytesRead = 0;
			numOfBytesRead = in.read(buffer, offset, left);
			offset = offset + numOfBytesRead;
			left = left - numOfBytesRead;
		}
		
		int value = ((buffer[0] & 0xFF) << 8)
				+ (buffer[1] & 0xFF);

		return value;
		
	}

	/**
	 * Read 4 bytes from socket and return integer value.
	 * 
	 * @return integer
	 * @throws IOException
	 */
	private int readU32int() throws IOException {
		
		byte[] buffer = new byte[4];
		
		int offset = 0, left = buffer.length;
		while (offset < buffer.length) {
			int numOfBytesRead = 0;
			numOfBytesRead = in.read(buffer, offset, left);
			offset = offset + numOfBytesRead;
			left = left - numOfBytesRead;
		}
		
		int value = (
				  ((buffer[0] << 24) & 0xFF000000)
				| ((buffer[1] << 16) & 0x00FF0000)
				| ((buffer[2] << 8 ) & 0x0000FF00)
				| ( buffer[3]        & 0x000000FF));

		return value;
		
	}

	/**
	 * Write 2 bytes on socket.
	 * 
	 * @param value integer value between 0 and 65535
	 * @throws IOException
	 */
	private void writeU16int(int value) throws IOException {
		
		byte[] buffer = new byte[2];
		
		buffer[0] = (byte) ((value & 0x0000FF00) >> 8);
		buffer[1] = (byte) ((value & 0x000000FF) );
		
		out.write(buffer);
		
	}
	
	/**
	 * Write 4 bytes on socket.
	 * 
	 * @param value integer value between <I>Integer.MIN_VALUE</I> and <I>Integer.MAX_VALUE</I>
	 * @throws IOException
	 */
	private void writeS32int(int value) throws IOException {

		byte[] buffer = new byte[4];
		
		buffer[0] = (byte) ((value & 0xFF000000) >> 24);
		buffer[1] = (byte) ((value & 0x00FF0000) >> 16);
		buffer[2] = (byte) ((value & 0x0000FF00) >> 8);
		buffer[3] = (byte) ((value & 0x000000FF) );
		
		out.write(buffer);
		
	}
	
	/**
	 * Send protocol version. A string is defined in {@link #RFB_VER} member.<BR>
	 * This is first method that should be executed when a client establish TCP connection.
	 * 
	 * @throws IOException
	 */
	private void sendProtocolVersion() throws IOException {
		out.write(RFB_VER);
		out.flush();
	}
	
	/**
	 * Read array of bytes from socket.
	 * 
	 * @param len how many bytes to read
	 * @return byte array
	 * @throws IOException
	 */
	private byte[] readU8Array(int len) throws IOException {
		byte[] buffer = new byte[len];
		int offset = 0, left = buffer.length;
		while (offset < buffer.length) {
			int numOfBytesRead = 0;
			numOfBytesRead = in.read(buffer, offset, left);
			offset = offset + numOfBytesRead;
			left = left - numOfBytesRead;
		}
		return buffer;		
	}
	
	/**
	 * Read byte array that represents protocol version.
	 * It's something like: <I>RFB 003.008\n</I>
	 * 
	 * @return string that is built on byte array
	 * @throws IOException
	 */
	private String readProtocolVersion() throws IOException {
		byte[] buffer = readU8Array(12); 
		return new String(buffer);
	}
	
	/**
	 * Send security type bytes. In this implementation, it is the most simple:
	 * no authentication. Other implementations usually require user to type 
	 * password to access RFB service with VNC viewer.
	 * 
	 * @throws IOException
	 */
	private void sendSecurityType() throws IOException {
		out.write(SECURITY_TYPE);
		out.flush();
	}
	
	/**
	 * Shared desktop flag tells RFB server if more VNC viewers are allowed to access
	 * screen at same time. Protocol gives possibility that VNC viewer(s) can decide
	 * to share screen or not.
	 *  
	 * @return byte value <I>0x00</I> or <I>0x01</I>, see RFB protocol specification
	 * @throws IOException
	 */
	private byte readSharedDesktop() throws IOException {
		int buffer = 0;
		buffer = in.read();
		return (byte) buffer;
	}

	/**
	 * Send server initial message to client. All RFB implementations have to do this.
	 * This message tells VNC viewer what are RFB server capabilities, like bits per pixel,
	 * depth, screen width and height, etc.
	 * 
	 * @param width
	 * @param height
	 * @param windowTitle
	 * @throws IOException
	 */
	private void sendServerInit(int width, int height, String windowTitle) throws IOException {
		
		writeU16int(width);
		writeU16int(height);
		
		byte bits_per_pixel = (byte) Toolkit.getDefaultToolkit().getColorModel().getPixelSize();
		byte depth = bits_per_pixel;
		byte big_endian = 0;
		byte true_color = 1;
		
		byte red_max = (byte) 0xFF, green_max = (byte) 0xFF, blue_max = (byte) 0xFF;
		byte red_shift = 16, green_shift = 8, blue_shift = 0;
		
		if (bits_per_pixel == 24) {
			/*
			 * VNC viewers do not support color mode of 24-bits.
			 */
			bits_per_pixel = 32;
			depth = bits_per_pixel;
		}
		
		if (bits_per_pixel == 16) {
			/*
			 * Just in case that display is a 16-bit color mode.
			 * Use appropriate maximum color values and bit positions at which 
			 * each color value begins.
			 */
			red_max = (byte) 0x1F; green_max = (byte) 0x3F; blue_max = (byte) 0x1F;
			red_shift = 11; green_shift = 5; blue_shift = 0;
		}
		
		byte[] pixel_format = {
				bits_per_pixel, // bits-per-pixel
				depth, // depth
				big_endian,  // big-endian-flag
				true_color,  // true-color-flag
				0, red_max, // red-max
				0, green_max, // green-max
				0, blue_max, // blue-max
				red_shift, // red-shift
				green_shift, // green-shift
				blue_shift, // blue-shift
				0, 0, 0 // padding
				};		
		out.write(pixel_format);

		if (windowTitle.length() > 255) {
			windowTitle = windowTitle.substring(0, 255);
		}

		byte[] titleLen = {0, 0, 0, (byte) windowTitle.length()};
		out.write(titleLen);
		out.write(windowTitle.getBytes());			

		out.flush();
	}
	
	/**
	 * Read pixel format message sent from client.
	 * Here the most important is value of bits per pixel. Client may ask to
	 * send frame buffers with lower color value (eg. 8-bits). Other
	 * parameters are ignored.
	 * 
	 * @throws IOException
	 */
	private void readSetPixelFormat() throws IOException {

		byte[] header = readU8Array(4);
		
		if (header[0] != 0x00) {
			throw new IOException();
		}
		
		bits_per_pixel = (byte) in.read();
		depth = (byte) in.read();
		big_endian = (byte) in.read();
		true_color = (byte) in.read();
		red_maximum = readU16int();
		green_maximum = readU16int();
		blue_maximum = readU16int();
		red_shift = (byte) in.read();
		green_shift = (byte) in.read();
		blue_shift = (byte) in.read();
		
		byte[] padding = new byte[3];
		if (in.read(padding) < 3) {
			int max = in.available();
			byte[] b= new byte[max]; 
			in.read(b, 0, max);
			b = null;
			System.out.println("Error?IOException?");
			//throw new IOException();
		}
		
		log ("Bits per pixel: " + bits_per_pixel);
		log ("Depth: " + depth);
		log ("Big endian flag: " + big_endian);
		log ("True color flag: " + true_color);
		log ("Red, green, blue max. : " + red_maximum + ", " + green_maximum + ", " + blue_maximum);
		log ("Red, green, blue shift: " + red_shift + ", " + green_shift + ", " + blue_shift);

	}

	/**
	 * Populate list of supported encodings that VNC viewer supports.
	 * This list is for example used when screen size change (eg. JFrame change it's size)
	 * and clients who support desktop size pseudo encoding can be informed about it.
	 * 
	 * @throws IOException
	 */
	private void readSetEncoding() throws IOException {
		
		int messageType = in.read();
		int padding = in.read();
		
		if (messageType == 0x02) {
			
			int numberOfEncodings = readU16int();
			while (numberOfEncodings > 0) {
				int encoding = readU32int();
				supportedEncoding.add(encoding);
				System.out.println("Encoding:"+encoding);
				numberOfEncodings--;
			}
		}
		else {
			throw new IOException(
					"Error in readSetEncoding(): messageType = " + messageType + ", padding = " + padding + ". Available in buffer: " + in.available() + " bytes.");
		}
	}

	/**
	 * Read key event that is sent from client.
	 * Keystroke is then send to system just like the user hit the key.
	 * 
	 * @throws IOException
	 */
	private void readKeyEvent() throws IOException {
		
		int messageType = in.read();
		if (messageType != 0x04) {
			throw new IOException();
		}
		
		/*
		 * Down flag:
		 *  0 -  user released key.
		 *  1 -  user press key.
		 */
		int downFlag = in.read();
		
		@SuppressWarnings("unused")
		int padding = readU16int();
		
		int keyValue = readU32int();		

		RobotKeyboard.robo.sendKey(keyValue, downFlag);

	}
	
	/**
	 * Read pointer event data. This happens when
	 * user moves mouse in VNC viewer, clicks, etc.
	 * 
	 * @throws IOException
	 */
	private void readPointerEvent() throws IOException {
		
		int messageType = in.read();
		if (messageType != 0x05) {
			throw new IOException();
		}
		
		/*
		 * Button mask:
		 * 1 - left button
		 * 2 - middle button
		 * 4 - right button
		 * 8 - wheel up
		 * 16 - wheel down
		 */
		int buttonMask = in.read();
		
		int x_pos = readU16int();
		int y_pos = readU16int();
		
		if (buttonMask > 0) {
				
				/*
				 * Check for button and generate mouse click.
				 */
				if (buttonMask == 1) {
					/*
					 * Left click.
					 */
					RobotMouse.robo.mouseClick(x_pos, y_pos);
					deltaUpdate();
				}
				else if (buttonMask == 3) {
					/*
					 * Middle click.
					 */
					RobotMouse.robo.mouseMiddleClick(x_pos, y_pos);
				}
				else if (buttonMask == 4) {
					/*
					 * Right click.
					 */
					RobotMouse.robo.mouseRightClick(x_pos, y_pos);
					deltaUpdate();
				}
				else if (buttonMask == 8) {
					/*
					 * Mouse wheel.
					 */
					RobotMouse.robo.mouseWheel(-100);
					deltaUpdate();
				}
				else if (buttonMask == 16) {
					/*
					 * Mouse wheel.
					 */
					RobotMouse.robo.mouseWheel(100);
					deltaUpdate();
				}
				//deltaUpdateInBackground deltaUpd = new deltaUpdateInBackground(screenWidth, screenHeight);
				//deltaUpd.main();
		}
		else {
			/*
			 * Move mouse pointer only.
			 */
			RobotMouse.robo.mouseMove(x_pos, y_pos);
		}
		
	}

	/**
	 * Transfer client text to system clipboard.
	 * This is triggered when VNC viewer detects
	 * text in clipboard and decide to transfer it to RFB server.
	 * It's up to RFB server how to handle text, it can be transfered
	 * to system clipboard or simply ignored.
	 * 
	 * @throws IOException
	 */
	private void readClientCutText() throws IOException {
		
		byte[] header = readU8Array(4);
		int textLen = readU32int();

		if ( header[0] == 0x06 ) {
			
			/*
			 * Read bytes that are encoded as Latin-1 characters.
			 */
			byte[] textBuffer = readU8Array(textLen);
		    String textLine = new String(textBuffer);
		    
		    /*
		     * Transfer to system clipboard.
		     */
			StringSelection selection = new StringSelection(textLine);
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		    clipboard.setContents(selection, selection);
			
		}
		else {
			throw new IOException();
		}
		
	}

	/**
	 * Reads frame buffer update request.<BR>
	 * Two types are possible: <I>full</I> and <I>incremental</I>
	 * Full request will immediately trigger sending of complete screen to
	 * client, while incremental will let GUI/Swing components to decide when to
	 * send update to clients.
	 * 
	 * @throws IOException
	 */
	@SuppressWarnings("unused")
	private void readFrameBufferUpdateRequest() throws IOException {
		
		int messageType = in.read();
		int incremental = in.read();
		
		if (messageType == 0x03) {
			
			int x_pos = readU16int(); 
			int y_pos = readU16int();
			int width = readU16int();
			int height = readU16int();
			
			if (incremental == 0x00) {
				log ("Frame buffer update request received. Full update requested.");
				incrementalFrameBufferUpdate = false;
				
				RobotScreen.robo.getScreenshot(0, 0, screenWidth, screenHeight); 
				byte[] data = encoders.encode(RobotScreen.robo.getColorImageBuffer(), 6, 32);
				sendFrameBufferUpdate(0, 0, screenWidth, screenHeight, 6, data);
				
				data = null;
			} else if (incremental == 0x01) {
				incrementalFrameBufferUpdate = true;
				deltaUpdate();
			} else {
				throw new IOException();
			}
		}
		else {
			throw new IOException();
		}
	
	}

	/**
	 * Send frame buffer update to client.
	 * 
	 * @param x position of rectangle, value 0 is upper top corner
	 * @param y position of rectangle, value 0 is left corner
	 * @param width width in pixels
	 * @param height height in pixels
	 * @param encodingType encoding type, 0 is raw, for other values see RFB protocol spec.
	 * @param screen screen buffer which holds image
	 * @throws IOException
	 */
	public void sendFrameBufferUpdate(int x, int y, int width, int height, int encodingType, byte[] screen) throws IOException {
		if (x + width > screenWidth || y + height > screenHeight) {
			err ("Invalid frame update size request:"); 
			err (" x, y = " + x + ", " + y);
			err (" Width x height = " + width + " x " + height);
			err (" Screen width x height = " + screenWidth + " x " + screenHeight);
			return;
		}
		
		// ensure everything is flushed out
		//out.flush();
		
		byte messageType = 0x00;
		byte padding     = 0x00;
		
		out.write(messageType);
		out.write(padding);
		
		int numberOfRectangles = 1;
		
		writeU16int(numberOfRectangles);	
		
		writeU16int(x);
		writeU16int(y);
		writeU16int(width);
		writeU16int(height);
		writeS32int(encodingType);

		log ("Framebuffer update at (" + x + ", " + y + "). Rectangle: " + width + "x" + height + 
				", encoding: " + encodingType + ", incremental: " + incrementalFrameBufferUpdate +
				", bits per pixel: " + bits_per_pixel); //*/
		if(encodingType == 0) {
			for(byte dataI: screen) {
				out.write(dataI);
			}
		} else if(encodingType == 6) {
			writeS32int(screen.length);
			out.write(screen, 0, screen.length);
		}
		out.flush();
	}
	
	
	/**
     * Send cursor position encoding allows to transfer remote cursor position to
     * client side.
     */
	public void sendCursorPos() throws IOException {
		// TODO
		//CURSOR_POS(0xFFFFFF18, "CursorPos", CursorPosDecoder.class),
		if (supportedEncoding.contains(-239)) {
			/**
			* No. of bytes 	                    Type 	        Description
			* width * height * bytesPerPixel 	PIXEL array 	cursor-pixels
			* floor((width + 7) / 8) * height 	U8 array 	    bitmask
			*/
			// DO SOMETHING			
		} else {
			err ("Client does not support Cursor pseudo encoding.");
		}
	}
	
	
	/**
	 * Sends two rectangles in frame buffer update, first is screen with old dimensions,
	 * and second is desktop size pseudo encoding rectangle.
	 *  
	 * @throws IOException
	 */
	public void sendDesktopSize() throws IOException
	{
		
		if (supportedEncoding.contains(-223)) {
			
			RobotScreen.robo.getScreenshot(0, 0, screenWidth, screenHeight);
			
			int encodingType = 0;
			int[] screen = RobotScreen.robo.getColorImageBuffer();
					
			byte messageType = 0x00;
			byte padding     = 0x00;
			
			out.write(messageType);
			out.write(padding);
			
			int numberOfRectangles = 2;
			
			writeU16int(numberOfRectangles);	
			
			writeU16int(0);
			writeU16int(0);
			writeU16int(screenWidth);
			writeU16int(screenHeight);
			writeS32int(encodingType);

			for (int rgbValue : screen) {

				int red   = (rgbValue & 0x000000FF);
				int green = (rgbValue & 0x0000FF00) >> 8;
				int blue  = (rgbValue & 0x00FF0000) >> 16;

				if (bits_per_pixel == 8) {
					System.out.println("8bitPixel - SD");
					out.write((byte) colorMap.get8bitPixelValue(red, green, blue));
				}
				else {
					out.write(red);
					out.write(green);
					out.write(blue);
					out.write(0);
				}
			}
			encodingType = -223;
			
			writeU16int(0);
			writeU16int(0);
			writeU16int(screenWidth);
			writeU16int(screenHeight);
			writeS32int(encodingType);
			
			out.flush();
			
			log ("New screen size: " + screenWidth + " x " + screenHeight);
			
		} else {
			err ("Client does not support DesktopSize pseudo encoding.");
		}
	}
	

	public void RFB() {
		try {

			/*
			 * RFB server has to send protocol version string first.
			 * And wait for VNC viewer to replay with protocol version string.
			 */
			log ("Sending protocol version...");
			sendProtocolVersion();
			log ("Reading protocol version...");
			String protocolVer = readProtocolVersion();
			if (!protocolVer.startsWith("RFB")) {
				throw new IOException();
			}
			log ("Protocol ver.: " + protocolVer.substring(0, protocolVer.length() - 1));
			/*
			 * RFB server sends security type bytes that may request a user to type password.
			 * In this implementation, this is set to simples possible option: no authentication at all.
			 */
			sendSecurityType();

			/*
			 * RFB server reads shared desktop flag. It's a single byte that tells RFB server
			 * should it support multiple VNC viewers connected at same time or not. 
			 */
			byte sharedDesktop = readSharedDesktop();
			log ("Shared desktop flag seleced by client: " + sharedDesktop);
			
			/*
			 * RFB server sends ServerInit message that includes screen resolution,
			 * number of colors, depth, screen title, etc.
			 */
			// get screen bound
	        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
	        GraphicsDevice gd = ge.getDefaultScreenDevice();
	        Rectangle screenbound = gd.getDefaultConfiguration().getBounds();
	        screenWidth =  (int) screenbound.getWidth();
	        screenHeight=  (int) screenbound.getHeight();
	        
			String windowTitle = MainWindow.frmOpenrviewer.getTitle();
			sendServerInit(screenWidth, screenHeight, windowTitle);			
			
			/*
			 * Main loop where clients messages are read from socket.
			 */
			while (true) {

				/*
				 * Mark first byte and read it.
				 */
				in.mark(1);
				int messageType = in.read();
				if (messageType == -1) {
					break;
				}
				/*
				 * Go one byte back. 
				 */
				in.reset();
				
				/*
				 * Depending on message type, read complete message on socket.
				 */
				if (messageType == 0) {
					/*
					 * Set Pixel Format
					 */
					readSetPixelFormat();
				} else if (messageType == 2) {
					/*
					 * Set Encodings
					 */
					readSetEncoding();
				} else if (messageType == 3) {
					/*
					 * Frame Buffer Update Request
					 */
					readFrameBufferUpdateRequest();
				} else if (messageType == 4) {
					/*
					 * Key Event
					 */
					readKeyEvent();
				} else if (messageType == 5) {
					/*
					 * Pointer Event
					 */
					readPointerEvent();
				} else if (messageType == 6) {
					/*
					 * Client Cut Text
					 */
					readClientCutText();
				} else if (messageType == 255 || messageType == 33 || messageType == 24) { 
					// This part of code is probably useful because of padding error in readSetPixelFormat
					in.read();
					//System.out.println(in.read()); // 255,	33,	255, 255, 255,	24
				} else {
					err("Unknown message type. Received message type = " + messageType);
					in.read();
				}
			}
			
			log("Client connection closed.");
			log("");
			
			clientSocket.close();
			
			OpenRViewer.rfbClientList.remove(this);
		
		} catch (SocketException e) {		
			log("Client connection closed.");
			OpenRViewer.rfbClientList.remove(this);
			
		} catch (IOException e) {		
			e.printStackTrace();
		}
	}
	
	public void deltaUpdate() {	
		int offsetX = 0;
		int offsetY = 0;
			
		/*
		 * Capture new screenshot into image buffer.
		 */
		RobotScreen.robo.getScreenshot(offsetX, offsetY, screenWidth, screenHeight);
		int[] delta = RobotScreen.robo.getDeltaImageBuffer();					
		
		if (delta != null && incrementalFrameBufferUpdate) {
				offsetX = RobotScreen.robo.getDeltaX();
				offsetY = RobotScreen.robo.getDeltaY();
			
				int width = RobotScreen.robo.getDeltaWidth();
				int height = RobotScreen.robo.getDeltaHeight();
		
				try {
					/*
					 * Send only delta rectangle.
					 */
						byte[] data = encoders.encode(delta, usedEncodingType, 32);
						sendFrameBufferUpdate(
									offsetX, offsetY, 
									width, height, 
									usedEncodingType, 
									data);
				} catch (IOException ex) {
					System.out.println(ex.getMessage());
				}
				incrementalFrameBufferUpdate = false;
		 } else if (incrementalFrameBufferUpdate) {
				/*
				 * Send complete window.
				 */
				try {
					byte[] data = encoders.encode(RobotScreen.robo.getColorImageBuffer(), usedEncodingType, 32);
					sendFrameBufferUpdate(
							offsetX, offsetY, 
							screenWidth, screenHeight, 
							usedEncodingType, 
							data);
				} catch (IOException e) {
					System.out.println(e.getMessage());
				}
				incrementalFrameBufferUpdate = true;
		}
	}
	  
	
	
	/**
	 * Write line of text on std.out.
	 * 
	 * @param line text line
	 */
	private void log(String line) {
		System.out.println(line);
	}

	/**
	 * Write line of text on std.err.
	 * 
	 * @param line text line
	 */
	private void err(String line) {
		System.err.println(line);
	}
	
}
