package ebus.conn;

import java.io.InputStream;

public interface AbstractSerialConnection
{
	public void init(String portName, int baudRate) throws Exception;

	public InputStream getInputStream() throws Exception;

	public void close();
}
