package ebus.conn.impl;

import java.io.InputStream;

import com.fazecast.jSerialComm.SerialPort;

import ebus.conn.AbstractSerialConnection;

public class SerialConnection implements AbstractSerialConnection
{
	private SerialPort comPort;
	private String		portName;
	private int			baudRate;

	@Override
	public void init(final String portName, final int baudRate) throws Exception
	{
		this.portName = portName;
		this.baudRate = baudRate;

		comPort = SerialPort.getCommPort(portName);
		comPort.openPort();
		comPort.setBaudRate(baudRate);
		comPort.setNumDataBits(8);
		comPort.setNumStopBits(1);
		comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);
		// final InputStream in = comPort.getInputStream();
		// try
		// {
		// for (int j = 0; j < 1000; ++j)
		// System.out.print((char) in.read());
		// in.close();
		// }
		// catch (final Exception e)
		// {
		// e.printStackTrace();
		// }
		System.out.println("SerialConnection initialized: " + portName + ", " + baudRate);
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

	@Override
	public String toString()
	{
		return "SerialConnection [comPort=" + comPort + ", portName=" + portName + ", baudRate=" + baudRate + "]";
	}
}
