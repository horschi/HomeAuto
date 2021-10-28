package data;

public class KnownValueEntry
{
	private Object	value;
	private long	tsLastUpdate;
	private long	tsLastChange;

	public KnownValueEntry(final Object value)
	{
		super();
		this.value = value;
		this.tsLastUpdate = System.currentTimeMillis();
		this.tsLastChange = 0L;
	}

	public void setValue(final Object value)
	{
		if (this.value != null && !this.value.equals(value))
		{
			this.tsLastChange = System.currentTimeMillis();
		}
		this.tsLastUpdate = System.currentTimeMillis();
		this.value = value;
	}

	public Object getValue()
	{
		return value;
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