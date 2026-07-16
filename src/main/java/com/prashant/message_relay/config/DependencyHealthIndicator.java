package com.prashant.message_relay.config;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Status;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class DependencyHealthIndicator implements HealthIndicator {

    private static final Status DEGRADED = new Status("DEGRADED");

    private final MongoTemplate mongoTemplate;
    private final String kafkaBootstrapServers;
    private final String elasticsearchUri;
    private final HttpClient httpClient;

    public DependencyHealthIndicator(
            MongoTemplate mongoTemplate,
            @Value("${spring.kafka.bootstrap-servers}") String kafkaBootstrapServers,
            @Value("${spring.elasticsearch.uris:http://localhost:9200}") String elasticsearchUri
    ) {
        this.mongoTemplate = mongoTemplate;
        this.kafkaBootstrapServers = kafkaBootstrapServers;
        this.elasticsearchUri = elasticsearchUri;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    @Override
    public Health health() {
        boolean mongoUp = isMongoUp();
        boolean kafkaUp = isKafkaUp();
        boolean elasticsearchUp = isElasticsearchUp();

        Status status;
        if (!mongoUp || !kafkaUp) {
            status = Status.DOWN;
        } else if (!elasticsearchUp) {
            status = DEGRADED;
        } else {
            status = Status.UP;
        }

        return Health.status(status)
                .withDetail("mongo", mongoUp ? "UP" : "DOWN")
                .withDetail("kafka", kafkaUp ? "UP" : "DOWN")
                .withDetail("elasticsearch", elasticsearchUp ? "UP" : "DOWN")
                .build();
    }

    protected boolean isMongoUp() {
        try {
            mongoTemplate.executeCommand("{ ping: 1 }");
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    protected boolean isKafkaUp() {
        Map<String, Object> config = new HashMap<>();
        config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
        config.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 3000);
        config.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 3000);

        try (AdminClient adminClient = AdminClient.create(config)) {
            adminClient.describeCluster().clusterId().get(3, TimeUnit.SECONDS);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    protected boolean isElasticsearchUp() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(elasticsearchUri))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            int statusCode = response.statusCode();
            return statusCode >= 200 && statusCode < 500;
        } catch (Exception ignored) {
            return false;
        }
    }
}



