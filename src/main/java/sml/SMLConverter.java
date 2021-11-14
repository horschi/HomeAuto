package sml;

import java.util.ArrayList;
import java.util.List;

import sml.messages.SMLMessageGetListRes;
import sml.messages.SMLMessagePublicOpenRes;

public class SMLConverter
{
	public static List<Object> convert(final Object in)
	{
		if (!(in instanceof List))
			throw new IllegalArgumentException();

		final List inlist = (List) in;
		final List<Object> ret = new ArrayList<Object>();
		for (final Object obj : inlist)
		{
			if (obj == null)
				continue;
			final SMLMessage msg = new SMLMessage((List) obj);

			switch (msg.getCmdType())
			{
				case 0x0101:
					ret.add(new SMLMessagePublicOpenRes(msg));
					break;

				case 0x0701:
					ret.add(new SMLMessageGetListRes(msg));
					break;

				default:
					System.out.println("Unknown command: " + String.format("0x%04x", msg.getCmdType()));
					break;
			}

		}
		return ret;
	}
}
