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

import orv.RCuser.ColorMap8bit;

public class RawEncode {
	
	public static byte[] RawEncoder(int screen[], int bits_per_pixel) {
		byte[] output;
		if (bits_per_pixel == 8) {
			output = new byte[screen.length];
		} else {
			output = new byte[screen.length*4];
		}
		int i = 0;

		for (int rgbValue : screen) {

			int red   = (rgbValue & 0x000000FF);
			int green = (rgbValue & 0x0000FF00) >> 8;
			int blue  = (rgbValue & 0x00FF0000) >> 16;

			if (bits_per_pixel == 8) {
				ColorMap8bit colorMap = new ColorMap8bit();
				output[i]   = (byte) colorMap .get8bitPixelValue(red, green, blue);
			} else {
				output[i] = (byte) red  ; i++;
				output[i] = (byte) green; i++;
				output[i] = (byte) blue ; i++;
				output[i] = (byte) 0    ; i++;
			}
		}

		return output;
	}

}
