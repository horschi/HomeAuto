package data;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import com.pi4j.util.StringUtil;

public class ValueRegistry
{
	private final Map<String, KnownValueEntry>	knownValues	= Collections.synchronizedMap(new TreeMap<>());
	private final FileWriter					valueLog;
	private final BufferedWriter				valueLogWriter;

	public ValueRegistry() throws Exception
	{
		valueLog = new FileWriter("values.csv", true);
		valueLogWriter = new BufferedWriter(valueLog);
	}

	public void setValue(final String key, final Object value)
	{
		setValue(key, value, null);
	}

	public void setValue(final String key, final Object value, final Object text)
	{
		final KnownValueEntry ent = getKnownValueObj(key);
		ent.setValue(value, text, false);
	}

	public void setValueDebug(final String key, final Object value)
	{
		final KnownValueEntry ent = getKnownValueObj(key);
		ent.setValue(value, null, true);
	}

	public Map<String, KnownValueEntry> getKnownValues()
	{
		return new TreeMap<>(knownValues);
	}

	public KnownValueEntry getKnownValueObj(final String key)
	{
		KnownValueEntry r = this.knownValues.get(key);
		if (r == null)
		{
			r = new KnownValueEntry(null);
			this.knownValues.put(key, r);
		}
		return r;
	}

	public void writeValueToLog(final String prop, final Object val)
	{
		if (val == null)
			return;
		final String vstr = val.toString();
		if (StringUtil.isNullOrEmpty(vstr))
			return;
		try
		{
			synchronized (valueLog)
			{
				valueLogWriter.write("" + System.currentTimeMillis() + "," + prop + "," + vstr + "\n");
			}
		}
		catch (final IOException e)
		{
			e.printStackTrace();
		}
	}

	public void close()
	{
		try
		{
			valueLogWriter.flush();
			valueLogWriter.close();
			synchronized (valueLog)
			{
				valueLog.close();
			}
		}
		catch (final IOException e)
		{
			e.printStackTrace();
		}
	}
}
