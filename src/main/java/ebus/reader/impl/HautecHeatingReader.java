package ebus.reader.impl;

import java.util.Date;

import data.DebugRegistry;
import data.KnownValueEntry;
import data.ValueRegistry;
import ebus.protocol.EBusData;
import ebus.reader.EBusReader;

public class HautecHeatingReader implements EBusReader
{
	private final ValueRegistry			registry;
	private final DebugRegistry			debugRegistry;

	public HautecHeatingReader(final ValueRegistry registry, final DebugRegistry debugRegistry) throws Exception
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
			case 0x0621: // triggered by panel action
			{
				final int prop = (o.getData1bi(true, 0) << 24) | (o.getData1bi(true, 1) << 16) | (o.getData1bi(true, 2) << 8) | o.getData1bi(true, 3);
				switch (prop)
				{
					case 0x78890000:
					{ // response: 1481 0d02 f401 0cfe 4000
						final float v = o.getData2bf(false, 8, 10);
						registry.setValue("Outside temp avg", v); // correct?
						registry.writeValueToLog("OutsideTempAvg", v);
						break;
					}

					case 0x7d810002:
					{
						final float v = o.getData2bf(false, 8, 10);
						registry.setValue("Ruecklauftemp", v); //
						registry.writeValueToLog("Ruecklauftemp", v);
						break;
					}

					case 0x7d830002:
					{
						registry.setValue("Brine in", o.getData2bf(false, 8, 10)); //
						break;
					}

					case 0x7d820002:
					{
						registry.setValue("Brine out", o.getData2bf(false, 8, 10)); //
						break;
					}

					case 0x00800048:
					{
						final float outsideTemp = o.getData2bf(false, 8, 10);
						registry.setValue("Outside temp", outsideTemp);
						// writeValueToLog("OutsideTemp", outsideTemp);
						break;
					}

					case 0x0084004e:
					{
						// final float unknown6 = o.getData2bf(false, 1, 1);
						// getKnownValueObj("unknown6").setValue("" + unknown6 + " " + (unknown6 / 256));
						// final float unknown5 = o.getData2bf(false, 2, 1);
						// getKnownValueObj("unknown5").setValue("" + unknown5 + " " + (unknown5 / 256));
						// final float unknown4 = o.getData2bf(false, 4, 1);
						// getKnownValueObj("unknown4").setValue("" + unknown4 + " " + (unknown4 / 256));

						final float waterTemp = o.getData2bf(false, 8, 10);
						registry.setValue("Water temp", waterTemp);
						registry.writeValueToLog("WaterTemp", waterTemp);
						break;
					}
					default:
				}
				break;
			}
			case 0x0700: // broadcast
			{
				final float v = o.getData2bf(true, 0, 256);
				registry.setValue("Outside temp", v);
				registry.writeValueToLog("OutsideTemp", v);

				final int seconds = o.getData1bi(true, 2);
				final int minutes = o.getData1bi(true, 3);
				final int hours = o.getData1bi(true, 4);
				final int day = o.getData1bi(true, 5);
				final int month = o.getData1bi(true, 6);
				final int weekday = o.getData1bi(true, 7);
				final int year = o.getData1bi(true, 8);

				final String dateStr = String.format("%02X:%02X:%02X ", hours, minutes, seconds) + String.format("%02X.%02X.20%02X ", day, month, year);
				registry.setValue("Date/Time", dateStr); //
				break;
			}
			case 0x0801: // broadcast
			{ // request: 9a17 b32a 0000 8017

				final float waterTemp = o.getData2bf(true, 2, 256);
				registry.setValue("Water temp", waterTemp);
				// writeValueToLog("WaterTemp", v);

				// final float vorlaufTemp = o.getData2bf(true, 0, 256); // values switch sometimes?
				// getKnownValueObj("Ruecklauftemp?").setValue(vorlaufTemp);

				// final float ruecklaufTemp = o.getData2bf(true, 6, 256); // values switch sometimes?
				// getKnownValueObj("Vorlauftemp?").setValue(ruecklaufTemp);

				break;
			}
			case 0x0802: // broadcast
			{
				final int onoffflagval = o.getData2bi(true, 0);
				String onoffflagstr;
				switch (onoffflagval >>> 12)
				{
					case 0x00:
						onoffflagstr = "off";
						break;

					case 0x02: // 10
						onoffflagstr = "Heizung";
						break;

					case 0x03: // 11
						onoffflagstr = "Warmwasser";
						break;

					default:
						onoffflagstr = Integer.toString(onoffflagval, 16);
						registry.setValue("Heat - On/Off flag - Unknown value=" + onoffflagstr, "" + new Date());
						break;
				}

				final KnownValueEntry obj = registry.getKnownValueObj("Heat - On/Off flag");
				if (obj.getValue() != null && !obj.getValue().equals(onoffflagstr) && obj.getTsLastChange() > 0L)
				{
					final long v = ((System.currentTimeMillis() - obj.getTsLastChange()) / 1000 / 60);
					registry.getKnownValueObj("Heat - On/Off flag - time " + obj.getValue()).setValue("" + v + "m");
					registry.writeValueToLog("Time" + obj.getValue(), v);
				}
				obj.setValue(onoffflagstr);
				registry.writeValueToLog("OnOffFlag", onoffflagstr);

				// int xx = o.getData2bi(true, 2); // val=0a
				// int xx = o.getData2bi(true, 6); // val=00|40|3f|10

				break;
			}

			case 0x100A: // broadcast
			{
				switch (o.getData1bi(true, 0) << 8 | o.getData1bi(true, 1))
				{
					case 0x1000:
					{
						final float vorlaufTemp = o.getData2bf(true, 2, 10);
						registry.setValue("Heat - Vorlauftemp", vorlaufTemp);
						registry.writeValueToLog("Vorlauftemp", vorlaufTemp);

						final float ruecklauftemp = o.getData2bf(true, 4, 10);
						registry.getKnownValueObj("Heat - Ruecklauftemp").setValue(ruecklauftemp);
						registry.writeValueToLog("Ruecklauftemp", ruecklauftemp);

						// knownValues.put("Vorlauftemp", new KnownValueEntry(o.getData2bf(true, 6, 10))); // ok
						// 8 = always 0000

						final float waterTemp = o.getData2bf(true, 10, 10);
						registry.getKnownValueObj("Water temp").setValue(waterTemp);
						registry.writeValueToLog("WaterTemp", waterTemp);

						break;
					}
					case 0x1001:
					{
						final float waterTemp = o.getData2bf(true, 10, 10);
						registry.getKnownValueObj("Water temp").setValue(waterTemp);
						registry.writeValueToLog("WaterTemp", waterTemp);

						final float vorlaufTemp = o.getData2bf(true, 2, 10);
						registry.getKnownValueObj("Heat - Vorlauftemp").setValue(vorlaufTemp);
						registry.writeValueToLog("Vorlauftemp", vorlaufTemp);
						break;
					}
					case 0x1002:
					{
						final float vorlaufSoll = o.getData2bf(true, 6, 10);
						registry.getKnownValueObj("Heat - Vorlaufsoll").setValue(vorlaufSoll); // knownValues.put("Vorlaufsoll??", new KnownValueEntry(o.getData2bf(true, 6, 10))); //
						registry.writeValueToLog("VorlaufSoll", vorlaufSoll);

						break;
					}
					case 0x1003:
						// knownValues.put("Unknown3", new KnownValueEntry(o.getData2bi(true, 10))); // val = 64h = 100
						break;
					case 0x1100:
						registry.setValue("Outside temp", o.getData2bf(true, 2, 10)); //
						break;
					// case 0x1102:
					// knownValues.put("Unknown1", new KnownValueEntry(o.getData2bf(true, 4, 1))); //
					// // + a lot of fixes values: 1102 0000 370a 0102 0021 3500
					// break;
					// case 0x1103: // while heating is runnning?
					// knownValues.put("Unknown1", new KnownValueEntry(o.getData2bf(true, 4, 1))); //
					// // + a lot of fixes values: 1103 0000 2b0a 0100 0020 3100
					// break;

					default:
						break;
				}
				break;
			}
			case 0xFE01:
			{
				try
				{
					registry.setValue("Heat - Error FE01", new String(o.getRequest())); //
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
