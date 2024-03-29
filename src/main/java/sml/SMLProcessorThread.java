package sml;

import java.io.Closeable;
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
		System.out.println("SML Thread started: " + this);

		try
		{
			final InputStream inputStream;
			try
			{
				inputStream = conn.getInputStream();
				while (true)
				{
					inputStream.skip(inputStream.available());
					Thread.sleep(50l);
					if (closed)
						break;
					System.out.println("SML init avail: " + inputStream.available() + " (" + this + ")");
					if (inputStream.available() == 0)
						break;
				}
			}
			catch (final Exception e)
			{
				System.err.println("Error while waiting for inital pause from SML");
				e.printStackTrace();
				return;
			}

			while (!closed)
			{
				final List data;
				try
				{
					data = SMLParser.readPacket(inputStream);
				}
				catch (final Exception e)
				{
					reader.parseError(e);
					cleanStream(inputStream);
					continue;
				}
				try
				{
					if (debugRegistry != null)
						debugRegistry.incNumParsed();
					if (debugRegistry != null)
						debugRegistry.incNumWithMessage();

					reader.parseCommands(data);
					if (debugRegistry != null)
						debugRegistry.incNumValid();
				}
				catch (final Exception e)
				{
					e.printStackTrace();
					cleanStream(inputStream);
				}
			} // while
		}
		finally
		{
			System.out.println("SML Processor closing ...");
			conn.close();
		}
	}

	private void cleanStream(final InputStream inputStream)
	{
		try
		{
			inputStream.readNBytes(inputStream.available());
		}
		catch (final IOException e1)
		{
			e1.printStackTrace();
		}
	}
	@Override
	public void close()
	{
		closed = true;
	}
}
