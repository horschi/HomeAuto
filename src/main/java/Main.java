import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class Main
{
	public static void main(String[] args)
	{
		try
		{
			String username = System.getProperty("user.name");
			System.out.println("Running as: " + username);
			System.out.println("Heap size: " + (Runtime.getRuntime().maxMemory() >> 20) + "M");

			int port;
			if ("root".equals(username))
				port = 80;
			else
				port = 8000;
			HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
			server.createContext("/", new MyHandler());
			server.setExecutor(null); // creates a default executor
			
			System.out.println("Starting webserver ...");
			server.start();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	static class MyHandler implements HttpHandler
	{
		private HomeAutoWebHandler handler = new HomeAutoWebHandler();

		@Override
		public void handle(HttpExchange t) throws IOException
		{
			try
			{
				String queryStr = t.getRequestURI().getRawQuery();
				Map<String, String> params = new HashMap<>();
				if (queryStr != null)
				{
					
					for (String p : StringUtils.split(queryStr, '&'))
					{
						String[] pkv = StringUtils.split(p, '=');
						if(pkv.length < 2 )
							continue;
						String k = URLDecoder.decode(pkv[0]);
						String v = URLDecoder.decode(pkv[1]);
						params.put(k, v);
					}
					handler.handleParams(params);
				}

				StringWriter writer = new StringWriter();
				handler.writeOutput("/", params, writer);
				String response = writer.toString();

				byte[] data = response.getBytes("UTF-8");
				t.sendResponseHeaders(200, data.length);
				try (OutputStream os = t.getResponseBody())
				{
					os.write(data);
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
}
