package data;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Queue;

import org.apache.commons.codec.Charsets;
import org.apache.commons.lang3.StringUtils;

public class Sender extends Thread implements Closeable
{
	private final ValueRegistry	registry;
	private final URL			url;
	private String				cloudUrl;
	private final String				user;
	private final String				password;
	private boolean				closed	= false;

	public Sender(final ValueRegistry registry, final String cloudUrl, final String user, final String password) throws Exception
	{
		this.registry = registry;
		if (StringUtils.isBlank(cloudUrl))
			url = null;
		else
			url = new URL(cloudUrl);
		this.user = user;
		this.password = password;
	}

	@Override
	public void run()
	{
		while (!closed)
		{
			try
			{
				Thread.sleep(10000L);
			}
			catch (final InterruptedException e)
			{
			}
			try
			{
				sendQueue();
			}
			catch (final Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	public void sendQueue() throws Exception
	{
		if (url == null)
			return;

		final Queue<KnownValueQueueEntry> queue = registry.getQueue();
		if (queue.isEmpty())
			return;
		if (queue.size() > 100)
			System.out.println("Queue is large: " + queue.size());

		// System.out.println("Sending to " + url+" using "+user);
		final HttpURLConnection conn = (HttpURLConnection) (url.openConnection());
		try
		{
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "text/csv");
			// conn.setRequestProperty("Accept", "text/csv");
			conn.setRequestProperty("thingid", user);
			conn.setRequestProperty("password", password);
			conn.setDoOutput(true);

			try (OutputStream os = conn.getOutputStream())
			{
				try (Writer w = new OutputStreamWriter(os, Charsets.UTF_8))
				{
					try (BufferedWriter bw = new BufferedWriter(w))
					{
						while (!queue.isEmpty())
						{
							final KnownValueQueueEntry ent = queue.poll();
							if (ent == null)
								break;
							String k = ent.getKey();
							k = k.trim();
							k = k.replace(" ", "");
							k = k.replace("-", "");
							k = k.replace("/", "");

							bw.write(Long.toString(ent.getTs()));
							bw.write(",");
							bw.write(k);
							bw.write(",");
							bw.write(ent.getValue().toString());
							bw.write("\n");
						}
					}
				}
			}
			switch (conn.getResponseCode())
			{
				case 200:
					break;

				default:
					System.out.println("Could not send data: " + conn.getResponseCode() + " " + conn.getResponseMessage());
			}
		}
		finally
		{
			conn.disconnect();
		}
	}

	@Override
	public void close() throws IOException
	{
		closed = true;
	}
}
