package sml;

import java.util.List;

import data.DebugRegistry;
import data.ValueRegistry;
import sml.messages.SMLMessageGetListRes;
import sml.protocol.SMLConverter;
import sml.protocol.SMLObis;
import util.StringUtil;

public class SMLReader
{
	private final ValueRegistry	registry;
	private final DebugRegistry	debugRegistry;

	public SMLReader(final ValueRegistry registry, final DebugRegistry debugRegistry) throws Exception
	{
		this.registry = registry;
		this.debugRegistry = debugRegistry;
	}

	public void parseCommands(final List data)
	{
		final List<Object> cmds = SMLConverter.convert(data);
		for (final Object cmd : cmds)
		{
			if (cmd instanceof SMLMessageGetListRes)
			{
				final SMLMessageGetListRes cmdGetList = (SMLMessageGetListRes) cmd;
				final byte[] serverId = cmdGetList.getServerId();
				final String serverIdStr = StringUtil.encodeHex(serverId);
				for (final SMLMessageGetListRes.ListEntry entry : cmdGetList.getValList())
				{
					final long id = SMLObis.getId(entry.getObjName());
					if (id == 0x0100100700ffL)
					{
						registry.setValue("Meter " + serverIdStr + " - Momentaner Stromverbrauch", entry.getValueStr());
					}
					else if (id == 0x0100010800ffL)
					{
						registry.setValue("Meter " + serverIdStr + " - Zaehlerstand", entry.getValueStr());
					}
					else
					{
						final String label = SMLObis.getLabel(entry.getObjName());
						// System.out.println("" + label + " " + entry.getValueStr());
					}
				}
			}
			// System.out.println("" + cmd);
		}
	}
}
