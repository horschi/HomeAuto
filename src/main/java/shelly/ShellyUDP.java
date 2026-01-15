package shelly;

import java.io.Closeable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import data.KnownValueEntry;
import data.ValueListener;
import data.ValueRegistry;

public class ShellyUDP extends Thread implements Closeable, ValueListener
{
	private final DatagramSocket	serverSocket;
	private boolean					closed				= false;

	private final ValueRegistry		valueRegistry;
	// private final ObjectMapper mapper = new ObjectMapper();
	private final String			deviceId;					// shellypro3em-b827eb364242
	private final String			meterValueKey;

	private int						aggressiveness		= 0;
	private boolean					continousUpdate		= false;
	private double					increaseFactor		= 0.1;
	private double					decreaseFactor		= 0.9;
	private double					increaseLimit		= 10.0;
	private double					decreaseLimit		= 400.0;
	private double					offset				= -20;
	private final double			neutralOffset		= 20;

	private int						numCorrDirection	= 0;
	private double					lastCorrection		= 0;
	private long					lastUpdate			= 0;

	public ShellyUDP(final ValueRegistry valueRegistry, final String deviceId, final String meterValueKey) throws Exception
	{
		this.valueRegistry = valueRegistry;
		this.deviceId = deviceId;
		this.meterValueKey = meterValueKey;
		serverSocket = new DatagramSocket(2220);
		System.out.println("Shelly enabled: " + deviceId + ", " + meterValueKey);
		setAggressiveness(0);

		valueRegistry.registerKnownValueListener(meterValueKey, this);
	}

	public void setAggressiveness(final int v)
	{
		switch (v)
		{
			case -1:
				increaseFactor = 0.2;
				decreaseFactor = 0.4;
				increaseLimit = 50.0;
				decreaseLimit = 200.0;
				offset = -15;
				break;
			case 0:
				increaseFactor = 0.4;
				decreaseFactor = 0.4;
				increaseLimit = 100.0;
				decreaseLimit = 150.0;
				offset = -5;
				break;
			case 1:
				increaseFactor = 0.4;
				decreaseFactor = 0.3;
				increaseLimit = 100.0;
				decreaseLimit = 100.0;
				offset = 5;
				break;

			default:
				throw new IllegalArgumentException();
		}
		aggressiveness = v;
	}

	public int getAggressiveness()
	{
		return aggressiveness;
	}

	public boolean isContinousUpdate()
	{
		return continousUpdate;
	}

	public void setContinousUpdate(final boolean continousUpdate)
	{
		this.continousUpdate = continousUpdate;
	}

	private InetAddress	address	= null;
	private int			port	= -1;

	@Override
	public void run()
	{
		System.out.println("Shelly Thread started: " + this);
		try
		{
			final byte[] buf = new byte[8192];
			while (!closed)
			{
				final DatagramPacket packet = new DatagramPacket(buf, buf.length);
				serverSocket.receive(packet);

				address = packet.getAddress();
				port = packet.getPort();

				final String requestBody = new String(buf, 0, packet.getLength());

				valueRegistry.setValueDebug("Shelly UDP last received", System.currentTimeMillis() + " " + address + " " + port + ": " + requestBody);

				// TODO: store final String request_id
			}
			serverSocket.close();
		}
		catch (final Exception e)
		{
			e.printStackTrace();
			valueRegistry.incCountDebug("Shelly receive error: " + e);
			// valueRegistry.setValue("system.shelly.error", "" + e);
		}
	}

	@Override
	public void updatedValue(final String key, final Object oldVal, final Object newVal)
	{
		if (address == null)
			return;
		try
		{
			final String request_id = "1";
			// final byte[] responseBytes = mapper.writeValueAsBytes(createEmGetStatusResponse());
			final String responseString = createEmGetStatusResponseLean(request_id);
			final byte[] responseBytes = responseString.getBytes();

			final DatagramPacket packetResponse = new DatagramPacket(responseBytes, responseBytes.length, address, port);
			serverSocket.send(packetResponse);
			// valueRegistry.setValueDebug("Shelly.UDP.Last.Sent", "" + responseString);
		}
		catch (final Exception e)
		{
			e.printStackTrace();
			valueRegistry.incCountDebug("Shelly send error: " + e);
			// valueRegistry.setValue("system.shelly.error", "" + e);
		}
	}

	private String createEmGetStatusResponseLean(final String request_id)
	{

		final KnownValueEntry valObj = valueRegistry.getKnownValueObj(meterValueKey);
		Double powerByMeter = valObj.getValueDouble();
		if (powerByMeter == null)
			powerByMeter = 0.0;

		final long readingTimestamp = valObj.getTsLastUpdate();

		double totalPower;
		if (readingTimestamp > lastUpdate || continousUpdate)
		{
			// new reading
			powerByMeter += offset;

			if (Math.abs(powerByMeter) < neutralOffset)
			{
				numCorrDirection = 0;
			}
			else if (powerByMeter > 0)
			{
				if (powerByMeter > neutralOffset * 2)
					numCorrDirection = clamp(numCorrDirection + 1, 0, 10);
				else
					numCorrDirection = clamp(numCorrDirection + 1, 0, 5);
			}
			else if (powerByMeter < 0)
			{
				if (powerByMeter < -neutralOffset * 2)
					numCorrDirection = clamp(numCorrDirection - 1, -10, 0);
				else
					numCorrDirection = clamp(numCorrDirection - 1, -5, 0);
			}

			if (powerByMeter > 0)
			{
				totalPower = Math.min(increaseLimit, powerByMeter * increaseFactor);
			}
			else
			{
				totalPower = Math.max(-decreaseLimit, powerByMeter * decreaseFactor);
			}

			final double directionCorrFactor = 1.0 + (Math.abs(numCorrDirection) / 10.0);
			totalPower *= directionCorrFactor;

			final boolean flaky = (lastCorrection < 0) != (totalPower < 0);
			if (flaky)
			{
				numCorrDirection = 0;
				totalPower /= 2;
			}

			lastCorrection = totalPower;
			valueRegistry.setValueDebug("Shelly debug", "powerByMeter=" + String.format("%.1f", valObj.getValueDouble()) + " & corrMeter=" + String.format("%.1f", powerByMeter) + " / out=" + String.format("%.1f", totalPower) + " / flaky=" + flaky
					+ " / numCorrDirection=" + numCorrDirection + " / aggressiveness=" + aggressiveness);
			valueRegistry.setValue("Shelly reported", totalPower, null, false);
		}
		else
		{
			// no new reading
			totalPower = 0.0;
			valueRegistry.setValueDebug("Shelly debug", "-");
		}

		lastUpdate = readingTimestamp;

		final double phasePower1 = totalPower / 3;
		final double phasePowerRest = phasePower1;

		final StringBuilder ret = new StringBuilder();

		ret.append("{");
		ret.append("\"id\":").append(request_id).append(",");
		ret.append("\"src\":\"").append(deviceId).append("\",");
		ret.append("\"dst\":").append("\"unknown\"").append(",");
		{
			ret.append(" \"result\": {");
			ret.append("\"a_act_power\":").append(String.format("%.1f", phasePower1)).append(",");
			ret.append("\"b_act_power\":").append(String.format("%.1f", phasePowerRest)).append(",");
			ret.append("\"c_act_power\":").append(String.format("%.1f", phasePowerRest)).append(",");
			ret.append("\"total_act_power\":").append(String.format("%.1f", totalPower)).append("");
			ret.append("}");
		}
		ret.append("}");

		return ret.toString();
	}

	private int clamp(final int in, final int min, final int max)
	{
		if (in < min)
			return min;
		if (in > max)
			return max;
		return in;
	}

	private double clamp(final double in, final double min, final double max)
	{
		if (in < min)
			return min;
		if (in > max)
			return max;
		return in;
	}

	// private EmGetStatusResponse createEmGetStatusResponse()
	// {
	// final double factor = 1.0;
	//
	// final double power = 20;
	// final double apparentPower = power;
	// final double voltage = 230;
	// final double current = 20 / voltage;
	// final double frequency = 50;
	// final double powerFactor = 1.0;
	// final double totalPower = (power + power + power) * factor;
	//
	// return new EmGetStatusResponse( //
	// 0, //
	// current * factor, //
	// voltage, //
	// power * factor, //
	// apparentPower * factor, //
	// powerFactor, //
	// frequency, //
	// null, //
	// current * factor, //
	// voltage, //
	// power * factor, //
	// apparentPower * factor, //
	// powerFactor, //
	// frequency, //
	// null, //
	// current * factor, //
	// voltage, //
	// power * factor, //
	// apparentPower * factor, //
	// powerFactor, //
	// frequency, //
	// null, //
	// null, //
	// null, //
	// (current + current + current) * factor, //
	// totalPower, //
	// (apparentPower + apparentPower + apparentPower) * factor, //
	// null, //
	// null //
	// );
	// }

	@Override
	public void close()
	{
		closed = true;
	}

	//

	// @JsonInclude(JsonInclude.Include.NON_NULL)
	// @JsonIgnoreProperties(ignoreUnknown = true)
	// @JsonPropertyOrder({ "id", "a_current", "a_voltage", "a_act_power", "a_aprt_power", "a_pf", "a_freq", "a_errors", "b_current", "b_voltage", "b_act_power", "b_aprt_power", "b_pf", "b_freq", "b_errors", "c_current", "c_voltage", "c_act_power",
	// "c_aprt_power", "c_pf", "c_freq", "c_errors", "n_current", "n_errors", "total_current", "total_act_power", "total_aprt_power", "user_calibrated_phase", "errors" })
	// public class EmGetStatusResponse
	// {
	//
	// public EmGetStatusResponse()
	// {
	// }
	//
	// public EmGetStatusResponse(final Integer id, final Double a_current, final Double a_voltage, final Double a_act_power, final Double a_aprt_power, final Double a_pf, final Double a_freq, final List<String> a_errors, final Double b_current,
	// final Double b_voltage, final Double b_act_power, final Double b_aprt_power, final Double b_pf, final Double b_freq, final List<String> b_errors, final Double c_current, final Double c_voltage, final Double c_act_power,
	// final Double c_aprt_power, final Double c_pf, final Double c_freq, final List<String> c_errors, final Double n_current, final List<String> n_errors, final Double total_current, final Double total_act_power, final Double total_aprt_power,
	// final List<String> user_calibrated_phase, final List<String> errors)
	// {
	// super();
	// this.id = id;
	// this.a_current = a_current;
	// this.a_voltage = a_voltage;
	// this.a_act_power = a_act_power;
	// this.a_aprt_power = a_aprt_power;
	// this.a_pf = a_pf;
	// this.a_freq = a_freq;
	// this.a_errors = a_errors;
	// this.b_current = b_current;
	// this.b_voltage = b_voltage;
	// this.b_act_power = b_act_power;
	// this.b_aprt_power = b_aprt_power;
	// this.b_pf = b_pf;
	// this.b_freq = b_freq;
	// this.b_errors = b_errors;
	// this.c_current = c_current;
	// this.c_voltage = c_voltage;
	// this.c_act_power = c_act_power;
	// this.c_aprt_power = c_aprt_power;
	// this.c_pf = c_pf;
	// this.c_freq = c_freq;
	// this.c_errors = c_errors;
	// this.n_current = n_current;
	// this.n_errors = n_errors;
	// this.total_current = total_current;
	// this.total_act_power = total_act_power;
	// this.total_aprt_power = total_aprt_power;
	// this.user_calibrated_phase = user_calibrated_phase;
	// this.errors = errors;
	// }
	//
	// @JsonProperty("id")
	// Integer id;
	// @JsonProperty("a_current")
	// Double a_current;
	// @JsonProperty("a_voltage")
	// Double a_voltage;
	// @JsonProperty("a_act_power")
	// Double a_act_power;
	// @JsonProperty("a_aprt_power")
	// Double a_aprt_power;
	// @JsonProperty("a_pf")
	// Double a_pf;
	// @JsonProperty("a_freq")
	// Double a_freq;
	// @JsonProperty("a_errors")
	// List<String> a_errors;
	// @JsonProperty("b_current")
	// Double b_current;
	// @JsonProperty("b_voltage")
	// Double b_voltage;
	// @JsonProperty("b_act_power")
	// Double b_act_power;
	// @JsonProperty("b_aprt_power")
	// Double b_aprt_power;
	// @JsonProperty("b_pf")
	// Double b_pf;
	// @JsonProperty("b_freq")
	// Double b_freq;
	// @JsonProperty("b_errors")
	// List<String> b_errors;
	// @JsonProperty("c_current")
	// Double c_current;
	// @JsonProperty("c_voltage")
	// Double c_voltage;
	// @JsonProperty("c_act_power")
	// Double c_act_power;
	// @JsonProperty("c_aprt_power")
	// Double c_aprt_power;
	// @JsonProperty("c_pf")
	// Double c_pf;
	// @JsonProperty("c_freq")
	// Double c_freq;
	// @JsonProperty("c_errors")
	// List<String> c_errors;
	// @JsonProperty("n_current")
	// Double n_current;
	// @JsonProperty("n_errors")
	// List<String> n_errors;
	// @JsonProperty("total_current")
	// Double total_current;
	// @JsonProperty("total_act_power")
	// Double total_act_power;
	// @JsonProperty("total_aprt_power")
	// Double total_aprt_power;
	// @JsonProperty("user_calibrated_phase")
	// List<String> user_calibrated_phase;
	// @JsonProperty("errors")
	// List<String> errors;
	// }

}
