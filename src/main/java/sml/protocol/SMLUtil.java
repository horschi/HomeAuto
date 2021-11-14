package sml.protocol;

import java.util.List;

public class SMLUtil
{
	public static List convertList(final Object in)
	{
		return (List) in;
	}

	public static byte[] convertBytes(final Object in)
	{
		return (byte[]) in;
	}

	public static Integer convertInt(final Object in)
	{
		if (in == null)
			return null;
		return ((Number) in).intValue();
	}

	public static Integer convertInt(final Object in, final int defaultValue)
	{
		if (in == null)
			return defaultValue;
		return ((Number) in).intValue();
	}

	public static Long convertLong(final Object in)
	{
		if (in == null)
			return null;
		return ((Number) in).longValue();
	}

	public static Long convertTime(final Object in)
	{
		if (in == null)
			return null;
		if (in instanceof Number)
			return ((Number) in).longValue() * 1000L;

		final List l = (List)in;
		
		final int secIndex = convertInt(l.get(0));
		final Long timestamp = convertLong(l.get(1));
		// SML_TimestampLocal localTimestamp = l.get(2);
			
		return timestamp.longValue() * 1000L;
	}
}
