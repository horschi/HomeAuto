package sml.protocol;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class SMLObis
{
	private static final Map<Long, String> labels = new HashMap<Long, String>();
	static
	{
		//
		//
		labels.put(0x0100000009ffL, "ServerID");
		labels.put(0x0100010800ffL, "Wirkenergie Bezug gesamt tariflos");
		labels.put(0x0100010801ffL, "Wirkenergie Bezug Tarif1");
		labels.put(0x0100010802ffL, "Wirkenergie Bezug Tarif2");

		labels.put(0x0100020800ffL, "Wirkenergie Einspeisung gesamt tariflos");
		labels.put(0x0100020801ffL, "Wirkenergie Einspeisung Tarif1");
		labels.put(0x0100020802ffL, "Wirkenergie Einspeisung Tarif2");

		labels.put(0x0100100700ffL, "momentane Gesamtwirkleistung");
		labels.put(0x070100240700ffL, "momentane Wirkleistung in Phase L1");
		labels.put(0x070100380700ffL, "momentane Wirkleistung in Phase L2");
		labels.put(0x0701004c0700ffL, "momentane Wirkleistung in Phase L3");
		labels.put(0x070100020800ffL, "Wirkenergie Einspeisung gesamt tariflos");
		labels.put(0x070100020801ffL, "Wirkenergie Einspeisung Tarif1");
		labels.put(0x070100020802ffL, "Wirkenergie Einspeisung Tarif2");

		labels.put(0x8181c78203ffL, "Hersteller");
		labels.put(0x8181c78205ffL, "publicKey");

		labels.put(0x10060320101L, "Hersteller");
		labels.put(0x100600100FFL, "Device");

		// labels.put(0xL, "");

	}

	public static String getLabel(final Long id)
	{
		if (id == null)
			return null;
		final String ret = labels.get(id);
		if (ret != null)
			return ret;
		return "" + id;
	}

	public static long getId(final byte[] id)
	{
		if (id == null)
			return 0;
		final ByteBuffer buf = ByteBuffer.allocate(8);
		for (int i = 0; i < 8 - id.length; i++)
			buf.put((byte) 0);
		buf.put(id);
		buf.flip();
		return buf.getLong();
	}

	public static String getLabel(final byte[] id)
	{
		return labels.get(getId(id));
	}
}
