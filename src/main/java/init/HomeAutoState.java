package init;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.pi4j.util.StringUtil;

import data.DebugRegistry;
import data.ValueRegistry;
import ebus.EBusProcessorThread;
import ebus.conn.AbstractSerialConnection;
import ebus.conn.SerialConnectionFactory;
import ebus.reader.EBusReader;
import ebus.reader.EBusReaderFactory;

public class HomeAutoState
{
	private final List<EBusProcessorThread>	threads			= new ArrayList<>();
	private final ValueRegistry				valueRegistry	= new ValueRegistry();
	private final DebugRegistry				debugRegistry	= new DebugRegistry();

	public HomeAutoState() throws Exception
	{
		final Properties props = new Properties();
		final File configFile = new File("homeauto.properties");
		if (configFile.exists())
			props.load(new FileInputStream(configFile));

		for (int i = 0; i < 10; i++)
		{
			try
			{
				final String id = "if" + i + ".";
				final String strName = getProperty(props, id + "serial.name", "");
				// final String strAddr = getProperty(props, id + "serial.addr", "");
				final String strDriver = getProperty(props, id + "serial.driver", "rpiserial");
				final String strProcessor = getProperty(props, id + "processor", "");
				final boolean debug = Boolean.parseBoolean(getProperty(props, id + "debug", "false"));

				if (StringUtil.isNullOrEmpty(strProcessor))
					break;

				final AbstractSerialConnection conn = SerialConnectionFactory.create(strDriver);
				conn.init(strName);

				final EBusReader reader = EBusReaderFactory.create(strProcessor, valueRegistry, debug ? debugRegistry : null);

				final EBusProcessorThread thread = new EBusProcessorThread(id + "_" + strProcessor, reader, conn, debugRegistry);
				threads.add(thread);
				thread.start();
			}
			catch (final Exception e)
			{
				e.printStackTrace();
			}
		}

		if (!configFile.exists())
		{
			try (FileOutputStream fout = new FileOutputStream(configFile))
			{
				props.store(fout, "");
			}
			catch (final Exception e)
			{
				e.printStackTrace();
			}
		}

	}

	public ValueRegistry getValueRegistry()
	{
		return valueRegistry;
	}

	public DebugRegistry getDebugRegistry()
	{
		return debugRegistry;
	}

	private String getProperty(final Properties props, final String key, final String def)
	{
		final String v = props.getProperty(key);
		if (v == null)
		{
			props.setProperty(key, def);
			return def;
		}
		return v;
	}

	public void close()
	{
		for (final EBusProcessorThread t : threads)
			t.close();
		valueRegistry.close();
	}
}
