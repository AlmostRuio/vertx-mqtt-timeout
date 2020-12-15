package io.vertx.test.mqtt;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.mqtt.MqttServer;
import io.vertx.mqtt.MqttTopicSubscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ServerMqttVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerMqttVerticle.class);

    @Override
    public void start() {

        MqttServer mqttServer = MqttServer.create(vertx);

        mqttServer
                .endpointHandler(endpoint -> {

                    // shows main connect info
                    LOGGER.info("MQTT client [" + endpoint.clientIdentifier() + "] request to connect, clean session = " + endpoint.isCleanSession());

                    if (endpoint.auth() != null) {
                        LOGGER.info("[username = " + endpoint.auth().getUsername() + ", password = " + endpoint.auth().getPassword() + "]");
                    }
                    if (endpoint.will() != null) {
                        LOGGER.info("[will flag = " + endpoint.will().isWillFlag() + " topic = " + endpoint.will().getWillTopic() + " msg = " + endpoint.will().getWillMessage() +
                                " QoS = " + endpoint.will().getWillQos() + " isRetain = " + endpoint.will().isWillRetain() + "]");
                    }

                    LOGGER.info("[keep alive timeout = " + endpoint.keepAliveTimeSeconds() + "]");

                    // accept connection from the remote client
                    endpoint.accept(false);

                    // handling requests for subscriptions
                    endpoint.subscribeHandler(subscribe -> {

                        List<MqttQoS> grantedQosLevels = new ArrayList<>();
                        for (MqttTopicSubscription s : subscribe.topicSubscriptions()) {
                            LOGGER.info("Subscription for " + s.topicName() + " with QoS " + s.qualityOfService());
                            grantedQosLevels.add(s.qualityOfService());
                        }
                        // ack the subscriptions request
                        endpoint.subscribeAcknowledge(subscribe.messageId(), grantedQosLevels);

                        // just as example, publish a message on the first topic with requested QoS
                        endpoint.publish(subscribe.topicSubscriptions().get(0).topicName(),
                                Buffer.buffer("Hello from the Vert.x MQTT server"),
                                subscribe.topicSubscriptions().get(0).qualityOfService(),
                                false,
                                false);

                        // specifing handlers for handling QoS 1 and 2
                        endpoint.publishAcknowledgeHandler(messageId -> {

                            LOGGER.info("Received ack for message = " + messageId);

                        }).publishReceivedHandler(messageId -> {

                            endpoint.publishRelease(messageId);

                        }).publishCompletionHandler(messageId -> {

                            LOGGER.info("Received ack for message = " + messageId);
                        });
                    });

                    // handling requests for unsubscriptions
                    endpoint.unsubscribeHandler(unsubscribe -> {

                        for (String t : unsubscribe.topics()) {
                            LOGGER.info("Unsubscription for " + t);
                        }
                        // ack the subscriptions request
                        endpoint.unsubscribeAcknowledge(unsubscribe.messageId());
                    });

                    // handling ping from client
                    endpoint.pingHandler(v -> {

                        LOGGER.info("Ping received from client");
                    });

                    // handling disconnect message
                    endpoint.disconnectHandler(v -> {

                        LOGGER.info("Received disconnect from client");
                    });

                    // handling closing connection
                    endpoint.closeHandler(v -> {

                        LOGGER.info("Connection closed");
                    });

                    // handling incoming published messages
                    endpoint.publishHandler(message -> {

                        LOGGER.info("Just received message on [" + message.topicName() + "] payload [" + message.payload() + "] with QoS [" + message.qosLevel() + "]");

                        if (message.qosLevel() == MqttQoS.AT_LEAST_ONCE) {
                            endpoint.publishAcknowledge(message.messageId());
                        } else if (message.qosLevel() == MqttQoS.EXACTLY_ONCE) {
                            endpoint.publishReceived(message.messageId());
                        }

                    }).publishReleaseHandler(messageId -> {
                        endpoint.publishComplete(messageId);
                    });
                })
                .listen(1883, "0.0.0.0", ar -> {

                    if (ar.succeeded()) {
                        LOGGER.info("MQTT server is listening on port " + mqttServer.actualPort());
                    } else {
                        LOGGER.error("Error on starting the server" + ar.cause().getMessage());
                    }
                });
    }
}
