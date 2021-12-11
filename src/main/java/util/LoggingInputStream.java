package util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class LoggingInputStream extends InputStream
{
	private final InputStream			in;
	private final ByteArrayOutputStream	bout	= new ByteArrayOutputStream();

	public LoggingInputStream(final InputStream in)
	{
		this.in = in;
	}

	@Override
	public int read() throws IOException
	{
		final int c = in.read();
		if (c >= 0)
		{
			bout.write(c);
		}
		return c;
	}

	public byte[] getBytes()
	{
		return bout.toByteArray();
	}
}
