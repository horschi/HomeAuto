package ebus;

import ebus.protocol.EBusData;

public interface EBusReader
{
	public void parseCommands(final EBusData o);
}
