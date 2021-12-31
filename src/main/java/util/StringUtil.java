package util;

import java.util.Collection;

import org.apache.commons.codec.binary.Hex;

public class StringUtil
{
	public static String encodeHex(final byte[] in)
	{
		if (in == null)
			return "";
		return Hex.encodeHexString(in);
	}

	public static String toString(final Collection col)
	{
		if (col == null)
			return null;
		final StringBuilder ret = new StringBuilder();
		for (final Object ent : col)
		{
			ret.append(ent).append("\n");
		}
		return ret.toString();
	}

	public static String encodeTimeDif(long tdif)
	{
		if (tdif < 1000)
			return "" + tdif + " ms";

		tdif = tdif / 1000;
		if (tdif < 120)
			return "" + tdif + " s";

		tdif = tdif / 60;
		if (tdif < 120)
			return "" + tdif + " m";

		tdif = tdif / 60;
		if (tdif < 48)
			return "" + tdif + " h";

		tdif = tdif / 24;
		return "" + tdif + " d";
	}
}
