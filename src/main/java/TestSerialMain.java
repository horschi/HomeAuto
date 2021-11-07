import java.io.InputStream;

import com.fazecast.jSerialComm.SerialPort;

public class TestSerialMain
{

	public static void main(final String[] args)
	{
		SerialPort comPort;
		comPort = SerialPort.getCommPort("/dev/ttyUSB0");
		comPort.openPort();
		comPort.setBaudRate(2400);
		comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);
		final InputStream in = comPort.getInputStream();
		System.out.println("SerialConnection initialized: " + comPort);

		try
		{
			int n = 0;
			while (true)
			{

				System.out.print(String.format(" %02X", in.read()));
				n++;
				if (n > 8)
				{
					System.out.println();
					n = 0;
				}
			}
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
	}

}
