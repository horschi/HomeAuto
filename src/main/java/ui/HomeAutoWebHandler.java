package ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.Writer;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import data.DebugRegistry;
import data.KnownValueEntry;
import data.KnownValueQueueEntry;
import data.ValueRegistry;
import ebus.protocol.EBusData;
import init.HomeUIAccess;
import util.StringUtil;

public class HomeAutoWebHandler
{
	private String						lastError					= null;
	private final ValueRegistry			valueRegistry;
	private final DebugRegistry			debugRegistry;
	private final HomeUIAccess			uiAccess;

	private ScheduledExecutorService	scheduledExecutorService	= Executors.newScheduledThreadPool(1);
	private final List<ScheduleEntry>	schedules					= new ArrayList<>();
	private final SimpleDateFormat		timeParser					= new SimpleDateFormat("HH:mm");
	private final SimpleDateFormat		dateTimeParser				= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public HomeAutoWebHandler(final ValueRegistry valueRegistry, final DebugRegistry debugRegistry, final HomeUIAccess uiAccess)
	{
		this.valueRegistry = valueRegistry;
		this.debugRegistry = debugRegistry;
		this.lastError = null;
		this.uiAccess = uiAccess;
		loadSchedules();
		setupSchedules();
	}

	private void loadSchedules()
	{
		try
		{
			final Properties props = new Properties();
			final File configFile = new File("schedules.properties");
			if (configFile.exists())
				props.load(new FileInputStream(configFile));
			for (int i = 0;; i++)
			{
				final String spdStr = props.getProperty("sched_spd_" + i);
				final String timeStr = props.getProperty("sched_time_" + i);
				if (spdStr == null || timeStr == null || spdStr.isBlank() || timeStr.isBlank())
					break;

				final ScheduleEntry sched = new ScheduleEntry();
				sched.time = new Date(Long.parseLong(timeStr));
				sched.speed = Integer.parseInt(spdStr);
				schedules.add(sched);
			}
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
	}

	private void saveSchedules()
	{
		try
		{
			final File configFile = new File("schedules.properties");
			final Properties props = new Properties();
			int i = 0;
			for (final ScheduleEntry sched : schedules)
			{
				props.setProperty("sched_spd_" + i, Integer.toString(sched.speed));
				props.setProperty("sched_time_" + i, Long.toString(sched.time.getTime()));
				i++;
			}
			try (FileOutputStream fout = new FileOutputStream(configFile))
			{
				props.store(fout, "");
			}
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
	}

	public boolean handleParams(final Map<String, String> params)
	{
		// shelly
		if (uiAccess.hasShelly())
		{
			final String vent = params.get("shellyagg");
			if (vent != null)
			{
				try
				{
					final int val = Integer.parseInt(vent);
					uiAccess.setAggressiveness(val);
				}
				catch (final Exception e)
				{
					lastError = "Cannot parse agg value: " + vent;
				}
				return true;
			}
			final String cont = params.get("shellycont");
			if (cont != null)
			{
				try
				{
					final int val = Integer.parseInt(cont);
					uiAccess.setContinousUpdate(val == 1);
				}
				catch (final Exception e)
				{
					lastError = "Cannot parse cont value: " + vent;
				}
				return true;
			}
		}

		// vent
		if (uiAccess.hasVent())
		{
			final String vent = params.get("vent");
			if (vent != null)
			{
				try
				{
					final int val = Integer.parseInt(vent);
					uiAccess.setVent(val);
				}
				catch (final Exception e)
				{
					lastError = "Cannot parse vent value: " + vent;
				}
				return true;
			}
		}

		// schedules
		final String addsched = params.get("addsched");
		if (addsched != null)
		{
			if ("1".equals(addsched))
			{
				schedules.add(new ScheduleEntry());
			}
			if ("0".equals(addsched))
			{
				schedules.remove(schedules.size() - 1);
				setupSchedules();
				saveSchedules();
			}
		}
		final String setsched = params.get("setsched");
		if (setsched != null)
		{
			try
			{
				if ("1".equals(setsched))
				{
					for (int i = 0; i < schedules.size(); i++)
					{
						final String t = params.get("time" + i);
						final String s = params.get("speed" + i);
						final ScheduleEntry schedEnt = schedules.get(i);
						schedEnt.speed = Integer.parseInt(s);
						schedEnt.time = timeParser.parse(t);
					}
					setupSchedules();
					saveSchedules();
				}
			}
			catch (final Exception e)
			{
				e.printStackTrace();
				lastError = "Error while setting schedules: " + e;
			}
		}
		return false;
	}

	private synchronized void setupSchedules()
	{
		scheduledExecutorService.shutdownNow();
		scheduledExecutorService = Executors.newScheduledThreadPool(1);
		for (int i = 0; i < schedules.size(); i++)
		{
			final ScheduleEntry schedEnt = schedules.get(i);
			final Runnable r = new Runnable()
			{
				@Override
				public void run()
				{
					if (uiAccess.hasVent())
						uiAccess.setVent(schedEnt.speed);
					scheduledExecutorService.schedule(this, calcTimeDif(schedEnt.time), TimeUnit.MILLISECONDS);
				}
			};
			scheduledExecutorService.schedule(r, calcTimeDif(schedEnt.time), TimeUnit.MILLISECONDS);
		}
	}

	private long calcTimeDif(final Date time)
	{
		final Calendar calSched = Calendar.getInstance();
		calSched.setTime(time);

		final Calendar calNext = Calendar.getInstance();
		calNext.set(Calendar.HOUR_OF_DAY, calSched.get(Calendar.HOUR_OF_DAY));
		calNext.set(Calendar.MINUTE, calSched.get(Calendar.MINUTE));
		calNext.set(Calendar.SECOND, 0);
		long tdif = calNext.getTimeInMillis() - System.currentTimeMillis();
		if (tdif <= 1000)
			tdif += 1000L * 60L * 60L * 24L;
		return tdif;
	}

	private String buildLink(final String baseURL, final String url, final String text)
	{
		return "<a href=\"" + baseURL + "?" + url + "\">" + text + "</a>";
	}

	public void writeOutput(final String baseURL, final Map<String, String> params, final Writer writer) throws Exception
	{
		writer.write("<html><title>Home-automation</title><head><link rel=\"stylesheet\" href=\"style.css\"></head><body><br>");
		final long now = System.currentTimeMillis();
		writer.write("Time: " + new Date(now) + "<br>");

		final String deleteKey = params.get("delete");
		if (deleteKey != null && !deleteKey.isBlank())
		{
			valueRegistry.deleteKnownValueObj(deleteKey);
		}

		final String detailKey = params.get("detail");
		if (detailKey != null && !detailKey.isBlank())
		{
			writeOutputDetail(baseURL, params, writer, detailKey);
		}
		else
		{
			writeOutputMain(baseURL, params, writer, now);
		}
	}

	private void writeOutputDetail(final String baseURL, final Map<String, String> params, final Writer writer, final String detailKey) throws Exception
	{
		writer.write("<p>" + buildLink(baseURL, "", "Back") + "</p><br>");

		//
		// Show data
		//
		writer.write("<br><p><b>Data " + StringEscapeUtils.escapeHtml4(detailKey) + ":</b>");
		writer.write("<table border=\"1\">");
		final KnownValueEntry ent = valueRegistry.getKnownValueObj(detailKey);
		for (final Pair<Long, Object> row : ent.getHistoryTexts())
		{
			writer.write("<tr><td>");
			writer.write(dateTimeParser.format(new Date(row.getKey())));
			writer.write("</td><td>");
			writer.write(StringEscapeUtils.escapeHtml4(row.getRight().toString()));
			writer.write("</td></tr>");
		}
		writer.write("</table>");
		writer.write("</p>");

		writer.write(buildLink(baseURL, "delete=" + URLEncoder.encode(detailKey), "Delete"));
	}

	private void writeOutputMain(final String baseURL, final Map<String, String> params, final Writer writer, final long now) throws Exception
	{
		final boolean isDebug = "1".equals(params.get("debug"));

		writer.write("<p>" + buildLink(baseURL, "", "Refresh") + "</p><br>");

		if (!uiAccess.hasVent())
		{
			writer.write("GPIO could not be initialized");
		}
		else
		{
			writer.write("<p><b>Ventilation: </b>");
			switch (uiAccess.getVent())
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
					writer.write("Invalid vent value: " + uiAccess.getVent());
			}
		}
		writer.write("</p>");

		//
		// Ventilation schedule
		//
		writer.write("<p><b>Ventilation schedule: </b>");
		writer.write("<form action=\"" + baseURL + "\"><table border=\"1\">");
		for (int i = 0; i < schedules.size(); i++)
		{
			final ScheduleEntry schedEnt = schedules.get(i);
			writer.write("<tr><td>");
			writer.write("<input type=\"text\" name=\"time" + i + "\" value=\"" + timeParser.format(schedEnt.time) + "\">");
			writer.write("</td><td>");
			writer.write("<select name=\"speed" + i + "\">");
			writer.write("<option value=\"0\"" + (schedEnt.speed == 0 ? "selected=\"selected\"" : "") + ">Low</option>");
			writer.write("<option value=\"1\"" + (schedEnt.speed == 1 ? "selected=\"selected\"" : "") + ">Med</option>");
			writer.write("<option value=\"2\"" + (schedEnt.speed == 2 ? "selected=\"selected\"" : "") + ">High</option>");
			writer.write("</select>");
			writer.write("</td><td>");
			final long tdifmin = calcTimeDif(schedEnt.time) / 60000L;
			writer.write("in " + String.format("%02d:%02d", (int) (tdifmin / 60), (int) (tdifmin % 60)));
			writer.write("</td></tr>");
		}
		writer.write("<input type=\"hidden\" name=\"setsched\" value=\"1\">");
		writer.write("</table><input type=\"submit\" value=\"Set schedules\"></form>");
		writer.write(" " + buildLink(baseURL, "addsched=1", "Add schedule"));
		if (schedules.size() > 0)
			writer.write(" " + buildLink(baseURL, "addsched=0", "Remove schedule"));
		writer.write("</p>");

		//
		// Show shelly agg
		//
		if (!uiAccess.hasShelly())
		{
			writer.write("Shelly could not be initialized");
		}
		else
		{
			writer.write("<br><p><b>Shelly aggressiveness: </b>");
			switch (uiAccess.getAggressiveness())
			{
				case -1:
					writer.write("<b>conservative</b> " + buildLink(baseURL, "shellyagg=0", "balanced") + " " + buildLink(baseURL, "shellyagg=1", "aggressive"));
					break;
				case 0:
					writer.write("" + buildLink(baseURL, "shellyagg=-1", "conservative") + " <b>balanced</b> " + buildLink(baseURL, "shellyagg=1", "aggressive"));
					break;
				case 1:
					writer.write("" + buildLink(baseURL, "shellyagg=-1", "conservative") + " " + buildLink(baseURL, "shellyagg=0", "balanced") + " <b>aggressive</b>");
					break;

				default:
					writer.write("Invalid agg value: " + uiAccess.getVent());
			}

			writer.write("<br><p><b>Shelly continuous: </b>");
			if (uiAccess.isContinousUpdate())
			{
				writer.write("<b>continuous</b> " + buildLink(baseURL, "shellycont=0", "on change only"));
			}
			else
			{
				writer.write("<b>on change only</b> " + buildLink(baseURL, "shellycont=1", "continuous"));
			}
		}
		writer.write("</p>");

		//
		// Show data
		//
		writer.write("<br><p><b>Data:</b>");
		writer.write("<table border=\"1\">");
		for (final Map.Entry<String, KnownValueEntry> e : valueRegistry.getKnownValues().entrySet())
		{
			final KnownValueEntry eval = e.getValue();
			if (eval.isDebug() && !isDebug)
				continue;
			writer.write("<tr><td>");
			writer.write(buildLink(baseURL, "detail=" + URLEncoder.encode(e.getKey()), StringEscapeUtils.escapeHtml4(e.getKey())));
			writer.write("</td><td>");
			writer.write(StringEscapeUtils.escapeHtml4(eval.getText()));
			writer.write("</td><td>");
			final long tdifUpdate = (System.currentTimeMillis() - e.getValue().getTsLastUpdate());
			if (tdifUpdate > 0)
				writer.write("<small>updated " + StringUtil.encodeTimeDif(tdifUpdate) + " ago</small>");
			writer.write("</td><td>");
			if (e.getValue().getTsLastChange() > 0L)
			{
				final long tdifChange = (System.currentTimeMillis() - e.getValue().getTsLastChange());
				// if (tdifChange > 0)
				writer.write("<small>changed " + StringUtil.encodeTimeDif(tdifChange) + " ago</small>");
			}
			writer.write("</td></tr>");
		}
		writer.write("</table>");
		writer.write("</p>");

		if (isDebug)
		{
			final Queue<KnownValueQueueEntry> queue = this.valueRegistry.getQueue();
			writer.write("<br/>Send queue size: " + queue.size() + "<br/>");
			writer.write("<br/>Memory: free=" + (Runtime.getRuntime().freeMemory() >> 20) + "M / max=" + (Runtime.getRuntime().maxMemory() >> 20) + "M / total=" + (Runtime.getRuntime().totalMemory() >> 20) + "M<br/>");
		}

		//
		// Debug
		//
		final String commandStrParam = params.get("cmd");
		final String filtReqPrefixParam = params.get("filtReqPrefix");
		if (isDebug || StringUtils.isNotBlank(commandStrParam) || StringUtils.isNotBlank(filtReqPrefixParam))
		{
			writer.write("<p><b>Debug:</b><small>");

			final int numParsed = debugRegistry.getNumParsed();
			writer.write("<br>Num parsed: " + numParsed + "<br>");
			if (numParsed > 0)
			{
				final int numValid = debugRegistry.getNumValid();
				final int numWithMessage = debugRegistry.getNumWithMessage();
				writer.write("Num valid: " + numValid + " (" + (numValid * 100 / numParsed) + "%)<br>");
				writer.write("Num with message: " + numWithMessage + " (" + (numWithMessage * 100 / numParsed) + "%)<br>");
				writer.write("Num bytes: " + debugRegistry.getNumBytesRead() + "<br>");
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
			for (final EBusData eb : debugRegistry.getData(commandStrParam))
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
			for (final String cmd : debugRegistry.getIndexKeys(cmdsInList))
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

		//
		// Error
		//

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
		for (int i = 0; i < s; i += 4)
		{
			final String qstr = StringUtils.substring(in, i, i + 4);
			final int tt = Integer.parseInt(StringUtils.substring(in, i + 2, i + 4) + StringUtils.substring(in, i, i + 2), 16);
			ret.append(" <span title=\"equals to: int=" + tt + " or x/256=" + (((float) tt) / 256) + "\">").append(qstr).append("</span>");
		}
		return ret.toString();
	}

	private static class ScheduleEntry
	{
		int		speed	= 0;
		Date	time	= new Date(0L);
	}
}
