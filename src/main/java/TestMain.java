import java.util.Arrays;

import com.fazecast.jSerialComm.SerialPort;

public class TestMain
{

	public static void main(final String[] args)
	{
		final int outsidetempL = 0x06;
		final int outsidetempH = 0xb3;
		
		System.out.println(""+((float)0x06e6/256));
		
		final String dateStr = String.format("%02X:%02X:%02X ", 1, 2, 3) + String.format("%02X.%02X.20%02X ", 4, 5, 6);
		System.out.println("dateStr = " + dateStr);

		System.out.println("ports: " + Arrays.toString(SerialPort.getCommPorts()));

		final SerialPort comPort = SerialPort.getCommPort("/dev/ttyUSB0");
		System.out.println("name " + comPort.getSystemPortName());
		System.out.println("" + comPort.getDescriptivePortName());

		comPort.setComPortParameters(2400, 8, 1, 0);
		comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);
		// comPort.setBaudRate(2400);
		// comPort.setNumDataBits(8);
		// comPort.setParity(0);
		// comPort.setNumStopBits(1);
		// comPort.setFlowControl(0);

		final boolean success = comPort.openPort(100, 4096, 4096);
		if (!success)
			throw new IllegalStateException();
		try
		{
			while (true)
			{
				while (comPort.bytesAvailable() == 0)
					Thread.sleep(20);

				final byte[] readBuffer = new byte[comPort.bytesAvailable()];
				final int numRead = comPort.readBytes(readBuffer, readBuffer.length);
				System.out.println("Read " + numRead + " bytes.");
			}
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
		comPort.closePort();
	}

}
