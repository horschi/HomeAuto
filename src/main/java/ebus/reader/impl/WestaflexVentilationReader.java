package ebus.reader.impl;

import data.DebugRegistry;
import data.ValueRegistry;
import ebus.protocol.EBusData;
import ebus.reader.EBusReader;

public class WestaflexVentilationReader implements EBusReader
{
	private final ValueRegistry			registry;
	private final DebugRegistry			debugRegistry;

	public WestaflexVentilationReader(final ValueRegistry registry, final DebugRegistry debugRegistry) throws Exception
	{
		this.registry = registry;
		this.debugRegistry = debugRegistry;
	}


	@Override
	public void parseCommands(final EBusData o)
	{
		if (debugRegistry != null)
			debugRegistry.addToQueue(o);

		switch ((o.getCmdPri() << 8) | o.getCmdSec())
		{
			case 0xFE01:
			{
				try
				{
					registry.setValue("Error FE01", new String(o.getRequest())); //
				}
				catch (final Exception e)
				{
					System.err.println("Error parsing: " + o);
					e.printStackTrace();
				}
			}
			default:
				break;
		}
	}
}
