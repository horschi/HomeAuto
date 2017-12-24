import java.io.Writer;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import ebus.EBusData;
import heating.HeatingReader;
import ventilation.VentilationWriter;


public class HomeAutoWebHandler
{
	private String				lastError	= null;
	private VentilationWriter	ventWriter;
	private HeatingReader		heatingReader;

	public HomeAutoWebHandler()
	{
		try
		{
			heatingReader = new HeatingReader();
		}
		catch (final Throwable e)
		{
			heatingReader = null;
			e.printStackTrace();
			lastError = ExceptionUtils.getStackTrace(e);
		}
		try
		{
			ventWriter = new VentilationWriter();
		}
		catch (final Throwable e)
		{
			ventWriter = null;
			e.printStackTrace();
			lastError = ExceptionUtils.getStackTrace(e);
		}
	}

	public boolean handleParams(final Map<String, String> params)
	{
		if (ventWriter != null)
		{
			final String vent = params.get("vent");
			if (vent != null)
			{
				try
				{
					final int val = Integer.parseInt(vent);
					ventWriter.setVent(val);
				}
				catch (final Exception e)
				{
					lastError = "Cannot parse vent value: " + vent;
				}
				return true;
			}
		}
		return false;
	}

	private String buildLink(final String baseURL, final String url, final String text)
	{
		return "<a href=\"" + baseURL + "?" + url + "\">" + text + "</a>";
	}

	public void writeOutput(final String baseURL, final Map<String, String>  params, final Writer writer) throws Exception
	{
		writer.write("<html><title>Home-automation</title><body><b>Welcome to the greatest home-automation!</b><br>");
		final long now = System.currentTimeMillis();
		writer.write("Time: " + new Date(now) + "<br>");

		writer.write("<p>" + buildLink(baseURL, "", "Refresh") + "</p><br>");

		writer.write("<p><b>Ventilation:</b>");
		if (ventWriter == null)
		{
			writer.write("GPIO could not be initialized");
		}
		else
		{
			switch (ventWriter.getVent())
			{
				case 0:
					writer.write("<b>low</b> " + buildLink(baseURL, "vent=1", "med") + " " + buildLink(baseURL, "vent=2", "high"));
					break;
				case 1:
					writer.write("" + buildLink(baseURL, "vent=0", "low") + " <b>med</b> " + buildLink(baseURL, "vent=2", "high"));
					break;
				case 2:
					writer.write("" + buildLink(baseURL, "vent=0", "low") + " " + buildLink(baseURL, "vent=1", "med") + " <b>high</b>");
					break;

				default:
					writer.write("Invalid vent value: " + ventWriter.getVent());
			}
		}
		writer.write("</p>");

		writer.write("<br><p><b>Heating:</b>");
		if (heatingReader == null)
		{
			writer.write("EBus could not be initialized");
		}
		else
		{
			writer.write("<table border=\"1\">");
			for(final Map.Entry<String, HeatingReader.KnownValueEntry> e : heatingReader.getKnownValues().entrySet())
			{
				writer.write("<tr><td>");
				writer.write(e.getKey());
				writer.write("</td><td>");
				writer.write(e.getValue().getValue().toString());
				writer.write("</td><td>");
				final long tdifUpdate = (System.currentTimeMillis() - e.getValue().getTsLastUpdate()) / 1000 / 60;
				if (tdifUpdate > 0)
					writer.write("updated " + (tdifUpdate) + "m ago");
				writer.write("</td><td>");
				if(e.getValue().getTsLastChange() > 0L)
				{
					final long tdifChange = (System.currentTimeMillis() - e.getValue().getTsLastChange()) / 1000 / 60;
					// if (tdifChange > 0)
					writer.write("changed " + (tdifChange) + "m ago");
				}
				writer.write("</td></tr>");
			}
			writer.write("</table>");
			writer.write("</p>");



			final String debugStrParam = params.get("debug");
			final String commandStrParam = params.get("cmd");
			final String filtReqPrefixParam = params.get("filtReqPrefix");
			if ("1".equals(debugStrParam) || StringUtils.isNotBlank(commandStrParam) || StringUtils.isNotBlank(filtReqPrefixParam))
			{
				writer.write("<p><b>Debug:</b><small>");

				final int numParsed = heatingReader.getNumParsed();
				writer.write("<br>Num parsed: " + numParsed + "<br>");
				if (numParsed > 0)
				{
					final int numValid = heatingReader.getNumValid();
					final int numWithMessage = heatingReader.getNumWithMessage();
					writer.write("Num valid: " + numValid + " (" + (numValid * 100 / numParsed) + "%)<br>");
					writer.write("Num with message: " + numWithMessage + " (" + (numWithMessage * 100 / numParsed) + "%)<br>");
				}

				if (StringUtils.isNotBlank(commandStrParam))
				{
					writer.write(buildLink(baseURL, "debug=1", "Return to complete list"));
					writer.write(" ");
					if (StringUtils.isNotBlank(filtReqPrefixParam))
					{
						writer.write(buildLink(baseURL, "cmd=" + commandStrParam, "Return to command list"));
					}
					writer.write("<br>");
				}

				writer.write("</small><table border=\"1\">");
				writer.write("<tr><th>Time");
				writer.write("</th><th>Src");
				writer.write("</th><th>Dst");
				writer.write("</th><th>Cmd");
				writer.write("</th><th>Request");
				writer.write("</th><th>Ack");
				writer.write("</th><th>Response");
				writer.write("</th><th>Ack");
				writer.write("</th><th>Error");
				writer.write("</th></tr>");

				final Set<String> cmdsInList = new HashSet<>();
				for (final EBusData eb : heatingReader.getData(commandStrParam))
				{
					final String reqPrefix = eb.getRequestStrPrefix();
					if (StringUtils.isNotBlank(filtReqPrefixParam) && !filtReqPrefixParam.equals(reqPrefix))
						continue;

					cmdsInList.add(eb.getCmdStr());
					writer.write("<tr><td>");
					writer.write("" + ((eb.getTimestamp() - now) / 1000L) + "s");
					writer.write("</td><td>");
					writer.write(Integer.toString(eb.getSrcAddr(), 16));
					writer.write("</td><td>");
					writer.write(Integer.toString(eb.getDstAddr(), 16));
					writer.write("</td><td>");
					writer.write(buildLink(baseURL, "cmd=" + eb.getCmdStr(), eb.getCmdStr()));
					writer.write("</td><td>");
					writer.write(buildLink(baseURL, "cmd=" + eb.getCmdStr() + "&filtReqPrefix=" + reqPrefix, "#"));
					writer.write(" ");
					writer.write(formatHex(eb.getRequestStr()));
					writer.write("</td><td>");
					writer.write(Integer.toString(eb.getAckReq(), 16));
					writer.write("</td><td>");
					writer.write(formatHex(eb.getResponseStr()));
					writer.write("</td><td>");
					writer.write(Integer.toString(eb.getAckResp(), 16));

					if (eb.getMessage() != null)
					{
						writer.write("</td><td><small>");
						writer.write(eb.getMessage());
					}
					writer.write("</small></td></tr>");
				}
				writer.write("</table></p>");

				writer.write("<p>Command not in list: ");
				for (final String cmd : heatingReader.getIndexKeys(cmdsInList))
				{
					writer.write(buildLink(baseURL, "cmd=" + cmd, cmd));
					writer.write(" ");
				}
				writer.write("</table>");
				writer.write("</p>");

			}
			else
			{
				writer.write("<p>" + buildLink(baseURL, "debug=1", "Show debug") + "</p><br>");
			}

		}

		if (lastError != null)
		{
			writer.write("<p>Last error: <small>" + StringEscapeUtils.escapeHtml4(lastError).replace("\n", "<br>") + "</small></p>");
		}

		writer.write("</body></html>");
	}

	private static String formatHex(final String in)
	{
		final int s = in.length();
		final StringBuilder ret = new StringBuilder();
		for(int i=0;i<s;i+=4)
		{
			final String qstr = StringUtils.substring(in, i, i + 4);
			final int tt = Integer.parseInt(StringUtils.substring(in, i + 2, i + 4) + StringUtils.substring(in, i, i + 2), 16);
			ret.append(" <span title=\"equals to: int=" + tt + " or x/256=" + (((float) tt) / 256) + "\">").append(qstr).append("</span>");
		}
		return ret.toString();
	}
}
