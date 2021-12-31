package conn;

import java.io.Closeable;
import java.io.InputStream;
import java.net.Socket;

import data.ValueRegistry;
import util.StringUtil;

public class ConnTest extends Thread implements Closeable
{
	private boolean				closed	= false;

	private final ValueRegistry	valueRegistry;
	private final String		host;
	private final int			port;

	public ConnTest(final ValueRegistry valueRegistry, final String host, final int port)
	{
		this.valueRegistry = valueRegistry;
		this.host = host;
		this.port = port;
	}

	@Override
	public void run()
	{
		while (true)
		{
			try
			{
				final long startC = System.currentTimeMillis();
				try (final Socket clientSocket = new Socket(host, port);)
				{
					final long startR = System.currentTimeMillis();
					final long tdifConnect = startR - startC;
					valueRegistry.setValue("Conn - Init", tdifConnect, StringUtil.encodeTimeDif(tdifConnect));

					try (final InputStream in = clientSocket.getInputStream();)
					{
						while (!closed)
						{
							Thread.sleep(1000L);
							clientSocket.getOutputStream().write((int) (System.currentTimeMillis() & 0xff));
							final int r = in.read();
							if (r < 0)
								break;
						}
					}
					finally
					{
						final long tdif = System.currentTimeMillis() - startR;

						final Object l = valueRegistry.getKnownValueObj("Connection - Last").getText();
						final String n = "" + StringUtil.encodeTimeDif(tdif) + ", " + l;
						valueRegistry.setValueDebug("Connection - Last", n.substring(0, Math.min(64, n.length())));

						valueRegistry.setValue("Connection - Active", tdif, StringUtil.encodeTimeDif(tdif));
					}
				}
			}
			catch (final Exception e)
			{
				e.printStackTrace();
			}
			if (closed)
				return;
		}
	}

	@Override
	public void close()
	{
		closed = true;
	}
}
