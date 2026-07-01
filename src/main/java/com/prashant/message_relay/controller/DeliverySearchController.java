package com.prashant.message_relay.controller;

import com.prashant.message_relay.model.DeliveryRecord;
import com.prashant.message_relay.repository.DeliveryRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class DeliverySearchController {

    private final DeliveryRecordRepository recordRepository;


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
