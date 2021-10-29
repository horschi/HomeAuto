package ebus.conn.impl;

import java.io.InputStream;

import com.fazecast.jSerialComm.SerialPort;

import ebus.conn.AbstractConnection;

public class SerialConnection implements AbstractConnection
{
	private SerialPort comPort;

	@Override
	public void init() throws Exception
	{
		comPort = SerialPort.getCommPorts()[0];
		comPort.openPort();
		comPort.setBaudRate(2400);
		comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);
		final InputStream in = comPort.getInputStream();
		try
		{
			for (int j = 0; j < 1000; ++j)
				System.out.print((char) in.read());
			in.close();
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
		System.out.println("SerialConnection initialized: " + comPort);
	}

	@Override
	public InputStream getInputStream() throws Exception
	{
		return comPort.getInputStream();
	}
	
	@Override
	public void close()
	{
		comPort.closePort();
	}
}
