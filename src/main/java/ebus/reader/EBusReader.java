package ebus.reader;

import ebus.protocol.EBusData;

public interface EBusReader
{
	public void parseError(final EBusData o);

	public void parseCommands(final EBusData o);
}
