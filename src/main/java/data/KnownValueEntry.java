package data;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

public class KnownValueEntry
{
	private Object									value;
	private Object									text;
	private long									tsLastUpdate;
	private long									tsLastChange;
	private long									tsLastQueue;
	private boolean									isDebug;
	private final LinkedList<Pair<Long, Object>>	historyTexts	= new LinkedList<>();

	public KnownValueEntry(final Object value)
	{
		this.value = value;
		this.tsLastUpdate = System.currentTimeMillis();
		this.tsLastChange = 0L;
	}

	public void setValue(final Object value, final Object text, final boolean debug)
	{
		this.tsLastUpdate = System.currentTimeMillis();
		if (this.value != null && !this.value.equals(value))
		{
			this.tsLastChange = tsLastUpdate;
		}
		if (this.value == null || !this.value.equals(value))
		{
			synchronized (historyTexts)
			{
				this.historyTexts.addFirst(new ImmutablePair<Long, Object>(tsLastUpdate, text != null ? text : value));
				if (historyTexts.size() > 100)
				{
					historyTexts.removeLast();
				}
			}
		}
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

	public List<Pair<Long, Object>> getHistoryTexts()
	{
		synchronized (historyTexts)
		{
			return new ArrayList(historyTexts);
		}
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