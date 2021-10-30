package ebus.reader;

import data.DebugRegistry;
import data.ValueRegistry;
import ebus.reader.impl.HautecHeatingReader;
import ebus.reader.impl.WestaflexVentilationReader;

public class EBusReaderFactory
{
	public static EBusReader create(final String type, final ValueRegistry registry, final DebugRegistry debugRegistry) throws Exception
	{
		switch (type)
		{
			case "hautecheating":
				return new HautecHeatingReader(registry, debugRegistry);
			case "westaflexventilation":
				return new WestaflexVentilationReader(registry, debugRegistry);

			default:
				return null;
		}
	}
}
