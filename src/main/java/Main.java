import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import init.HomeAutoState;
import ui.HomeAutoWebHandler;

public class Main
{
	public static void main(final String[] args)
	{
		try
		{
			final String username = System.getProperty("user.name");
			System.out.println("Running as: " + username);
			System.out.println("Heap size: " + (Runtime.getRuntime().maxMemory() >> 20) + "M");

			int port;
			if ("root".equals(username))
				port = 80;
			else
				port = 8000;
			System.out.println("Using port " + port);

			final HomeAutoState state = new HomeAutoState();

			Runtime.getRuntime().addShutdownHook(new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						state.close();
					}
					catch (final Exception e)
					{
						e.printStackTrace();
					}
				}
			}, "ShutdownHook"));

			System.out.println("Creating webhandler ...");
			final HomeAutoWebHandler handler = new HomeAutoWebHandler(state.getValueRegistry(), state.getDebugRegistry());

			System.out.println("Creating webserver ...");
			final HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
			server.createContext("/", new HomeAutoHttpHandler(handler));
			server.setExecutor(new ThreadPoolExecutor(1, 4, 1, TimeUnit.SECONDS, new ArrayBlockingQueue<>(10)));

			System.out.println("Starting webserver ...");
			server.start();
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
	}

	static class HomeAutoHttpHandler implements HttpHandler
	{
		private final HomeAutoWebHandler handler;

		public HomeAutoHttpHandler(final HomeAutoWebHandler handler)
		{
			super();
			this.handler = handler;
		}

		@Override
		public void handle(final HttpExchange t) throws IOException
		{
			try
			{
				final String queryStr = t.getRequestURI().getRawQuery();
				final Map<String, String> params = new HashMap<>();
				if (queryStr != null)
				{

					for (final String p : StringUtils.split(queryStr, '&'))
					{
						final String[] pkv = StringUtils.split(p, '=');
						if (pkv.length < 2)
							continue;
						final String k = URLDecoder.decode(pkv[0]);
						final String v = URLDecoder.decode(pkv[1]);
						params.put(k, v);
					}
					if (handler.handleParams(params))
					{
						t.getResponseHeaders().add("Location", StringUtils.split(t.getRequestURI().toString(), '?')[0]);
						t.sendResponseHeaders(302, 0);
						return;
					}
				}

				final StringWriter writer = new StringWriter();
				handler.writeOutput("/", params, writer);
				final String response = writer.toString();

				final byte[] data = response.getBytes("UTF-8");
				t.sendResponseHeaders(200, data.length);
				try (OutputStream os = t.getResponseBody())
				{
					os.write(data);
				}
			}
			catch (final Exception e)
			{
				e.printStackTrace();
			}
		}
	}
}
