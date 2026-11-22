package marstek;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import data.ValueRegistry;
import mqtt.MqttBroker;

/**
 * Marstek B2500 gateway: embedded MQTT broker + cloud pass-through.
 *
 * The B2500 connects (via BLE configuration) to the embedded broker. All device traffic is mirrored to the Marstek cloud (mutual TLS, certs in "certs/") and vice versa, so the official app keeps working while we see every message.
 *
 * Status messages (device -> app) arrive in onDeviceMessage() for processing. The runtime state is requested with cd=01 in a fixed interval, so values stay fresh even when the app is closed.
 *
 * Topics: device publishes status on hame_energy/{type}/device/{mac}/ctrl, commands go to hame_energy/{type}/App/{mac}/ctrl. The cloud uses the same structure with a cloud side device id instead of the MAC (AES encrypted MAC for firmware 226.x/108.x) and the "marstek_energy" prefix.
 */
public class B2500 extends Thread implements Closeable, MqttCallbackExtended
{
	// hame-2025 topic id encryption key (see hm2mqtt src/utils/crypt.ts)
	// registry keys also read by other components (e.g. ShellyUDP)
	public static final String	VALUE_SOC							= "Battery - SOC";
	public static final String	VALUE_DISCHARGE_DEPTH				= "Battery - Discharge Depth";
	public static final String	VALUE_PV_TOTAL						= "Battery - PV Total";
	private static final String	VALUE_CLOUD_DEVICEID				= "Battery cloud deviceId";
	private static final String	VALUE_ONLINE						= "Battery - Online";
	private static final String	VALUE_SUBSCRIBE_ERROR				= "Battery subscribe error: ";
	private static final String	VALUE_LAST_MESSAGE					= "Battery last message";
	private static final String	VALUE_FORWARDED_STATUS				= "Battery forwarded status";
	private static final String	VALUE_MESSAGE_ERROR					= "Battery message error: ";
	private static final String	VALUE_CLOUD_SUBSCRIBE_ERROR			= "Battery cloud subscribe error: ";
	private static final String	VALUE_FORWARDED_COMMAND				= "Battery forwarded command: ";
	private static final String	VALUE_CLOUD_CONNECTION_LOST			= "Battery cloud connection lost: ";
	private static final String	VALUE_PV_1							= "Battery - PV 1";
	private static final String	VALUE_PV_2							= "Battery - PV 2";
	private static final String	VALUE_PV_PASS_THROUGH				= "Battery - PV Pass-Through";
	public static final String	VALUE_OUTPUT_TOTAL					= "Battery - Output Total";
	private static final String	VALUE_ENERGY_PV_TODAY				= "Battery - Energy PV Today";
	private static final String	VALUE_ENERGY_INVERTER_TODAY			= "Battery - Energy Inverter Today";
	private static final String	VALUE_ENERGY_DISCHARGE_TODAY		= "Battery - Energy Discharge Today";
	private static final String	VALUE_ENERGY_CHARGE_TODAY			= "Battery - Energy Charge Today";
	private static final String	VALUE_CAPACITY						= "Battery - Capacity";
	private static final String	VALUE_TEMP_MIN						= "Battery - Temp Min";
	private static final String	VALUE_TEMP_MAX						= "Battery - Temp Max";
	private static final String	VALUE_SOC_HOST						= "Battery - SOC Host";
	private static final String	VALUE_SOC_EXTRA_1					= "Battery - SOC Extra 1";
	private static final String	VALUE_SOC_EXTRA_2					= "Battery - SOC Extra 2";
	private static final String	VALUE_OUTPUT_THRESHOLD				= "Battery - Output Threshold";
	private static final String	VALUE_RATED_OUTPUT					= "Battery - Rated Output";
	private static final String	VALUE_RATED_INPUT					= "Battery - Rated Input";
	private static final String	VALUE_OUTPUT_1						= "Battery - Output 1";
	private static final String	VALUE_OUTPUT_2						= "Battery - Output 2";
	private static final String	VALUE_WIFI_RSSI						= "Battery - WiFi RSSI";
	private static final String	VALUE_CT_POWER						= "Battery - CT Power";
	private static final String	VALUE_CT_TARGET						= "Battery - CT Target";
	private static final String	VALUE_CT_CLIP_1						= "Battery - CT Clip 1";
	private static final String	VALUE_CT_CLIP_2						= "Battery - CT Clip 2";
	private static final String	VALUE_CT_CLIP_3						= "Battery - CT Clip 3";
	private static final String	VALUE_INVERTER_POWER				= "Battery - Inverter Power";
	private static final String	VALUE_CT_PHASE						= "Battery - CT Phase";
	private static final String	VALUE_DEVICE_ID						= "Battery - Device ID";
	private static final String	VALUE_BOOTLOADER_VERSION			= "Battery - Bootloader Version";
	private static final String	VALUE_TIMER_1_POWER					= "Battery - Timer 1 Power";
	private static final String	VALUE_TIMER_2_POWER					= "Battery - Timer 2 Power";
	private static final String	VALUE_TIMER_3_POWER					= "Battery - Timer 3 Power";
	private static final String	VALUE_TIMER_4_POWER					= "Battery - Timer 4 Power";
	private static final String	VALUE_TIMER_5_POWER					= "Battery - Timer 5 Power";
	private static final String	VALUE_OUTPUT_1_ON					= "Battery - Output 1 On";
	private static final String	VALUE_OUTPUT_2_ON					= "Battery - Output 2 On";
	private static final String	VALUE_EXTRA_PACK_1_CONNECTED		= "Battery - Extra Pack 1 Connected";
	private static final String	VALUE_EXTRA_PACK_2_CONNECTED		= "Battery - Extra Pack 2 Connected";
	private static final String	VALUE_ADAPTIVE_MODE					= "Battery - Adaptive Mode";
	private static final String	VALUE_CT_CONNECTED					= "Battery - CT Connected";
	private static final String	VALUE_TEMP_CHARGE_ALARM				= "Battery - Temp Charge Alarm";
	private static final String	VALUE_TEMP_DISCHARGE_ALARM			= "Battery - Temp Discharge Alarm";
	private static final String	VALUE_POWER_LIMITED					= "Battery - Power Limited";
	private static final String	VALUE_TIMER_1_ON					= "Battery - Timer 1 On";
	private static final String	VALUE_TIMER_2_ON					= "Battery - Timer 2 On";
	private static final String	VALUE_TIMER_3_ON					= "Battery - Timer 3 On";
	private static final String	VALUE_TIMER_4_ON					= "Battery - Timer 4 On";
	private static final String	VALUE_TIMER_5_ON					= "Battery - Timer 5 On";
	private static final String	VALUE_PV_1_CHARGING					= "Battery - PV 1 Charging";
	private static final String	VALUE_PV_1_PASS_THROUGH				= "Battery - PV 1 Pass-Through";
	private static final String	VALUE_PV_2_CHARGING					= "Battery - PV 2 Charging";
	private static final String	VALUE_PV_2_PASS_THROUGH				= "Battery - PV 2 Pass-Through";
	private static final String	VALUE_HOST_BATTERY_DISCHARGING		= "Battery - Host Battery Discharging";
	private static final String	VALUE_HOST_BATTERY_CHARGING			= "Battery - Host Battery Charging";
	private static final String	VALUE_HOST_BATTERY_DOD_REACHED		= "Battery - Host Battery DoD Reached";
	private static final String	VALUE_HOST_BATTERY_UNDERVOLTAGE		= "Battery - Host Battery Undervoltage";
	private static final String	VALUE_EXTRA_2_BATTERY_DISCHARGING	= "Battery - Extra 2 Battery Discharging";
	private static final String	VALUE_EXTRA_2_BATTERY_CHARGING		= "Battery - Extra 2 Battery Charging";
	private static final String	VALUE_EXTRA_2_BATTERY_DOD_REACHED	= "Battery - Extra 2 Battery DoD Reached";
	private static final String	VALUE_EXTRA_2_BATTERY_UNDERVOLTAGE	= "Battery - Extra 2 Battery Undervoltage";
	private static final String	VALUE_EXTRA_1_BATTERY_DISCHARGING	= "Battery - Extra 1 Battery Discharging";
	private static final String	VALUE_EXTRA_1_BATTERY_CHARGING		= "Battery - Extra 1 Battery Charging";
	private static final String	VALUE_EXTRA_1_BATTERY_DOD_REACHED	= "Battery - Extra 1 Battery DoD Reached";
	private static final String	VALUE_EXTRA_1_BATTERY_UNDERVOLTAGE	= "Battery - Extra 1 Battery Undervoltage";
	private static final String	VALUE_SURPLUS_FEED_IN_DISABLED		= "Battery - Surplus Feed-in Disabled";
	private static final String	VALUE_TIMER_1_START					= "Battery - Timer 1 Start";
	private static final String	VALUE_TIMER_1_END					= "Battery - Timer 1 End";
	private static final String	VALUE_TIMER_2_START					= "Battery - Timer 2 Start";
	private static final String	VALUE_TIMER_2_END					= "Battery - Timer 2 End";
	private static final String	VALUE_TIMER_3_START					= "Battery - Timer 3 Start";
	private static final String	VALUE_TIMER_3_END					= "Battery - Timer 3 End";
	private static final String	VALUE_TIMER_4_START					= "Battery - Timer 4 Start";
	private static final String	VALUE_TIMER_4_END					= "Battery - Timer 4 End";
	private static final String	VALUE_TIMER_5_START					= "Battery - Timer 5 Start";
	private static final String	VALUE_TIMER_5_END					= "Battery - Timer 5 End";
	private static final String	VALUE_CT_TYPE						= "Battery - CT Type";
	private static final String	VALUE_CT_STATUS						= "Battery - CT Status";
	private static final String	VALUE_CHIP_VERSION					= "Battery - Chip Version";
	private static final String	VALUE_FIRMWARE						= "Battery - Firmware";
	private static final String	VALUE_SCENE							= "Battery - Scene";
	private static final String	VALUE_CHARGING_MODE					= "Battery - Charging Mode";
	private static final String	VALUE_PUBLISH_ERROR					= "Battery publish error: ";
	private static final String	VALUE_LOCAL_CONNECTION_LOST			= "Battery local connection lost: ";

	private static final String	TOPIC_ID_AES_KEY					= "!@#$%^&*()_+{}[]";
	private static final String	LOCAL_CLIENT_ID						= "homeauto-b2500";
	private static final int	DEFAULT_POLL_SECONDS				= 300;
	private static final long	UI_ACTIVE_WINDOW					= 120000L;									// UI use accelerates polling for 2 min
	private static final long	UI_ACTIVE_INTERVAL					= 30000L;									// ... to one poll per 30 s
	private static final String	UI_LAST_ACCESS_KEY					= "UI last access";

	private final ValueRegistry	valueRegistry;
	private final MqttBroker	broker;
	private final MqttClient	localClient;
	private volatile MqttClient	cloudClient;																	// null during startup
	private final String		deviceType;
	private final String		mac;
	private final String		controlTopic;
	private final long			pollMillis;
	private final String		cloudDeviceId;
	private final String		localEncryptedId;																// id used locally by buggy firmware 226.5/108.7

	private volatile boolean	closed								= false;
	private long				lastRequest							= 0;
	private long				lastResponse						= 0;

	public B2500(final ValueRegistry valueRegistry, final int port, final String deviceType, final String mac, final String cloudUser, final String cloudPassword, final String cloudDeviceIdOverride, final int pollSeconds) throws Exception
	{
		super("B2500");
		this.valueRegistry = valueRegistry;
		this.mac = mac;
		this.deviceType = deviceType;
		this.controlTopic = "hame_energy/" + deviceType + "/App/" + mac + "/ctrl";
		this.pollMillis = pollSeconds * 1000L;
		this.localEncryptedId = aesTopicId(mac);
		final String[] idSource = new String[1];
		this.cloudDeviceId = resolveCloudDeviceId(cloudUser, cloudPassword, cloudDeviceIdOverride, idSource);

		broker = new MqttBroker(port, valueRegistry);

		// local loopback connection
		localClient = new MqttClient("tcp://127.0.0.1:" + port, LOCAL_CLIENT_ID, new MemoryPersistence());
		final MqttConnectOptions localOptions = new MqttConnectOptions();
		localOptions.setAutomaticReconnect(true);
		localOptions.setCleanSession(true);
		localOptions.setConnectionTimeout(10);
		localClient.setCallback(this);
		localClient.connect(localOptions);

		// cloud connection (mutual TLS, AWS IoT)
		cloudClient = new MqttClient(readUrl("hame-2025-url"), "mst_" + randomHex(24), new MemoryPersistence());
		final MqttConnectOptions cloudOptions = new MqttConnectOptions();
		cloudOptions.setSocketFactory(createSslSocketFactory("2025").getSocketFactory());
		cloudOptions.setKeepAliveInterval(30);
		cloudOptions.setAutomaticReconnect(true);
		cloudOptions.setCleanSession(true);
		cloudOptions.setConnectionTimeout(30);
		cloudClient.setCallback(cloudCallback());
		cloudClient.connect(cloudOptions);

		valueRegistry.setValueDebug(VALUE_CLOUD_DEVICEID, cloudDeviceId + " (" + idSource[0] + ")");
		System.out.println("B2500 gateway enabled: mac=" + mac + " cloudDeviceId=" + cloudDeviceId);
	}

	/**
	 * Requests the runtime state (cd=01) in a fixed interval (default 5 min). The device does not push data on its own, so this is what keeps the values fresh even when the app is closed. cd=01 is a read-only request, no flash writes. When the web UI was used recently, the interval shortens to 30 s so
	 * the displayed values stay current.
	 */
	@Override
	public void run()
	{
		while (!closed)
		{
			try
			{
				Thread.sleep(1000L);
			}
			catch (final InterruptedException e)
			{
				return;
			}

			final long now = System.currentTimeMillis();
			final long interval = currentInterval(now);
			if (now - lastRequest >= interval)
			{
				lastRequest = now;
				publish(localClient, controlTopic, "cd=01");

				valueRegistry.setValueDebug(VALUE_ONLINE, (now - lastResponse) < (3 * interval));
			}
		}
	}

	private long currentInterval(final long now)
	{
		final long uiAge = now - valueRegistry.getTsLastUpdate(UI_LAST_ACCESS_KEY);
		if (uiAge < UI_ACTIVE_WINDOW)
			return Math.min(pollMillis, UI_ACTIVE_INTERVAL);
		return pollMillis;
	}

	/**
	 * Callback of the LOCAL client only: receives the device's status messages.
	 */
	@Override
	public void connectComplete(final boolean reconnect, final String serverURI)
	{
		System.out.println("B2500 connected (" + (reconnect ? "reconnect" : "initial") + "): " + serverURI);
		try
		{
			// device status from the local broker (normal and buggy firmware topic form)
			localClient.subscribe("hame_energy/+/device/+/ctrl", 0);
			localClient.subscribe("marstek_energy/+/device/+/ctrl", 0);
		}
		catch (final MqttException e)
		{
			e.printStackTrace();
			valueRegistry.incCountDebug(VALUE_SUBSCRIBE_ERROR + e);
		}
	}

	/**
	 * Callback of the LOCAL client only: device status -> handler + cloud.
	 */
	@Override
	public void messageArrived(final String topic, final MqttMessage message)
	{
		final String[] parts = topic.split("/");
		if (parts.length != 5 || !parts[2].equals("device"))
			return;
		if (!parts[3].equals(mac) && !parts[3].equals(localEncryptedId))
			return;

		try
		{
			// status message from the device
			lastResponse = System.currentTimeMillis();
			valueRegistry.setValueDebug(VALUE_ONLINE, true);
			valueRegistry.setValueDebug(VALUE_LAST_MESSAGE, topic + ": " + new String(message.getPayload()));

			onDeviceMessage(topic, new String(message.getPayload(), StandardCharsets.UTF_8));

			// mirror to the cloud with the cloud side device id
			publish(cloudClient, "marstek_energy/" + parts[1] + "/device/" + cloudDeviceId + "/ctrl", message);
			valueRegistry.incCountDebug(VALUE_FORWARDED_STATUS);
		}
		catch (final Exception e)
		{
			e.printStackTrace();
			valueRegistry.incCountDebug(VALUE_MESSAGE_ERROR + e);
		}
	}

	/**
	 * Id resolution order: explicit override -> cloud API (salt based, needs account credentials) -> AES encrypted MAC (older firmware scheme).
	 */
	private String resolveCloudDeviceId(final String cloudUser, final String cloudPassword, final String override, final String[] sourceOut) throws Exception
	{
		if (override != null && !override.isBlank())
		{
			sourceOut[0] = "manual override";
			return override;
		}

		if (cloudUser != null && !cloudUser.isBlank() && cloudPassword != null && !cloudPassword.isBlank())
		{
			try
			{
				final String id = HameApi.resolveCloudDeviceId(cloudUser, cloudPassword, mac);
				if (id != null)
				{
					System.out.println("B2500 cloud device id resolved via API: " + id);
					sourceOut[0] = "cloud api (salt)";
					return id;
				}
				System.out.println("B2500: no salt based topic id available, using encrypted MAC id");
			}
			catch (final Exception e)
			{
				System.out.println("B2500: cloud API resolution failed (" + e + "), using encrypted MAC id");
			}
		}
		sourceOut[0] = "encrypted MAC fallback";
		return localEncryptedId;
	}

	/**
	 * Callback of the CLOUD client only: app commands -> to the device.
	 */
	private MqttCallbackExtended cloudCallback()
	{
		return new MqttCallbackExtended()
		{
			@Override
			public void connectComplete(final boolean reconnect, final String serverURI)
			{
				System.out.println("B2500 cloud connected (" + (reconnect ? "reconnect" : "initial") + "): " + serverURI);
				try
				{
					// app commands via the cloud side device id. NOTE: exact topics
					// only - the AWS IoT policy disconnects on wildcard filters
					cloudClient.subscribe("marstek_energy/" + deviceType + "/App/" + cloudDeviceId + "/ctrl", 0);
					cloudClient.subscribe("hame_energy/" + deviceType + "/App/" + cloudDeviceId + "/ctrl", 0);
				}
				catch (final MqttException e)
				{
					e.printStackTrace();
					valueRegistry.incCountDebug(VALUE_CLOUD_SUBSCRIBE_ERROR + e);
				}
			}

			@Override
			public void messageArrived(final String topic, final MqttMessage message)
			{
				final String[] parts = topic.split("/");
				if (parts.length != 5 || !parts[2].equals("App"))
					return;
				if (!parts[3].equals(cloudDeviceId) && !parts[3].equals(mac))
					return;

				// command from the app -> to the device on the local broker
				publish(localClient, "hame_energy/" + parts[1] + "/App/" + mac + "/ctrl", message);
				valueRegistry.incCountDebug(VALUE_FORWARDED_COMMAND + new String(message.getPayload()));
			}

			@Override
			public void connectionLost(final Throwable cause)
			{
				valueRegistry.incCountDebug(VALUE_CLOUD_CONNECTION_LOST + cause);
			}

			@Override
			public void deliveryComplete(final IMqttDeliveryToken token)
			{
			}
		};
	}

	/** Values shown on the main UI page; everything else is debug only. */
	private static final Set<String> PRIMARY_VALUES = new HashSet<>(Arrays.asList(VALUE_SOC, VALUE_PV_1, VALUE_PV_2, VALUE_PV_PASS_THROUGH, VALUE_PV_TOTAL, VALUE_OUTPUT_TOTAL, VALUE_ENERGY_PV_TODAY, VALUE_ENERGY_INVERTER_TODAY, VALUE_ENERGY_DISCHARGE_TODAY,
			VALUE_ENERGY_CHARGE_TODAY, VALUE_CAPACITY, VALUE_TEMP_MIN, VALUE_TEMP_MAX));

	/** Publishes into the ValueRegistry: non-debug for primary values, debug for the rest. */
	private void publish(final String key, final Object value, final String text)
	{
		if (PRIMARY_VALUES.contains(key))
			valueRegistry.setValue(key, value, text);
		else
			valueRegistry.setValueDebug(key, value, text);
	}

	// payload key -> registry key + unit suffix, published as numbers
	private static final String[][]	NUMERIC_FIELDS	= { { "pe", VALUE_SOC, " %" },																																													// state of charge
			{ "a0", VALUE_SOC_HOST, " %" },																																																							// per pack SoC (fw >= 212.17)
			{ "a1", VALUE_SOC_EXTRA_1, " %" }, { "a2", VALUE_SOC_EXTRA_2, " %" }, { "kn", VALUE_CAPACITY, " Wh" }, { "do", VALUE_DISCHARGE_DEPTH, " %" }, { "lv", VALUE_OUTPUT_THRESHOLD, " W" },																	// min load before output
			{ "lmo", VALUE_RATED_OUTPUT, " W" },																																																					// fw >= 220.1
			{ "lmi", VALUE_RATED_INPUT, " W" }, { "w1", VALUE_PV_1, " W" }, { "w2", VALUE_PV_2, " W" }, { "g1", VALUE_OUTPUT_1, " W" }, { "g2", VALUE_OUTPUT_2, " W" }, { "tl", VALUE_TEMP_MIN, " °C" }, { "th", VALUE_TEMP_MAX, " °C" },
			{ "ws", VALUE_WIFI_RSSI, " dBm" }, { "bc", VALUE_ENERGY_CHARGE_TODAY, " Wh" },																																											// daily energies (fw >= 218)
			{ "bs", VALUE_ENERGY_DISCHARGE_TODAY, " Wh" }, { "pt", VALUE_ENERGY_PV_TODAY, " Wh" }, { "it", VALUE_ENERGY_INVERTER_TODAY, " Wh" }, { "st", VALUE_CT_POWER, " W" }, { "sp", VALUE_CT_TARGET, " W" }, { "m0", VALUE_CT_CLIP_1, " W" },
			{ "m1", VALUE_CT_CLIP_2, " W" }, { "m2", VALUE_CT_CLIP_3, " W" }, { "m3", VALUE_INVERTER_POWER, " W" }, { "c0", VALUE_CT_PHASE, "" },																													// 0-3, 255 = none
			{ "id", VALUE_DEVICE_ID, "" }, { "uv", VALUE_BOOTLOADER_VERSION, "" }, { "h1", VALUE_TIMER_1_POWER, " W" }, { "h2", VALUE_TIMER_2_POWER, " W" }, { "h3", VALUE_TIMER_3_POWER, " W" }, { "h4", VALUE_TIMER_4_POWER, " W" },
			{ "h5", VALUE_TIMER_5_POWER, " W" }, };

	// payload key -> registry key, published as booleans
	private static final String[][]	BOOLEAN_FIELDS	= { { "o1", VALUE_OUTPUT_1_ON }, { "o2", VALUE_OUTPUT_2_ON }, { "b1", VALUE_EXTRA_PACK_1_CONNECTED }, { "b2", VALUE_EXTRA_PACK_2_CONNECTED }, { "md", VALUE_ADAPTIVE_MODE }, { "sg", VALUE_CT_CONNECTED },
			{ "tc", VALUE_TEMP_CHARGE_ALARM }, { "tf", VALUE_TEMP_DISCHARGE_ALARM }, { "lmf", VALUE_POWER_LIMITED }, { "d1", VALUE_TIMER_1_ON }, { "d2", VALUE_TIMER_2_ON }, { "d3", VALUE_TIMER_3_ON }, { "d4", VALUE_TIMER_4_ON }, { "d5", VALUE_TIMER_5_ON }, };

	// payload key + bit index -> registry key, published as booleans (bit flags)
	private static final String[][]	BIT_FIELDS		= { { "p1", "0", VALUE_PV_1_CHARGING },																																											// bit 0
			{ "p1", "1", VALUE_PV_1_PASS_THROUGH },																																																					// bit 1
			{ "p2", "0", VALUE_PV_2_CHARGING }, { "p2", "1", VALUE_PV_2_PASS_THROUGH }, { "l0", "0", VALUE_HOST_BATTERY_DISCHARGING },																																// pack status flags
			{ "l0", "1", VALUE_HOST_BATTERY_CHARGING }, { "l0", "2", VALUE_HOST_BATTERY_DOD_REACHED }, { "l0", "3", VALUE_HOST_BATTERY_UNDERVOLTAGE }, { "l1", "0", VALUE_EXTRA_2_BATTERY_DISCHARGING },															// bits 0-3 = extra 2
			{ "l1", "1", VALUE_EXTRA_2_BATTERY_CHARGING }, { "l1", "2", VALUE_EXTRA_2_BATTERY_DOD_REACHED }, { "l1", "3", VALUE_EXTRA_2_BATTERY_UNDERVOLTAGE }, { "l1", "4", VALUE_EXTRA_1_BATTERY_DISCHARGING },													// bits 4-7 = extra 1
			{ "l1", "5", VALUE_EXTRA_1_BATTERY_CHARGING }, { "l1", "6", VALUE_EXTRA_1_BATTERY_DOD_REACHED }, { "l1", "7", VALUE_EXTRA_1_BATTERY_UNDERVOLTAGE }, { "tc_dis", "0", VALUE_SURPLUS_FEED_IN_DISABLED }, };

	// payload key -> registry key, published as strings
	private static final String[][]	TEXT_FIELDS		= { { "e1", VALUE_TIMER_1_START }, { "f1", VALUE_TIMER_1_END }, { "e2", VALUE_TIMER_2_START }, { "f2", VALUE_TIMER_2_END }, { "e3", VALUE_TIMER_3_START }, { "f3", VALUE_TIMER_3_END },
			{ "e4", VALUE_TIMER_4_START }, { "f4", VALUE_TIMER_4_END }, { "e5", VALUE_TIMER_5_START }, { "f5", VALUE_TIMER_5_END }, };

	/**
	 * Called for every status message the device publishes. Payloads are comma separated key=value pairs, e.g. "p1=0,w1=0,pe=99,..." (see hm2mqtt docs/b2500.md for the full field reference).
	 */
	public void onDeviceMessage(final String topic, final String payload)
	{
		final Map<String, String> values = new HashMap<>();
		for (final String pair : payload.split(","))
		{
			final int idx = pair.indexOf('=');
			if (idx > 0)
				values.put(pair.substring(0, idx).trim(), pair.substring(idx + 1).trim());
		}

		// runtime info response (cd=01) is identified by the state of charge field
		if (!values.containsKey("pe"))
			return;

		for (final String[] field : NUMERIC_FIELDS)
		{
			final String v = values.get(field[0]);
			if (v == null)
				continue;
			try
			{
				final long num = Long.parseLong(v);
				publish(field[1], num, v + field[2]);
			}
			catch (final NumberFormatException e)
			{
				// key not present as a number in this response
			}
		}

		for (final String[] field : BOOLEAN_FIELDS)
		{
			final String v = values.get(field[0]);
			if (v != null)
				publish(field[1], "1".equals(v), null);
		}

		for (final String[] field : TEXT_FIELDS)
		{
			final String v = values.get(field[0]);
			if (v != null)
				publish(field[1], v, null);
		}

		for (final String[] field : BIT_FIELDS)
		{
			final String v = values.get(field[0]);
			if (v == null)
				continue;
			try
			{
				final int bits = Integer.parseInt(v);
				publish(field[2], ((bits >> Integer.parseInt(field[1])) & 1) == 1, null);
			}
			catch (final NumberFormatException e)
			{
				// key not present as a number in this response
			}
		}

		// reported CT type (own code space, differs from the cd=27 command codes)
		final String ctType = values.get("ct_t");
		if (ctType != null)
			publish(VALUE_CT_TYPE, ctTypeName(ctType), null);

		// CT diagnosis status
		final String ctStatus = values.get("c1");
		if (ctStatus != null)
			publish(VALUE_CT_STATUS, ctStatusName(ctStatus), null);

		// FC4.2D chip version string
		final String chip = values.get("fc");
		if (chip != null)
			publish(VALUE_CHIP_VERSION, chip, null);

		// pass-through: true while either PV input feeds through to the output
		try
		{
			final int p1 = Integer.parseInt(values.get("p1"));
			final int p2 = Integer.parseInt(values.get("p2"));
			publish(VALUE_PV_PASS_THROUGH, ((p1 >> 1) & 1) == 1 || ((p2 >> 1) & 1) == 1, null);
		}
		catch (final NumberFormatException | NullPointerException e)
		{
			// pass-through flags not present in this response
		}

		// derived totals
		setTotal(values, "w1", "w2", VALUE_PV_TOTAL);
		setTotal(values, "g1", "g2", VALUE_OUTPUT_TOTAL);

		// firmware version as "major.sub"
		final String vv = values.get("vv");
		if (vv != null)
		{
			final String sv = values.get("sv");
			publish(VALUE_FIRMWARE, vv + (sv != null ? "." + sv : ""), null);
		}

		// scene: 0=day, 1=night, 2=dusk
		final String cj = values.get("cj");
		if (cj != null)
			publish(VALUE_SCENE, "1".equals(cj) ? "night" : "2".equals(cj) ? "dusk" : "day", null);

		// charging mode: 0=simultaneous charge+discharge, 1=charge then discharge
		final String cs = values.get("cs");
		if (cs != null)
			publish(VALUE_CHARGING_MODE, "1".equals(cs) ? "charge-then-discharge" : "simultaneous", null);
	}

	/** Reported smart meter type codes (field ct_t). */
	private static String ctTypeName(final String code)
	{
		switch (code)
		{
			case "1":
				return "CT001";
			case "2":
				return "CT001.5";
			case "3":
				return "CT002";
			case "4":
				return "Shelly Pro 3EM";
			case "5":
				return "P1 Meter";
			case "6":
				return "CT003";
			case "7":
				return "Shelly EM Gen3";
			case "8":
				return "Shelly Pro EM50";
			case "9":
				return "EcoTracker";
			case "10":
				return "SMR-P1";
			case "11":
				return "SMR-IR";
			case "12":
				return "SMR-TIC";
			case "13":
				return "TPM2-100CT";
			default:
				return "unknown (" + code + ")";
		}
	}

	/** CT diagnosis status codes (field c1); anything else means not in diagnosis. */
	private static String ctStatusName(final String code)
	{
		switch (code)
		{
			case "5":
				return "preparing diagnosis (1)";
			case "6":
				return "preparing diagnosis (2)";
			case "7":
				return "diagnosing equipment";
			case "8":
				return "diagnosing channel";
			case "9":
				return "diagnosis timeout";
			case "10":
				return "charging in progress";
			case "11":
				return "channel not found";
			default:
				return "idle";
		}
	}

	private void setTotal(final Map<String, String> values, final String keyA, final String keyB, final String registryKey)
	{
		try
		{
			final long a = Long.parseLong(values.get(keyA));
			final long b = Long.parseLong(values.get(keyB));
			publish(registryKey, a + b, (a + b) + " W");
		}
		catch (final NumberFormatException | NullPointerException e)
		{
			// one of the inputs missing
		}
	}

	private void publish(final MqttClient client, final String topic, final String payload)
	{
		publish(client, topic, new MqttMessage(payload.getBytes()));
	}

	private void publish(final MqttClient client, final String topic, final MqttMessage message)
	{
		try
		{
			client.publish(topic, message);
		}
		catch (final MqttException e)
		{
			valueRegistry.incCountDebug(VALUE_PUBLISH_ERROR + e);
		}
	}

	@Override
	public void connectionLost(final Throwable cause)
	{
		valueRegistry.incCountDebug(VALUE_LOCAL_CONNECTION_LOST + cause);
	}

	@Override
	public void deliveryComplete(final IMqttDeliveryToken token)
	{
	}

	@Override
	public void close()
	{
		closed = true;
		try
		{
			localClient.disconnect();
			cloudClient.disconnect();
		}
		catch (final MqttException e)
		{
			// clients may never have connected
		}
		try
		{
			localClient.close();
			cloudClient.close();
		}
		catch (final MqttException e)
		{
			e.printStackTrace();
		}
		broker.close();
	}

	// --- TLS setup ---------------------------------------------------------

	/**
	 * Builds an SSLContext with the cloud CA as trust anchor and the client certificate/key for mutual TLS (AWS IoT). Keys must be PKCS#8 PEM (convert with: openssl pkcs8 -topk8 -nocrypt).
	 */
	private static SSLContext createSslSocketFactory(final String brokerYear) throws Exception
	{
		final CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");

		// trust store: cloud CA
		final KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
		trustStore.load(null, null);
		trustStore.setCertificateEntry("ca", certificateFactory.generateCertificate(new ByteArrayInputStream(readResource("ca.crt"))));
		final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		tmf.init(trustStore);

		// key store: client certificate + private key
		final Certificate certificate = certificateFactory.generateCertificate(new ByteArrayInputStream(readResource("hame-" + brokerYear + ".crt")));
		final PrivateKey privateKey = loadPemKey(readResource("hame-" + brokerYear + ".key"));

		final KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		keyStore.load(null, null);
		keyStore.setKeyEntry("client", privateKey, new char[0], new Certificate[] { certificate });
		final KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		kmf.init(keyStore, new char[0]);

		final SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
		return sslContext;
	}

	private static PrivateKey loadPemKey(final byte[] pem) throws Exception
	{
		final String base64 = new String(pem, StandardCharsets.UTF_8).replaceAll("-----(BEGIN|END) [^-]+-----", "").replaceAll("\\s", "");
		final byte[] der = Base64.getDecoder().decode(base64);
		return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
	}

	private static String readUrl(final String name) throws Exception
	{
		// url files contain "mqtt://host" - TLS is enforced, so map to ssl:// (port 8883 default)
		String url = new String(readResource(name), StandardCharsets.UTF_8).trim();
		if (url.startsWith("mqtt://"))
			url = "ssl://" + url.substring("mqtt://".length());
		return url;
	}

	/** Reads a file bundled in the jar (src/main/resources/certs). */
	private static byte[] readResource(final String name) throws IOException
	{
		try (InputStream in = B2500.class.getResourceAsStream("/certs/" + name))
		{
			if (in == null)
				throw new IOException("Certificate resource not found in jar: certs/" + name);
			return in.readAllBytes();
		}
	}

	// --- id helpers --------------------------------------------------------

	/**
	 * hame-2025 cloud topic id (firmware 226.x/108.x): MAC AES-128-CBC encrypted with a zero IV, hex encoded (see hm2mqtt src/utils/crypt.ts).
	 */
	static String aesTopicId(final String mac) throws Exception
	{
		final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(TOPIC_ID_AES_KEY.getBytes(StandardCharsets.UTF_8), "AES"), new IvParameterSpec(new byte[16]));
		final byte[] encrypted = cipher.doFinal(mac.getBytes(StandardCharsets.UTF_8));

		final StringBuilder hex = new StringBuilder(encrypted.length * 2);
		for (final byte b : encrypted)
			hex.append(String.format("%02x", b));
		return hex.toString();
	}

	private static String randomHex(final int length)
	{
		final String chars = "0123456789abcdef";
		final SecureRandom random = new SecureRandom();
		final StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++)
			sb.append(chars.charAt(random.nextInt(chars.length())));
		return sb.toString();
	}
}
