package init;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

import com.pi4j.util.StringUtil;

import conn.ConnTest;
import conn.PowerPoll;
import data.DebugRegistry;
import data.Sender;
import data.ValueRegistry;
import ebus.EBusProcessorThread;
import ebus.conn.AbstractSerialConnection;
import ebus.conn.SerialConnectionFactory;
import ebus.reader.EBusReader;
import ebus.reader.EBusReaderFactory;
import shelly.ShellyUDP;
import sml.SMLProcessorThread;
import sml.SMLReader;
import ventilation.VentilationWriter;

public class HomeAutoState
{
	private final List<Closeable>	threads			= new ArrayList<>();
	private final ValueRegistry		valueRegistry	= new ValueRegistry();
	private final DebugRegistry		debugRegistry	= new DebugRegistry();

	private VentilationWriter		ventWriter;
	private ShellyUDP				shelly;

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
				final boolean debug = getPropertyBoolean(props, id + "debug");

				if (StringUtil.isNullOrEmpty(strProcessor))
					break;

				final AbstractSerialConnection conn = SerialConnectionFactory.create(strDriver);

				if (strProcessor.equals("sml"))
				{
					conn.init(strName, 9600);

					final SMLReader reader = new SMLReader(valueRegistry, debug ? debugRegistry : null, props);

					final SMLProcessorThread thread = new SMLProcessorThread(id + "_" + strProcessor, reader, conn, debugRegistry);
					threads.add(thread);
					thread.start();
				}
				else
				{
					conn.init(strName, 2400);

					final EBusReader reader = EBusReaderFactory.create(strProcessor, valueRegistry, debug ? debugRegistry : null);

					final EBusProcessorThread thread = new EBusProcessorThread(id + "_" + strProcessor, reader, conn, debugRegistry);
					threads.add(thread);
					thread.start();
				}
			}
			catch (final Exception e)
			{
				e.printStackTrace();
				valueRegistry.incCountDebug("EBus startup error: " + e);
			}
		}

		try
		{
			ventWriter = new VentilationWriter();
		}
		catch (final Throwable e)
		{
			ventWriter = null;
			e.printStackTrace();
			valueRegistry.incCountDebug("VentilationWriter error: " + e);
			// lastError = ExceptionUtils.getStackTrace(e);
		}

		{
			final String cloudUrl = getProperty(props, "cloud.url", "");
			final String user = getProperty(props, "cloud.user", "");
			final String password = getProperty(props, "cloud.password", "");
			if (StringUtils.isNotBlank(cloudUrl) && StringUtils.isNotBlank(user) && StringUtils.isNotBlank(password))
			{
				final Sender sender = new Sender(valueRegistry, cloudUrl, user, password);
				threads.add(sender);
				sender.start();
			}
		}


		{
			final String host = getProperty(props, "conn.host", "");
			final int port = getPropertyInt(props, "conn.port");
			if (StringUtils.isNotBlank(host) && port > 0)
			{
				final ConnTest sender = new ConnTest(valueRegistry, host, port);
				threads.add(sender);
				sender.start();
			}
		}

		{
			final PowerPoll sender = new PowerPoll(valueRegistry);
			threads.add(sender);
			sender.start();
		}

		{
			final String deviceId = getProperty(props, "shelly.deviceid", "shellypro3em-b827eb364242");
			final String meterValueKey = getProperty(props, "shelly.meterKey", "");
			if (StringUtils.isNotBlank(meterValueKey))
			{
				shelly = new ShellyUDP(valueRegistry, deviceId, meterValueKey);
				threads.add(shelly);
				shelly.start();
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
				valueRegistry.incCountDebug("Config write error: " + e);
				// valueRegistry.setValue("system.config.error", "Error writing config: " + e);
			}
		}

	}

	public VentilationWriter getVentWriter()
	{
		return ventWriter;
	}

	public ShellyUDP getShelly()
	{
		return shelly;
	}

	public void setShelly(final ShellyUDP shelly)
	{
		this.shelly = shelly;
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

	private boolean getPropertyBoolean(final Properties props, final String key)
	{
		return Boolean.parseBoolean(getProperty(props, key, "false"));
	}

	private int getPropertyInt(final Properties props, final String key)
	{
		return Integer.parseInt(getProperty(props, key, "0"));
	}

	public void close() throws Exception
	{
		for (final Closeable t : threads)
			t.close();
		valueRegistry.close();
	}
}
