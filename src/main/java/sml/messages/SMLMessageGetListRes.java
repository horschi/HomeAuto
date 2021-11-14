package sml.messages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import sml.protocol.SMLMessage;
import sml.protocol.SMLObis;
import sml.protocol.SMLUtil;
import util.StringUtil;

public class SMLMessageGetListRes
{
	private final byte[]			clientId;		// Octet String OPTIONAL,
	private final byte[]			serverId;		// Octet String,
	private final byte[]			listName;		// Octet String OPTIONAL,
	private final Long				actSensorTime;	// SML_Time OPTIONAL,
	private final List<ListEntry>	valList;		// SML_List,
	private final byte[]			listSignature;	// SML_Signature OPTIONAL,
	private final Long				actGatewayTime;	// SML_Time OPTIONAL

	public SMLMessageGetListRes(final SMLMessage msg)
	{
		final List data = msg.getCmdData();

		clientId = SMLUtil.convertBytes(data.get(0));
		serverId = SMLUtil.convertBytes(data.get(1));
		listName = SMLUtil.convertBytes(data.get(2));
		actSensorTime = SMLUtil.convertTime(data.get(3)); // [1, 213259145] - uptime?
		valList = new ArrayList<>();
		for (final Object valEnt : SMLUtil.convertList(data.get(4)))
		{
			valList.add(new ListEntry((List) valEnt));
		}
		listSignature = SMLUtil.convertBytes(data.get(5));
		actGatewayTime = SMLUtil.convertTime(data.get(6)); // 14871
	}

	public byte[] getClientId()
	{
		return clientId;
	}

	public byte[] getServerId()
	{
		return serverId;
	}

	public byte[] getListName()
	{
		return listName;
	}

	public Long getActSensorTime()
	{
		return actSensorTime;
	}

	public List<ListEntry> getValList()
	{
		return valList;
	}

	public byte[] getListSignature()
	{
		return listSignature;
	}

	public Long getActGatewayTime()
	{
		return actGatewayTime;
	}

	@Override
	public String toString()
	{
		return "SMLMessageGetListRes\n[\nclientId=" + Arrays.toString(clientId) + //
				", \nserverId=" + StringUtil.encodeHex(serverId) + //
				", \nlistName=" + StringUtil.encodeHex(listName) + //
				", \nactSensorTime=" + actSensorTime + //
				", \nvalList=\n" + StringUtil.toString(valList) + //
				", \nlistSignature=" + StringUtil.encodeHex(listSignature) + //
				", \nactGatewayTime=" + actGatewayTime + "\n]";
	}

	public static class ListEntry
	{
		private final byte[]	objName;		// Octet String,
		private final Integer	status;			// SML_Status OPTIONAL,
		private final Long		valTime;		// SML_Time OPTIONAL,
		private final Integer	unit;			// SML_Unit OPTIONAL,
		private final int		scaler;			// *10^scaler Integer8 OPTIONAL,
		private final Object	value;			// SML_Value,
		private final byte[]	valueSignature;	// SML_Signature OPTIONAL

		public ListEntry(final List data)
		{
			objName = SMLUtil.convertBytes(data.get(0));
			status = SMLUtil.convertInt(data.get(1));
			valTime = SMLUtil.convertTime(data.get(2));
			unit = SMLUtil.convertInt(data.get(3));
			scaler = SMLUtil.convertInt(data.get(4), 0);
			value = data.get(5);
			valueSignature = SMLUtil.convertBytes(data.get(6));
		}

		public byte[] getObjName()
		{
			return objName;
		}

		public Integer getStatus()
		{
			return status;
		}

		public Long getValTime()
		{
			return valTime;
		}

		public Integer getUnit()
		{
			return unit;
		}

		public int getScaler()
		{
			return scaler;
		}

		public Object getValue()
		{
			return value;
		}

		public Object getValueStr()
		{
			final String unitLbl;
			if (unit == null)
				unitLbl = null;
			else
			{
				switch (unit)
				{
					case 27:
						unitLbl = "W";
						break;
					case 30:
						unitLbl = "Wh";
						break;

					default:
						unitLbl = null;
						break;
				}
			}
			if (unitLbl != null)
			{
				final double bv = ((Number) value).doubleValue();
				final double sv = Math.pow(10, scaler);
				final double rv = bv * sv;
				return String.format("%.4f", rv);
			}
			if (value instanceof byte[])
				return StringUtil.encodeHex((byte[]) value);
			return value;
		}

		public byte[] getValueSignature()
		{
			return valueSignature;
		}

		@Override
		public String toString()
		{
			return "ListEntry [objName=" + StringUtil.encodeHex(objName) + "(" + SMLObis.getLabel(objName) + ")" + ", status=" + status + ", valTime=" + valTime + ", unit=" + unit + ", scaler=" + scaler + ", value=" + Objects.toString(value)
					+ ", valueSignature=" + StringUtil.encodeHex(valueSignature) + "]";
		}
	}
}
