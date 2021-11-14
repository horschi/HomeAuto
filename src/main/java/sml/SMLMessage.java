package sml;

import java.util.List;

public class SMLMessage
{
	private final byte[]	transactionId;
	private final int		groupNo;
	private final int		abortOnError;

	private final int		cmdType;
	private final List		cmdData;

	public SMLMessage(final List data)
	{
		this.transactionId = SMLUtil.convertBytes(data.get(0));
		this.groupNo = SMLUtil.convertInt(data.get(1));
		this.abortOnError = SMLUtil.convertInt(data.get(2));
		final List subdata = SMLUtil.convertList(data.get(3));

		this.cmdType = SMLUtil.convertInt(subdata.get(0));
		this.cmdData = SMLUtil.convertList(subdata.get(1));

		final Integer crc = SMLUtil.convertInt(data.get(4));
	}

	public byte[] getTransactionId()
	{
		return transactionId;
	}

	public int getGroupNo()
	{
		return groupNo;
	}

	public int getAbortOnError()
	{
		return abortOnError;
	}

	public int getCmdType()
	{
		return cmdType;
	}

	public List getCmdData()
	{
		return cmdData;
	}
}
