package sml.messages;

import java.util.List;

import sml.SMLMessage;
import sml.SMLUtil;
import util.StringUtil;

public class SMLMessagePublicOpenRes
{

	private final byte[]	codepage;	// Octet String OPTIONAL,
	private final byte[]	clientId;	// Octet String OPTIONAL,
	private final byte[]	reqFileId;	// Octet String,
	private final byte[]	serverId;	// Octet String,
	private final Long		refTime;	// SML_Time OPTIONAL,
	private final Integer	smlVersion;	// Unsigned8 OPTIONAL

	public SMLMessagePublicOpenRes(final SMLMessage msg)
	{
		final List data = msg.getCmdData();
				
		codepage = SMLUtil.convertBytes(data.get(0));
		clientId  = SMLUtil.convertBytes(data.get(1));
		reqFileId  = SMLUtil.convertBytes(data.get(2));
		serverId  = SMLUtil.convertBytes(data.get(3));
		refTime = 0L;// data.get(4);
		smlVersion = SMLUtil.convertInt(data.get(5));
	}

	@Override
	public String toString()
	{
		return "SMLMessagePublicOpenRes [codepage=" + StringUtil.encodeHex(codepage) + ", clientId=" + StringUtil.encodeHex(clientId) + ", reqFileId=" + StringUtil.encodeHex(reqFileId) + ", serverId=" + StringUtil.encodeHex(serverId) + ", refTime=" + refTime
				+ ", smlVersion=" + smlVersion + "]";
	}
}
