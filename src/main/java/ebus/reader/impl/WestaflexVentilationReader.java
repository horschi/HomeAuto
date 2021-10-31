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
							{ // SAI
								final float temp = o.getData2bf(false, 2, 16);
								registry.setValue("Vent - Temp SAI", temp); //
								break;
							}

							case 0x02:
							{ // SAO - 0200 4001 00
								final float temp = o.getData2bf(false, 2, 16);
								registry.setValue("Vent - Temp SAO", temp); //
								break;
							}

							case 0x03:
							{ // EAI
								final float temp = o.getData2bf(false, 2, 16);
								registry.setValue("Vent - Temp EAI", temp); //
								break;
							}

							case 0x04:
							{ // EAO
								final float temp = o.getData2bf(false, 2, 16);
								registry.setValue("Vent - Temp EAO", temp); //
								break;
							}

							case 0x0a:
							{
								final int rueck = o.getData1bi(true, 3);
								registry.setValue("Vent - Rueckgewinnung", rueck); //
								break;
							}

							case 0x0f:
							{
								final int level = o.getData1bi(false, 2);
								registry.setValue("Vent - Level", level); //
								break;
							}
							
							case 0x46:
							{ // c0 b509 # 2946 00 0 4600 0b00 0
								registry.setValue("Vent - Filter days", o.getData1bi(false, 2)); //
								break;
							}

							default:
							{
								String str = o.getResponseStr().substring(4) + " ==> ";
								switch (o.getResponse().length - 2)
								{
									case 3:
										// str += " 1bi=" + o.getData1bi(false, 3);
									case 2:
										str += " 2bi=" + o.getData2bi(false, 2);
										str += " 2bf=" + o.getData2bf(false, 2, 16);
									case 1:
										str += " 1bi=" + o.getData1bi(false, 2);
										str += " 1bi=" + o.getData1bf(false, 2, 16);
										break;

									default:
										break;
								}
								registry.setValue("Vent - 29 " + Integer.toHexString(o.getData1bi(true, 1)), str); //
								break;
							}
						}
					}
				}
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
