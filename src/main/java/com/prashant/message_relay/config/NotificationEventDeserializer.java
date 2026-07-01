package com.prashant.message_relay.config;

import com.google.gson.Gson;
import com.prashant.message_relay.model.NotificationEvent;
import org.apache.kafka.common.serialization.Deserializer;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Kafka deserializer for NotificationEvent implemented with Gson to avoid Jackson
 * classpath/visibility issues after removing Elasticsearch.
 */
public class NotificationEventDeserializer implements Deserializer<NotificationEvent> {

    private final Gson gson = new Gson();

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        // no-op
    }

    @Override
    public NotificationEvent deserialize(String topic, byte[] data) {
        if (data == null) return null;
        String json = new String(data, StandardCharsets.UTF_8);
        return gson.fromJson(json, NotificationEvent.class);
    }

    @Override
    public void close() {
        // no-op
    }
}

