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

import java.awt.AWTException;

import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;

import static java.awt.event.KeyEvent.*;

/**
 * Robot class to control keyboard on system level.
 * With this class it is possible to send key events.
 * 
 * @author eigorde
 *
 */
public class RobotKeyboard {

	/**
	 * Instance of {@link RobotKeyboard} class.
	 */
	public static RobotKeyboard robo;

	private Robot robot;
	
	public RobotKeyboard() throws AWTException {
		this.robot = new Robot();
		//this.robot.setAutoDelay(10);
	    //this.robot.setAutoWaitForIdle(true);
	}

	/**
	 * Send key to system. Special key codes have 0xff00 bit mask. This 
	 * is according to RFB specification.
	 * @param keyCode see {@link KeyEvent}
	 * @param state key button pressed or released on keyboard
	 */
	public void sendKey(int keyCode, int state) {
		switch (keyCode) {
		case 176:
			// °
			if (state == 0) altNumpad("248");
			break;
		case 178:
			// ²
			if (state == 0) altNumpad("253");
			break;
		case 179:
			// ³
			if (state == 0) altNumpad("252");
			break;
		case 224: case 129:
			// à
			if (state == 0) altNumpad("160");
			break;
		case 231:
			// ç
			if (state == 0) altNumpad("135");
			break;
		case 232:
			// é
			if (state == 0) altNumpad("138");
			break;
		case 233:
			// è
			if (state == 0) altNumpad("130");
			break;
		case 5028: case 516:
			// €
			doType(VK_EURO_SIGN, state);
			break;
		case 0xff08:
			doType(VK_BACK_SPACE, state);
			break;
		case 0xff09:
			doType(VK_TAB, state);
			break;
		case 0xff0d: case 0xff8d:
			doType(VK_ENTER, state);
			break;
		case 0xff1b:
			doType(VK_ESCAPE, state);
			break;
		case 0xff63:
			doType(VK_INSERT, state);
			break;
		case 0xffff:
			doType(VK_DELETE, state);
			break;
		case 0xff50:
			doType(VK_HOME, state);
			break;
		case 0xff57:
			doType(VK_END, state);
			break;
		case 0xff55:
			doType(VK_PAGE_UP, state);
			break;
		case 0xff56:
			doType(VK_PAGE_DOWN, state);
			break;
		case 0xff51:
			doType(VK_LEFT, state);
			break;
		case 0xff52:
			doType(VK_UP, state);
			break;
		case 0xff53:
			doType(VK_RIGHT, state);
			break;
		case 0xff54:
			doType(VK_DOWN, state);
			break;
		case 0xffbe:
			doType(VK_F1, state);			
			break;
		case 0xffbf:
			doType(VK_F2, state);			
			break;
		case 0xffc0:
			doType(VK_F3, state);			
			break;
		case 0xffc1:
			doType(VK_F4, state);			
			break;
		case 0xffc2:
			doType(VK_F5, state);			
			break;
		case 0xffc3:
			doType(VK_F6, state);			
			break;
		case 0xffc4:
			doType(VK_F7, state);			
			break;									
		case 0xffc5:
			doType(VK_F8, state);			
			break;		
		case 0xffc6:
			doType(VK_F9, state);			
			break;			
		case 0xffc7:
			doType(VK_F10, state);			
			break;
		case 0xffc8:
			doType(VK_F11, state);			
			break;		
		case 0xffc9:
			doType(VK_F12, state);			
			break;		
		case 0xffe1: case 0xffe2:
			doType(VK_SHIFT, state);	
			break;				
		case 0xffe3: case 0xffe4:
			doType(VK_CONTROL, state);			
			break;			
		case 0xffe9: case 0xffea:
			doType(VK_ALT, state);			
			break;
		default:
			
			/*
			 * Translation of a..z keys.
			 */
			if (keyCode >= 97 && keyCode <= 122) {
				/*
				 * Turn lower-case a..z key codes into upper-case A..Z key codes.
				 */
				keyCode = keyCode - 32;
				
				doType(keyCode, state);	
			} else if(keyCode > 18) {
				if (state == 0) {
					altNumpad("" + keyCode + "");
				}
			}
		}
	}

	/**
	 * Send key event to system.
	 * 
	 * @param keyCode a key code to send, see {@link KeyEvent} list of codes
	 * @param state if <I>0</I>, key is released, otherwise key is pressed
	 */
	private void doType(int keyCode, int state) {
		try {
			if (state == 0) {
				robot.keyRelease(keyCode);
			} else if(state == 1) {
				robot.keyPress(keyCode);
			}
		} catch(Exception e) {
			System.out.println("Error:" + keyCode + " :: " + e.getMessage());
		}
	}
	
	@SuppressWarnings("unused")
	private void altNumpad(int... numpadCodes){
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		toolkit.setLockingKeyState(KeyEvent.VK_NUM_LOCK, true);
		robot.keyRelease(VK_ALT);
		robot.keyRelease(VK_SHIFT);
		robot.keyRelease(VK_CONTROL);
	    if (numpadCodes.length == 0) {
	        return;
	    }

	    robot.keyPress(VK_ALT);

	    for (int NUMPAD_KEY : numpadCodes){
	        robot.keyPress(NUMPAD_KEY);
	        robot.keyRelease(NUMPAD_KEY);
	    }

	    robot.keyRelease(VK_ALT);
	}

	private void altNumpad(String numpadCodes){
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		toolkit.setLockingKeyState(KeyEvent.VK_NUM_LOCK, true);
		robot.keyRelease(VK_ALT);
		robot.keyRelease(VK_SHIFT);
		robot.keyRelease(VK_CONTROL);
		System.out.print(numpadCodes);
	    if (numpadCodes == null || !numpadCodes.matches("^\\d+$")){
	        return;
	    }               

	    robot.keyPress(VK_ALT);

	    for (char charater : numpadCodes.toCharArray()){

	        int NUMPAD_KEY = getNumpad(charater);

	        if (NUMPAD_KEY != -1){
	            robot.keyPress(NUMPAD_KEY);
	            robot.keyRelease(NUMPAD_KEY);
	        }
	    }

	    robot.keyRelease(VK_ALT);   
	}
	
	private int getNumpad(char numberChar){
	    switch (numberChar){
	        case '0' : return VK_NUMPAD0;
	        case '1' : return VK_NUMPAD1;
	        case '2' : return VK_NUMPAD2;
	        case '3' : return VK_NUMPAD3;
	        case '4' : return VK_NUMPAD4;
	        case '5' : return VK_NUMPAD5;
	        case '6' : return VK_NUMPAD6;
	        case '7' : return VK_NUMPAD7;
	        case '8' : return VK_NUMPAD8;
	        case '9' : return VK_NUMPAD9;  
	        default: return -1;
	    }

	}
	
}

