import java.nio.charset.StandardCharsets;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import data.ValueRegistry;
import mqtt.MqttBroker;

/**
 * Smoke test: starts the embedded broker and verifies a publish/subscribe roundtrip.
 */
public class TestMqttMain
{
	public static void main(final String[] args) throws Exception
	{
		final int port = args.length > 0 ? Integer.parseInt(args[0]) : 11883;

		final MqttBroker broker = new MqttBroker(port, new ValueRegistry());
		System.out.println("Broker up on port " + port);

		final MqttClient sub = new MqttClient("tcp://127.0.0.1:" + port, "test-sub", new MemoryPersistence());
		sub.connect(null);
		final Object lock = new Object();
		sub.subscribe("hame_energy/+/device/+/ctrl", 0, (topic, message) -> {
			System.out.println("RECEIVED: " + topic + " -> " + new String(message.getPayload(), StandardCharsets.UTF_8));
			synchronized (lock)
			{
				lock.notifyAll();
			}
		});

		final MqttClient pub = new MqttClient("tcp://127.0.0.1:" + port, "test-pub", new MemoryPersistence());
		pub.connect(null);
		pub.publish("hame_energy/HMA/device/001122334455/ctrl", new MqttMessage("p1=0,w1=123,pe=77".getBytes()));

		synchronized (lock)
		{
			lock.wait(5000);
		}

		sub.disconnect();
		pub.disconnect();
		broker.close();
		System.out.println("Done");
	}
}
