package ebus;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.apache.commons.codec.binary.Hex;

public class EBusData
{
	private int		srcAddr	= -1;
	private int		dstAddr	= -1;
	private int		cmdPri	= -1;
	private int		cmdSec	= -1;
	private byte[]	request;
	private byte[]	response;
	private int		ackReq	= -1;
	private int		ackResp	= -1;

	private String	message;
	private boolean	isValid	= false;
	private long timestamp;

	public EBusData(InputStream in) throws IOException
	{
		int numSyns = -1;
		do
		{
			srcAddr = readByte(in);
			numSyns++;
		}
		while (srcAddr == 0xAA);

		timestamp = System.currentTimeMillis();
		
		dstAddr = readByte(in);
		if (dstAddr == 0xAA)
		{ // invalid, error!
			message = "destination = AA";
			isValid = false;
			return;
		}

		cmdPri = readByte(in);
		cmdSec = readByte(in);
		if (cmdPri == 0xAA || cmdSec == 0xAA)
		{ // invalid, error!
			message = "cmd = AA";
			isValid = false;
			return;
		}
		

		int sizeReq = readByte(in);
		if(sizeReq > 16)
		{ // invalid, error!
			message = "sizeReq = "+sizeReq;
			isValid = false;
			return;
		}
		request = readArray(in, sizeReq);
		int crcReq = readByte(in);

		if (dstAddr != 0xfe) // only if not a broadcast
		{
			ackReq = readByte(in); // always 00 ?
			if (ackReq != 0x00)
			{ // error
				message = "request ack = "+ackReq;
				isValid = false;
				return;
			}

			int sizeResp = readByte(in);
			if (sizeResp == 0xAA)
			{ // no reponse
				message = "response size = AA";
				isValid = true;
				return;
			}
			if(sizeResp > 16)
			{ // invalid, error!
				message = "sizeResp = "+sizeResp;
				isValid = false;
				return;
			}
			response = readArray(in, sizeResp);
			int crcResp = readByte(in);
			ackResp = readByte(in); // always 00 ?
			if(ackResp != 0x00)
			{
				message = "response ack = "+ackResp;
				isValid = false;
				return;
			}
		}

		int syn = readByte(in); // must be aa
		if (syn == 0xAA)
		{
			isValid = true;
		}
		else
		{ // error
			isValid = false;
			message = ("Packet did not end with 0xAA, but with " + syn + " (it started with " + numSyns + " syns. Next 32 bytes are: " + Hex.encodeHexString(readArray(in, 16)) + ")");
		}
	}

	private static int readByte(InputStream in) throws IOException
	{
		int r = in.read();
		if (r < 0)
			throw new IOException("Read EOF from stream: " + r);
		return r;
	}

	private static byte[] readArray(InputStream in, int len) throws IOException
	{
		byte[] ret = new byte[len];
		for (int i = 0; i < len; i++)
		{
			int b = in.read();
			if (b < 0)
				throw new IOException("Read EOF from stream at " + i + " of " + len);
			ret[i] = (byte) (b);
		}
		// int numread = in.read(ret);
		// if(numread < len)
		// throw new IOException("Read EOF from stream: "+numread+" (expected "+len+")");
		return ret;
	}

	public long getTimestamp()
	{
		return timestamp;
	}

	public int getSrcAddr()
	{
		return srcAddr;
	}

	public int getDstAddr()
	{
		return dstAddr;
	}

	public int getCmdPri()
	{
		return cmdPri;
	}

	public int getCmdSec()
	{
		return cmdSec;
	}
	
	public String getCmdStr()
	{
		return Hex.encodeHexString(new byte[] {(byte)getCmdPri(), (byte)getCmdSec()});
	}

	public byte[] getRequest()
	{
		return request;
	}

	public byte[] getResponse()
	{
		return response;
	}

	public String getRequestStr()
	{
		if (request == null)
			return "";
		return Hex.encodeHexString(request);
	}
	
	public String getRequestStrPrefix()
	{
		String ret = getRequestStr();
		if(ret.length() <= 4)
			return ret;
		else
			return ret.substring(0, 4);
	}

	public String getResponseStr()
	{
		if (response == null)
			return "";
		return Hex.encodeHexString(response);
	}

	public int getAckReq()
	{
		return ackReq;
	}

	public int getAckResp()
	{
		return ackResp;
	}

	public String getMessage()
	{
		return message;
	}
	
	public boolean isValid()
	{
		return isValid;
	}

	public int getData1bi(boolean fromRequest, int idx)
	{
		byte[] arr = fromRequest ? request:response;
		int a = ((int)(arr[idx]))&0xff;
		return a;
	}
	
	public float getData1bf(boolean fromRequest, int idx, int div)
	{
		float v = getData1bi(fromRequest, idx);
		return v / div;
	}
	
	
	public int getData2bi(boolean fromRequest, int idx)
	{
		byte[] arr = fromRequest ? request : response;
		try
		{
			int a = ((int) (arr[idx])) & 0xff;
			int b = ((int) (arr[idx + 1])) ; // & 0xff
			return a | b << 8;
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("arr=" + Hex.encodeHexString(arr), e);
		}
	}
	
	public float getData2bf(boolean fromRequest, int idx, int div)
	{
		float v = getData2bi(fromRequest, idx);
		return v / div;
	}
	
	@Override
	public String toString()
	{
		StringBuilder ret = new StringBuilder();

		// "[src=" + srcAddr + ", dst=" + dstAddr + ", cmdPri=" + cmdPri + ", cmdSec=" + cmdSec + ", req=" + Hex.encodeHexString(request) + ", resp=" + Hex.encodeHexString(response) + "]";
		ret.append("[src=").append(srcAddr);
		ret.append(", dst=").append(dstAddr);
		ret.append(", cmdPri=").append(cmdPri);
		ret.append(", cmdSec=").append(cmdSec);

		if (request != null)
			ret.append(", req=").append(Hex.encodeHexString(request));

		if (response != null)
			ret.append(", resp=").append(Hex.encodeHexString(response));

		ret.append("]");
		return ret.toString();
	}

}
