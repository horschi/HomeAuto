package sml.protocol;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.codec.binary.Hex;

import util.CRC16X25InputStream;
import util.LoggingInputStream;

public class SMLParser
{
	public static List readPacket(final InputStream in) throws IOException
	{
		readAndAssert(in, new byte[] { 0x1b, 0x1b, 0x1b, 0x1b, 0x01, 0x01, 0x01, 0x01 }, "MessageStart");
		final List ret = new ArrayList();
		while (true)
		{
			final int cmd = in.read();
			if (cmd == 0x1b)
				break;
			ret.add(readMessage(cmd, in));
		}
		readAndAssert(in, new byte[] { 0x1b, 0x1b, 0x1b, 0x1a }, "MessageEnd");
		final byte[] end = in.readNBytes(3);
		return ret;
	}

	public static Object readMessage(final int cmd, final InputStream inRaw) throws IOException
	{
		final LoggingInputStream pin = new LoggingInputStream(inRaw);
		final CRC16X25InputStream cin = new CRC16X25InputStream(pin);
		cin.pushByte(cmd);

		try (final DataInputStream din = new DataInputStream(cin);)
		{
			final int type = (cmd >>> 4) & 0x0f;
			if (type == 0)
				return null;
			final int lenRaw = (cmd) & 0x0f;
			if (type != 7)
				throw new IllegalStateException("Invalid data type for message: " + type);

			final List ret = new ArrayList(lenRaw);
			int lastCalcCrc = -1;
			for (int i = 1; i < lenRaw; i++)
			{
				lastCalcCrc = cin.getCrc();
				final int subcmd = din.read();
				final Object r = readEntry(subcmd, din);
				ret.add(r);
			}

			final int subcmdEnd = din.read();
			if (subcmdEnd != 0)
			{
				final Object r = readEntry(subcmdEnd, din);
				ret.add(r);
				// System.out.println("Invalid end command of message: " + subcmdEnd);
				// throw new IllegalArgumentException("Invalid end command of message: " + subcmdEnd);
			}
			// System.out.println("Message: " + Hex.encodeHexString(pin.getBytes()));

			final Object lastEntryCrc = ret.get(ret.size() - 1);
			if (lastEntryCrc instanceof Number)
			{
				int lastEntryCrcFound = ((Number) lastEntryCrc).intValue();
				lastEntryCrcFound = ((lastEntryCrcFound >> 8) & 0xff) | ((lastEntryCrcFound & 0xff) << 8); // SML hash is not in network byte order. Wtf is wrong with BSI?
				if (lastEntryCrcFound != lastCalcCrc)
				{
					System.out.println("SML wrong CRC: " + String.format("%04x != %04x  ", lastEntryCrcFound, lastCalcCrc));
					return null;
				}
			}

			return ret;
		}
	}

	public static Object readEntry(final int cmd, final DataInputStream din) throws IOException
	{
		try
		{
			// read fixed length types
			switch (cmd)
			{
				// end
				case 0x00:
					return null;

				// null
				case 0x01:
					return null;

				// bool
				case 0x42:
					return din.readBoolean();

				// signed int
				case 0x52:
					return din.readByte();
				case 0x53:
					return din.readShort();
				case 0x56:
					din.read(); // drop first byte - TODO: calculate 5 byte integer
				case 0x55:
					return din.readInt();
				case 0x59:
					return din.readLong();

				// unsigned int
				case 0x62:
					return din.readUnsignedByte();
				case 0x64:
					din.read(); // drop first byte - TODO: calculate 3 byte integer
				case 0x63:
					return din.readUnsignedShort();
				case 0x65:
					return Integer.toUnsignedLong(din.readInt());
				case 0x69:
					return din.readLong();
			}

			// read variable length types
			int type = (cmd >>> 4) & 0x0f;
			int lenRaw = (cmd) & 0x0f;

			if ((cmd & 0x80) > 0)
			{ // multi byte
				final int cmd2 = din.read();
				lenRaw = (lenRaw << 4) | (cmd2 & 0x0f);
				type &= 0x07;
				// System.out.println("" + String.format("%02x %02x", cmd, cmd2));
			}

			switch (type)
			{
				case 0: // octet
				{
					final int len = lenRaw - 1;
					if (len < 0)
						throw new IllegalStateException("Invalid length " + lenRaw + " for octet command: " + cmd);
					return din.readNBytes(len);
				}

				case 7: // list
				{
					final List ret = new ArrayList(lenRaw);
					for (int i = 0; i < lenRaw; i++)
					{
						final int subcmd = din.read();
						final Object r = readEntry(subcmd, din);
						ret.add(r);
					}
					return ret;
				}

				case 5: // signed int
				case 6: // unsigned int
				case 4: // bool
					throw new IllegalStateException("Invalid length " + lenRaw + " for command: " + cmd);

				default:
					throw new IllegalStateException("Unknown command: " + cmd);
			}
		}
		catch (final Exception e)
		{
			throw new RuntimeException("Error parsing command: " + String.format("0x%02x", cmd) + " /  bytes available: " + din.available(), e);
		}
	}

	public static void readAndAssert(final InputStream in, final byte[] expected, final String type) throws IOException
	{
		final byte[] actual = in.readNBytes(expected.length);
		if (!Arrays.equals(actual, expected))
			throw new IllegalStateException("Read invalid data for " + type + ": " + Hex.encodeHexString(actual));
	}
}
