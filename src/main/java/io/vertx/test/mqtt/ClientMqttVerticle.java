package io.vertx.test.mqtt;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;
import io.vertx.mqtt.messages.MqttConnAckMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientMqttVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientMqttVerticle.class);

    private static final int BROKER_PORT = 1883;
    private static final String BROKER_HOST = "localhost";
    private static final String MQTT_TOPIC = "test/topic";

    private MqttClient client;

    @Override
    public void start() {

        MqttClientOptions options = new MqttClientOptions();
        options.setKeepAliveInterval(10);
        client = MqttClient.create(vertx, options);

        Handler<AsyncResult<MqttConnAckMessage>> connectionHandler = ar -> {
            if (ar.succeeded()) {
                client.subscribe(MQTT_TOPIC, MqttQoS.AT_MOST_ONCE.value());
                LOGGER.info("connected to {}:{}, subscribed topic \"{}\"", BROKER_HOST, BROKER_PORT, MQTT_TOPIC);
            } else {
                LOGGER.error("failed to connect to MQTT broker" + ar.cause());
            }
        };

        client.connect(BROKER_PORT, BROKER_HOST, connectionHandler);

        client.closeHandler(handler -> {
            LOGGER.warn("connection to broker closed, reconnect");
            client.connect(BROKER_PORT, BROKER_HOST, connectionHandler);
        });

    }

}
