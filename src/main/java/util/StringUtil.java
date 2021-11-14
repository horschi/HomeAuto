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
}
