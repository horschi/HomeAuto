package init;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

import ebus.conn.ConnectionFactory;

public class HomeAutoState
{

	public HomeAutoState() throws Exception
	{
		final Properties props = new Properties();
		final File configFile = new File("homeauto.properties");
		if (configFile.exists())
			props.load(new FileInputStream(configFile));


		{
			final String heatingPortName = getProperty(props, "heating.port.name", "");
			final String heatingPortAddr = getProperty(props, "heating.port.addr", "");
			final String heatingPortDriver = getProperty(props, "heating.port.driver", "rpiserial");
			ConnectionFactory.create(heatingPortDriver);
		}

		{
		final String ventilationPortName = getProperty(props, "ventilation.port.name", "");
		final String ventilationPortAddr = getProperty(props, "ventilation.port.addr", "");
		final String ventilationPortDriver = getProperty(props, "ventilation.port.driver", "serial");
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

	public String getProperty(final Properties props, final String key, final String def)
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

	}
}
