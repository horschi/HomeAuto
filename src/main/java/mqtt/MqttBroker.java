package mqtt;

import java.io.Closeable;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import io.moquette.BrokerConstants;
import io.moquette.broker.Server;
import io.moquette.broker.config.MemoryConfig;
import io.moquette.interception.AbstractInterceptHandler;
import io.moquette.interception.messages.InterceptConnectMessage;
import io.moquette.interception.messages.InterceptPublishMessage;
import io.moquette.interception.messages.InterceptSubscribeMessage;

import data.ValueRegistry;

/**
 * Embedded MQTT broker. The Marstek B2500 can be configured (via BLE) to connect
 * to this broker instead of the Marstek cloud. All traffic passing through is
 * logged to the ValueRegistry for the web UI.
 */
public class MqttBroker implements Closeable
{
	private final Server	server	= new Server();

	public MqttBroker(final int port, final ValueRegistry valueRegistry) throws Exception
	{
		final Properties props = new Properties();
		props.setProperty(BrokerConstants.PORT_PROPERTY_NAME, Integer.toString(port));
		props.setProperty(BrokerConstants.HOST_PROPERTY_NAME, BrokerConstants.HOST);
		props.setProperty(BrokerConstants.ALLOW_ANONYMOUS_PROPERTY_NAME, Boolean.TRUE.toString());

		server.startServer(new MemoryConfig(props));

		server.addInterceptHandler(new AbstractInterceptHandler()
		{
			@Override
			public String getID()
			{
				return "HomeAutoLogger";
			}

			@Override
			public void onConnect(final InterceptConnectMessage msg)
			{
				valueRegistry.setValueDebug("MQTT last connect", msg.getClientID() + " (" + msg.getProtocolName() + " v" + msg.getProtocolVersion() + ", keepalive " + msg.getKeepAlive() + "s)");
			}

			@Override
			public void onSubscribe(final InterceptSubscribeMessage msg)
			{
				valueRegistry.setValueDebug("MQTT last subscribe", msg.getClientID() + " -> " + msg.getTopicFilter());
			}

			@Override
			public void onPublish(final InterceptPublishMessage msg)
			{
				valueRegistry.setValueDebug("MQTT last publish", msg.getClientID() + " -> " + msg.getTopicName() + ": " + msg.getPayload().toString(StandardCharsets.UTF_8));
			}

			@Override
			public void onConnectionLost(final io.moquette.interception.messages.InterceptConnectionLostMessage msg)
			{
				valueRegistry.setValueDebug("MQTT last disconnect", msg.getClientID());
			}

			@Override
			public void onDisconnect(final io.moquette.interception.messages.InterceptDisconnectMessage msg)
			{
				valueRegistry.setValueDebug("MQTT last disconnect", msg.getClientID());
			}
		});

		System.out.println("MQTT broker started on port " + port);
	}

	@Override
	public void close()
	{
		try
		{
			server.stopServer();
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
	}
}
