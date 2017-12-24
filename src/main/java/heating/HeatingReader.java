package heating;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.lang3.StringUtils;

import com.pi4j.io.serial.Baud;
import com.pi4j.io.serial.DataBits;
import com.pi4j.io.serial.FlowControl;
import com.pi4j.io.serial.Parity;
import com.pi4j.io.serial.Serial;
import com.pi4j.io.serial.SerialConfig;
import com.pi4j.io.serial.SerialDataEvent;
import com.pi4j.io.serial.SerialDataEventListener;
import com.pi4j.io.serial.SerialFactory;
import com.pi4j.io.serial.StopBits;
import com.pi4j.util.StringUtil;

import ebus.EBusData;

public class HeatingReader
{
	private final Serial										serial;
	private final PipedInputStream								inputStream		= new PipedInputStream(4096);
	private final PipedOutputStream								outputStream	= new PipedOutputStream(inputStream);

	private static final int									QUEUESIZE		= 1000;
	private final Queue<EBusData>								queue			= new ArrayBlockingQueue<>(QUEUESIZE, true);
	private final Map<String, LinkedBlockingQueue<EBusData>>	index			= new HashMap<>();

	private final Thread										processingThread;
	private int													numParsed		= 0;
	private int													numValid		= 0;
	private int													numWithMessage	= 0;
	private final Map<String, KnownValueEntry>					knownValues		= Collections.synchronizedMap(new TreeMap<>());
	// private HeatingDL dlPredictHeatingTime;
	// private HeatingDL dlPredictHotwaterTime;
	private FileWriter											valueLog;

	public HeatingReader() throws Exception
	{
		// System.out.println("HeatingReader: initializing neuronal nets ...");
		// {
		// Range outputConf = new Range(0.0, 120.0);
		// Map<String, Range> inputConf = new TreeMap<>();
		// inputConf.put("Outside temp", new Range(-10, 15.0));
		// inputConf.put("Ruecklauftemp", new Range(20.0, 35.0));
		// inputConf.put("Vorlauftemp", new Range(20.0, 40.0));
		// inputConf.put("Vorlaufsoll", new Range(25.0, 35.0));
		// dlPredictHeatingTime = new HeatingDL(outputConf, inputConf);
		// }
		// {
		// Range outputConf = new Range(0.0, 120.0);
		// Map<String, Range> inputConf = new TreeMap<>();
		// inputConf.put("Outside temp", new Range(-10, 15.0));
		// inputConf.put("Ruecklauftemp", new Range(20.0, 35.0));
		// inputConf.put("Vorlauftemp", new Range(20.0, 40.0));
		// inputConf.put("Vorlaufsoll", new Range(25.0, 35.0));
		// dlPredictHotwaterTime = new HeatingDL(outputConf, inputConf);
		// }
		// System.out.println("HeatingReader: neuronal nets initialized");

		valueLog = new FileWriter("values.csv", true);
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					valueLog.close();
				}
				catch (final IOException e)
				{
					e.printStackTrace();
				}
			}
		}, "ShutdownHook"));

		// https://www.cube-controls.com/2015/11/02/disable-serial-port-terminal-output-on-raspbian/
		// --> disable serial log in raspi-config!
		// --> add "enable_uart=1" to /boot/config.txt

		// set up processing
		processingThread = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					while (inputStream.read() != 0xAA)
					{
					}
				}
				catch (final IOException e)
				{
					System.err.println("Error while reading initial syn from EBus");
					e.printStackTrace();
					return;
				}
				while (true)
				{
					try
					{
						final EBusData o = new EBusData(inputStream);
						synchronized (queue)
						{
							numParsed++;
							if (o.isValid())
							{
								numValid++;
								parseKnownProperties(o);
							}
							if (o.getMessage() != null)
								numWithMessage++;
							if (queue.size() >= QUEUESIZE)
								queue.poll();
							queue.add(o);
						}
					}
					catch (final Exception e)
					{
						e.printStackTrace();
					}
				}
			}
		}, "HeatingReaderProcessing");
		processingThread.start();

		// set up serial
		serial = SerialFactory.createInstance();
		serial.addListener(new SerialDataEventListener()
		{
			@Override
			public void dataReceived(final SerialDataEvent event)
			{
				try
				{
					final byte[] data = event.getBytes();
					outputStream.write(data);
				}
				catch (final Exception e)
				{
					e.printStackTrace();
					try
					{
						for (int i = 0; i < 255; i++)
							outputStream.write(0xaa);
					}
					catch (final IOException e1)
					{
					}
				}
			}
		});

		final SerialConfig config = new SerialConfig();
		try
		{
			final String p = "/dev/ttyS0";// SerialPort.getDefaultPort();
			config.device(p);
			config.baud(Baud._2400);
			config.dataBits(DataBits._8);
			config.parity(Parity.NONE);
			config.stopBits(StopBits._1);
			config.flowControl(FlowControl.NONE);
			serial.open(config);
		}
		catch (final Exception e)
		{
			throw new Exception("serial open with config: " + config, e);
		}
		System.out.println("HeatingReader initialized: " + serial);
	}

	private void writeValueToLog(final String prop, final Object val)
	{
		if (val == null)
			return;
		final String vstr = val.toString();
		if (StringUtil.isNullOrEmpty(vstr))
			return;
		try
		{
			valueLog.write("" + System.currentTimeMillis() + "," + prop + "," + vstr + "\n");
		}
		catch (final IOException e)
		{
			e.printStackTrace();
		}
	}

	public static class KnownValueEntry
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

	private KnownValueEntry getKnownValueObj(final String key)
	{
		KnownValueEntry r = this.knownValues.get(key);
		if (r == null)
		{
			r = new KnownValueEntry(null);
			this.knownValues.put(key, r);
		}
		return r;
	}

	private void parseKnownProperties(final EBusData o)
	{
		final String key = o.getCmdStr();
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
						knownValues.put("Outside temp avg", new KnownValueEntry(v)); // correct?
						writeValueToLog("OutsideTempAvg", v);
						break;
					}

					case 0x7d810002:
					{
						final float v = o.getData2bf(false, 8, 10);
						knownValues.put("Ruecklauftemp", new KnownValueEntry(v)); //
						writeValueToLog("Ruecklauftemp", v);
						break;
					}

					case 0x7d830002:
					{
						knownValues.put("Brine in", new KnownValueEntry(o.getData2bf(false, 8, 10))); //
						break;
					}

					case 0x7d820002:
					{
						knownValues.put("Brine out", new KnownValueEntry(o.getData2bf(false, 8, 10))); //
						break;
					}

					case 0x00800048:
					{
						final float outsideTemp = o.getData2bf(false, 8, 10);
						getKnownValueObj("Outside temp 3").setValue(outsideTemp);
						// writeValueToLog("OutsideTemp", outsideTemp);
					}

					case 0x0084004e:
					{
						final float unknown6 = o.getData2bf(false, 1, 1);
						getKnownValueObj("unknown6").setValue("" + unknown6 + " " + (unknown6 / 256));
						final float unknown5 = o.getData2bf(false, 2, 1);
						getKnownValueObj("unknown5").setValue("" + unknown5 + " " + (unknown5 / 256));
						final float unknown4 = o.getData2bf(false, 4, 1);
						getKnownValueObj("unknown4").setValue("" + unknown4 + " " + (unknown4 / 256));

						final float waterTemp = o.getData2bf(false, 8, 10);
						getKnownValueObj("Water temp 2").setValue(waterTemp);
						// writeValueToLog("WaterTemp", waterTemp);
					}
					default:
				}
				break;
			}
			case 0x0700: // broadcast
			{
				final float v = o.getData2bf(true, 0, 256);
				getKnownValueObj("Outside temp").setValue(v);
				writeValueToLog("OutsideTemp", v);

				final int seconds = o.getData1bi(true, 2);
				final int minutes = o.getData1bi(true, 3);
				final int hours = o.getData1bi(true, 4);
				final int day = o.getData1bi(true, 5);
				final int month = o.getData1bi(true, 6);
				final int weekday = o.getData1bi(true, 7);
				final int year = o.getData1bi(true, 8);

				knownValues.put("Date/Time", new KnownValueEntry(
						"" + Integer.toString(hours, 16) + ":" + Integer.toString(minutes, 16) + ":" + Integer.toString(seconds, 16) + " " + Integer.toString(day, 16) + "." + Integer.toString(month, 16) + ".20" + Integer.toString(year, 16))); //
				break;
			}
			case 0x0801: // broadcast
			{ // request: 9a17 b32a 0000 8017
				final float vorlaufTemp = o.getData2bf(true, 0, 256);
				getKnownValueObj("Vorlauftemp?").setValue(vorlaufTemp);

				final float waterTemp = o.getData2bf(true, 2, 256);
				getKnownValueObj("Water temp?").setValue(waterTemp);
				// writeValueToLog("WaterTemp", v);

				final float ruecklaufTemp = o.getData2bf(true, 6, 256);
				getKnownValueObj("Ruecklauftemp?").setValue(ruecklaufTemp);

				break;
			}
			case 0x0802: // broadcast
			{
				final int onoffflagval = o.getData2bi(true, 0);
				String onoffflagstr;
				switch (onoffflagval)
				{
					case 0x0000:
						onoffflagstr = "off";
						break;
					case 0x2a00: // 101010
					case 0x2b00: // 101011
					case 0x2900: // 101001
					case 0x2800: // 101000
						onoffflagstr = "Heizung";
						break;

					case 0x3600: // 110110
						onoffflagstr = "Warmwasser";
						break;

					default:
						onoffflagstr = Integer.toString(onoffflagval, 16);
						getKnownValueObj("On/Off flag - Unknown value=" + onoffflagstr).setValue("" + new Date());
						break;
				}

				final KnownValueEntry obj = getKnownValueObj("On/Off flag");
				if (obj.getValue() != null && !obj.getValue().equals(onoffflagstr) && obj.getTsLastChange() > 0L)
				{
					final long v = ((System.currentTimeMillis() - obj.getTsLastChange()) / 1000 / 60);
					getKnownValueObj("On/Off flag - time " + obj.getValue()).setValue("" + v + "m");
					writeValueToLog("Time" + obj.getValue(), v);
				}
				obj.setValue(onoffflagstr);
				writeValueToLog("OnOffFlag", onoffflagstr);

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
						getKnownValueObj("Vorlauftemp").setValue(vorlaufTemp);
						writeValueToLog("Vorlauftemp", vorlaufTemp);

						final float ruecklauftemp = o.getData2bf(true, 4, 10);
						getKnownValueObj("Ruecklauftemp").setValue(ruecklauftemp);
						writeValueToLog("Ruecklauftemp", ruecklauftemp);

						// knownValues.put("Vorlauftemp", new KnownValueEntry(o.getData2bf(true, 6, 10))); // ok
						// 8 = always 0000

						final float waterTemp = o.getData2bf(true, 10, 10);
						getKnownValueObj("Water temp").setValue(waterTemp);
						writeValueToLog("WaterTemp", waterTemp);

						break;
					}
					case 0x1001:
					{
						final float waterTemp = o.getData2bf(true, 10, 10);
						getKnownValueObj("Water temp").setValue(waterTemp);
						writeValueToLog("WaterTemp", waterTemp);

						final float vorlaufTemp = o.getData2bf(true, 2, 10);
						getKnownValueObj("Vorlauftemp").setValue(vorlaufTemp);
						writeValueToLog("Vorlauftemp", vorlaufTemp);
						break;
					}
					case 0x1002:

						final float vorlaufSoll = o.getData2bf(true, 6, 10);
						getKnownValueObj("Vorlaufsoll").setValue(vorlaufSoll); // knownValues.put("Vorlaufsoll??", new KnownValueEntry(o.getData2bf(true, 6, 10))); //
						writeValueToLog("VorlaufSoll", vorlaufSoll);

						break;
					case 0x1003:
						// knownValues.put("Unknown3", new KnownValueEntry(o.getData2bi(true, 10))); // val = 64h = 100
						break;
					case 0x1100:
						knownValues.put("Outside temp 2", new KnownValueEntry(o.getData2bf(true, 2, 10))); //
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
					getKnownValueObj("Error FE01").setValue(new String(o.getRequest())); //
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

	public Map<String, KnownValueEntry> getKnownValues()
	{
		return knownValues;
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

}
