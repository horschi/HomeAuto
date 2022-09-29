package conn;

import java.io.Closeable;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.codec.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import data.ValueRegistry;

public class PowerPoll extends Thread implements Closeable
{
	private boolean				closed	= false;

	private final ValueRegistry	valueRegistry;

	public PowerPoll(final ValueRegistry valueRegistry)
	{
		this.valueRegistry = valueRegistry;
	}

	@Override
	public void run()
	{
		while (true)
		{
			try
			{
				Thread.sleep(5000L);
				final URL url = new URL("https://www.netzfrequenz.info/json/aktuell2.json?_=" + System.currentTimeMillis());
				final HttpURLConnection con = (HttpURLConnection) url.openConnection();
				try
				{
					con.setRequestMethod("GET");
					con.setDoOutput(false);
					con.setConnectTimeout(5000);
					con.setReadTimeout(5000);
					con.setInstanceFollowRedirects(false);
					final int status = con.getResponseCode();
					if (status == 200)
					{
						try (final InputStream in = con.getInputStream();)
						{
							final String content = IOUtils.toString(in, Charsets.UTF_8);
							final String content2 = StringUtils.substringBetween(content, "[", "]");
							final String[] content3 = StringUtils.split(content2, ',');
							final String tsStr = content3[0];
							final String freqStr = content3[1];

							final long ts = Long.parseLong(tsStr);
							final float freq = Float.parseFloat(freqStr);

							// System.out.println("tsStr " + ts);
							// System.out.println("freqStr " + freq);
							valueRegistry.setValue("Power - Frequency", freq, "" + freq + " Hz");
						}
					}
					else
					{
						System.out.println("Could not load power network frequency: " + status);
					}
				}
				finally
				{
					con.disconnect();
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
