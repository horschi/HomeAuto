package sml;

import java.util.Dictionary;
import java.util.List;
import java.util.Objects;

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
	private final Dictionary	props;

	public SMLReader(final ValueRegistry registry, final DebugRegistry debugRegistry, final Dictionary props) throws Exception
	{
		this.registry = registry;
		this.debugRegistry = debugRegistry;
		this.props = props;
	}

	public void parseCommands(final List data)
	{
		if (data == null)
			return;

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
					final String name = Objects.toString(props.get("metername." + serverIdStr), serverIdStr);

					if (id == 0x0100100700ffL)
					{
						registry.setValue("Meter " + name + " - Momentaner Stromverbrauch", entry.getValueScaled(), entry.getValueStr());
					}
					else if (id == 0x0100010800ffL)
					{
						registry.setValue("Meter " + name + " - Zaehlerstand", entry.getValueScaled(), entry.getValueStr());
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
