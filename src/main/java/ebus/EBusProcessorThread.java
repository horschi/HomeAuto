package ebus;

import java.io.InputStream;

import data.DebugRegistry;
import ebus.conn.AbstractSerialConnection;
import ebus.protocol.EBusData;
import ebus.reader.EBusReader;

public class EBusProcessorThread extends Thread
{
	private boolean							closed	= false;
	private final EBusReader				reader;
	private final AbstractSerialConnection	conn;
	private final DebugRegistry				debugRegistry;

	public EBusProcessorThread(final String name, final EBusReader reader, final AbstractSerialConnection conn, final DebugRegistry debugRegistry)
	{
		super("ReaderProcessing_" + name);
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
				while (inputStream.read() != 0xAA)
				{
					debugRegistry.incNumBytesRead();
					if (closed)
						return;
				}
			}
			catch (final Exception e)
			{
				System.err.println("Error while reading initial syn from EBus");
				e.printStackTrace();
				return;
			}
			while (!closed)
			{
				try
				{
					final EBusData o = new EBusData(inputStream);

					if (debugRegistry != null)
						debugRegistry.incNumParsed();

					if (o.getMessage() != null && debugRegistry != null)
						debugRegistry.incNumWithMessage();

					if (o.isValid())
					{
						if (debugRegistry != null)
							debugRegistry.incNumValid();

						reader.parseCommands(o);
					}
				}
				catch (final Exception e)
				{
					e.printStackTrace();
				}
			}
		}
		finally
		{
			conn.close();
		}
	}

	public void close()
	{
		closed = true;
	}
}
