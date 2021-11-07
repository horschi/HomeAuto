package ebus.reader.impl;

import org.apache.commons.lang3.StringUtils;

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
							{ // resp= 0100 ae00 00
								final float temp = o.getData2bf(false, 2, 16);
								registry.setValue("Vent - Temp SAI", temp); //
								break;
							}

							case 0x02:
							{ // resp= 0200 4001 00
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

							// case 0x09:
							// { //
							// final float xx = o.getData2bi(false, 2); // 0
							// break;
							// }

							case 0x0f:
							{ // resp= 0f00 01
								final int level = o.getData1bi(false, 2);
								registry.setValue("Vent - Level", level); //
								break;
							}
							
							case 0x46:
							{ // c0 b509 # 2946 00 0 4600 0b00 0
								registry.setValue("Vent - Filter days", o.getData2bi(false, 2)); //
								break;
							}

							case 0x0e:
							{ // resp= 0e00 00
								registry.setValue("Vent - b509 29 0e", o.getData1bi(false, 2)); //
								break;
							}

							// case 0x1c:
							// { // resp=1c00 0000
							// registry.setValue("Vent - b509 29 1c", o.getData2bi(false, 2)); // 0
							// break;
							// }

							// case 0x1d:
							// { // resp=1d00 0000
							// registry.setValue("Vent - b509 29 1d", o.getData2bi(false, 2)); // 0
							// break;
							// }


							// case 0x20:
							// { //
							// registry.setValue("Vent - b509 29 20", o.getData1bi(false, 2)); // 03
							// break;
							// }

							// case 0x45:
							// { //
							// registry.setValue("Vent - b509 29 45", o.getData1bi(false, 2)); // 9600 = 150 = 9.4
							// break;
							// }

							case 0x47:
							{ //
								registry.setValue("Vent - b509 29 47", o.getData1bi(false, 2)); // 0
								break;
							}

							case 0x4d:
							{ // resp= 4d00 9237
								registry.setValue("Vent - Energy saved", o.getData2bi(false, 2)); //
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
								registry.setValue("Vent - b509 29 " + Integer.toHexString(o.getData1bi(true, 1)), str); //
								break;
							}
						}
						break;
					}

					case 0x0e:
					{
						switch (o.getData1bi(true, 1))
						{
							case 0x00:
							{ // req= 0e00 003d 0100
								registry.setValue("Vent - Temp avg inside (Avg SAO,EAI)", o.getData2bf(true, 3, 16));
								break;
							}
							case 0x0a:
							{ // req= 0e0a 0001
								registry.setValue("Vent - Rueckgewinnung?", o.getData1bi(true, 3));
								break;
							}

							case 0x15:
							{ // req= 0e15 00a0 01
								final int spd = o.getData2bi(true, 3);
								final int spdq = (int) (Math.round(0.3 * spd));
								registry.setValue("Vent - Speed", "" + spdq + "m&#179; (" + spd + ")");
								break;
							}

							default:
							{
								String str = StringUtils.substring(o.getRequestStr(), 4) + " ==> ";
								switch (o.getResponse().length - 2)
								{
									case 6:
									case 5:
									case 4:
									case 3:
										// str += " 1bi=" + o.getData1bi(false, 3);
									case 2:
										str += " 2bi=" + o.getData2bi(true, 2);
										str += " 2bf=" + o.getData2bf(true, 2, 16);
									case 1:
										str += " 1bi=" + o.getData1bi(true, 2);
										str += " 1bi=" + o.getData1bf(true, 2, 16);
										break;

									default:
										break;
								}
								registry.setValue("Vent - b509 0e " + Integer.toHexString(o.getData1bi(true, 1)), str); //
								break;
							}
						}
						break;
					}
				}
				break;
			}
			case 0xb516:
			{ //
				switch (o.getData1bi(true, 0))
				{
					case 0x00:
					{ //
						switch (o.getData1bi(true, 1))
						{
							case 0x03:
							{ // Date / time
								// req: 0003 2822 0111 0121
								final int minutes = o.getData1bi(true, 2);
								final int hours = o.getData1bi(true, 3);
								final int day = o.getData1bi(true, 4);
								final int month = o.getData1bi(true, 5);
								final int weekday = o.getData1bi(true, 6);
								final int year = o.getData1bi(true, 7);

								final String dateStr = String.format("%02X:%02X ", hours, minutes) + String.format("%02X.%02X.20%02X ", day, month, year);
								registry.setValue("Vent - Date/Time", dateStr); //
								break;
							}
						}
						break;
					}
					default:
					{
						registry.setValue("Vent - b516 00 ", o.getRequestStr()); //
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
