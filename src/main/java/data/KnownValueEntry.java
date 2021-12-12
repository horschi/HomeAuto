package data;

public class KnownValueEntry
{
	private Object	value;
	private Object	text;
	private long	tsLastUpdate;
	private long	tsLastChange;
	private long	tsLastQueue;
	private boolean	isDebug;

	public KnownValueEntry(final Object value)
	{
		super();
		this.value = value;
		this.tsLastUpdate = System.currentTimeMillis();
		this.tsLastChange = 0L;
	}

	public void setValue(final Object value, final Object text, final boolean debug)
	{
		if (this.value != null && !this.value.equals(value))
		{
			this.tsLastChange = System.currentTimeMillis();
		}
		this.tsLastUpdate = System.currentTimeMillis();
		this.value = value;
		this.text = text;
		this.isDebug = debug;
	}

	public long getTsLastQueue()
	{
		return tsLastQueue;
	}

	public void setTsLastQueue(final long tsLastQueue)
	{
		this.tsLastQueue = tsLastQueue;
	}

	public Object getValue()
	{
		return value;
	}

	public Object getText()
	{
		if (text != null)
			return text;
		return value;
	}

	public boolean isDebug()
	{
		return isDebug;
	}

	public long getTsLastUpdate()
	{
		return tsLastUpdate;
	}

	public long getTsLastChange()
	{
		return tsLastChange;
	}

	@Override
	public String toString()
	{
		final long tdif = ((System.currentTimeMillis() - tsLastUpdate) / 1000 / 60);
		if (tdif < 1)
			return value.toString();
		else
			return "" + value + ", " + tdif + "m ago";
	}

}