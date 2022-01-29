package util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileUtil
{
	public static byte[] loadFile(final String name) throws IOException
	{
		InputStream rin = FileUtil.class.getClassLoader().getResourceAsStream(name);
		if (rin == null)
			rin = FileUtil.class.getResourceAsStream(name);
		if (rin == null)
			rin = FileUtil.class.getClassLoader().getResourceAsStream("classpath:" + name);
		if (rin == null)
			rin = FileUtil.class.getResourceAsStream("classpath:" + name);

		if (rin == null)
		{
			final File f = new File("src/main/resources/" + name);
			if (f.exists())
				rin = new FileInputStream(f);
		}
		if (rin == null)
			return new byte[0];
		try
		{
			final byte[] data = rin.readAllBytes();
			// final byte[] data = IOUtils.resourceToByteArray("style.css");

			return data;
		}
		finally
		{
			rin.close();
		}
	}
}
