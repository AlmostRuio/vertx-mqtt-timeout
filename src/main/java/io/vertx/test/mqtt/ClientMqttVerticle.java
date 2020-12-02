package io.vertx.test.mqtt;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;
import io.vertx.mqtt.messages.MqttConnAckMessage;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class ClientMqttVerticle extends AbstractVerticle {

    private static final int BROKER_PORT = 1883;
    private static final String BROKER_HOST = "localhost";

    private MqttClient client;

    private final Random random = new Random();

    @Override
    public void start() {

        MqttClientOptions options = new MqttClientOptions();
        options.setAutoKeepAlive(true); // useless, default should be true
        client = MqttClient.create(vertx, options);

        Handler<AsyncResult<MqttConnAckMessage>> connectionHandler = ar -> {
            if (ar.succeeded()) {
                System.out.println("connected to " + BROKER_HOST + ":" + BROKER_PORT);
            } else {
                System.out.println("failed to connect to MQTT broker" + ar.cause());
            }
        };

        client.connect(BROKER_PORT, BROKER_HOST, connectionHandler);

        client.closeHandler(handler -> {
            System.out.println("connection to broker closed, reconnect");

            // the handling of the ping-req for connection "keepAlive" does not work correctly, if there
            // is no publishing (only consuming messages disables the ping's in the implementation of
            // io.vertx.mqtt.impl.MqttClientImpl#initChannel since version 3.9.4 due to the changes described
            // in this issue: https://github.com/vert-x3/vertx-mqtt/issues/123)
            // workaround for now is to reconnect if the MQTT server closes the connection
            client.connect(BROKER_PORT, BROKER_HOST, connectionHandler);
        });

        scheduleNextUpdate();

        System.out.println("ClientMqttVerticle ready!");
    }

    private void scheduleNextUpdate() {
        vertx.setTimer(2000, this::update);
    }

    private void update(long id) {
        String outsideId = "outside";
        JsonObject outside = build(outsideId);
        String topicOut = "tele/" + outsideId + "/SENSOR";

        String insideId = "inside";
        JsonObject inside = build(insideId);
        String topicIn = "tele/" + insideId + "/SENSOR";

        if (client.isConnected()) {
            client.publish(
                    topicOut,
                    outside.toBuffer(),
                    MqttQoS.AT_MOST_ONCE,
                    false,
                    false);

            client.publish(
                    topicIn,
                    inside.toBuffer(),
                    MqttQoS.AT_MOST_ONCE,
                    false,
                    false);
        } else {
            System.out.println("Mqtt client is not connected, publish skipped. timerId " + id);
        }

        scheduleNextUpdate();
    }

    private JsonObject build(String sensorId) {
        double temperature = 18 + random.nextDouble() + ThreadLocalRandom.current().nextInt(-2, 3);
        double humidity = 52.5 + random.nextDouble() + ThreadLocalRandom.current().nextInt(-10, 11);
        double dewPoint = Math.round((temperature - ((100 - humidity) / 5)) * 100.0) / 100.0;

        JsonObject SI7021 = new JsonObject();
        SI7021.put("Temperature", temperature);
        SI7021.put("Humidity", humidity);
        SI7021.put("DewPoint", dewPoint);

        JsonObject payload = new JsonObject();
        long timestamp = System.currentTimeMillis();
        payload.put("Time", timestamp);
        payload.put("SI7021", SI7021);
        payload.put("TempUnit", "C");
        payload.put("id", sensorId);

        System.out.println("Publishing data with timestamp " + timestamp);

        return payload;
    }
}
