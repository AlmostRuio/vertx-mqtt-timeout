package io.vertx.test.mqtt;

import io.vertx.core.Vertx;

public class Main {

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(ClientMqttVerticle.class.getName());
    }

}
