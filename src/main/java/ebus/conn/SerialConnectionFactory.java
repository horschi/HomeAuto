package ebus.conn;

import ebus.conn.impl.RPIPipedSerialConnection;
import ebus.conn.impl.RPISerialConnection;
import ebus.conn.impl.SerialConnection;

public class SerialConnectionFactory
{
	public static AbstractSerialConnection create(final String type) throws Exception
	{
		switch (type)
		{
			case "rpiserial":
				return new RPISerialConnection();
			case "rpipipedserial":
				return new RPIPipedSerialConnection();
			case "serial":
				return new SerialConnection();

			default:
				return null;
		}
	}
}
