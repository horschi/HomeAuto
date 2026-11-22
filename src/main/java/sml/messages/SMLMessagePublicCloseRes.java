package sml.messages;

import java.util.List;

import sml.protocol.SMLMessage;

public class SMLMessagePublicCloseRes
{
	private final Object globalSignature;

	public SMLMessagePublicCloseRes(final SMLMessage msg)
	{
		final List data = msg.getCmdData();

		globalSignature = (data.get(0));
	}

	@Override
	public String toString()
	{
		return "SMLMessagePublicCloseRes [globalSignature=" + (globalSignature) + "]";
	}
}
