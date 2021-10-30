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
			case 0xb509:
			{ // main screen
				switch (o.getData1bi(true, 0))
				{
					case 0x29:
					{ // request value
						switch (o.getData1bi(true, 1))
						{
							case 0x01:
							{ // ??
								registry.setValue("Vent - 29 01", o.getResponseStr()); //

								final float temp = o.getData1bf(false, 2, 10);
								registry.setValue("Vent - Inside temp (SAO)", temp); //
								break;
							}
							case 0x09:
							{ // ??
								registry.setValue("Vent - 29 09", o.getResponseStr()); //
								break;
							}
							case 0x0f:
							{ // s 10 c0 b509 # 290f 00 0 0f00 02 0
								registry.setValue("Vent - 29 0f", o.getResponseStr()); //

								final int level = o.getData1bi(false, 2);
								registry.setValue("Vent - Level", level); //
								break;
							}
							case 0x0e:
							{ // ??
								registry.setValue("Vent - 29 0e", o.getResponseStr()); //
								break;
							}
							case 0x4d:
							{ // ??
								registry.setValue("Vent - 29 4d", o.getResponseStr()); //
								break;
							}
						}
					}
				}
				// switch (o.getData1bi(true, 0) << 8 | o.getData1bi(true, 1))
				// {
				// case 0x2901:
				// { // 0100 be00 00
				// final float temp = o.getData1bf(false, 2, 10);
				// registry.setValue("Vent - Inside temp (SAO)", temp); //
				// break;
				// }
				// case 0x2909:
				// { // 0900 00
				// break;
				// }
				// case 0x290f:
				// { // 0f00 00
				// break;
				// }
				// case 0x294d:
				// { // 4d00 6637
				// break;
				// }
				// }
				break;
			}
			case 0xb503:
			{ // service menu
				break;
			}
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
				break;
			}
			default:
				break;
		}
	}
}
