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
				Thread.sleep(1000L);
				final long startC = System.currentTimeMillis();
				try (final Socket clientSocket = new Socket(host, port);)
				{
					final long startR = System.currentTimeMillis();
					final long tdifConnect = startR - startC;
					valueRegistry.setValue("Connection - Init", tdifConnect, StringUtil.encodeTimeDif(tdifConnect));

					try (final InputStream in = clientSocket.getInputStream();)
					{
						while (!closed)
						{
							Thread.sleep(5000L);

							final long startS = System.currentTimeMillis();
							final int sb = (int) (System.currentTimeMillis() & 0xff);
							clientSocket.getOutputStream().write(sb);
							final int r = in.read();
							if (r < 0)
							{
								valueRegistry.setValue("Connection - Ping", -1, "-");
								break;
							}
							final long tdifSend = System.currentTimeMillis() - startS;
							valueRegistry.setValue("Connection - Ping", tdifSend, StringUtil.encodeTimeDif(tdifSend));
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
