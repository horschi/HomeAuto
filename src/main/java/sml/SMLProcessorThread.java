package sml;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import data.DebugRegistry;
import ebus.conn.AbstractSerialConnection;
import sml.protocol.SMLParser;

public class SMLProcessorThread extends Thread implements Closeable
{
	private boolean							closed	= false;
	private final SMLReader					reader;
	private final AbstractSerialConnection	conn;
	private final DebugRegistry				debugRegistry;

	public SMLProcessorThread(final String name, final SMLReader reader, final AbstractSerialConnection conn, final DebugRegistry debugRegistry)
	{
		super("SMLProcessing_" + name);
		this.reader = reader;
		this.conn = conn;
		this.debugRegistry = debugRegistry;
	}

	@Override
	public void run()
	{
		try
		{
			final InputStream inputStream;
			try
			{
				inputStream = conn.getInputStream();
				while (true)
				{
					inputStream.readNBytes(inputStream.available());
					Thread.sleep(100l);
					if (closed)
						return;
					if (inputStream.available() == 0)
						break;
				}
			}
			catch (final Exception e)
			{
				System.err.println("Error while reading initial syn from EBus");
				e.printStackTrace();
				return;
			}

			final DataInputStream din = new DataInputStream(inputStream);
			while (!closed)
			{
				try
				{
					final List data = SMLParser.readPacket(din);

					if (debugRegistry != null)
						debugRegistry.incNumParsed();
					if (debugRegistry != null)
						debugRegistry.incNumWithMessage();


					reader.parseCommands(data);
				}
				catch (final Exception e)
				{
					e.printStackTrace();
					if (debugRegistry != null)
						debugRegistry.incNumValid();
					try
					{
						inputStream.readNBytes(inputStream.available());
					}
					catch (final IOException e1)
					{
						e1.printStackTrace();
					}
				}
			}
		}
		finally
		{
			conn.close();
		}
	}

	@Override
	public void close()
	{
		closed = true;
	}
}
