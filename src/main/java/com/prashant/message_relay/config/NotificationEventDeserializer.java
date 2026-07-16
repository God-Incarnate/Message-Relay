package com.prashant.message_relay.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.prashant.message_relay.model.NotificationEvent;
import org.apache.kafka.common.serialization.Deserializer;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Kafka deserializer for NotificationEvent implemented with Gson to avoid Jackson
 * classpath/visibility issues after removing Elasticsearch.
 */
public class NotificationEventDeserializer implements Deserializer<NotificationEvent> {

    private final Gson gson = new GsonBuilder()
            // Java 21 modules block reflective access to java.time internals unless a custom adapter is used.
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) ->
                            json == null || json.getAsString().isBlank()
                                    ? null
                                    : LocalDateTime.parse(json.getAsString()))
            .create();

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

