package data;

import java.util.Collections;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;

public class ValueRegistry
{
	private final Map<String, KnownValueEntry>	knownValues	= Collections.synchronizedMap(new TreeMap<>());
	private final Queue<KnownValueQueueEntry>	queue		= new ArrayBlockingQueue<KnownValueQueueEntry>(10000);
	// private final FileWriter valueLog;
	// private final BufferedWriter valueLogWriter;

	public ValueRegistry() throws Exception
	{
		// valueLog = new FileWriter("values.csv", true);
		// valueLogWriter = new BufferedWriter(valueLog);
	}

	public Queue<KnownValueQueueEntry> getQueue()
	{
		return queue;
	}

	public void setValue(final String key, final Object value)
	{
		setValue(key, value, null);
	}

	public void setValue(final String key, final Object value, final Object text)
	{
		final KnownValueEntry ent = getKnownValueObj(key);
		final Object oldVal = ent.getValue();
		if (oldVal == null || !oldVal.equals(value))
		{
			ent.setValue(value, text, false);
			if (queue.size() < 10000 && !key.endsWith("?"))
				queue.add(new KnownValueQueueEntry(System.currentTimeMillis(), key, value));
		}
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

	// private void writeValueToLog(final String prop, final Object val)
	// {
	// if (val == null)
	// return;
	// final String vstr = val.toString();
	// if (StringUtil.isNullOrEmpty(vstr))
	// return;
	// try
	// {
	// synchronized (valueLog)
	// {
	// valueLogWriter.write("" + System.currentTimeMillis() + "," + prop + "," + vstr + "\n");
	// }
	// }
	// catch (final IOException e)
	// {
	// e.printStackTrace();
	// }
	// }

	public void close()
	{
		try
		{
			// valueLogWriter.flush();
			// valueLogWriter.close();
			// synchronized (valueLog)
			// {
			// valueLog.close();
			// }
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
	}
}
