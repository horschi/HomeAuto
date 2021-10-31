public class TestMain
{

	public static void main(final String[] args)
	{
		// final String dateStr = String.format("%02X:%02X:%02X ", 1, 2, 3) + String.format("%02X.%02X.20%02X ", 4, 5, 6);

		System.out.println(" bcd = " + String.format("%04X", encode2BCD(175)));
	}

	public static int encode1BCD(final int data)
	{
		return (byte) ((byte) ((byte) (data / 10) << 4) | data % 10);
	}

	public static int encode2BCD(final int data)
	{
		final int d1 = data % 10;
		final int d2 = ((data / 10) % 10) << 4;
		final int d3 = ((data / 100) % 10) << 8;
		final int d4 = ((data / 1000) % 10) << 12;

		return d1 | d2 | d3 | d4;
	}

}
