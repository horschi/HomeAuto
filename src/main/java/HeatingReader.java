import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

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

public class HeatingReader
{
	private final Serial						serial;
	private PipedInputStream					inputStream		= new PipedInputStream(4096);
	private PipedOutputStream					outputStream	= new PipedOutputStream(inputStream);

	private static final int					QUEUESIZE		= 500;
	private final Queue<EBusData>				queue			= new ArrayBlockingQueue<>(QUEUESIZE, true);
	private final Map<String, LinkedBlockingQueue<EBusData>>	index			= new HashMap<>();

	private final Thread						processingThread;
	private int									numParsed		= 0;
	private int									numValid		= 0;
	private int									numWithMessage	= 0;
	private Map<String, KnownValueEntry>		knownValues		= Collections.synchronizedMap(new TreeMap<>());
	
	public static class KnownValueEntry
	{
		private Object value;
		private long ts;
		public KnownValueEntry(Object value)
		{
			super();
			this.value = value;
			this.ts = System.currentTimeMillis();
		}
		
		public Object getValue()
		{
			return value;
		}

		public long getTs()
		{
			return ts;
		}

		@Override
		public String toString()
		{
			long tdif = ((System.currentTimeMillis()- ts)/1000/60);
			if(tdif < 1)
				return value.toString();
			else
				return "" + value + ", " + tdif + "m ago";
		}
		
	}
	public HeatingReader() throws Exception
	{
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
				catch (IOException e)
				{
					System.err.println("Error while reading initial syn from EBus");
					e.printStackTrace();
					return;
				}
				while (true)
				{
					try
					{
						EBusData o = new EBusData(inputStream);
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
					catch (Exception e)
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
			public void dataReceived(SerialDataEvent event)
			{
				try
				{
					byte[] data = event.getBytes();
					outputStream.write(data);
				}
				catch (Exception e)
				{
					e.printStackTrace();
					try
					{
						for (int i = 0; i < 255; i++)
							outputStream.write(0xaa);
					}
					catch (IOException e1)
					{
					}
				}
			}
		});

		SerialConfig config = new SerialConfig();
		try
		{
			String p = "/dev/ttyS0";// SerialPort.getDefaultPort();
			config.device(p);
			config.baud(Baud._2400);
			config.dataBits(DataBits._8);
			config.parity(Parity.NONE);
			config.stopBits(StopBits._1);
			config.flowControl(FlowControl.NONE);
			serial.open(config);
		}
		catch (Exception e)
		{
			throw new Exception("serial open with config: " + config, e);
		}
		System.out.println("HeatingReader initialized: " + serial);
	}

	private void parseKnownProperties(EBusData o)
	{
		String key = o.getCmdStr();
		LinkedBlockingQueue<EBusData> indexQueue = index.get(key);
		if(indexQueue == null)
		{
			indexQueue = new LinkedBlockingQueue<>();
			index.put(key, indexQueue);
		}
		else
		{
			if(indexQueue.size() >= QUEUESIZE)
			{
				indexQueue.poll();
			}
		}
		indexQueue.add(o);
		
		
		switch ((o.getCmdPri() << 8) | o.getCmdSec())
		{
			case 0x0621: // triggered by panel action
			{
				int prop = (o.getData1bi(true, 0) << 24) | (o.getData1bi(true, 1) << 16) | (o.getData1bi(true, 2) << 8) | o.getData1bi(true, 3);
				switch (prop)
				{
					case 0x7d810002:
						knownValues.put("Ruecklauftemp", new KnownValueEntry( o.getData2bf(false, 8, 10))); //
						break;

					case 0x7d830002:
						knownValues.put("Brine in", new KnownValueEntry(o.getData2bf(false, 8, 10))); //
						break;

					case 0x7d820002:
						knownValues.put("Brine out", new KnownValueEntry(o.getData2bf(false, 8, 10))); //
						break;

					default:
				}
				break;
			}
			case 0x0700: // broadcast
			{
				knownValues.put("Outside temp", new KnownValueEntry( o.getData2bf(true, 0, 256))); //
					
				int seconds = o.getData1bi(true, 2);
				int minutes = o.getData1bi(true, 3);
				int hours = o.getData1bi(true, 4);
				int day = o.getData1bi(true, 5);
				int month = o.getData1bi(true, 6);
				int weekday = o.getData1bi(true, 7);
				int year = o.getData1bi(true, 8);
				
				knownValues.put("Date/Time", new KnownValueEntry(""+Integer.toString(hours,16)+":"+Integer.toString(minutes,16)+":"+Integer.toString(seconds,16) +" "+Integer.toString(day,16)+"."+Integer.toString(month,16)+".20"+Integer.toString(year,16))); //
				break;
			}
			case 0x0802: // broadcast
			{
				int onoffflagval = o.getData2bi(true, 0);
				String onoffflagstr;
				switch (onoffflagval)
				{
					case 0x0000:
						onoffflagstr = "off";
						break;
					case 0x2a00:
					case 0x2b00:
					case 0x2900:
					case 0x2800:
						onoffflagstr = "Heizung";
						break;

//					case 0x9999:
//						onoffflagstr = "Warmwasser";
//						break;

					default:
						onoffflagstr = Integer.toString(onoffflagval, 16);
						break;
				}
				knownValues.put("On/Off flag", new KnownValueEntry( onoffflagstr)); // 2b = on / 00 = off ?
				
				// int xx = o.getData2bi(true, 2); // val=0a 
				// int xx = o.getData2bi(true, 6); // val=00|40|3f|10

				break;
			}
			
			case 0x100A: // broadcast
			{
				switch (o.getData1bi(true, 0) << 8 | o.getData1bi(true, 1))
				{
					case 0x1000:
						knownValues.put("Vorlauftemp", new KnownValueEntry(o.getData2bf(true, 2, 10))); // ok
						knownValues.put("Ruecklauftemp", new KnownValueEntry(o.getData2bf(true, 4, 10))); // ok
						//knownValues.put("Vorlauftemp", new KnownValueEntry(o.getData2bf(true, 6, 10))); // ok
						// 8 = always 0000
						knownValues.put("Water temp", new KnownValueEntry(o.getData2bf(true, 10, 10))); // ok
						break;
					case 0x1001:
						knownValues.put("Water temp", new KnownValueEntry(o.getData2bf(true, 10, 10))); // ok
						knownValues.put("Vorlauftemp", new KnownValueEntry(o.getData2bf(true, 2, 10))); // ok
						break;
					case 0x1002:
						knownValues.put("Unknown2", new KnownValueEntry(o.getData2bf(true, 2, 10))); // 
						knownValues.put("Vorlaufsoll??", new KnownValueEntry(o.getData2bf(true, 6, 10))); // 
						break;
					case 0x1003:
						//knownValues.put("Unknown3", new KnownValueEntry(o.getData2bi(true, 10))); //  val = 64h = 100
						break;
					case 0x1100:
						knownValues.put("Energieintegral", new KnownValueEntry(o.getData2bf(true, 2, 1))); // 
						break;
					case 0x1101:
						knownValues.put("Energieintegral", new KnownValueEntry(o.getData2bf(true, 2, 1))); // 
						break;
					case 0x1102:
						knownValues.put("Unknown1", new KnownValueEntry(o.getData2bf(true, 4, 1))); // 
						// + a lot of fixes values: 1102 0000 370a 0102 0021 3500
						break;
					case 0x1103: // while heating is runnning?
						knownValues.put("Unknown1", new KnownValueEntry(o.getData2bf(true, 4, 1))); // 
						// + a lot of fixes values:  1103 0000 2b0a 0100 0020 3100	
						break;

					default:
						break;
				}
				break;
			}
			case 0xFE01:
			{
				try
				{
					knownValues.put("Error FE01", new KnownValueEntry(new String(o.getRequest()))); //
				}
				catch (Exception e)
				{
					System.err.println("Error parsing: "+o);
					e.printStackTrace();
				}
			}
			default:
				break;
		}
	}

	public  EBusData pollData()
	{
		synchronized (queue)
		{
			return queue.poll();
		}
	}

	public List< EBusData> getData(String commandStr)
	{
		
		synchronized (queue)
		{
			final List<EBusData> ret ;
			if(StringUtils.isNotBlank( commandStr ))
			{
				if( index.get(commandStr) == null)
					ret = new ArrayList<>();
				else
					ret = new ArrayList<>(index.get(commandStr));
			}
			else
			{
				ret = new ArrayList<>(queue);
			}
			
			Collections.sort(ret, new Comparator< EBusData>()
			{
				@Override
				public int compare(EBusData a,  EBusData b)
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
