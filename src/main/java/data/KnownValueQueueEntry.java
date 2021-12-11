package data;

public class KnownValueQueueEntry
{
	private final long		ts;
	private final String	key;
	private final Object	value;

	public KnownValueQueueEntry(final long ts, final String key, final Object value)
	{
		this.ts = ts;
		this.key = key;
		this.value = value;
	}

	public long getTs()
	{
		return ts;
	}

	public String getKey()
	{
		return key;
	}

	public Object getValue()
	{
		return value;
	}
}