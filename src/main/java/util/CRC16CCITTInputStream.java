package util;

import java.io.IOException;
import java.io.InputStream;

public class CRC16CCITTInputStream extends InputStream
{
	private final InputStream	in;
	private int					crc			= 0xFFFF;

	public CRC16CCITTInputStream(final InputStream in)
	{
		this.in = in;
	}

	public void pushByte(final int b)
	{
		// for (int i = 0; i < 8; i++)
		// {
		// final int crcbit = (crc & 0x8000);
		// final int databit = (b & 0x80);
		// crc <<= 1;
		// b = b << 1;
		// if (crcbit != databit)
		// crc = crc ^ 0x1021;
		// }

		for (int i = 0; i < 8; i++)
		{
			final boolean bit = ((b >> (7 - i) & 1) == 1);
			final boolean c15 = ((crc >> 15 & 1) == 1);
			crc <<= 1;
			if (c15 ^ bit)
				crc ^= 0x1021;
		}
	}

	public int getCrc()
	{
		return crc & 0xffff;
	}

	@Override
	public int read() throws IOException
	{
		final int c = in.read();
		if (c >= 0)
		{
			final byte b = (byte) c;
			pushByte(b);
		}
		return c;
	}
}
