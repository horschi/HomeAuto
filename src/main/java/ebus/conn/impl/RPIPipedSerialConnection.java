package ebus.conn.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

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

import ebus.conn.AbstractSerialConnection;

public class RPIPipedSerialConnection implements AbstractSerialConnection
{
	private final Serial			serial;
	private final PipedInputStream	inputStream		= new PipedInputStream(4096);
	private final PipedOutputStream	outputStream	= new PipedOutputStream(inputStream);

	public RPIPipedSerialConnection() throws Exception
	{
		// https://www.cube-controls.com/2015/11/02/disable-serial-port-terminal-output-on-raspbian/
		// --> disable serial log in raspi-config!
		// --> add "enable_uart=1" to /boot/config.txt

		// apt install wiringpi

		serial = SerialFactory.createInstance();
	}

	@Override
	public void init(final String portName, final int baudRate) throws Exception
	{
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
			final String p = portName;// SerialPort.getDefaultPort(); // "/dev/ttyS0";
			System.out.println("RPI using serial port " + p);
			config.device(p);
			switch (baudRate)
			{
				case 2400:
					config.baud(Baud._2400);
					break;
				case 9600:
					config.baud(Baud._9600);
					break;

				default:
					throw new IllegalStateException("Invalid baud rate");
			}
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

		System.out.println("RPISerialConnection initialized: " + serial);
	}

	@Override
	public InputStream getInputStream() throws Exception
	{
		return inputStream;
	}

	@Override
	public void close()
	{
		try
		{
			serial.close();
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
	}
}
