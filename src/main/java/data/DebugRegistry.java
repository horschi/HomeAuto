package data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.lang3.StringUtils;

import ebus.protocol.EBusData;

public class DebugRegistry
{
	private static final int									QUEUESIZE		= 1000;
	private final Queue<EBusData>								queue			= new ArrayBlockingQueue<>(QUEUESIZE, true);
	private final Map<String, LinkedBlockingQueue<EBusData>>	index			= new HashMap<>();

	private int													numParsed		= 0;
	private int													numValid		= 0;
	private int													numWithMessage	= 0;
	private int													numBytesRead	= 0;

	public void addToQueue(final EBusData o)
	{
		final String key = o.getCmdStr();

		if (queue.size() >= QUEUESIZE)
			queue.poll();
		queue.add(o);

		LinkedBlockingQueue<EBusData> indexQueue = index.get(key);
		if (indexQueue == null)
		{
			indexQueue = new LinkedBlockingQueue<>();
			index.put(key, indexQueue);
		}
		else
		{
			if (indexQueue.size() >= QUEUESIZE)
			{
				indexQueue.poll();
			}
		}
		indexQueue.add(o);
	}

	public void incNumParsed()
	{
		numParsed++;
	}

	public void incNumValid()
	{
		numValid++;
	}

	public void incNumWithMessage()
	{
		numWithMessage++;
	}

	public void incNumBytesRead()
	{
		numBytesRead++;
	}

	public int getNumParsed()
	{
		return numParsed;
	}

	public int getNumValid()
	{
		return numValid;
	}

	public int getNumWithMessage()
	{
		return numWithMessage;
	}

	public int getNumBytesRead()
	{
		return numBytesRead;
	}

	public EBusData pollData()
	{
		synchronized (queue)
		{
			return queue.poll();
		}
	}

	public Set<String> getIndexKeys(final Set<String> exclude)
	{
		final Set<String> ret = new TreeSet<>();
		for (final String i : index.keySet())
		{
			if (!exclude.contains(i))
			{
				ret.add(i);
			}
		}
		return ret;
	}

	public List<EBusData> getData(final String commandStr)
	{
		synchronized (queue)
		{
			final List<EBusData> ret;
			if (StringUtils.isNotBlank(commandStr))
			{
				if (index.get(commandStr) == null)
					ret = new ArrayList<>();
				else
					ret = new ArrayList<>(index.get(commandStr));
			}
			else
			{
				ret = new ArrayList<>(queue);
			}

			Collections.sort(ret, new Comparator<EBusData>()
			{
				@Override
				public int compare(final EBusData a, final EBusData b)
				{
					return Long.compare(b.getTimestamp(), a.getTimestamp());
				}
			});
			return ret;
		}
	}
}
