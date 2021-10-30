package ebus.conn.impl;

import java.io.InputStream;

import com.pi4j.io.serial.Baud;
import com.pi4j.io.serial.DataBits;
import com.pi4j.io.serial.FlowControl;
import com.pi4j.io.serial.Parity;
import com.pi4j.io.serial.Serial;
import com.pi4j.io.serial.SerialConfig;
import com.pi4j.io.serial.SerialFactory;
import com.pi4j.io.serial.StopBits;

import ebus.conn.AbstractSerialConnection;

public class RPISerialConnection implements AbstractSerialConnection
{
	private final Serial			serial;

	public RPISerialConnection() throws Exception
	{
		// https://www.cube-controls.com/2015/11/02/disable-serial-port-terminal-output-on-raspbian/
		// --> disable serial log in raspi-config!
		// --> add "enable_uart=1" to /boot/config.txt

		// apt install wiringpi

		serial = SerialFactory.createInstance();
	}

	@Override
	public void init(final String portName) throws Exception
	{
		final SerialConfig config = new SerialConfig();
		try
		{
			final String p = portName;// SerialPort.getDefaultPort(); // "/dev/ttyS0";
			System.out.println("RPI using serial port " + p);
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

		System.out.println("RPISerialConnection initialized: " + serial);
	}

	@Override
	public InputStream getInputStream() throws Exception
	{
		return serial.getInputStream();
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
