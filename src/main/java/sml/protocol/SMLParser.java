package sml.protocol;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.codec.binary.Hex;

public class SMLParser
{

	public static List readPacket(final DataInputStream in) throws IOException
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

	public static Object readMessage(final int cmd, final DataInputStream in) throws IOException
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
					return in.readBoolean();

				// signed int
				case 0x52:
					return in.readByte();
				case 0x53:
					return in.readShort();
				case 0x56:
					in.read(); // drop first byte - TODO: calculate 5 byte integer
				case 0x55:
					return in.readInt();
				case 0x59:
					return in.readLong();

				// unsigned int
				case 0x62:
					return in.readUnsignedByte();
				case 0x64:
					in.read(); // drop first byte - TODO: calculate 3 byte integer
				case 0x63:
					return in.readUnsignedShort();
				case 0x65:
					return Integer.toUnsignedLong(in.readInt());
				case 0x69:
					return in.readLong();
			}

			// read variable length types
			int type = (cmd >>> 4) & 0x0f;
			int lenRaw = (cmd) & 0x0f;

			if ((cmd & 0x80) > 0)
			{ // multi byte
				final int cmd2 = in.read();
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
					return in.readNBytes(len);
				}

				case 7: // list
				{
					final List ret = new ArrayList(lenRaw);
					for (int i = 0; i < lenRaw; i++)
					{
						final int subcmd = in.read();
						final Object r = readMessage(subcmd, in);
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
			throw new RuntimeException("Error parsing command: " + String.format("0x%02x", cmd) + " /  bytes available: " + in.available(), e);
		}
	}

	public static void readAndAssert(final InputStream in, final byte[] expected, final String type) throws IOException
	{
		final byte[] actual = in.readNBytes(expected.length);
		if (!Arrays.equals(actual, expected))
			throw new IllegalStateException("Read invalid data for " + type + ": " + Hex.encodeHexString(actual));
	}
}
