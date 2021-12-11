package data;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.codec.Charsets;
import org.apache.commons.lang3.StringUtils;

public class Sender
{
	private final URL	url;
	private String		cloudUrl;
	private String		user;
	private String		password;

	public Sender(final String cloudUrl, final String user, final String password) throws Exception
	{
		if (StringUtils.isBlank(cloudUrl))
			url = null;
		else
			url = new URL(cloudUrl);
	}

	public void sendQueue() throws Exception
	{
		if (url == null)
			return;

		final HttpURLConnection conn = (HttpURLConnection) (url.openConnection());
		try
		{
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "text/csv; utf-8");
			conn.setRequestProperty("Accept", "text/csv");
			conn.setDoOutput(true);

			try (OutputStream os = conn.getOutputStream())
			{
				try (Writer w = new OutputStreamWriter(os, Charsets.UTF_8))
				{
					// try (BufferedWriter bw = new BufferedWriter(w))
					// {
					//
					// bw.write(Long.toString(ts));
					// bw.write(",");
					// bw.write(parameter);
					// bw.write(",");
					// bw.write(value);
					// bw.write("\n");
					// }
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
}
