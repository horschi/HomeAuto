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
//					-s	1	15	0621	# 02b3 0048	0	3381 0000 ff00 0000 0100	0
//					-s	1	15	0621	# 02b4 004e	0	3481 0000 ff00 0000 0000	0
//					-s	1	15	0621	# 00ba 004a	0	3a80 0308 6400 0000 0080	0
//					-s	1	15	0621	# 02b3 004a	0	3381 0000 ff00 0000 0000	0
//					-s	1	15	0621	# 02c8 0040	0	4841 042a 9f05 0000 8105	0
//					-s	1	15	0621	# 02c9 0040	0	49c1 0000 0600 0000 0300	0
//					-s	1	15	0621	# 0081 0048	0	0180 0d02 f401 0000 c900	0
//					-s	1	15	0621	# 00ba 0048	0	3a80 0308 6400 0000 0080	0
//					-s	1	15	0621	# 0082 0048	0	0280 0d02 e803 0000 f100	0
//					-s	1	15	0621	# 0084 004e	0	0480 0d02 e803 0000 9001	0
//					-s	1	15	0621	# 0080 0048	0	0080 0d02 f401 0cfe 4d00	0
					
					case 0x78890000:
					{ // response: 1481 0d02 f401 0cfe 4000
						final float v = o.getData2bf(false, 8, 10);
						registry.setValue("Outside Temp Avg", v); // correct?
						// registry.writeValueToLog("OutsideTempAvg", v);
						break;
					}

					case 0x7d810002:
					{
						final float v = o.getData2bf(false, 8, 10);
						registry.setValue("Ruecklauf Temp", v); //
						// registry.writeValueToLog("Ruecklauftemp", v);
						break;
					}

					case 0x7d830002:
					{
						registry.setValue("Brine In", o.getData2bf(false, 8, 10)); //
						break;
					}

					case 0x7d820002:
					{
						registry.setValue("Brine Out", o.getData2bf(false, 8, 10)); //
						break;
					}

					case 0x00800048:
					{
						// response: 0080 0d02 f401 0cfe 4d00
						final float outsideTemp = o.getData2bf(false, 8, 10);
						registry.setValue("Outside Temp", outsideTemp);
						// writeValueToLog("OutsideTemp", outsideTemp);
						break;
					}

					case 0x0084004e:
					{
						// response: 0480 0d02 e803 0000 9001

						// final float unknown6 = o.getData2bf(false, 1, 1);
						// getKnownValueObj("unknown6").setValue("" + unknown6 + " " + (unknown6 / 256));
						// final float unknown5 = o.getData2bf(false, 2, 1);
						// getKnownValueObj("unknown5").setValue("" + unknown5 + " " + (unknown5 / 256));
						// final float unknown4 = o.getData2bf(false, 4, 1);
						// getKnownValueObj("unknown4").setValue("" + unknown4 + " " + (unknown4 / 256));

						final float waterTemp = o.getData2bf(false, 8, 10);
						registry.setValue("Water Temp", waterTemp);
						// registry.writeValueToLog("WaterTemp", waterTemp);
						break;
					}
					default:
				}
				break;
			}
			case 0x0501:
			{
				// req= 0000 0a08 0000 0000 0100
				// req= 0028 0a08 0000 0000 0000
				break;
			}
			case 0x0503:
			{ // req= 0100 0000 31ff ff3f 4300

				break;
			}
			case 0x0700: // broadcast
			{
				final float v = o.getData2bf(true, 0, 256);
				registry.setValue("Outside Temp", v);
				// registry.writeValueToLog("OutsideTemp", v);

				final int seconds = o.getData1bi(true, 2);
				final int minutes = o.getData1bi(true, 3);
				final int hours = o.getData1bi(true, 4);
				final int day = o.getData1bi(true, 5);
				final int month = o.getData1bi(true, 6);
				final int weekday = o.getData1bi(true, 7);
				final int year = o.getData1bi(true, 8);

				final String dateStr = String.format("%02X:%02X:%02X ", hours, minutes, seconds) + String.format("%02X.%02X.20%02X ", day, month, year);
				registry.setValue("Heat - Date/Time", dateStr); //
				break;
			}
			case 0x0801: // broadcast
			{
				// request: ???? wwww ???? ????
				// request: 9a17 b32a 0000 8017
				// request: 9a18 3328 0000 e616
				// request: 9a18 3328 0000 cd16
				// request: 8018 3328 0000 cd16
				// request: 8018 3328 0000 b316
				// request: 8018 3328 0000 9a16
				// request: 6618 3328 0000 9a16

				final float waterTemp = o.getData2bf(true, 2, 256);
				registry.setValue("Water Temp", waterTemp);

				// writeValueToLog("WaterTemp", v);

				// final float vorlaufTemp = o.getData2bf(true, 0, 256); // values switch sometimes?
				// getKnownValueObj("Vorlauftemp?").setValue(vorlaufTemp);

				// final float ruecklaufTemp = o.getData2bf(true, 6, 256); // values switch sometimes?
				// getKnownValueObj("Ruecklauftemp?").setValue(ruecklaufTemp);

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
						registry.setValue("Heat - On/Off Flag - Unknown Value=" + onoffflagstr, "" + new Date());
						break;
				}

				final KnownValueEntry obj = registry.getKnownValueObj("Heat - On/Off Flag");
				if (obj.getValue() != null && !obj.getValue().equals(onoffflagstr) && obj.getTsLastChange() > 0L)
				{
					final long vs = (System.currentTimeMillis() - obj.getTsLastChange()) / 1000;
					final long vm = (vs / 60);
					registry.setValue("Heat - On/Off flag - time " + obj.getValue(), vs, "" + vm + "m");
					// registry.writeValueToLog("Time" + obj.getValue(), vm);
				}
				registry.setValue("Heat - On/Off Flag", onoffflagstr);
				// registry.writeValueToLog("OnOffFlag", onoffflagstr);

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
						// req = .... VVVV RRRR ???? ???? WWWW RRRR
						// req = 1000 f600 e500 f600 0000 9201 c700
						// req = 1000 f300 de00 f300 0000 9101 c800
						// req = 1000 f200 e000 f200 0000 9001 ca00
						final float vorlaufTemp = o.getData2bf(true, 2, 10);
						registry.setValue("Heat - Vorlauftemp", vorlaufTemp);
						// registry.writeValueToLog("Vorlauftemp", vorlaufTemp);

						final float ruecklauftemp = o.getData2bf(true, 4, 10);
						registry.setValue("Heat - Ruecklauftemp", ruecklauftemp);
						// registry.writeValueToLog("Ruecklauftemp", ruecklauftemp);

						// knownValues.put("Heat - Vorlauftemp", new KnownValueEntry(o.getData2bf(true, 6, 10))); // 24,2

						// 8 = always 0000

						final float waterTemp = o.getData2bf(true, 10, 10);
						registry.setValue("Water Temp", waterTemp);
						// registry.writeValueToLog("WaterTemp", waterTemp);

						final float roomTemp  = o.getData2bf(true, 12, 10); // 19,9
						registry.setValue("Heat - Room Temp", roomTemp);
						
						break;
					}
					case 0x1001:
					{
						// req = .... VVVV ???? ???? ???? WWWW ????
						// req = 1001 f300 0000 0000 0000 9101 0000
						
						final float vorlaufTemp = o.getData2bf(true, 2, 10);
						registry.setValue("Heat - Vorlauftemp", vorlaufTemp);
						// registry.writeValueToLog("Vorlauftemp", vorlaufTemp);
						
						final float waterTemp = o.getData2bf(true, 10, 10);
						registry.setValue("Water Temp", waterTemp);
						// registry.writeValueToLog("WaterTemp", waterTemp);

						break;
					}
					case 0x1002:
					{
						// req = .... ???? ???? VVVV ???? ???? ????
						// req = 1002 0000 0000 0001 0000 6400 e100
						
						final float vorlaufSoll = o.getData2bf(true, 6, 10);
						registry.setValue("Heat - VorlaufSoll", vorlaufSoll); // knownValues.put("Vorlaufsoll??", new KnownValueEntry(o.getData2bf(true, 6, 10))); //
						// registry.writeValueToLog("VorlaufSoll", vorlaufSoll);

						registry.setValue("Water Temp Sollwert", o.getData2bf(true, 10, 10)); // 100A 1102 b10, 64 = 100 = 10.0
						registry.setValueDebug("Heat - 100A 1102 b12", o.getData2bf(true, 12, 10)); // e1 = 225 = 22.5
						break;
					}
					case 0x1003:
					{
						// req = 1003 0000 0000 0000 0000 6400 0000
						// knownValues.put("Unknown3", new KnownValueEntry(o.getData2bi(true, 10))); // val = 64h = 100
						break;
					}
					case 0x1100:
					{
						// req = .... OOOO ???? ???? ???? 2400
						// req = 1100 5000 0000 0000 0000 2400
						// req = 1100 4e00 0000 0000 0000 2400
						registry.setValue("Outside Temp", o.getData2bf(true, 2, 10)); //
						break;
					}
					case 0x1101:
					{
						// req = 1101 5000 0000 0000 0000 0000
						break;
					}
					case 0x1102:
					{
						// req = 1102 0000 370a 0102 0021 3500
						// req = 1102 0000 3116 0102 0021 3500
						// req = 1102 0000 3816 0102 0021 3500

						registry.setValueDebug("Heat - 100A 1102", o.getData2bf(true, 4, 256)); //
						// used to be 370a -> 2615 -> 10,21484375
						// now is 3116 -> 5681 -> 22,19140625
						// now is 3816 -> 5688 -> 22,21875

						//
						break;
					}
					case 0x1103: // while heating is runnning?
					{
						// req = 1103 0000 2b0a 0100 0020 3100
						// req = 1103 0000 3916 0000 0020 3100
						// req = 1103 0000 3816 0000 0020 3100
						// req = 1103 0000 3716 0000 0020 3100
						// ...............counter
						// req = 1103 0000 2b16 0000 0020 3100

						registry.setValueDebug("Heat - 100A 1103", o.getData2bf(true, 4, 1)); //
						//
						// 3716 -> 5687 -> 22,21
						break;
					}

					default:
						break;
				}
				break;
			}
			case 0xFE01:
			{
				try
				{
					registry.setValue("Heat - Error", new String(o.getRequest())); //
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
