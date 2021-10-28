package ebus.conn;

import java.io.InputStream;

public interface AbstractConnection
{
	public void init() throws Exception;

	public InputStream getInputStream() throws Exception;

	public void close();
}
