package com.prashant.message_relay.controller;


import com.prashant.message_relay.model.DeliveryDocument;
import com.prashant.message_relay.model.DeliveryRecord;
import com.prashant.message_relay.repository.DeliveryRecordRepository;
import com.prashant.message_relay.repository.DeliverySearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class DeliverySearchController {

    private final DeliverySearchRepository searchRepository;
    private final DeliveryRecordRepository recordRepository;

    /**
     * Full-text search across delivery records via Elasticsearch.
     * GET /api/messages/search?recipient=+91****7890&status=SENT&channel=SMS&page=0&size=20
     */
    @GetMapping("/search")
    public ResponseEntity<Page<DeliveryDocument>> search(
            @RequestParam(required = false) String recipient,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String clientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<DeliveryDocument> results;

        if (clientId != null && status != null) {
            results = searchRepository.findByClientIdAndStatus(clientId, status, pageable);
        } else if (channel != null && status != null) {
            results = searchRepository.findByChannelAndStatus(channel, status, pageable);
        } else if (recipient != null) {
            results = searchRepository.findByRecipient(recipient, pageable);
        } else if (clientId != null) {
            results = searchRepository.findByClientId(clientId, pageable);
        } else if (status != null) {
            results = searchRepository.findByStatus(status, pageable);
        } else {
            results = searchRepository.findAll(pageable);
        }

        return ResponseEntity.ok(results);
    }

    /**
     * Get full delivery record with status history from MongoDB.
     * GET /api/messages/{eventId}
     */
    @GetMapping("/{eventId}")
    public ResponseEntity<DeliveryRecord> getByEventId(@PathVariable String eventId) {
        return recordRepository.findByEventId(eventId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * DLQ stats endpoint.
     * GET /api/messages/stats/dlq
     */
    @GetMapping("/stats/dlq")
    public ResponseEntity<Map<String, Object>> dlqStats() {
        long dlqCount = recordRepository.countByStatus(DeliveryRecord.DeliveryStatus.DEAD_LETTERED);
        long sentCount = recordRepository.countByStatus(DeliveryRecord.DeliveryStatus.SENT);
        long pendingCount = recordRepository.countByStatus(DeliveryRecord.DeliveryStatus.PENDING);
        long total = recordRepository.count();

        double successRate = total > 0 ? (double) sentCount / total * 100 : 0;

        return ResponseEntity.ok(Map.of(
                "total", total,
                "sent", sentCount,
                "deadLettered", dlqCount,
                "pending", pendingCount,
                "successRatePct", String.format("%.2f", successRate)
        ));
    }
}
