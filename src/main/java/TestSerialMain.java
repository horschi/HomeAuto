import java.io.InputStream;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortTimeoutException;

public class TestSerialMain
{

	public static void main(final String[] args)
	{
		// eHZ-IW8E2A5
		// Dieser Zaehler kommuniziert an der Frontschnittstelle mit 9600bd, 8N1 und sendet ohne Aufforderung im SML-Format.

		SerialPort comPort;
		comPort = SerialPort.getCommPort("/dev/ttyUSB0");
		comPort.setBaudRate(9600);
		comPort.setNumDataBits(8);
		comPort.setParity(0);
		comPort.setNumStopBits(1);
		comPort.openPort();

		comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 500, 0);
		final InputStream in = comPort.getInputStream();
		System.out.println("SerialConnection initialized: " + comPort);

		try
		{
			int n = 0;
			while (true)
			{
				try
				{
					System.out.print(String.format(" %02X", in.read()));
					n++;
					if (n > 32)
					{
						System.out.println();
						n = 0;
					}
				}
				catch (final SerialPortTimeoutException e)
				{
					n = 0;
					System.out.println("\n");
					while (in.available() <= 0)
						Thread.sleep(10);
				}
			}
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
	}

}
