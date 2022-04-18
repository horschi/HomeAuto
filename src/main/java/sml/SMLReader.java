package sml;

import java.util.Calendar;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

	private final Map<String, Context>	meters	= new HashMap<>();
	private String						lastName	= null;

	private static class Context
	{
		int		lastDay	= -999;
		double	lastDayValue	= -1.0;
	}

	public SMLReader(final ValueRegistry registry, final DebugRegistry debugRegistry, final Dictionary props) throws Exception
	{
		this.registry = registry;
		this.debugRegistry = debugRegistry;
		this.props = props;
	}

	public void parseError(final Exception e)
	{
		registry.incCountDebug("Meter " + lastName + " - count parse error");
	}

	public void parseCommands(final List data)
	{
		if (data == null)
			return;

		final Calendar cal = Calendar.getInstance();
		final int curDay = cal.get(Calendar.DAY_OF_WEEK);

		final List<Object> cmds = SMLConverter.convert(data);
		for (final Object cmd : cmds)
		{
			if (cmd instanceof SMLMessageGetListRes)
			{
				final SMLMessageGetListRes cmdGetList = (SMLMessageGetListRes) cmd;
				final byte[] serverId = cmdGetList.getServerId();
				final String serverIdStr = StringUtil.encodeHex(serverId);
				Context ctx = meters.get(serverIdStr);
				if (ctx == null)
				{
					ctx = new Context();
					meters.put(serverIdStr, ctx);
				}
				final String name = Objects.toString(props.get("metername." + serverIdStr), serverIdStr);

				for (final SMLMessageGetListRes.ListEntry entry : cmdGetList.getValList())
				{
					final long id = SMLObis.getId(entry.getObjName());
					this.lastName = name;

					if (id == 0x0100100700ffL)
					{
						registry.setValue("Meter " + name + " - Momentaner Stromverbrauch", entry.getValueScaled(), entry.getValueStr());
					}
					else if (id == 0x0100010800ffL)
					{
						registry.setValue("Meter " + name + " - Zaehlerstand", entry.getValueScaled(), String.format("%.4f", entry.getValueScaled() / 1000) + " kWh");

						if (ctx.lastDay != curDay)
						{
							if (ctx.lastDay >= 0)
							{
								final double dif = entry.getValueScaled() - ctx.lastDayValue;
								registry.setValue("Meter " + name + " - Tagesverbrauch", dif, String.format("%.4f", dif / 1000) + " kWh", true);
							}
							ctx.lastDay = curDay;
							ctx.lastDayValue = entry.getValueScaled();
						}
						else
						{
							final double dif = entry.getValueScaled() - ctx.lastDayValue;
							registry.setValue("Meter " + name + " - Tagesverbrauch Aktuell", dif, String.format("%.4f", dif / 1000) + " kWh", false);
						}
					}
					else
					{
						final String label = SMLObis.getLabel(entry.getObjName());
						// System.out.println("" + label + " " + entry.getValueStr());
						registry.setValueDebug("Meter " + name + " - " + label, entry.getValueStr());
					}
				}
				registry.incCountDebug("Meter " + name + " - count success");
			}
			// System.out.println("" + cmd);
		}
	}
}
