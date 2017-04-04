// Copyright (C) 2010 - 2014 GlavSoft LLC.
// All rights reserved.
//
// -----------------------------------------------------------------------
// This file is part of the TightVNC software.  Please visit our Web site:
//
//                       http://www.tightvnc.com/
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, write to the Free Software Foundation, Inc.,
// 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
// -----------------------------------------------------------------------
//
package com.glavsoft.rfb.encoding.decoder;

import com.glavsoft.drawing.Renderer;
import com.glavsoft.exceptions.TransportException;
import com.glavsoft.transport.Transport;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class ZlibDecoder extends Decoder {
	private Inflater decoder;

	@Override
	public void decode(Transport transport, Renderer renderer,
			FramebufferUpdateRectangle rect) throws TransportException {
		int zippedLength = (int) transport.readUInt32();
		if (0 == zippedLength) return;
		int length = rect.width * rect.height * renderer.getBytesPerPixel();
		byte[] bytes = unzip(transport, zippedLength, length);
		Transport unzippedReader =
			new Transport(
					new ByteArrayInputStream(bytes, zippedLength, length));
		RawDecoder.getInstance().decode(unzippedReader, renderer, rect);
	}

	protected byte[] unzip(Transport transport, int zippedLength, int length)
			throws TransportException {
		
		// Don't know why the following code won't work in the way I want ?
		/*byte [] bytes = ByteBuffer.getInstance().getBuffer(zippedLength + length);
		transport.readBytes(bytes, 0, zippedLength);
		if (null == decoder) {
			decoder = new Inflater();
		}
		decoder.setInput(bytes, 0, zippedLength);
		try {			
			decoder.inflate(bytes, zippedLength, length);
		} catch (DataFormatException e) {
			throw new TransportException("cannot inflate Zlib data (ZL:"+zippedLength+" L:"+length+")", e);
		}
		return bytes;*/
		
		
		byte [] zippedBytes = new byte[zippedLength];
		transport.readBytes(zippedBytes, 0, zippedLength);
		
		byte[] bytes2 = new byte[length];
		bytes2 = decompress(zippedBytes);
		zippedBytes = null;
		byte [] bytes = ByteBuffer.getInstance().getBuffer(zippedLength + length);
		System.arraycopy(bytes2, 0, bytes, zippedLength, bytes2.length);
		bytes2 = null;
		return bytes;
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

	@Override
	public void reset() {
		decoder = null;
	}

}
