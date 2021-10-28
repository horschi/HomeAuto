package ebus;

import java.io.InputStream;

import data.DebugRegistry;
import ebus.conn.AbstractConnection;
import ebus.protocol.EBusData;

public class EBusProcessorThread extends Thread
{
	private final EBusReader			reader;
	private final AbstractConnection	conn;
	private final DebugRegistry			debugRegistry;

	public EBusProcessorThread(final String name, final EBusReader reader, final AbstractConnection conn, final DebugRegistry debugRegistry)
	{
		super("ReaderProcessing_" + name);
		this.reader = reader;
		this.conn = conn;
		this.debugRegistry = debugRegistry;
	}

	@Override
	public void run()
	{
		final InputStream inputStream;
		try
		{
			inputStream = conn.getInputStream();
			while (inputStream.read() != 0xAA)
			{
				debugRegistry.incNumBytesRead();
			}
		}
		catch (final Exception e)
		{
			System.err.println("Error while reading initial syn from EBus");
			e.printStackTrace();
			return;
		}
		while (true)
		{
			try
			{
				final EBusData o = new EBusData(inputStream);

				debugRegistry.incNumParsed();
				if (o.getMessage() != null)
					debugRegistry.incNumWithMessage();
				if (o.isValid())
				{
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
}
