package com.prashant.message_relay.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
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
            // Handles LocalDateTime serialized as ISO string ("2026-07-16T21:27:25")
            // OR as a Jackson-style int array ([2026,7,16,21,27,25,940000000]).
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) -> {
                        if (json == null || json.isJsonNull()) return null;
                        if (json.isJsonPrimitive()) {
                            JsonPrimitive primitive = json.getAsJsonPrimitive();
                            String text = primitive.getAsString();
                            return text == null || text.isBlank() ? null : LocalDateTime.parse(text);
                        }
                        if (json.isJsonArray()) {
                            // Jackson default: [year, month, day, hour, minute, second, nanoOfSecond]
                            JsonArray arr = json.getAsJsonArray();
                            int year        = arr.get(0).getAsInt();
                            int month       = arr.get(1).getAsInt();
                            int day         = arr.get(2).getAsInt();
                            int hour        = arr.size() > 3 ? arr.get(3).getAsInt() : 0;
                            int minute      = arr.size() > 4 ? arr.get(4).getAsInt() : 0;
                            int second      = arr.size() > 5 ? arr.get(5).getAsInt() : 0;
                            int nano        = arr.size() > 6 ? arr.get(6).getAsInt() : 0;
                            return LocalDateTime.of(year, month, day, hour, minute, second, nano);
                        }
                        return null;
                    })
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

