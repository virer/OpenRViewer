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

import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import orv.RCuser.ColorMap8bit;


public class ZlibED {
	
	public static byte[] ZlibRFBEncoder(int[] ToEncode, int bits_per_pixel) {
		byte[] output;
		if (bits_per_pixel == 8) {
			output = new byte[ToEncode.length];
		} else {
			output = new byte[ToEncode.length*4]; 
		}
		int i = 0;

		for (int rgbValue : ToEncode) {

			int red   = (rgbValue & 0x000000FF);
			int green = (rgbValue & 0x0000FF00) >> 8;
			int blue  = (rgbValue & 0x00FF0000) >> 16;
			
			if( (i+4) > output.length ) { System.out.println("Zlib length too large => break!");  break; }
			
			if (bits_per_pixel == 8) {
				ColorMap8bit colorMap = new ColorMap8bit();
				output[i]   = (byte) colorMap.get8bitPixelValue(red, green, blue);
			} else {
				output[i] = (byte) red  ; i++;
				output[i] = (byte) green; i++;
				output[i] = (byte) blue ; i++;
				output[i] = (byte) 0    ; i++;
			}
		}
		
		/*byte[] returnValues = new byte[i];

		System.arraycopy
		(
			output,
			0,
			returnValues,
			0,
			i
		);*/
		/*System.out.println("1=>>" + output[0]+","+output[1]+","+output[2]+","+output[3]);
		byte[] zdata = compress(output);
		System.out.println("Zlib data length:" + zdata.length + "   ");
		byte[] uzdata =  decompress(zdata);
		System.out.println("UZ=>>" + uzdata[0]+","+uzdata[1]+","+uzdata[2]+","+uzdata[3]);*/
		
		
		return compress(output);
	}
	
	public int[] ZlibEncoder(int[] ToEncode, int bits_per_pixel) {
		return func.bytesToIntegers( compress( func.integersToBytes(ToEncode) ) );
	}
	
	public static byte[] compress(byte[] bytesToCompress)
	{		
		Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, false); //Deflater.BEST_COMPRESSION, false 
		deflater.setInput(bytesToCompress);
		deflater.finish();

		byte[] bytesCompressed = new byte[bytesToCompress.length];

		int numberOfBytesAfterCompression = deflater.deflate(bytesCompressed);
		
		byte[] returnValues = new byte[numberOfBytesAfterCompression];

		System.arraycopy
		(
			bytesCompressed,
			0,
			returnValues,
			0,
			numberOfBytesAfterCompression
		);
		
		numberOfBytesAfterCompression = 0;
		bytesCompressed = null;
		deflater.end();

		return returnValues;
	}
	
	public int[] ZlibDecode(int[] input) {
		return func.bytesToIntegers(decompress(func.integersToBytes(input)));
		
	}
	public byte[] decompress(byte[] bytesToDecompress)
	{
		byte[] returnValues = null;

		Inflater inflater = new Inflater();

		int numberOfBytesToDecompress = bytesToDecompress.length;

		inflater.setInput
		(
			bytesToDecompress,
			0,
			numberOfBytesToDecompress
		);

		int bufferSizeInBytes = numberOfBytesToDecompress;

		@SuppressWarnings("unused")
		int numberOfBytesDecompressedSoFar = 0;
		List<Byte> bytesDecompressedSoFar = new ArrayList<Byte>();

		try
		{
			while (inflater.needsInput() == false)
			{
				byte[] bytesDecompressedBuffer = new byte[bufferSizeInBytes];

				int numberOfBytesDecompressedThisTime = inflater.inflate
				(
					bytesDecompressedBuffer
				);

				numberOfBytesDecompressedSoFar += numberOfBytesDecompressedThisTime;

				for (int b = 0; b < numberOfBytesDecompressedThisTime; b++)
				{
					bytesDecompressedSoFar.add(bytesDecompressedBuffer[b]);
				}
			}

			returnValues = new byte[bytesDecompressedSoFar.size()];
			for (int b = 0; b < returnValues.length; b++) 
			{
				returnValues[b] = (byte)(bytesDecompressedSoFar.get(b));
			}

		}
		catch (DataFormatException dfe)
		{
			dfe.printStackTrace();
		}

		inflater.end();

		return returnValues;
	}
	
	
	/*public int[] ZlibDecode(int[] input) {
		if(null == decompresser) { Inflater decompresser = new Inflater(); }
    
		byte[] decompressedBuffer = new byte[input.length];
		byte[] output = new byte[input.length];
		output = lib.integersToBytes(input); 
		decompresser.setInput(output);
		try {
			int ln = decompresser.inflate(decompressedBuffer);
			return lib.bytesToIntegers(decompressedBuffer);
	    
		} catch (DataFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}*/
}
