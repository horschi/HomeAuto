import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.Conversion;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;

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
		catch (Throwable e)
		{
			heatingReader = null;
			e.printStackTrace();
			lastError = ExceptionUtils.getStackTrace(e);
		}
		try
		{
			ventWriter = new VentilationWriter();
		}
		catch (Throwable e)
		{
			ventWriter = null;
			e.printStackTrace();
			lastError = ExceptionUtils.getStackTrace(e);
		}
	}

	public void handleParams(Map<String, String> params)
	{
		if (ventWriter != null)
		{
			String vent = params.get("vent");
			if (vent != null)
			{
				try
				{
					int val = Integer.parseInt(vent);
					ventWriter.setVent(val);
				}
				catch (Exception e)
				{
					lastError = "Cannot parse vent value: " + vent;
				}
			}
		}
	}

	private String buildLink(String baseURL, String url, String text)
	{
		return "<a href=\"" + baseURL + "?" + url + "\">" + text + "</a>";
	}

	public void writeOutput(String baseURL, Map<String, String>  params, Writer writer) throws Exception
	{
		writer.write("<html><title>Home-automation</title><body><b>Welcome to the greatest home-automation!</b><br>");
		long now = System.currentTimeMillis();
		writer.write("Time: " + new Date(now) + "<br>");

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
			for(Map.Entry<String, HeatingReader.KnownValueEntry> e : heatingReader.getKnownValues().entrySet())
			{
				writer.write("<tr><td>");
				writer.write(e.getKey());
				writer.write("</td><td>");
				writer.write(e.getValue().getValue().toString());
				writer.write("</td><td>");
				long tdifUpdate= (System.currentTimeMillis()- e.getValue().getTsLastUpdate())/1000;
				writer.write("updated "+((tdifUpdate)/60)+"m ago");
				writer.write("</td><td>");
				if(e.getValue().getTsLastChange() > 0L)
				{
					long tdifChange = (System.currentTimeMillis()- e.getValue().getTsLastChange())/1000;
					writer.write("changed "+((tdifChange)/60)+"m ago");
				}
				writer.write("</td></tr>");
			}
			writer.write("</table>");

			
			writer.write("</p>");

			writer.write("<p><b>Debug:</b><small>");

			int numParsed = heatingReader.getNumParsed();
			writer.write("<br>Num parsed: "+numParsed+"<br>");
			if(numParsed > 0)
			{
				int numValid = heatingReader.getNumValid();
				int numWithMessage = heatingReader.getNumWithMessage();
				writer.write("Num valid: "+numValid+" ("+(numValid*100/numParsed)+"%)<br>");
				writer.write("Num with message: "+numWithMessage+" ("+(numWithMessage*100/numParsed)+"%)<br>");
			}
			

			String commandStrParam = params.get("cmd");
			String filtReqPrefixParam = params.get("filtReqPrefix");
			if(StringUtils.isNotBlank(commandStrParam))
			{
				writer.write(buildLink(baseURL, "", "Return to complete list"));
				writer.write(" ");
				if(StringUtils.isNotBlank(filtReqPrefixParam))
				{
					writer.write(buildLink(baseURL, "cmd="+commandStrParam, "Return to command list"));
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
			
			for ( EBusData eb : heatingReader.getData(commandStrParam))
			{
				String reqPrefix = eb.getRequestStrPrefix();
				if(StringUtils.isNotBlank(filtReqPrefixParam) && !filtReqPrefixParam.equals(reqPrefix))
					continue;
				
				writer.write("<tr><td>");
				writer.write("" + ((eb.getTimestamp() - now) / 1000L) + "s");
				writer.write("</td><td>");
				writer.write(Integer.toString(eb.getSrcAddr(),16));
				writer.write("</td><td>");
				writer.write(Integer.toString(eb.getDstAddr(),16));
				writer.write("</td><td>");
				writer.write(buildLink(baseURL, "cmd="+eb.getCmdStr(), eb.getCmdStr()));
				writer.write("</td><td>");
				writer.write(buildLink(baseURL, "cmd="+eb.getCmdStr()+"&filtReqPrefix="+reqPrefix, "#"));
				writer.write(" ");
				writer.write(formatHex(eb.getRequestStr()));
				writer.write(" (");
				writer.write(formatText(eb.getRequest()));
				writer.write(")</td><td>");
				writer.write(Integer.toString(eb.getAckReq(),16));
				writer.write("</td><td>");
				writer.write(formatHex( eb.getResponseStr()));
				writer.write(" (");
				writer.write(formatText(eb.getResponse()));
				writer.write(")</td><td>");
				writer.write(Integer.toString(eb.getAckResp(),16));
				
				if(eb.getMessage() != null)
				{
					writer.write("</td><td>");
					writer.write(eb.getMessage());
				}
				writer.write("</td></tr>");
			}
			writer.write("</table>");

		}
		writer.write("</p>");

		if (lastError != null)
		{
			writer.write("<p>Last error: <small>" + StringEscapeUtils.escapeHtml4(lastError).replace("\n", "<br>") + "</small></p>");
		}

		writer.write("</body></html>");
	}

	private static String formatText(byte[] in)
	{
		if(in == null)
			return "";
		int s = in.length;
		StringBuilder ret = new StringBuilder();
		for(int i=0;i<s;i++)
		{
			char c = (char)in[i];
			if(Character.isAlphabetic(c) || Character.isDigit(c) && c < 128)
				ret.append(c);
			else
				ret.append(".");
		}
		return ret.toString();
	}
	
	private static String formatHex(String in)
	{
		int s = in.length();
		StringBuilder ret = new StringBuilder();
		for(int i=0;i<s;i+=4)
		{
			ret.append(" ").append(StringUtils.substring(in, i, i+4));
			
		}
		return ret.toString();
	}
}
